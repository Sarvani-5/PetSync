package com.example.petsync

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.petsync.databinding.ActivityPetOrganizationsMapBinding
import com.example.petsync.models.UserType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException

class PetOrganizationsMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetOrganizationsMapBinding
    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val REQUEST_LOCATION_PERMISSION = 1
    private var userLocation: OsmGeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set OSMDroid configuration
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        binding = ActivityPetOrganizationsMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pet Organizations"

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the map
        setupMap()

        // Load all organizations initially
        loadAllOrganizations()

        // Check for location permission
        if (hasLocationPermission()) {
            setupLocationOverlay()
        } else {
            requestLocationPermission()
        }

        // Set up find nearby button
        binding.btnFindNearby.setOnClickListener {
            if (hasLocationPermission()) {
                getUserLocationAndShowNearbyOrganizations()
            } else {
                requestLocationPermission()
            }
        }

        // Set up show all button
        binding.btnShowAll.setOnClickListener {
            loadAllOrganizations()
        }
    }

    private fun setupMap() {
        map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(5.0)  // Start with a wider view

        // Default center to US (or your preferred location)
        mapController.setCenter(OsmGeoPoint(37.0902, -95.7129))
    }

    private fun setupLocationOverlay() {
        // Add my location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationOverlay()
                getUserLocationAndShowNearbyOrganizations()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required to find nearby organizations",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getUserLocationAndShowNearbyOrganizations() {
        if (hasLocationPermission()) {
            binding.progressBar.visibility = View.VISIBLE

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        userLocation = OsmGeoPoint(location.latitude, location.longitude)
                        map.controller.setCenter(userLocation)
                        map.controller.setZoom(12.0)  // Zoom in

                        // Load nearby organizations
                        loadNearbyOrganizations(userLocation!!)
                    } else {
                        Toast.makeText(
                            this,
                            "Could not get current location. Please ensure location services are enabled.",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Failed to get location: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
        } else {
            requestLocationPermission()
        }
    }

    private fun loadAllOrganizations() {
        binding.progressBar.visibility = View.VISIBLE

        // Clear existing markers
        clearMarkers()

        // Query Firestore for all organizations
        db.collection("users")
            .whereEqualTo("userType", UserType.ORGANIZATION.name)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    Toast.makeText(
                        this,
                        "No pet organizations found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                // Process each organization
                processOrganizations(documents)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Error loading organizations: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadNearbyOrganizations(userLocation: OsmGeoPoint) {
        binding.progressBar.visibility = View.VISIBLE

        // Clear existing markers
        clearMarkers()

        // Query Firestore for all organizations
        db.collection("users")
            .whereEqualTo("userType", UserType.ORGANIZATION.name)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    Toast.makeText(
                        this,
                        "No pet organizations found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                // Process each organization but only display those within range
                processOrganizations(documents, userLocation, 20.0) // 20 km radius
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Error loading organizations: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun clearMarkers() {
        val markersToRemove = map.overlays.filterIsInstance<Marker>()
        map.overlays.removeAll(markersToRemove)
    }

    private fun processOrganizations(
        documents: com.google.firebase.firestore.QuerySnapshot,
        centerPoint: OsmGeoPoint? = null,
        maxDistanceKm: Double = Double.MAX_VALUE
    ) {
        var organizationsFound = 0
        var pendingGeocodings = documents.size()

        // This function will be called after each geocoding attempt (success or failure)
        // When all attempts are completed, it will invalidate the map
        fun geocodingCompleted() {
            pendingGeocodings--
            if (pendingGeocodings <= 0) {
                if (organizationsFound == 0) {
                    val message = if (centerPoint != null) {
                        "No pet organizations found within ${maxDistanceKm.toInt()} km"
                    } else {
                        "No pet organizations found"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
                map.invalidate()
            }
        }

        for (document in documents) {
            val orgId = document.id
            val orgName = document.getString("name") ?: "Unknown Organization"
            val address = document.getString("address") ?: ""

            if (address.isEmpty()) {
                // Skip organizations without addresses
                Log.d("PetSync", "Skipping $orgName - no address provided")
                geocodingCompleted()
                continue
            }

            // First try to get location from the location field if it exists
            if (document.contains("location") && document.get("location") is GeoPoint) {
                val geoPoint = document.getGeoPoint("location")!!
                val location = OsmGeoPoint(geoPoint.latitude, geoPoint.longitude)

                // If filtering by distance, check if this org is within range
                if (centerPoint != null) {
                    val distance = calculateDistance(
                        centerPoint.latitude, centerPoint.longitude,
                        location.latitude, location.longitude
                    )

                    if (distance > maxDistanceKm) {
                        geocodingCompleted()
                        continue  // Skip this organization if too far
                    }
                }

                // Add marker for this organization
                addOrganizationMarker(location, orgName, orgId, address)
                organizationsFound++
                geocodingCompleted()
            } else {
                // If no location field exists, use the Geocoder to get coordinates from address
                try {
                    val geocoder = Geocoder(this)

                    // Using try-catch for older Android versions
                    try {
                        // For Android 33+ (Tiramisu)
                        geocoder.getFromLocationName(address, 1) { geocodeResults ->
                            if (geocodeResults.isNotEmpty()) {
                                val result = geocodeResults[0]
                                val location = OsmGeoPoint(result.latitude, result.longitude)

                                // Check distance if needed
                                if (centerPoint != null) {
                                    val distance = calculateDistance(
                                        centerPoint.latitude, centerPoint.longitude,
                                        location.latitude, location.longitude
                                    )

                                    if (distance > maxDistanceKm) {
                                        runOnUiThread { geocodingCompleted() }
                                        return@getFromLocationName
                                    }
                                }

                                // Add marker on UI thread
                                runOnUiThread {
                                    addOrganizationMarker(location, orgName, orgId, address)
                                    organizationsFound++
                                    geocodingCompleted()
                                }
                            } else {
                                Log.e("PetSync", "Could not geocode address for $orgName: $address")
                                runOnUiThread { geocodingCompleted() }
                            }
                        }
                    } catch (e: NoSuchMethodError) {
                        // For older Android versions (pre-Tiramisu)
                        @Suppress("DEPRECATION")
                        val geocodeResults = geocoder.getFromLocationName(address, 1)

                        if (!geocodeResults.isNullOrEmpty()) {
                            val result = geocodeResults[0]
                            val location = OsmGeoPoint(result.latitude, result.longitude)

                            // Check distance if needed
                            if (centerPoint != null) {
                                val distance = calculateDistance(
                                    centerPoint.latitude, centerPoint.longitude,
                                    location.latitude, location.longitude
                                )

                                if (distance > maxDistanceKm) {
                                    geocodingCompleted()
                                    continue
                                }
                            }

                            addOrganizationMarker(location, orgName, orgId, address)
                            organizationsFound++
                        } else {
                            Log.e("PetSync", "Could not geocode address for $orgName: $address")
                        }
                        geocodingCompleted()
                    }
                } catch (e: IOException) {
                    // Log error and skip this organization
                    Log.e("PetSync", "Geocoding error for $orgName: ${e.message}")
                    geocodingCompleted()
                }
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    private fun addOrganizationMarker(location: OsmGeoPoint, title: String, orgId: String, address: String) {
        val marker = Marker(map)
        marker.position = location
        marker.title = title
        marker.snippet = address

        // Set marker click listener to show pets from this organization
        marker.setOnMarkerClickListener { clickedMarker, _ ->
            val intent = Intent(this, OrganizationPetsActivity::class.java)
            intent.putExtra("ORGANIZATION_ID", orgId)
            intent.putExtra("ORGANIZATION_NAME", clickedMarker.title)
            startActivity(intent)
            true
        }

        map.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}