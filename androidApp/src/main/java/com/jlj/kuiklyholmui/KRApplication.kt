package com.jlj.kuiklyholmui

import android.app.Application
import com.jlj.kuiklybase.holm.HolmAndroid

class KRApplication : Application() {

    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        HolmAndroid.init(this)
    }

    companion object {
        lateinit var application: Application
    }
}