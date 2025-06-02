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
class ProductAdapter(
    private val products: List<Product>,
    private val onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.productImageView)
        private val productName: TextView = itemView.findViewById(R.id.productNameTextView)
        private val productDescription: TextView = itemView.findViewById(R.id.productDescriptionTextView)
        private val productPrice: TextView = itemView.findViewById(R.id.productPriceTextView)
        private val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)

        fun bind(product: Product) {
            productImage.setImageResource(product.imageResId)
            productName.text = product.name
            productDescription.text = product.description
            productPrice.text = "₹${String.format("%.2f", product.price)}"

            addToCartButton.setOnClickListener {
                onAddToCart(product)
            }
        }
    }
}

