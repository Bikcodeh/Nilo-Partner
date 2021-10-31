package com.bikcode.nilopartner.presentation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.ProductDTO
import com.bikcode.nilopartner.databinding.ItemProductBinding
import com.bikcode.nilopartner.presentation.listeners.OnProductListener

class ProductAdapter: RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val products: MutableList<ProductDTO> = mutableListOf()
    private lateinit var listener: OnProductListener
    private lateinit var context: Context

    fun setData(data: List<ProductDTO>) {
        products.clear()
        products.addAll(data)
        notifyDataSetChanged()
    }

    fun setListener(listener: OnProductListener) {
        this.listener = listener
    }

    fun add(productDTO: ProductDTO) {
        if(products.contains(productDTO).not()) {
            products.add(productDTO)
            notifyItemInserted(products.count() - 1)
        }
    }

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val binding = ItemProductBinding.bind(view)

        fun bind(productDTO: ProductDTO) {
            with(binding) {
                tvName.text = productDTO.name
                tvPrice.text = productDTO.price.toString()
                tvQuantity.text = productDTO.quantity.toString()
            }
        }

        fun setListener(productDTO: ProductDTO) {
            binding.root.setOnClickListener {
                listener.onClick(productDTO)
            }

            binding.root.setOnLongClickListener {
                listener.onClick(productDTO)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
        holder.setListener(product)
    }

    override fun getItemCount(): Int = products.count()
}