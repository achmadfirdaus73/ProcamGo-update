package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs

object AntiFakeLocationDetector {

    private var baselineBootEpoch: Long? = null
    private var lastRecordedSystemTime: Long = 0L

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
        baselineBootEpoch = null
        lastRecordedSystemTime = 0L
    }

    fun analyzeLocation(context: Context, location: Location?): SecurityCheckResult {
        val anomalies = mutableListOf<String>()
        var isMock = false
        var isTimeFake = false

        val systemTime = System.currentTimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()

        // -------------------------------------------------------------
        // 1. CEK WAKTU: Validasi Integritas Jam Sistem
        // -------------------------------------------------------------
        // Periksa apakah "Otomatis Tanggal & Waktu" aktif di Android Settings.
        // Jika aktif, waktu disinkronkan langsung dari jaringan operator seluler / NTP server resmi,
        // sehingga dijamin ASLI dan tidak dapat dipalsukan secara manual.
        val isAutoTimeEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) != 0
        } catch (_: Exception) {
            true
        }

        if (!isAutoTimeEnabled) {
            // Jika pengguna mematikan auto-time, periksa apakah jam sengaja digeser / dimanipulasi
            // saat aplikasi aktif berjalan (pergeseran drastis > 10 menit)
            val currentBootEpoch = systemTime - elapsedRealtime
            if (baselineBootEpoch == null) {
                baselineBootEpoch = currentBootEpoch
            } else {
                val drift = abs(currentBootEpoch - baselineBootEpoch!!)
                if (drift > 10 * 60 * 1000L) { // > 10 menit
                    isTimeFake = true
                    anomalies.add("Jam perangkat digeser manual secara drastis (${drift / 60000} menit)")
                }
            }

            // Periksa jika jam sengaja dimundurkan > 10 menit
            if (lastRecordedSystemTime > 0L && systemTime < (lastRecordedSystemTime - 10 * 60 * 1000L)) {
                isTimeFake = true
                anomalies.add("Jam sistem dimundurkan secara manual")
            }
        } else {
            // Ketika auto-time aktif, update baseline secara mulus tanpa memicu false positive
            baselineBootEpoch = systemTime - elapsedRealtime
        }
        lastRecordedSystemTime = systemTime

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
        // 3. CEK FAKE GPS: Deteksi Aplikasi Mock / Fake GPS Terkenal
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
                    anomalies.add("Aplikasi Fake GPS terpasang (${pkg.packageName})")
                    break
                }
            }
        } catch (_: Exception) {}

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

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
