package com.bikcode.nilopartner.presentation.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(@StringRes messageResourceId: Int) {
    Toast.makeText(this, getString(messageResourceId), Toast.LENGTH_SHORT).show()
}