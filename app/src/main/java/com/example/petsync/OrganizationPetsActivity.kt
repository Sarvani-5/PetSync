package com.example.petsync

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.petsync.adapters.PetAdapter
import com.example.petsync.databinding.ActivityOrganizationPetsBinding
import com.example.petsync.models.Pet
import com.example.petsync.models.PetStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OrganizationPetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizationPetsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var petAdapter: PetAdapter
    private var organizationId: String = ""
    private var organizationName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizationPetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get organization details from intent
        organizationId = intent.getStringExtra("ORGANIZATION_ID") ?: ""
        organizationName = intent.getStringExtra("ORGANIZATION_NAME") ?: "Organization Pets"

        // Set up toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = organizationName

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Set up RecyclerView
        setupRecyclerView()

        // Load pets from this organization
        loadOrganizationPets()
    }

    private fun setupRecyclerView() {
        petAdapter = PetAdapter { pet ->
            // Handle pet item click - open pet details
            val intent = android.content.Intent(this, PetDetailActivity::class.java)
            intent.putExtra("PET_ID", pet.id)
            startActivity(intent)
        }

        binding.recyclerViewPets.apply {
            layoutManager = GridLayoutManager(this@OrganizationPetsActivity, 2)
            adapter = petAdapter
        }
    }

    private fun loadOrganizationPets() {
        if (organizationId.isEmpty()) {
            binding.tvNoPets.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoPets.visibility = View.GONE

        // Query Firestore for pets from this organization that are available
        db.collection("pets")
            .whereEqualTo("organizationId", organizationId)
            .whereEqualTo("status", PetStatus.AVAILABLE.name)
            .orderBy("dateAdded", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    binding.tvNoPets.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val petsList = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Pet::class.java).copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }

                petAdapter.submitList(petsList)
                binding.tvNoPets.visibility = if (petsList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvNoPets.visibility = View.VISIBLE
                Toast.makeText(
                    this,
                    "Error loading pets: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}