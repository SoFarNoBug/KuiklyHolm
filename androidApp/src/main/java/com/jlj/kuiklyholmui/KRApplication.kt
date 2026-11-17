package com.jlj.kuiklyholmui

import android.app.Application
import com.jlj.kuiklybase.holm.GlBackendProvider
import com.jlj.kuiklybase.holm.HolmAndroid
import com.jlj.kuiklybase.holm.HolmProviders
import com.jlj.kuiklybase.holm.WgpuBackendProvider

class KRApplication : Application() {

    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        HolmAndroid.init(this)
        // 宿主显式注册（ADR 0008）；Demo 侧 HolmDemoBootstrap 再 register 幂等
        HolmProviders.register(GlBackendProvider)
        HolmProviders.register(WgpuBackendProvider)
        if (HolmProviders.defaultIdOrNull().isNullOrBlank()) {
            HolmProviders.setDefault(GlBackendProvider.ID)
        }
    }

    companion object {
        lateinit var application: Application
    }
}