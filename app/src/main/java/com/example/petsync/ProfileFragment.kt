package com.example.petsync.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.petsync.R
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)
        val textView = root.findViewById<TextView>(R.id.text_profile)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get current user's email
        val userEmail = auth.currentUser?.email ?: "No User"
        textView.text = "Profile: $userEmail"

        return root
    }
}