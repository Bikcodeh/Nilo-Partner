package com.bikcode.nilopartner.base

import android.app.Application
import com.bikcode.nilopartner.data.service.VolleyHelper

class NiloPartnerApplication: Application() {

    companion object {
        lateinit var volleyHelper: VolleyHelper
    }

    override fun onCreate() {
        super.onCreate()

        volleyHelper = VolleyHelper.getInstance(this)
    }
}