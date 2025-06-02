package com.example.petsync.ui.shops
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petsync.R
import com.example.petsync.ui.shops.model.CartItem
import com.example.petsync.ui.shops.model.Product

class CartAdapter(
    private val cartItems: List<CartItem>,
    private val onIncreaseQuantity: (Int) -> Unit,
    private val onDecreaseQuantity: (Int) -> Unit,
    private val onRemoveItem: (Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount() = cartItems.size

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.cartItemImageView)
        private val productName: TextView = itemView.findViewById(R.id.cartItemNameTextView)
        private val productPrice: TextView = itemView.findViewById(R.id.cartItemPriceTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val increaseButton: ImageButton = itemView.findViewById(R.id.increaseQuantityButton)
        private val decreaseButton: ImageButton = itemView.findViewById(R.id.decreaseQuantityButton)
        private val removeButton: ImageButton = itemView.findViewById(R.id.removeItemButton)

        fun bind(cartItem: CartItem) {
            productImage.setImageResource(cartItem.product.imageResId)
            productName.text = cartItem.product.name
            productPrice.text = "₹${String.format("%.2f", cartItem.product.price)}"
            quantityTextView.text = cartItem.quantity.toString()

            increaseButton.setOnClickListener {
                onIncreaseQuantity(adapterPosition)
            }

            decreaseButton.setOnClickListener {
                onDecreaseQuantity(adapterPosition)
            }

            removeButton.setOnClickListener {
                onRemoveItem(adapterPosition)
            }
        }
    }
}