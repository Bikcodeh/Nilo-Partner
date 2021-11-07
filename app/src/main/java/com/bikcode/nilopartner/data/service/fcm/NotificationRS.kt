package com.bikcode.nilopartner.data.service.fcm

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.bikcode.nilopartner.base.NiloPartnerApplication
import com.bikcode.nilopartner.presentation.util.Constants.NILO_PARTNER_RS
import com.bikcode.nilopartner.presentation.util.Constants.PARAM_MESSAGE
import com.bikcode.nilopartner.presentation.util.Constants.PARAM_METHOD
import com.bikcode.nilopartner.presentation.util.Constants.PARAM_SUCCESS
import com.bikcode.nilopartner.presentation.util.Constants.PARAM_TITLE
import com.bikcode.nilopartner.presentation.util.Constants.PARAM_TOKENS
import com.bikcode.nilopartner.presentation.util.Constants.SEND_NOTIFICATION
import org.json.JSONException
import org.json.JSONObject

class NotificationRS {

    fun sendNotification(title: String, message: String, tokens: String) {
        val params = JSONObject()

        params.put(PARAM_METHOD, SEND_NOTIFICATION)
        params.put(PARAM_TITLE, title)
        params.put(PARAM_MESSAGE, message)
        params.put(PARAM_TOKENS, tokens)

        val jsonObjectRequest: JsonObjectRequest =
            object : JsonObjectRequest(Method.POST,
                NILO_PARTNER_RS,
                params,
                Response.Listener { response ->
                    try {
                        val success = response.getInt(PARAM_SUCCESS)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    if (error.localizedMessage != null) {
                        Log.e("ERROR VOLLEY *-*-*-*", error.message ?: "")
                    }
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): MutableMap<String, String> {
                    val paramsHeader = HashMap<String, String>()
                    paramsHeader["Content-Type"] = "application/json; charset=utf-8"
                    return super.getHeaders()
                }
            }
        NiloPartnerApplication.volleyHelper.addToRequestQueue(jsonObjectRequest)
    }
}