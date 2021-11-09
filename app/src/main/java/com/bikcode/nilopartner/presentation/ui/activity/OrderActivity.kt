package com.bikcode.nilopartner.presentation.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.OrderDTO
import com.bikcode.nilopartner.data.service.fcm.NotificationRS
import com.bikcode.nilopartner.databinding.ActivityOrderBinding
import com.bikcode.nilopartner.presentation.adapter.OrderAdapter
import com.bikcode.nilopartner.presentation.listeners.OnOrderListener
import com.bikcode.nilopartner.presentation.listeners.OrderAux
import com.bikcode.nilopartner.presentation.ui.fragment.chat.ChatFragment
import com.bikcode.nilopartner.presentation.util.Constants.PROP_DATE
import com.bikcode.nilopartner.presentation.util.Constants.PROP_STATUS
import com.bikcode.nilopartner.presentation.util.Constants.PROP_TOKEN
import com.bikcode.nilopartner.presentation.util.Constants.REQUESTS_COLLECTION
import com.bikcode.nilopartner.presentation.util.Constants.TOKENS_COLLECTION
import com.bikcode.nilopartner.presentation.util.Constants.USERS_COLLECTION
import com.bikcode.nilopartner.presentation.util.showToast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding
    private val ordersAdapter: OrderAdapter by lazy { OrderAdapter(this) }
    private var orderSelected: OrderDTO? = null

    private val statusKeys: Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }
    private val statusValues: Array<String> by lazy {
        resources.getStringArray(R.array.status_value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupFirestore()
    }


    private fun setupFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection(REQUESTS_COLLECTION)
            .orderBy(PROP_DATE, Query.Direction.DESCENDING)
            .get()
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

    private fun notifyClient(order: OrderDTO) {
        val db = FirebaseFirestore.getInstance()
        db.collection(USERS_COLLECTION).document(order.clientId)
            .collection(TOKENS_COLLECTION)
            .get()
            .addOnSuccessListener {
                var tokenStr = ""
                for(document in it) {
                    val tokenMap = document.data
                    tokenStr += "${tokenMap.getValue(PROP_TOKEN)},"
                }

                if(tokenStr.isNotEmpty()) {
                    tokenStr = tokenStr.dropLast(1)
                    var names = ""
                    order.products.forEach {
                        names += "${it.value.name}, "
                    }

                    names = names.dropLast(2)
                    val index = statusKeys.indexOf(order.status)

                    val notificationRS = NotificationRS()
                    notificationRS.sendNotification(
                        title = "Your order have been ${statusValues[index]}",
                        message = names,
                        tokens = tokenStr
                    )
                }
            }.addOnFailureListener {
                showToast(R.string.error_fetching_data)
            }
    }

    override fun onStartChat(order: OrderDTO) {
        orderSelected = order
        val chatFragment = ChatFragment()

        supportFragmentManager.beginTransaction()
            .add(R.id.containerOrder, chatFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onStatusChange(order: OrderDTO) {
        val db = FirebaseFirestore.getInstance()
        db.collection(REQUESTS_COLLECTION).document(order.id)
            .update(PROP_STATUS, order.status)
            .addOnSuccessListener {
                val analytics = Firebase.analytics
                analytics.logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO) {
                    val products = mutableListOf<Bundle>()
                    order.products.forEach {
                        val bundle = Bundle()
                        bundle.putString("id_product", it.key)
                        products.add(bundle)
                    }

                    param(FirebaseAnalytics.Param.SHIPPING, products.toTypedArray())
                    param(FirebaseAnalytics.Param.PRICE, order.totalPrice)
                }
                showToast(R.string.status_updated)
                notifyClient(order)
            }.addOnFailureListener {
                showToast(R.string.error_fetching_data)
            }
    }

    override fun getOrderSelected(): OrderDTO? = orderSelected
}