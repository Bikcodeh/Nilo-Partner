package com.bikcode.nilopartner.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.OrderDTO
import com.bikcode.nilopartner.databinding.ItemOrderBinding
import com.bikcode.nilopartner.presentation.listeners.OnOrderListener

class OrderAdapter(private val listener: OnOrderListener) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val orders = mutableListOf<OrderDTO>()
    private lateinit var context: Context

    private val statusKeys: Array<Int> by lazy {
        context.resources.getIntArray(R.array.status_key).toTypedArray()
    }
    private val statusValues: Array<String> by lazy {
        context.resources.getStringArray(R.array.status_value)
    }

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemOrderBinding.bind(view)

        fun bind(orderDTO: OrderDTO) {
            with(binding) {
                var names = ""
                orderDTO.products.forEach { order ->
                    names += "${order.value.name}, "
                }
                tvProductNames.text = names.dropLast(2)
                tvTotalPrice.text = context.getString(R.string.cart_full, orderDTO.totalPrice)
                tvId.text = context.getString(R.string.order_id, orderDTO.id)

                val index = statusKeys.indexOf(orderDTO.status)
                val status = if (index != -1) {
                    statusValues[index]
                } else {
                    context.getString(R.string.order_status_unknown)
                }
                tvStatus.text = context.getString(R.string.status, status)
            }
        }

        fun setListener(orderDTO: OrderDTO) {
            with(binding) {
                chpChat.setOnClickListener {
                    listener.onStartChat(orderDTO)
                }
            }
        }
    }

    fun add(orderDTO: OrderDTO) {
        orders.add(orderDTO)
        notifyItemInserted(orders.count() - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.setListener(order)
        holder.bind(order)
    }

    override fun getItemCount(): Int = orders.count()
}