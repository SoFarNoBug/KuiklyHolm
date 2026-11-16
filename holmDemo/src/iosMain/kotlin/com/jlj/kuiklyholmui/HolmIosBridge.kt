package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.GlBackendProvider
import com.jlj.kuiklybase.holm.HolmIos
import com.jlj.kuiklybase.holm.HolmProviders
import com.jlj.kuiklybase.holm.WgpuBackendProvider

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: 壳侧调用入口；显式 register Provider（对齐 Android KRApplication）+ HolmIos.init。
 */
object HolmIosBridge {
    fun configure(cacheDir: String) {
        HolmProviders.register(GlBackendProvider)
        HolmProviders.register(WgpuBackendProvider)
        if (HolmProviders.defaultIdOrNull().isNullOrBlank()) {
            HolmProviders.setDefault(GlBackendProvider.ID)
        }
        HolmIos.init(cacheDir)
    }
}
