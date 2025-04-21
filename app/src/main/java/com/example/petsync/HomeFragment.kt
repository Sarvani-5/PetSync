package com.example.petsync.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.petsync.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the home screen UI
        setupHomeScreen()
    }

    private fun setupHomeScreen() {
        // Here you can add code to load featured pets, promotions, etc.
        binding.tvWelcome.text = "Welcome to PetSync!"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}