package com.jlj.kuiklybase.holm

import kotlin.concurrent.AtomicInt

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: KuiklyHolmCore umbrella 自动注册 GL + wgpu Provider（Ohos；兼容只引 Core 的宿主）。
 */
object HolmUmbrellaBootstrap {
    private val _state = AtomicInt(0)

    fun ensure() {
        if (_state.value != 0) return
        if (_state.compareAndSet(0, 1)) {
            HolmProviders.register(GlBackendProvider)
            HolmProviders.register(WgpuBackendProvider)
            if (HolmProviders.defaultIdOrNull().isNullOrBlank()) {
                HolmProviders.setDefault(GlBackendProvider.ID)
            }
            _state.value = 2
        } else {
            while (_state.value < 2) {
                // spin briefly until registrant finishes
            }
        }
    }
}
