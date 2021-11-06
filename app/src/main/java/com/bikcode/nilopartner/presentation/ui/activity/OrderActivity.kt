package com.bikcode.nilopartner.presentation.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.OrderDTO
import com.bikcode.nilopartner.databinding.ActivityOrderBinding
import com.bikcode.nilopartner.presentation.adapter.OrderAdapter
import com.bikcode.nilopartner.presentation.listeners.OnOrderListener
import com.bikcode.nilopartner.presentation.listeners.OrderAux
import com.bikcode.nilopartner.presentation.util.Constants.REQUESTS_COLLECTION
import com.bikcode.nilopartner.presentation.util.showToast
import com.google.firebase.firestore.FirebaseFirestore

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding
    private val ordersAdapter: OrderAdapter by lazy { OrderAdapter(this) }
    private var orderSelected: OrderDTO? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupFirestore()
    }


    private fun setupFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection(REQUESTS_COLLECTION).get()
            .addOnSuccessListener {
                for(document in it) {
                    val order = document.toObject(OrderDTO::class.java)
                    order.id = document.id
                    ordersAdapter.add(order)
                }
            }.addOnFailureListener {
                showToast(R.string.error_fetching_data)
            }
    }

    private fun setupRecycler() {
        binding.rvOrder.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = ordersAdapter
        }
    }

    override fun onStartChat(order: OrderDTO) {
        TODO("Not yet implemented")
    }

    override fun onStatusChange(order: OrderDTO) {
        TODO("Not yet implemented")
    }

    override fun getOrderSelected(): OrderDTO? = orderSelected
}