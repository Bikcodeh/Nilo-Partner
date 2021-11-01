package com.bikcode.nilopartner.presentation.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.ProductDTO
import com.bikcode.nilopartner.databinding.FragmentDialogAddBinding
import com.bikcode.nilopartner.presentation.listeners.MainAux
import com.bikcode.nilopartner.presentation.util.Constants.PRODUCTS_COLLECTION
import com.bikcode.nilopartner.presentation.util.showToast
import com.google.firebase.firestore.FirebaseFirestore

class AddDialogFragment(private val product: ProductDTO? = null) : DialogFragment(), DialogInterface.OnShowListener {

    private var _binding: FragmentDialogAddBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    private var productSelected: ProductDTO? = null

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
            }
        }
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        productSelected = product
        initProduct()

        val dialog = dialog as? AlertDialog
        dialog?.let {
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            val textButtonId = if(productSelected != null) {
                R.string.update_product
            } else {
                R.string.add_product
            }

            positiveButton?.text = getString(textButtonId)
            positiveButton?.setOnClickListener {
                enableUI(enable = false)
                _binding?.let { binding ->
                    if(productSelected != null) {
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
                            price = binding.tiePrice.text.toString().toDouble()
                        )
                        save(product)
                    }
                }
            }

            negativeButton?.setOnClickListener {
                dismiss()
            }
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

    private fun save(productDTO: ProductDTO) {
        val db = FirebaseFirestore.getInstance()
        db.collection(PRODUCTS_COLLECTION).add(productDTO)
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