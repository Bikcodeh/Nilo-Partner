package com.bikcode.nilopartner.presentation.ui.activity

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.ProductDTO
import com.bikcode.nilopartner.databinding.ActivityMainBinding
import com.bikcode.nilopartner.presentation.adapter.ProductAdapter
import com.bikcode.nilopartner.presentation.listeners.MainAux
import com.bikcode.nilopartner.presentation.listeners.OnProductListener
import com.bikcode.nilopartner.presentation.ui.dialog.AddDialogFragment
import com.bikcode.nilopartner.presentation.util.Constants.PATH_PRODUCTS_IMAGES
import com.bikcode.nilopartner.presentation.util.Constants.PRODUCTS_COLLECTION
import com.bikcode.nilopartner.presentation.util.showToast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var productAdapter: ProductAdapter
    private lateinit var firestoreListener: ListenerRegistration
    private var productSelected: ProductDTO? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val response = IdpResponse.fromResultIntent(it.data)

            if (it.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show()
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                        param(FirebaseAnalytics.Param.SUCCESS, 100) // 1000 = login successfully
                        param(FirebaseAnalytics.Param.METHOD, "login")
                    }
                } else {
                    Toast.makeText(this, "Not logged", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, "See you", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    response.error?.let {
                        if (it.errorCode == ErrorCodes.NO_NETWORK) {
                            Toast.makeText(this, "No connection", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error code: ${it.errorCode}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }

    private var count = 0
    private val uriList = mutableListOf<Uri>()
    private val progressSnackBar: Snackbar by lazy {
        Snackbar.make(binding.root,
            "",
            Snackbar.LENGTH_INDEFINITE)
    }

    private var galleryResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                if (it.data?.clipData != null) {
                    count = it.data!!.clipData!!.itemCount
                    for (i in 0..count - 1) {
                        uriList.add(it.data!!.clipData!!.getItemAt(i).uri)
                    }

                    if (count > 0) uploadImage(0)
                }
            }
        }

    private fun uploadImage(position: Int) {
        FirebaseAuth.getInstance().currentUser?.let { user ->

            progressSnackBar.apply {
                setText("Uploading picture ${position + 1} of $count")
                    .show()
            }

            val productRef = FirebaseStorage.getInstance().reference
                .child(user.uid)
                .child(PATH_PRODUCTS_IMAGES)
                .child(productSelected?.id!!)
                .child("image${position + 1}")

            productRef.putFile(uriList[position])
                .addOnSuccessListener {
                    if (position < count - 1) {
                        uploadImage(position + 1)
                    } else {
                        progressSnackBar.apply {
                            setText(getString(R.string.pictures_uploaded))
                            setDuration(Snackbar.LENGTH_SHORT)
                                .show()
                        }
                    }
                }.addOnFailureListener {
                    progressSnackBar.apply {
                        setText("Error uploading photo ${position + 1}")
                        setDuration(Snackbar.LENGTH_LONG)
                            .show()
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
        //setupFirestore()
        //setupFirestoreRealtime()
        setupButtons()
        setupAnalytics()
    }

    private fun setupAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun setupFirestoreRealtime() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(PRODUCTS_COLLECTION)

        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                showToast(R.string.error_fetching_data)
                return@addSnapshotListener
            }

            for (snapshot in snapshots!!.documentChanges) {
                val product = snapshot.document.toObject(ProductDTO::class.java)
                product.id = snapshot.document.id
                when (snapshot.type) {
                    DocumentChange.Type.ADDED -> productAdapter.add(product)
                    DocumentChange.Type.REMOVED -> productAdapter.delete(product)
                    DocumentChange.Type.MODIFIED -> productAdapter.update(product)
                }
            }
        }
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
    }

    private fun configAuth() {

        firebaseAuth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.lyProgress.visibility = View.GONE
                binding.nsvProducts.visibility = View.VISIBLE
                binding.fabCreate.show()
            } else {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                authLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .build()
                )
            }
        }
    }

    private fun setupFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection(PRODUCTS_COLLECTION).get()
            .addOnSuccessListener { snapshots ->
                for (document in snapshots) {
                    val product = document.toObject(ProductDTO::class.java)
                    product.id = document.id
                    productAdapter.add(product)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtons() {
        binding.fabCreate.setOnClickListener {
            productSelected = null
            AddDialogFragment().show(supportFragmentManager,
                AddDialogFragment::class.java.simpleName)
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        setupFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
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
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                            param(FirebaseAnalytics.Param.SUCCESS, 100) // 100 = login successfully
                            param(FirebaseAnalytics.Param.METHOD, "sign_out")
                        }
                    }.addOnCompleteListener {
                        if (it.isSuccessful) {
                            binding.nsvProducts.visibility = View.GONE
                            binding.lyProgress.visibility = View.VISIBLE
                            binding.fabCreate.hide()
                        } else {
                            Toast.makeText(this, "A problem has occurred", Toast.LENGTH_SHORT)
                                .show()
                            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                                param(FirebaseAnalytics.Param.SUCCESS, 201) // 201 = error sign out
                                param(FirebaseAnalytics.Param.METHOD, "sign_out")
                            }
                        }
                    }
            }
            R.id.action_history -> startActivity(Intent(this, OrderActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(product: ProductDTO) {
        productSelected = product
        AddDialogFragment(product).show(
            supportFragmentManager,
            AddDialogFragment::class.java.simpleName
        )
    }

    override fun onLongClick(product: ProductDTO) {
        val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        adapter.add("Delete")
        adapter.add("Add photos")

        MaterialAlertDialogBuilder(this)
            .setAdapter(adapter) { dialogInterface: DialogInterface, position: Int ->
                when (position) {
                    0 -> confirmDeleteProduct(product)
                    1 -> {
                        productSelected = product
                        Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also {
                            it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            galleryResult.launch(it)
                        }
                    }
                }
            }.show()
    }

    private fun confirmDeleteProduct(product: ProductDTO) {

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_product_title)
            .setMessage(R.string.dialog_delete_product_message)
            .setPositiveButton(R.string.dialog_delete_action_confirm) { _, _ ->
                product.imgUrl?.let { imageUrl ->
                    val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                    val db = FirebaseFirestore.getInstance()
                    val productRef = db.collection(PRODUCTS_COLLECTION)
                    product.id?.let { id ->
                        //FirebaseStorage.getInstance().reference.child(Constants.PATH_PRODUCTS_IMAGES).child(id)
                        imageRef
                            .delete()
                            .addOnSuccessListener {
                                productRef.document(id)
                                    .delete()
                                    .addOnFailureListener {
                                        showToast(R.string.delete_error)
                                    }
                            }.addOnFailureListener {
                                showToast(R.string.error_deleting_image)
                            }
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun getProductSelected(): ProductDTO? = productSelected
}