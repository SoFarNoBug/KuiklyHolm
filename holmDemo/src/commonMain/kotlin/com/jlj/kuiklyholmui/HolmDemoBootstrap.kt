package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.GlBackendProvider
import com.jlj.kuiklybase.holm.HolmProviders
import com.jlj.kuiklybase.holm.WgpuBackendProvider

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Demo 显式注册 Holm Provider（演示 ADR 0008；不依赖仅 umbrella 静默注册）。
 */
object HolmDemoBootstrap {
    private var _done: Boolean = false

    fun ensure() {
        if (_done) return
        HolmProviders.register(GlBackendProvider)
        HolmProviders.register(WgpuBackendProvider)
        HolmProviders.setDefault(GlBackendProvider.ID)
        _done = true
    }
}
