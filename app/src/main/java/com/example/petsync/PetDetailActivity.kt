package com.example.petsync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petsync.databinding.ActivityPetDetailBinding
import com.example.petsync.ui.pets.PetDetailFragment

class PetDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the pet ID from the intent
        val petId = intent.getStringExtra("PET_ID") ?: ""

        if (petId.isNotEmpty() && savedInstanceState == null) {
            // Create and show the PetDetailFragment with the pet ID
            val fragment = PetDetailFragment.newInstance(petId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        } else if (petId.isEmpty()) {
            // Handle error case when no pet ID is provided
            finish()
        }
    }
}