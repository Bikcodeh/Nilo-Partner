package com.bikcode.nilopartner

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bikcode.nilopartner.data.model.ProductDTO
import com.bikcode.nilopartner.databinding.ActivityMainBinding
import com.bikcode.nilopartner.presentation.adapter.ProductAdapter
import com.bikcode.nilopartner.presentation.listeners.OnProductListener
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), OnProductListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var productAdapter: ProductAdapter

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val response = IdpResponse.fromResultIntent(it.data)

            if (it.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Not logged", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, "See you", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    response.error?.let {
                        if(it.errorCode == ErrorCodes.NO_NETWORK) {
                            Toast.makeText(this, "No connection", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error code: ${it.errorCode}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configAuth()
        setupRecycler()
    }

    private fun setupRecycler() {
        productAdapter = ProductAdapter()
        productAdapter.setListener(this)
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(
                this@MainActivity,
                3,
                GridLayoutManager.HORIZONTAL,
                false
            )
            adapter = productAdapter
        }

        (1..20).forEach {
            val product = ProductDTO(
                it.toString(),
                "Product $it",
                "Product is $it",
                "",
                it,
                it * 1.1
            )
            productAdapter.add(product)
        }
    }

    private fun configAuth() {

        firebaseAuth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.lyProgress.visibility = View.GONE
                binding.nsvProducts.visibility = View.VISIBLE
            } else {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Log out", Toast.LENGTH_SHORT).show()
                    }.addOnCompleteListener {
                        if (it.isSuccessful) {
                            binding.nsvProducts.visibility = View.GONE
                            binding.lyProgress.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(this, "A problem has occurred", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(product: ProductDTO) {
        TODO("Not yet implemented")
    }

    override fun onLongClick(product: ProductDTO) {
        TODO("Not yet implemented")
    }
}