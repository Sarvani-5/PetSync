package com.example.petsync.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.petsync.databinding.ItemAdoptionRequestBinding
import com.example.petsync.models.AdoptionRequest
import com.example.petsync.models.RequestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdoptionRequestAdapter(
    private val onApprove: (AdoptionRequest) -> Unit,
    private val onReject: (AdoptionRequest) -> Unit,
    private val onViewDetails: (AdoptionRequest) -> Unit
) : ListAdapter<AdoptionRequest, AdoptionRequestAdapter.RequestViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemAdoptionRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequestViewHolder(private val binding: ItemAdoptionRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onViewDetails(getItem(position))
                }
            }

            binding.btnApprove.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onApprove(getItem(position))
                }
            }

            binding.btnReject.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onReject(getItem(position))
                }
            }
        }

        fun bind(request: AdoptionRequest) {
            binding.tvPetName.text = request.petName
            binding.tvUserName.text = "From: ${request.userName}"
            binding.tvDate.text = "Visit: ${dateFormatter.format(Date(request.visitDate))} at ${request.visitTime}"
            binding.tvStatus.text = "Status: ${request.status}"

            // Adjust buttons based on status
            val isPending = request.status == RequestStatus.PENDING
            binding.btnApprove.isEnabled = isPending
            binding.btnReject.isEnabled = isPending
        }
    }

    class RequestDiffCallback : DiffUtil.ItemCallback<AdoptionRequest>() {
        override fun areItemsTheSame(oldItem: AdoptionRequest, newItem: AdoptionRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdoptionRequest, newItem: AdoptionRequest): Boolean {
            return oldItem == newItem
        }
    }
}