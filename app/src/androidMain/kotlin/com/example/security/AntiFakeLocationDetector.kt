package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

object AntiFakeLocationDetector {

    // Cache package scan results so we never thrash Binder/PackageManager in 1-second loops
    private var lastPackageCheckTime: Long = 0L
    private var cachedMockPackageDetected: Boolean = false
    private var cachedPackageAnomaly: String? = null

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
        lastPackageCheckTime = 0L
        cachedMockPackageDetected = false
        cachedPackageAnomaly = null
    }

    /**
     * Memeriksa apakah fitur 'Otomatis Tanggal & Waktu' (Network Time Provider / NTP) aktif di pengaturan Android.
     * Mengembalikan true jika otomatis jaringan aktif (ASLI), atau false jika diatur ke manual (PALSU).
     */
    fun isAutomaticTimeEnabled(context: Context): Boolean {
        return try {
            val autoTime = Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, -1)
            if (autoTime != -1) {
                autoTime == 1
            } else {
                @Suppress("DEPRECATION")
                val systemAutoTime = Settings.System.getInt(context.contentResolver, Settings.System.AUTO_TIME, -1)
                if (systemAutoTime != -1) systemAutoTime == 1 else true
            }
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                Settings.System.getInt(context.contentResolver, Settings.System.AUTO_TIME, 1) == 1
            } catch (_: Exception) {
                true
            }
        }
    }

    fun analyzeLocation(context: Context, location: Location?): SecurityCheckResult {
        val anomalies = mutableListOf<String>()
        var isMock = false
        var isTimeFake = false

        val systemTime = System.currentTimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()

        // -------------------------------------------------------------
        // 1. CEK WAKTU: Validasi Otomatis Jaringan vs Manual
        // -------------------------------------------------------------
        // Aturan: Jika waktu diubah ke manual (bukan otomatis jaringan),
        // sistem langsung menampilkan FAKE / PALSU seketika.
        // Jika diatur kembali ke otomatis (mengikuti waktu asli jaringan),
        // sistem otomatis pulih menjadi ASLI & VALID.
        val isAutoTimeEnabled = isAutomaticTimeEnabled(context)

        if (!isAutoTimeEnabled) {
            isTimeFake = true
            anomalies.add("Waktu manual aktif: Waktu perangkat disetel manual, bukan waktu otomatis jaringan (Wajib aktifkan 'Waktu Otomatis' di Pengaturan Android)")
        } else {
            isTimeFake = false
        }

        // -------------------------------------------------------------
        // 2. CEK FAKE GPS: API Resmi Android isMock / isFromMockProvider
        // -------------------------------------------------------------
        if (location != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (location.isMock) {
                    isMock = true
                    anomalies.add("Mock location terdeteksi via Android API (isMock = true)")
                }
            } else {
                @Suppress("DEPRECATION")
                if (location.isFromMockProvider) {
                    isMock = true
                    anomalies.add("Mock provider terdeteksi (isFromMockProvider = true)")
                }
            }

            val extras = location.extras
            if (extras != null && extras.getBoolean("mockLocation", false)) {
                isMock = true
                anomalies.add("Flag mockLocation aktif pada metadata lokasi")
            }
        }

        // -------------------------------------------------------------
        // 3. CEK FAKE GPS: Deteksi Aplikasi Mock / Fake GPS Terkenal (Cached)
        // -------------------------------------------------------------
        val (packageMockDetected, packageAnomaly) = checkFakeGpsPackages(context)
        if (packageMockDetected) {
            isMock = true
            packageAnomaly?.let { anomalies.add(it) }
        }

        // -------------------------------------------------------------
        // 4. Tanda Tangan Kriptografis Integritas Data
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

    private fun checkFakeGpsPackages(context: Context): Pair<Boolean, String?> {
        val now = SystemClock.elapsedRealtime()
        // Cache paket selama 60 detik untuk mencegah Binder exhaustion / GC pause
        if (now - lastPackageCheckTime < 60_000L && lastPackageCheckTime > 0L) {
            return Pair(cachedMockPackageDetected, cachedPackageAnomaly)
        }
        lastPackageCheckTime = now
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
                    cachedMockPackageDetected = true
                    cachedPackageAnomaly = "Aplikasi Fake GPS terpasang (${pkg.packageName})"
                    return Pair(true, cachedPackageAnomaly)
                }
            }
            cachedMockPackageDetected = false
            cachedPackageAnomaly = null
        } catch (_: Exception) {}
        return Pair(cachedMockPackageDetected, cachedPackageAnomaly)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
