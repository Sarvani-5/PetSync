package com.example.petsync.ui.shops

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.petsync.R
import com.example.petsync.databinding.FragmentShopsBinding
import com.example.petsync.models.User
import com.example.petsync.models.UserType
import com.example.petsync.ui.shops.model.CartItem
import com.example.petsync.ui.shops.model.Product
import com.example.petsync.utils.NotificationUtils
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShopsFragment : Fragment() {
    private val TAG = "ShopsFragment"

    private var _binding: FragmentShopsBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()
    private val productList = mutableListOf<Product>()
    private var cartBadge: BadgeDrawable? = null

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Current user data - Will be fetched from Firebase
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get current user data from Firebase
        fetchCurrentUser()

        setupCartBadge()
        setupProductsRecyclerView()
        loadProducts()
        setupCartRecyclerView()
        setupCheckoutButton()

        // Toggle between products and cart views
        binding.toggleCartButton.setOnClickListener {
            toggleCartVisibility()
        }
    }

    /**
     * Fetch current user data from Firebase
     */
    private fun fetchCurrentUser() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        currentUser = document.toObject(User::class.java)
                        Log.d(TAG, "Current user data fetched: ${currentUser?.name}")
                    } else {
                        Log.e(TAG, "No user document found")
                        Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user data", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e(TAG, "No user logged in")
            // Handle case where no user is logged in
        }
    }

    private fun setupCartBadge() {
        cartBadge = BadgeDrawable.create(requireContext())
        cartBadge?.apply {
            isVisible = false
            backgroundColor = resources.getColor(android.R.color.holo_red_dark, null)
            BadgeUtils.attachBadgeDrawable(this, binding.toggleCartButton)
        }
        updateCartBadge()
    }

    private fun updateCartBadge() {
        val itemCount = cartItems.sumOf { it.quantity }
        cartBadge?.apply {
            isVisible = itemCount > 0
            number = itemCount
        }
    }

    private fun setupProductsRecyclerView() {
        productAdapter = ProductAdapter(productList) { product ->
            addToCart(product)
        }

        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
        }
    }

    private fun setupCartRecyclerView() {
        cartAdapter = CartAdapter(cartItems,
            onIncreaseQuantity = { position ->
                cartItems[position].quantity++
                cartAdapter.notifyItemChanged(position)
                updateTotalPrice()
                updateCartBadge()
            },
            onDecreaseQuantity = { position ->
                val item = cartItems[position]
                if (item.quantity > 1) {
                    item.quantity--
                    cartAdapter.notifyItemChanged(position)
                } else {
                    cartItems.removeAt(position)
                    cartAdapter.notifyItemRemoved(position)
                }
                updateTotalPrice()
                updateCartBadge()
            },
            onRemoveItem = { position ->
                cartItems.removeAt(position)
                cartAdapter.notifyItemRemoved(position)
                updateTotalPrice()
                updateCartBadge()
            }
        )

        binding.cartRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            adapter = cartAdapter
        }
    }

    private fun loadProducts() {
        // Sample product data
        val products = listOf(
            Product(1, "Premium Dog Food", "High-quality nutrition for your dog", 699.99, R.drawable.ic_pet_food_dog),
            Product(2, "Cat Food - Fish Flavor", "Tasty fish-flavored food for cats", 499.99, R.drawable.ic_pet_food_cat),
            Product(3, "Dog Collar - Medium", "Comfortable collar for medium-sized dogs", 299.99, R.drawable.ic_pet_collar),
            Product(4, "Pet Bed - Small", "Cozy bed for small pets", 899.99, R.drawable.ic_pet_bed),
            Product(5, "Squeaky Toy", "Fun toy for dogs", 149.99, R.drawable.ic_pet_toy),
            Product(6, "Cat Scratching Post", "Durable scratching post for cats", 799.99, R.drawable.ic_cat_scratch),
            Product(7, "Fish Food", "Nutrition for aquarium fish", 199.99, R.drawable.ic_fish_food),
            Product(8, "Pet Grooming Brush", "For easy pet grooming", 249.99, R.drawable.ic_grooming),
            Product(9, "Pet Carrier", "Safe carrier for small pets", 999.99, R.drawable.ic_pet_carrier),
            Product(10, "Winter Pet Jacket", "Keep your pet warm", 599.99, R.drawable.ic_pet_jacket)
        )

        productList.clear()
        productList.addAll(products)
        productAdapter.notifyDataSetChanged()
    }

    private fun addToCart(product: Product) {
        // Check if product already exists in cart
        val existingItem = cartItems.find { it.product.id == product.id }

        if (existingItem != null) {
            existingItem.quantity++
            cartAdapter.notifyItemChanged(cartItems.indexOf(existingItem))
        } else {
            cartItems.add(CartItem(product, 1))
            cartAdapter.notifyItemInserted(cartItems.size - 1)
        }

        // Show notification
        Toast.makeText(requireContext(), "${product.name} added to cart", Toast.LENGTH_SHORT).show()

        // Removed the SMS notification for adding to cart
        // sendProductAddedSMS(product) - removed

        updateTotalPrice()
        updateCartBadge()
    }

    private fun updateTotalPrice() {
        val total = cartItems.sumOf { it.product.price * it.quantity }
        binding.totalPriceTextView.text = "Total: ₹${String.format("%.2f", total)}"
    }

    private fun toggleCartVisibility() {
        val showingCart = binding.cartContainer.visibility == View.VISIBLE

        if (showingCart) {
            binding.productsContainer.visibility = View.VISIBLE
            binding.cartContainer.visibility = View.GONE
            binding.toggleCartButton.text = "View Cart"
        } else {
            binding.productsContainer.visibility = View.GONE
            binding.cartContainer.visibility = View.VISIBLE
            binding.toggleCartButton.text = "Continue Shopping"
        }
    }

    private fun setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showPaymentOptionsDialog()
        }
    }

    private fun showPaymentOptionsDialog() {
        val options = arrayOf("Cash on Delivery", "UPI Payment")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Payment Method")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> processOrder("Cash on Delivery")
                    1 -> processOrder("UPI Payment")
                }
            }
            .show()
    }

    private fun processOrder(paymentMethod: String) {
        // Calculate order total
        val orderTotal = cartItems.sumOf { it.product.price * it.quantity }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Order Confirmed")
            .setMessage("Your order has been placed successfully!\n\nPayment Method: $paymentMethod\nTotal: ₹${String.format("%.2f", orderTotal)}\n\nYou will receive a confirmation via SMS shortly.")
            .setPositiveButton("OK") { _, _ ->
                // Clear cart
                cartItems.clear()
                cartAdapter.notifyDataSetChanged()
                updateTotalPrice()
                updateCartBadge()

                // Go back to products view
                binding.productsContainer.visibility = View.VISIBLE
                binding.cartContainer.visibility = View.GONE
                binding.toggleCartButton.text = "View Cart"

                // Send confirmation SMS message using NotificationUtils
                sendOrderConfirmationSMS(orderTotal, paymentMethod)

                // Also show in-app notification for order confirmation
                NotificationUtils.showNotification(
                    requireContext(),
                    "Order Confirmed",
                    "Your order of ₹${String.format("%.2f", orderTotal)} has been placed successfully!"
                )
            }
            .show()
    }

    private fun sendOrderConfirmationSMS(orderTotal: Double, paymentMethod: String) {
        try {
            currentUser?.let { user ->
                val phone = formatPhoneNumber(user.phone)
                if (phone.isNotEmpty()) {
                    val orderItems = cartItems.joinToString(", ") {
                        "${it.product.name} x${it.quantity}"
                    }

                    val message = "PetSync Order Confirmation: Hi ${user.name}, thank you for your order! " +
                            "Items: $orderItems. " +
                            "Total: ₹${String.format("%.2f", orderTotal)}. " +
                            "Payment: $paymentMethod. " +
                            "Delivery to: ${user.address}. " +
                            "Thank you for shopping with PetSync!"

                    // Use NotificationUtils to send SMS message
                    NotificationUtils.sendSMS(requireContext(), phone, message)

                    Log.d(TAG, "Sending SMS order confirmation for total $orderTotal")
                } else {
                    Log.w(TAG, "No valid phone number available for order confirmation")
                    Toast.makeText(requireContext(), "No valid phone number to send SMS notification", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.e(TAG, "Current user is null for order confirmation")
                Toast.makeText(requireContext(), "User data not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS order confirmation", e)
        }
    }

    /**
     * Format phone number to ensure it has the country code
     * Note: For SMS, we might need to remove the '+' symbol for some SMS APIs
     */
    private fun formatPhoneNumber(phone: String): String {
        // If phone number doesn't start with +, assume it's an Indian number and add +91
        return if (phone.startsWith("+")) {
            phone
        } else if (phone.length == 10) {
            "+91$phone"
        } else {
            phone // Return as is if it doesn't match expected format
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}