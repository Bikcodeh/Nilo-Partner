package com.bikcode.nilopartner.presentation.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.ProductDTO
import com.bikcode.nilopartner.databinding.FragmentDialogAddBinding
import com.bikcode.nilopartner.presentation.util.Constants.PATH_PRODUCTS_IMAGES
import com.bikcode.nilopartner.presentation.util.Constants.PRODUCTS_COLLECTION
import com.bikcode.nilopartner.presentation.util.EventPost
import com.bikcode.nilopartner.presentation.util.showToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AddDialogFragment(private val product: ProductDTO? = null) : DialogFragment(),
    DialogInterface.OnShowListener {

    private var _binding: FragmentDialogAddBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    private var productSelected: ProductDTO? = null
    private var photoSelectedUri: Uri? = null
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                photoSelectedUri = it.data?.data

                _binding?.imgProductPreview?.setImageURI(photoSelectedUri)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            _binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))

            _binding?.let {
                val builder = AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.add_product))
                    .setPositiveButton(getString(R.string.add), null)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setView(it.root)

                val dialog = builder.create()
                dialog.setOnShowListener(this)

                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun initProduct() {
        //val product = (activity as? MainAux).getProductSelected()
        productSelected?.let { productExist ->
            _binding?.let {
                it.tieName.setText(productExist.name)
                it.tieDescription.setText(productExist.description)
                it.tiePrice.setText(productExist.price.toString())
                it.tieQuantity.setText(productExist.quantity.toString())

                Glide.with(this)
                    .load(productExist.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(it.imgProductPreview)
            }
        }
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        productSelected = product
        initProduct()
        configButtons()

        val dialog = dialog as? AlertDialog
        dialog?.let {
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            val textButtonId = if (productSelected != null) {
                R.string.update_product
            } else {
                R.string.add_product
            }

            positiveButton?.text = getString(textButtonId)
            positiveButton?.setOnClickListener {
                enableUI(enable = false)

                uploadImage() { eventPost ->
                    if (eventPost.isSuccess) {
                        _binding?.let { binding ->
                            if (productSelected != null) {
                                productSelected?.apply {
                                    name = binding.tieName.text.toString().trim()
                                    description = binding.tieDescription.text.toString().trim()
                                    quantity = binding.tieQuantity.text.toString().toInt()
                                    price = binding.tiePrice.text.toString().toDouble()

                                    update(this)
                                }
                            } else {
                                val product = ProductDTO(
                                    name = binding.tieName.text.toString().trim(),
                                    description = binding.tieDescription.text.toString().trim(),
                                    quantity = binding.tieQuantity.text.toString().toInt(),
                                    price = binding.tiePrice.text.toString().toDouble(),
                                    imgUrl = eventPost.photoUrl
                                )
                                save(product, eventPost.documentId!!)
                            }
                        }
                    }
                }
            }

            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun uploadImage(callback: (EventPost) -> Unit) {
        val eventPost = EventPost()
        eventPost.documentId =
            FirebaseFirestore.getInstance().collection(PRODUCTS_COLLECTION).document().id
        val storageRef = FirebaseStorage.getInstance().reference.child(PATH_PRODUCTS_IMAGES)

        photoSelectedUri?.let { uri ->
            _binding?.let { binding ->
                val photoRef = storageRef.child(eventPost.documentId!!)
                photoRef.putFile(uri)
                    .addOnSuccessListener {
                        it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                            eventPost.isSuccess = true
                            eventPost.photoUrl = downloadUrl.toString()
                            callback(eventPost)
                        }
                    }.addOnFailureListener {
                        eventPost.isSuccess = false
                        callback(eventPost)
                    }
            }
        }
    }

    private fun configButtons() {
        _binding?.let {
            it.ibProduct.setOnClickListener {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also {
            resultLauncher.launch(it)
        }
    }

    private fun update(productDTO: ProductDTO) {
        val db = FirebaseFirestore.getInstance()

        productDTO.id?.let {
            db.collection(PRODUCTS_COLLECTION)
                .document(it)
                .set(productDTO)
                .addOnSuccessListener {
                    context?.showToast(R.string.product_updated)
                }.addOnFailureListener {
                    context?.showToast(R.string.insert_error)
                }.addOnCompleteListener {
                    enableUI(enable = true)
                    dismiss()
                }
        }
    }

    private fun save(productDTO: ProductDTO, documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(PRODUCTS_COLLECTION).document(documentId).set(productDTO)
            .addOnSuccessListener {
                context?.showToast(R.string.product_added)
            }.addOnFailureListener {
                context?.showToast(R.string.insert_error)
            }.addOnCompleteListener {
                enableUI(enable = true)
                dismiss()
            }
    }

    private fun enableUI(enable: Boolean) {
        positiveButton?.let { it.isEnabled = enable }
        negativeButton?.let { it.isEnabled = enable }

        _binding?.let {
            with(it) {
                tieQuantity.isEnabled = enable
                tiePrice.isEnabled = enable
                tieName.isEnabled = enable
                tieDescription.isEnabled = enable
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}