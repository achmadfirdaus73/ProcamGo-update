package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.Looper
import com.example.security.AntiFakeLocationDetector
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationManager(private val context: Context, private val scope: CoroutineScope) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val sysLocationManager: android.location.LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager

    var lastRawLocation: Location? = null
        private set
    private var lastRealLat: Double = -6.175392
    private var lastRealLon: Double = 106.827153
    private var lastRealAddr: String = "Jl. Raya No. 1, Jakarta, Indonesia"

    private var tickerJob: Job? = null

    companion object {
        fun formatCurrentTime(): String {
            return try {
                SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date())
            } catch (_: Exception) {
                ""
            }
        }

        fun formatCurrentDate(): String {
            return try {
                SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
            } catch (_: Exception) {
                ""
            }
        }
    }

    // Initialize with REAL CURRENT SYSTEM TIME & DATE immediately!
    private val initialSecurity = AntiFakeLocationDetector.analyzeLocation(context, null)

    private val _locationData = MutableStateFlow(
        LiveLocationData(
            latitude = lastRealLat,
            longitude = lastRealLon,
            realLatitude = lastRealLat,
            realLongitude = lastRealLon,
            address = lastRealAddr,
            realAddress = lastRealAddr,
            formattedTime = formatCurrentTime(),
            formattedDate = formatCurrentDate(),
            securityResult = initialSecurity
        )
    )
    val locationData: StateFlow<LiveLocationData> = _locationData.asStateFlow()

    private val fusedLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { updateLocation(it) }
        }
    }

    private val nativeLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        ensureTickerRunning()
    }

    /**
     * Memastikan coroutine ticker waktu selalu aktif berdetak setiap 1 detik.
     * Menggunakan try-catch internal agar loop tidak pernah mati akibat error sistem/memori.
     */
    fun ensureTickerRunning() {
        if (tickerJob?.isActive == true) return

        tickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val curTime = formatCurrentTime()
                    val curDate = formatCurrentDate()
                    val sec = AntiFakeLocationDetector.analyzeLocation(context, lastRawLocation)

                    withContext(Dispatchers.Main) {
                        val current = _locationData.value
                        _locationData.value = current.copy(
                            formattedTime = if (curTime.isNotEmpty()) curTime else current.formattedTime,
                            formattedDate = if (curDate.isNotEmpty()) curDate else current.formattedDate,
                            securityResult = sec
                        )
                    }
                } catch (_: Exception) {
                    // Mencegah loop terminasi saat perangkat sleep atau memori rendah
                }
                delay(1000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        ensureTickerRunning()

        // Perbarui waktu & status keamanan seketika tanpa menunggu siklus delay 1 detik
        val nowTime = formatCurrentTime()
        val nowDate = formatCurrentDate()
        val sec = AntiFakeLocationDetector.analyzeLocation(context, lastRawLocation)
        val cur = _locationData.value
        _locationData.value = cur.copy(
            formattedTime = if (nowTime.isNotEmpty()) nowTime else cur.formattedTime,
            formattedDate = if (nowDate.isNotEmpty()) nowDate else cur.formattedDate,
            securityResult = sec
        )

        // 1. Google Play Services Fused Location
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build()

            fusedClient.requestLocationUpdates(request, fusedLocationCallback, Looper.getMainLooper())
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { updateLocation(it) }
            }
        } catch (_: Exception) {}

        // 2. Dual Provider Fallback: Native Android LocationManager (GPS & Network)
        try {
            sysLocationManager?.let { lm ->
                if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        1000L,
                        1f,
                        nativeLocationListener,
                        Looper.getMainLooper()
                    )
                }
                if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        1000L,
                        1f,
                        nativeLocationListener,
                        Looper.getMainLooper()
                    )
                }
                val lastKnownGps = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                val lastKnownNet = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                (lastKnownGps ?: lastKnownNet)?.let { updateLocation(it) }
            }
        } catch (_: Exception) {}
    }

    fun stopLocationUpdates() {
        try {
            fusedClient.removeLocationUpdates(fusedLocationCallback)
        } catch (_: Exception) {}
        try {
            sysLocationManager?.removeUpdates(nativeLocationListener)
        } catch (_: Exception) {}
    }

    private fun updateLocation(loc: Location) {
        lastRawLocation = loc
        val curTime = formatCurrentTime()
        val curDate = formatCurrentDate()
        val security = AntiFakeLocationDetector.analyzeLocation(context, loc)

        scope.launch {
            val addressText = getAddressFromCoords(loc.latitude, loc.longitude)

            if (!security.isMockDetected) {
                lastRealLat = loc.latitude
                lastRealLon = loc.longitude
                lastRealAddr = addressText
            }

            withContext(Dispatchers.Main) {
                val current = _locationData.value
                _locationData.value = LiveLocationData(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    realLatitude = if (security.isMockDetected) lastRealLat else loc.latitude,
                    realLongitude = if (security.isMockDetected) lastRealLon else loc.longitude,
                    altitude = loc.altitude,
                    accuracy = loc.accuracy,
                    address = addressText,
                    realAddress = if (security.isMockDetected) lastRealAddr else addressText,
                    formattedTime = if (curTime.isNotEmpty()) curTime else current.formattedTime,
                    formattedDate = if (curDate.isNotEmpty()) curDate else current.formattedDate,
                    securityResult = security
                )
            }
        }
    }

    private suspend fun getAddressFromCoords(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lon, 1)
            if (!list.isNullOrEmpty()) {
                val addr = list[0]
                val line = addr.getAddressLine(0)
                if (!line.isNullOrEmpty()) {
                    return@withContext line
                }
                val sb = StringBuilder()
                addr.subLocality?.let { sb.append(it).append(", ") }
                addr.locality?.let { sb.append(it).append(", ") }
                addr.subAdminArea?.let { sb.append(it).append(", ") }
                addr.adminArea?.let { sb.append(it).append(", ") }
                addr.countryName?.let { sb.append(it) }
                return@withContext sb.toString().trim().trimEnd(',')
            }
        } catch (_: Exception) {}
        "Gg. Kuncir No.27, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424"
    }
}
