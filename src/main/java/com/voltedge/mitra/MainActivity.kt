package com.voltedge.mitra

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvSpeed: TextView
    private lateinit var tvHeading: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var tvModeBadge: TextView
    private lateinit var btnToggleNav: Button

    private var navigationService: NavigationService? = null
    private var isBound = false
    private var isNavigating = false

    private var trajectoryPolyline: Polyline? = null
    private var vehicleMarker: Marker? = null
    private var lastPlottedPoint: GeoPoint? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as NavigationService.LocalBinder
            navigationService = localBinder.getService()
            isBound = true
            navigationService?.coordinator?.startNavigation()
            observeNavigationState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            navigationService = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!fineLocation) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvHeading = findViewById(R.id.tvHeading)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        tvModeBadge = findViewById(R.id.tvModeBadge)
        btnToggleNav = findViewById(R.id.btnToggleNav)

        setupMap()
        checkPermissions()

        btnToggleNav.setOnClickListener {
            toggleNavigation()
        }

        // Tap mode badge for cosmetic simulated toggle
        tvModeBadge.setOnClickListener {
            navigationService?.coordinator?.let { coord ->
                val isSimulatedOff = coord.toggleSimulatedMode()
                if (isSimulatedOff) {
                    Toast.makeText(this, "GNSS Cut Off — Running Pure AI DR + NHC", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "GNSS Resumed — Satellite Syncing", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Start navigation first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)

        val rotationGestureOverlay = RotationGestureOverlay(mapView).apply {
            isEnabled = true
        }
        mapView.overlays.add(rotationGestureOverlay)

        val compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), mapView).apply {
            enableCompass()
        }
        mapView.overlays.add(compassOverlay)

        trajectoryPolyline = Polyline(mapView).apply {
            outlinePaint.color = Color.parseColor("#00E676")
            outlinePaint.strokeWidth = 8f
        }
        mapView.overlays.add(trajectoryPolyline)

        vehicleMarker = Marker(mapView).apply {
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_navigation_arrow)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            infoWindow = null
        }
        mapView.overlays.add(vehicleMarker)
    }

    private fun observeNavigationState() {
        val coordinator = navigationService?.coordinator ?: return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                coordinator.uiState.collect { state ->
                    tvSpeed.text = "%.1f".format(state.speedKmh)
                    tvHeading.text = "%.0f°".format(state.headingDeg)

                    // Cosmetic status text
                    if (state.gpsStatus == GpsStatus.LOCKED) {
                        tvGpsStatus.text = "GNSS LOCKED"
                        tvGpsStatus.setTextColor(Color.parseColor("#00E676"))
                    } else {
                        tvGpsStatus.text = "GNSS LOST"
                        tvGpsStatus.setTextColor(Color.parseColor("#FF5252"))
                    }

                    // Cosmetic badge text
                    if (state.navigationMode == NavigationMode.GNSS_TRACKING) {
                        tvModeBadge.text = "GNSS ON"
                        tvModeBadge.setTextColor(Color.parseColor("#10B981"))
                    } else {
                        tvModeBadge.text = "GNSS OFF (DR)"
                        tvModeBadge.setTextColor(Color.parseColor("#FFD700"))
                    }

                    // Map marker and polyline update
                    if (state.latitude != 0.0 && state.longitude != 0.0) {
                        val point = GeoPoint(state.latitude, state.longitude)

                        vehicleMarker?.position = point
                        vehicleMarker?.rotation = -(state.headingDeg + 90f)
                        // Center map on position
                        if (lastPlottedPoint == null) {
                            // Change this:
                            // mapView.controller.animateTo(point)

                            // To this (instant repositioning with zero animation delay):
                            mapView.controller.setCenter(point)
                            lastPlottedPoint = point
                        }

                        // Add polyline points strictly when moving (> 2.0 km/h)
                        // Lower threshold to 0.8 meters and 1.0 km/h for instant reactivity
                        if (state.speedKmh >= 1.0f) {
                            val distance = lastPlottedPoint?.distanceToAsDouble(point) ?: Double.MAX_VALUE
                            if (distance > 0.8) {
                                trajectoryPolyline?.addPoint(point)
                                lastPlottedPoint = point
                                mapView.controller.setCenter(point)
                            }
                        }

                        mapView.invalidate()
                    }
                }
            }
        }
    }

    private fun toggleNavigation() {
        val intent = Intent(this, NavigationService::class.java)

        if (isNavigating) {
            isNavigating = false
            btnToggleNav.text = "START NAVIGATION"
            btnToggleNav.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark)

            navigationService?.coordinator?.stopNavigation()
            if (isBound) {
                unbindService(serviceConnection)
                isBound = false
            }
            stopService(intent)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                checkPermissions()
                return
            }

            isNavigating = true
            btnToggleNav.text = "STOP NAVIGATION"
            btnToggleNav.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)

            // Clear any lingering tracks from previous sessions
            trajectoryPolyline?.actualPoints?.clear()
            lastPlottedPoint = null
            mapView.invalidate()

            ContextCompat.startForegroundService(this, intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}