package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.security.MessageDigest
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object AntiFakeLocationDetector {

    private var persistentMockDetected: Boolean = false
    private var persistentTimeTampered: Boolean = false
    private var baselineBootEpoch: Long? = null

    private val knownFakeGpsPackages = setOf(
        "com.lexa.fakegps",
        "com.rosteam.gpsemulator",
        "com.incorporateapps.fakegps",
        "com.incorporateapps.fakegps_route",
        "com.fakegps.mock",
        "com.marlove.fackgps",
        "ru.gavrikov.mocklocations",
        "com.gsmartstudio.fakegps",
        "com.blogspot.newprocesstechnologies.fakegps",
        "com.location.mocklocation",
        "com.hope.fakegps",
        "com.mocker.mockgps"
    )

    fun resetDetection() {
        persistentMockDetected = false
        persistentTimeTampered = false
        baselineBootEpoch = null
    }

    fun analyzeLocation(context: Context, location: Location?): SecurityCheckResult {
        val anomalies = mutableListOf<String>()
        var isMock = persistentMockDetected
        var isTimeFake = persistentTimeTampered

        val systemTime = System.currentTimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()

        // -------------------------------------------------------------
        // 1. CEK WAKTU: Perubahan Jam Manual Real-Time (Boot Epoch Drift)
        // -------------------------------------------------------------
        val currentBootEpoch = systemTime - elapsedRealtime
        if (baselineBootEpoch == null) {
            baselineBootEpoch = currentBootEpoch
        } else {
            val drift = abs(currentBootEpoch - baselineBootEpoch!!)
            // Jika jam perangkat digeser secara manual saat aplikasi aktif (> 4 detik)
            if (drift > 4000L) {
                isTimeFake = true
                persistentTimeTampered = true
                anomalies.add("Jam perangkat diubah manual saat aplikasi aktif (${drift / 1000}d pergeseran)")
            }
        }

        // -------------------------------------------------------------
        // 2. CEK WAKTU: Pengaturan Waktu Otomatis Sistem
        // -------------------------------------------------------------
        var autoTimeOff = false
        try {
            val autoTime = Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1)
            if (autoTime == 0) autoTimeOff = true
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                val autoTime = Settings.System.getInt(context.contentResolver, Settings.System.AUTO_TIME, 1)
                if (autoTime == 0) autoTimeOff = true
            } catch (_: Exception) {}
        }

        var autoTimeZoneOff = false
        try {
            val autoTz = Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, 1)
            if (autoTz == 0) autoTimeZoneOff = true
        } catch (_: Exception) {}

        if (autoTimeOff || autoTimeZoneOff) {
            isTimeFake = true
            persistentTimeTampered = true
            val detail = when {
                autoTimeOff && autoTimeZoneOff -> "Waktu & Zona Waktu otomatis dinonaktifkan (Manual Clock)"
                autoTimeOff -> "Waktu otomatis dinonaktifkan (Jam Manual)"
                else -> "Zona waktu otomatis dinonaktifkan (Manual Time Zone)"
            }
            if (!anomalies.contains(detail)) {
                anomalies.add(detail)
            }
        }

        // -------------------------------------------------------------
        // 3. CEK WAKTU: Sinkronisasi Jam Sistem vs Satelit GPS / Network
        // -------------------------------------------------------------
        if (location != null && location.time > 0) {
            val timeDiff = abs(systemTime - location.time)
            // Selisih waktu sistem dengan waktu GPS satelit > 30 detik
            if (timeDiff > 30_000L) {
                isTimeFake = true
                persistentTimeTampered = true
                anomalies.add("Waktu sistem berbeda dengan waktu satelit GPS (${timeDiff / 1000}d selisih)")
            }

            // Cek elapsed realtime nanos drift (hardware GPS clock vs system clock)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && location.elapsedRealtimeNanos > 0) {
                val locElapsedMs = location.elapsedRealtimeNanos / 1_000_000L
                val ageMs = elapsedRealtime - locElapsedMs
                val expectedFixTime = systemTime - ageMs
                val divergence = abs(location.time - expectedFixTime)
                if (divergence > 30_000L) {
                    isTimeFake = true
                    persistentTimeTampered = true
                    anomalies.add("Ketidaksesuaian clock hardware GPS dengan sistem (${divergence / 1000}d)")
                }
            }
        }

        // -------------------------------------------------------------
        // 4. CEK FAKE GPS: API Android Location isMock
        // -------------------------------------------------------------
        if (location != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (location.isMock) {
                    isMock = true
                    persistentMockDetected = true
                    anomalies.add("Mock location terdeteksi via Android API (isMock = true)")
                }
            } else {
                @Suppress("DEPRECATION")
                if (location.isFromMockProvider) {
                    isMock = true
                    persistentMockDetected = true
                    anomalies.add("Mock provider terdeteksi (isFromMockProvider = true)")
                }
            }

            val extras = location.extras
            if (extras != null && extras.getBoolean("mockLocation", false)) {
                isMock = true
                persistentMockDetected = true
                anomalies.add("Flag mockLocation aktif pada metadata lokasi")
            }
        }

        // -------------------------------------------------------------
        // 5. CEK FAKE GPS: Pengaturan Sistem Allow Mock Location
        // -------------------------------------------------------------
        try {
            @Suppress("DEPRECATION")
            val allowMock = Settings.Secure.getString(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION)
            if (allowMock == "1") {
                isMock = true
                persistentMockDetected = true
                anomalies.add("Pengaturan Mock Location aktif di Opsi Pengembang")
            }
        } catch (_: Exception) {}

        // -------------------------------------------------------------
        // 6. CEK FAKE GPS: Mock Provider pada System LocationManager
        // -------------------------------------------------------------
        try {
            val sysLm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            if (sysLm != null) {
                for (p in sysLm.allProviders) {
                    if (p.contains("mock", ignoreCase = true) || p.contains("test", ignoreCase = true)) {
                        isMock = true
                        persistentMockDetected = true
                        anomalies.add("Mock Provider aktif di sistem ($p)")
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        // -------------------------------------------------------------
        // 7. CEK FAKE GPS: Deteksi Aplikasi Mock / Fake GPS Terpasang
        // -------------------------------------------------------------
        try {
            val pm = context.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            for (pkg in packages) {
                if (pkg.packageName == context.packageName) continue

                if (knownFakeGpsPackages.contains(pkg.packageName)) {
                    isMock = true
                    persistentMockDetected = true
                    anomalies.add("Aplikasi Fake GPS terpasang (${pkg.packageName})")
                    break
                }

                val perms = pkg.requestedPermissions
                if (perms != null && perms.contains("android.permission.ACCESS_MOCK_LOCATION")) {
                    val isSystemApp = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                    if (!isSystemApp) {
                        isMock = true
                        persistentMockDetected = true
                        anomalies.add("Aplikasi tiruan GPS terpasang (${pkg.packageName})")
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        // -------------------------------------------------------------
        // 8. Tanda Tangan Kriptografis Integritas Data
        // -------------------------------------------------------------
        val lat = location?.latitude ?: 0.0
        val lon = location?.longitude ?: 0.0

        val rawSignature = "$lat|$lon|$systemTime|$elapsedRealtime|$isMock|$isTimeFake"
        val hash = sha256(rawSignature)
        val token = hash.take(8).uppercase(Locale.ROOT)

        return SecurityCheckResult(
            isAuthentic = !isMock && !isTimeFake,
            isMockDetected = isMock,
            isTimeManipulated = isTimeFake,
            integrityHash = hash,
            signatureToken = token,
            detectedAnomalies = anomalies
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
