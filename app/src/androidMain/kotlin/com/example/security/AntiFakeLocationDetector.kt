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

    private var lastRecordedSystemTime: Long = 0L
    private var lastRecordedElapsedRealtime: Long = 0L

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
        lastRecordedSystemTime = 0L
        lastRecordedElapsedRealtime = 0L
        lastPackageCheckTime = 0L
        cachedMockPackageDetected = false
        cachedPackageAnomaly = null
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
        // Periksa apakah "Otomatis Tanggal & Waktu" (NTP/Network Time) aktif.
        val isAutoTimeEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) != 0
        } catch (_: Exception) {
            true
        }

        if (isAutoTimeEnabled) {
            // Jika waktu otomatis aktif, waktu dikelola langsung oleh sistem seluler / Google NTP server.
            // Dijamin ASLI & AUTHENTIC tanpa false positive meskipun aplikasi berjalan seharian.
            isTimeFake = false
            lastRecordedSystemTime = systemTime
            lastRecordedElapsedRealtime = elapsedRealtime
        } else {
            // Ketika auto-time mati, deteksi hanya jika ada perubahan jam manual yang disengaja
            // saat aplikasi aktif digunakan (pergeseran drastis > 15 menit dalam selang waktu singkat)
            if (lastRecordedSystemTime > 0L && lastRecordedElapsedRealtime > 0L) {
                val realElapsedDelta = elapsedRealtime - lastRecordedElapsedRealtime
                val systemTimeDelta = systemTime - lastRecordedSystemTime

                // Hanya validasi jika jeda waktu antar-tick masih dalam status aktif (< 5 menit).
                // Jika perangkat tidur / sleep berjam-jam saat "jalan seharian", jeda diabaikan dengan mulus.
                if (realElapsedDelta in 0..300_000L) {
                    val stepDiscrepancy = abs(systemTimeDelta - realElapsedDelta)
                    if (stepDiscrepancy > 15 * 60 * 1000L) {
                        isTimeFake = true
                        anomalies.add("Jam perangkat diubah manual saat aplikasi aktif (${stepDiscrepancy / 60000} menit)")
                    }
                }
            }
            lastRecordedSystemTime = systemTime
            lastRecordedElapsedRealtime = elapsedRealtime
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
