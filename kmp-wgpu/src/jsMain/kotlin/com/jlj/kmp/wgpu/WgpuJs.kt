package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: H5 kmp-wgpu 绑定；依赖 window.__holmRuntime（kmp_wgpu_native wasm 兼容名）。
 */

actual typealias WgpuHandle = Any

object JsWgpuPlatform : WgpuPlatform {
    override fun isLibraryLoaded(): Boolean {
        return js(
            "typeof window !== 'undefined' && !!window.__holmRuntime && window.__holmRuntime.ready === true",
        ).unsafeCast<Boolean>()
    }

    override fun isAvailable(): Boolean {
        if (!isLibraryLoaded()) return false
        return js(
            "typeof window.__holmRuntime.hasWgpu === 'function' && window.__holmRuntime.hasWgpu() === 1",
        ).unsafeCast<Boolean>()
    }

    override fun blockedMessage(): String {
        val loadErr = js(
            "typeof window !== 'undefined' && window.__holmRuntime && window.__holmRuntime.error",
        ).unsafeCast<String?>()
        return if (!loadErr.isNullOrBlank()) {
            "kmp-wgpu BLOCKED: holm-runtime wasm load failed: $loadErr"
        } else if (!isLibraryLoaded()) {
            "kmp-wgpu BLOCKED: holm-runtime wasm not loaded (build holm-runtime/build-wasm.ps1). No silent non-wgpu stack."
        } else {
            "kmp-wgpu BLOCKED: holm-runtime wasm loaded but has_wgpu=0. No silent GLES stand-in."
        }
    }

    override fun destroyDevice(handle: WgpuHandle) {
        js(
            "(function(s){ if (s && typeof s.free === 'function') s.free(); })",
        ).unsafeCast<(Any?) -> Unit>()(handle)
    }
}

actual object WgpuRuntime {
    actual fun createInstance(): Instance = Instance(JsWgpuPlatform)

    actual fun isLibraryLoaded(): Boolean = JsWgpuPlatform.isLibraryLoaded()

    actual fun isAvailable(): Boolean = JsWgpuPlatform.isAvailable()

    actual fun blockedMessage(): String = JsWgpuPlatform.blockedMessage()
}

fun jsPlatform(): JsWgpuPlatform = JsWgpuPlatform

fun isLoadPending(): Boolean {
    return js(
        """
        (typeof window !== 'undefined' && !!window.__holmRuntime
          && window.__holmRuntime.ready !== true
          && !window.__holmRuntime.error)
        """,
    ).unsafeCast<Boolean>()
}
