package com.example.petsync.ui.shops

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.petsync.R

class ShopsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_shops, container, false)
        val textView = root.findViewById<TextView>(R.id.text_shops)
        textView.text = "Shops coming soon!"
        return root
    }
}