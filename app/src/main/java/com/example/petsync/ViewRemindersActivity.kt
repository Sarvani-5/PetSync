package com.example.petsync

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petsync.adapters.RemindersAdapter
import com.example.petsync.databinding.ActivityViewRemindersBinding
import com.example.petsync.managers.RemindersManager
import com.example.petsync.models.PetReminder
import com.google.firebase.auth.FirebaseAuth

class ViewRemindersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewRemindersBinding
    private lateinit var remindersManager: RemindersManager
    private lateinit var remindersAdapter: RemindersAdapter

    private val allReminders = mutableListOf<PetReminder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // Initialize managers
        remindersManager = RemindersManager(this)

        // Setup RecyclerView
        setupRecyclerView()

        // Load reminders
        loadReminders()

        // Setup filter options
        setupFilterOptions()
    }

    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter(
            reminders = allReminders,
            onReminderCompleted = { reminder ->
                markReminderAsCompleted(reminder)
            },
            onReminderDeleted = { reminder ->
                deleteReminder(reminder)
            }
        )

        binding.recyclerReminders.apply {
            layoutManager = LinearLayoutManager(this@ViewRemindersActivity)
            adapter = remindersAdapter
        }
    }

    private fun loadReminders() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerReminders.visibility = View.GONE

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            binding.progressBar.visibility = View.GONE
            return
        }

        remindersManager.getUserReminders(userId) { reminders ->
            binding.progressBar.visibility = View.GONE

            allReminders.clear()
            allReminders.addAll(reminders.sortedBy { it.reminderTime })

            if (allReminders.isEmpty()) {
                binding.recyclerReminders.visibility = View.GONE
            } else {
                binding.recyclerReminders.visibility = View.VISIBLE
            }

            remindersAdapter.notifyDataSetChanged()
        }
    }

    private fun setupFilterOptions() {
        // Filter by all reminders
        binding.chipAllReminders.setOnClickListener {
            filterReminders(null)
        }

        // Filter by active reminders
        binding.chipActive.setOnClickListener {
            filterReminders(false)
        }

        // Filter by completed reminders
        binding.chipCompleted.setOnClickListener {
            filterReminders(true)
        }

        // Filter by general reminders
        binding.chipGeneral.setOnClickListener {
            filterByGeneralReminders()
        }

        // Filter by pet reminders
        binding.chipPetSpecific.setOnClickListener {
            filterByPetReminders()
        }
    }

    private fun filterReminders(isCompleted: Boolean?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        remindersManager.getUserReminders(userId) { reminders ->
            allReminders.clear()

            val filtered = if (isCompleted == null) {
                reminders
            } else {
                reminders.filter { it.isCompleted == isCompleted }
            }

            allReminders.addAll(filtered.sortedBy { it.reminderTime })

            if (allReminders.isEmpty()) {
                binding.recyclerReminders.visibility = View.GONE
            } else {
                binding.recyclerReminders.visibility = View.VISIBLE
            }

            remindersAdapter.notifyDataSetChanged()
        }
    }

    private fun filterByGeneralReminders() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        remindersManager.getGeneralReminders(userId) { reminders ->
            allReminders.clear()
            allReminders.addAll(reminders.sortedBy { it.reminderTime })

            if (allReminders.isEmpty()) {
                binding.recyclerReminders.visibility = View.GONE
            } else {
                binding.recyclerReminders.visibility = View.VISIBLE
            }

            remindersAdapter.notifyDataSetChanged()
        }
    }

    private fun filterByPetReminders() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        remindersManager.getUserReminders(userId) { reminders ->
            allReminders.clear()
            // Filter only reminders with non-null petId
            allReminders.addAll(reminders.filter { it.petId != null }.sortedBy { it.reminderTime })

            if (allReminders.isEmpty()) {
                binding.recyclerReminders.visibility = View.GONE
            } else {
                binding.recyclerReminders.visibility = View.VISIBLE
            }

            remindersAdapter.notifyDataSetChanged()
        }
    }

    private fun markReminderAsCompleted(reminder: PetReminder) {
        remindersManager.markReminderAsCompleted(reminder.id) { success ->
            if (success) {
                // Update the UI
                val position = allReminders.indexOfFirst { it.id == reminder.id }
                if (position != -1) {
                    allReminders[position] = allReminders[position].copy(isCompleted = true)
                    remindersAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun deleteReminder(reminder: PetReminder) {
        remindersManager.deleteReminder(reminder.id) { success ->
            if (success) {
                // Update the UI
                val position = allReminders.indexOfFirst { it.id == reminder.id }
                if (position != -1) {
                    allReminders.removeAt(position)
                    remindersAdapter.notifyItemRemoved(position)

                    if (allReminders.isEmpty()) {
                        binding.recyclerReminders.visibility = View.GONE
                    }
                }
            }
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