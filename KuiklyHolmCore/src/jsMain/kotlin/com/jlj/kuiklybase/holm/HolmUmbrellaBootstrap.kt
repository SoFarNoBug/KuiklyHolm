package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: KuiklyHolmCore umbrella 自动注册 GL + wgpu Provider（H5）。
 */
object HolmUmbrellaBootstrap {
    private var _done: Boolean = false

    fun ensure() {
        if (_done) return
        HolmProviders.register(GlBackendProvider)
        HolmProviders.register(WgpuBackendProvider)
        if (HolmProviders.defaultIdOrNull().isNullOrBlank()) {
            HolmProviders.setDefault(GlBackendProvider.ID)
        }
        _done = true
    }
}
