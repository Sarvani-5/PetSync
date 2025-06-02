package com.example.petsync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petsync.databinding.ActivityPetReminderBinding
import com.example.petsync.managers.RemindersManager
import com.example.petsync.models.PetReminder
import com.example.petsync.models.PetReminderType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PetReminderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetReminderBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var remindersManager: RemindersManager

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var selectedPetId: String? = null
    private var selectedReminderType = PetReminderType.MEDICATION
    private var isGeneralReminder = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reminders"

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize RemindersManager
        remindersManager = RemindersManager(this)

        // Setup general reminder checkbox
        setupGeneralReminderToggle()

        // Setup spinner for reminder types
        setupReminderTypeSpinner()

        // Setup date and time pickers
        setupDateTimePickers()

        // Load user's pets
        loadUserPets()

        // Setup save button
        binding.btnSaveReminder.setOnClickListener {
            saveReminder()
        }

        // Setup view reminders button
        binding.btnViewReminders.setOnClickListener {
            // Navigate to view reminders screen
            val intent = android.content.Intent(this, ViewRemindersActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupGeneralReminderToggle() {
        // Assuming you've added a checkbox to your layout with id "checkboxGeneralReminder"
        binding.checkboxGeneralReminder.setOnCheckedChangeListener { _, isChecked ->
            isGeneralReminder = isChecked

            // Show/hide pet selection based on toggle
            if (isChecked) {
                binding.petSelectionLayout.visibility = View.GONE
                selectedPetId = null
            } else {
                binding.petSelectionLayout.visibility = View.VISIBLE
                // Restore selected pet ID if pets are loaded
                if (binding.spinnerPet.count > 0 && binding.spinnerPet.selectedItemPosition >= 0) {
                    // This assumes you've stored pet IDs somewhere accessible
                    val position = binding.spinnerPet.selectedItemPosition
                    if (petsList.isNotEmpty() && position < petsList.size) {
                        selectedPetId = petsList[position].first
                    }
                }
            }
        }
    }

    // Store pets list to reference IDs
    private val petsList = mutableListOf<Pair<String, String>>()

    private fun setupReminderTypeSpinner() {
        val reminderTypes = PetReminderType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reminderTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerReminderType.adapter = adapter

        binding.spinnerReminderType.setOnItemSelectedListener { parent, _, position, _ ->
            selectedReminderType = PetReminderType.values()[position]

            // Update hint based on selected type
            binding.etReminderMessage.hint = when (selectedReminderType) {
                PetReminderType.MEDICATION -> "Enter medication details..."
                PetReminderType.FEEDING -> "Enter feeding instructions..."
                PetReminderType.VET_VISIT -> "Enter vet appointment details..."
                PetReminderType.GROOMING -> "Enter grooming details..."
                PetReminderType.EXERCISE -> "Enter exercise details..."
                PetReminderType.OTHER -> "Enter reminder details..."
            }
        }
    }

    private fun setupDateTimePickers() {
        // Set current date and time initially
        updateDateButtonText()
        updateTimeButtonText()

        // Date picker
        binding.btnSelectDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateButtonText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Time picker
        binding.btnSelectTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    updateTimeButtonText()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun updateDateButtonText() {
        binding.btnSelectDate.text = dateFormat.format(calendar.time)
    }

    private fun updateTimeButtonText() {
        binding.btnSelectTime.text = timeFormat.format(calendar.time)
    }

    private fun loadUserPets() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { documents ->
                petsList.clear()
                petsList.addAll(documents.map { doc ->
                    Pair(doc.id, doc.getString("name") ?: "Unknown Pet")
                })

                if (petsList.isEmpty()) {
                    binding.spinnerPet.isEnabled = false
                    binding.spinnerPet.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        listOf("No pets found")
                    )

                    // Set general reminder as default if no pets
                    binding.checkboxGeneralReminder.isChecked = true
                    isGeneralReminder = true
                    binding.petSelectionLayout.visibility = View.GONE
                } else {
                    val petNames = petsList.map { it.second }
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        petNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerPet.adapter = adapter

                    binding.spinnerPet.setOnItemSelectedListener { parent, _, position, _ ->
                        selectedPetId = petsList[position].first
                    }
                    // Select first pet by default
                    selectedPetId = petsList.firstOrNull()?.first
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load pets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveReminder() {
        val title = binding.etReminderTitle.text.toString().trim()
        val message = binding.etReminderMessage.text.toString().trim()

        if (title.isEmpty()) {
            binding.etReminderTitle.error = "Title is required"
            return
        }

        if (message.isEmpty()) {
            binding.etReminderMessage.error = "Message is required"
            return
        }

        // Check if pet selection is required
        if (!isGeneralReminder && selectedPetId == null) {
            Toast.makeText(this, "Please select a pet or make this a general reminder", Toast.LENGTH_SHORT).show()
            return
        }

        // Create reminder object
        val userId = auth.currentUser?.uid ?: return
        val reminder = PetReminder(
            id = UUID.randomUUID().toString(),
            userId = userId,
            petId = if (isGeneralReminder) null else selectedPetId,  // Make petId nullable in your model
            title = title,
            message = message,
            reminderType = selectedReminderType,
            reminderTime = calendar.timeInMillis,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        // Save to database and schedule notification
        remindersManager.saveReminder(reminder) { success ->
            if (success) {
                Toast.makeText(this, "Reminder saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save reminder", Toast.LENGTH_SHORT).show()
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

// Extension function for Spinner
fun android.widget.Spinner.setOnItemSelectedListener(
    onItemSelected: (parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) -> Unit
) {
    this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
            onItemSelected(parent, view, position, id)
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
            // Do nothing
        }
    }
}