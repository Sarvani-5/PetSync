package com.example.petsync.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petsync.R
import com.example.petsync.databinding.ItemPetBinding
import com.example.petsync.models.Pet

class PetAdapter(private val onPetClick: (Pet) -> Unit) :
    ListAdapter<Pet, PetAdapter.PetViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetViewHolder(private val binding: ItemPetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPetClick(getItem(position))
                }
            }
        }

        fun bind(pet: Pet) {
            binding.tvPetName.text = pet.name
            binding.tvPetBreed.text = pet.breed
            binding.tvPetAge.text = "${pet.age} years"
            binding.tvPetPrice.text = "$${pet.price}"

            if (pet.imageUrls.isNotEmpty()) {
                Glide.with(binding.ivPetImage.context)
                    .load(pet.imageUrls[0])
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .centerCrop()
                    .into(binding.ivPetImage)
            } else {
                binding.ivPetImage.setImageResource(R.drawable.ic_pet_placeholder)
            }
        }
    }

    class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem == newItem
        }
    }
}