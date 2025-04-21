package com.example.petsync.ui.pets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petsync.adapters.PetAdapter
import com.example.petsync.databinding.FragmentPetsBinding
import com.example.petsync.models.Pet
import com.example.petsync.models.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PetsFragment : Fragment() {

    private var _binding: FragmentPetsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PetsViewModel
    private lateinit var petAdapter: PetAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userType: UserType = UserType.USER

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[PetsViewModel::class.java]
        _binding = FragmentPetsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        setupRecyclerView()

        // Check user type and adjust UI accordingly
        checkUserType()

        // Observe pets
        viewModel.pets.observe(viewLifecycleOwner) { pets ->
            petAdapter.submitList(pets)
            binding.progressBar.visibility = View.GONE

            if (pets.isEmpty()) {
                binding.tvNoPets.visibility = View.VISIBLE
            } else {
                binding.tvNoPets.visibility = View.GONE
            }
        }

        // Add new pet button (only visible for organizations)
        binding.fabAddPet.setOnClickListener {
            // Navigate to add pet screen
            val addPetFragment = AddPetFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, addPetFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        petAdapter = PetAdapter { pet ->
            // Handle pet click - show details
            val petDetailFragment = PetDetailFragment.newInstance(pet.id)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, petDetailFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerViewPets.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = petAdapter
        }
    }

    private fun checkUserType() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userTypeString = document.getString("userType") ?: UserType.USER.name
                    userType = UserType.valueOf(userTypeString)

                    // Show add pet button only for organizations
                    binding.fabAddPet.visibility = if (userType == UserType.ORGANIZATION) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    // Load pets
                    loadPets()
                }
            }
    }

    private fun loadPets() {
        binding.progressBar.visibility = View.VISIBLE

        if (userType == UserType.ORGANIZATION) {
            // If organization, load only their pets
            val userId = auth.currentUser?.uid ?: return
            viewModel.loadOrganizationPets(userId)
        } else {
            // If regular user, load all available pets
            viewModel.loadAllAvailablePets()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}