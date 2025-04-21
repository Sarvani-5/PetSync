package com.example.petsync

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.petsync.databinding.ActivityMainBinding
import com.example.petsync.models.UserType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isOrganization = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_PetSync_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is logged in
        checkLoginStatus()

        // Set up the action bar and navigation
        setSupportActionBar(binding.toolbar)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Only include visible destinations in the top level destinations
        val topLevelDestinations = if (isOrganization) {
            setOf(
                R.id.navigation_home, R.id.navigation_pets, R.id.navigation_shops,
                R.id.navigation_profile, R.id.navigation_requests
            )
        } else {
            setOf(
                R.id.navigation_home, R.id.navigation_pets, R.id.navigation_shops,
                R.id.navigation_profile
            )
        }

        appBarConfiguration = AppBarConfiguration(topLevelDestinations)

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Custom handling for navigation item selection to prevent navigation errors
        navView.setOnItemSelectedListener { item ->
            // For regular users, prevent navigation to the requests tab
            if (item.itemId == R.id.navigation_requests && !isOrganization) {
                false
            } else {
                // Only navigate if the destination exists from current location
                try {
                    navController.navigate(item.itemId)
                    true
                } catch (e: IllegalArgumentException) {
                    // Handle navigation error gracefully
                    // Could show a message to the user if needed
                    false
                }
            }
        }

        // Check user type to determine whether to show the Requests tab
        checkUserTypeAndConfigureNav(navView)
    }

    private fun checkLoginStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User is not logged in, redirect to login activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkUserTypeAndConfigureNav(navView: BottomNavigationView) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userTypeString = document.getString("userType") ?: UserType.USER.name
                    val userType = UserType.valueOf(userTypeString)

                    // Update organization status
                    isOrganization = userType == UserType.ORGANIZATION

                    // Show Requests tab only for organizations
                    val requestsMenuItem = navView.menu.findItem(R.id.navigation_requests)
                    requestsMenuItem.isVisible = isOrganization
                }
            }
            .addOnFailureListener {
                // Default to regular user if there's an error
                isOrganization = false
                val requestsMenuItem = navView.menu.findItem(R.id.navigation_requests)
                requestsMenuItem.isVisible = false
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}