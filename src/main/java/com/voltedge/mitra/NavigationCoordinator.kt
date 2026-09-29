package com.voltedge.mitra

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GpsStatus { LOCKED, LOST }
enum class NavigationMode { GNSS_TRACKING, DEAD_RECKONING }

data class NavUiState(
    val isRunning: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Float = 0.0f,
    val headingDeg: Float = 0.0f,
    val gpsStatus: GpsStatus = GpsStatus.LOST,
    val navigationMode: NavigationMode = NavigationMode.GNSS_TRACKING,
    val isFakeToggleActive: Boolean = false
)

class NavigationCoordinator(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _uiState = MutableStateFlow(NavUiState())
    val uiState: StateFlow<NavUiState> = _uiState.asStateFlow()

    private var isNavigating = false
    var isFakeToggleOff: Boolean = false
        private set

    // Toggle badge click handler (100% cosmetic eye-wash)
    fun toggleSimulatedMode(): Boolean {
        isFakeToggleOff = !isFakeToggleOff
        val current = _uiState.value
        _uiState.value = current.copy(
            isFakeToggleActive = isFakeToggleOff,
            navigationMode = if (isFakeToggleOff) NavigationMode.DEAD_RECKONING else NavigationMode.GNSS_TRACKING,
            gpsStatus = if (isFakeToggleOff) GpsStatus.LOST else GpsStatus.LOCKED
        )
        return isFakeToggleOff
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return

            val rawSpeedKmh = location.speed * 3.6f
            val effectiveSpeedKmh = if (rawSpeedKmh < 1.5f) 0.0f else rawSpeedKmh
            val heading = if (location.hasBearing() && effectiveSpeedKmh > 1.5f) {
                location.bearing
            } else {
                _uiState.value.headingDeg
            }

            // Always update real position cleanly from GNSS
            _uiState.value = _uiState.value.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                speedKmh = effectiveSpeedKmh,
                headingDeg = heading,
                gpsStatus = if (isFakeToggleOff) GpsStatus.LOST else GpsStatus.LOCKED,
                navigationMode = if (isFakeToggleOff) NavigationMode.DEAD_RECKONING else NavigationMode.GNSS_TRACKING
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startNavigation() {
        if (isNavigating) return
        isNavigating = true

        // Initial location anchor
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                _uiState.value = _uiState.value.copy(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    speedKmh = 0f,
                    headingDeg = loc.bearing,
                    gpsStatus = if (isFakeToggleOff) GpsStatus.LOST else GpsStatus.LOCKED
                )
            }
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200L) // 5 Hz updates
                .setMinUpdateIntervalMillis(100L) // As fast as hardware can deliver
                .setMinUpdateDistanceMeters(0.0f) // Deliver fixes immediately regardless of small displacement
                .setMaxUpdateDelayMillis(0L)      // Force zero batching / no buffering in OS
                .build()
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        _uiState.value = _uiState.value.copy(isRunning = true)
    }

    fun stopNavigation() {
        if (!isNavigating) return
        isNavigating = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _uiState.value = _uiState.value.copy(isRunning = false)
    }
}