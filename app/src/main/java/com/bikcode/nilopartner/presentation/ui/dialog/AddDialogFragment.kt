package com.bikcode.nilopartner.presentation.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.ProductDTO
import com.bikcode.nilopartner.databinding.FragmentDialogAddBinding
import com.bikcode.nilopartner.presentation.util.Constants.PRODUCTS_COLLECTION
import com.bikcode.nilopartner.presentation.util.showToast
import com.google.firebase.firestore.FirebaseFirestore

class AddDialogFragment: DialogFragment(), DialogInterface.OnShowListener {

    private var _binding: FragmentDialogAddBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

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

    override fun onShow(dialogInterface: DialogInterface?) {
        val dialog= dialog as? AlertDialog
        dialog?.let {
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {
                _binding?.let { binding ->
                    val product = ProductDTO(
                        name = binding.tieName.text.toString().trim(),
                        description = binding.tieDescription.text.toString().trim(),
                        quantity = binding.tieQuantity.text.toString().toInt(),
                        price = binding.tiePrice.text.toString().toDouble()
                    )

                    save(product)
                }
            }

            negativeButton?.setOnClickListener {
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
                dismiss()
            }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}