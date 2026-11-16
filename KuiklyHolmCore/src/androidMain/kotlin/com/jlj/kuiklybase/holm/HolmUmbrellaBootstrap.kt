package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: KuiklyHolmCore umbrella 自动注册 GL + wgpu Provider（兼容只引 Core 的宿主）。
 */
object HolmUmbrellaBootstrap {
    @Volatile
    private var _done: Boolean = false

    fun ensure() {
        if (_done) return
        synchronized(this) {
            if (_done) return
            HolmProviders.register(GlBackendProvider)
            HolmProviders.register(WgpuBackendProvider)
            if (HolmProviders.defaultIdOrNull().isNullOrBlank()) {
                HolmProviders.setDefault(GlBackendProvider.ID)
            }
            _done = true
        }
    }
}
