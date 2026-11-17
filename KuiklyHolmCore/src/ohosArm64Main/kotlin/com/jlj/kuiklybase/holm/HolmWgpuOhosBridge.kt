@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jlj.kuiklybase.holm

import kotlinx.cinterop.COpaquePointer

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Ohos Runtime Present 门面；壳经 Demo @CName / dlsym 调 FullscreenPass，禁止直调 C Session。
 */
object HolmWgpuOhosBridge {
    private val _passes = mutableMapOf<Long, HolmFullscreenPass>()

    /**
     * @return null 表示 OK；否则为 BLOCKED 文案。
     */
    fun requireProvider(executor: String): String? {
        HolmUmbrellaBootstrap.ensure()
        return try {
            HolmProviders.requireForExecutor(executor)
            null
        } catch (e: IllegalStateException) {
            e.message ?: HolmProviders.missingDefaultMessage()
        }
    }

    fun hasWgpu(): Boolean = HolmWgpuNative.hasWgpu() == 1

    fun isLibraryLoaded(): Boolean = HolmWgpuNative.isLibraryLoaded()

    fun lastCreateError(): String = HolmWgpuNative.lastCreateError()

    /**
     * @param nativeWindow OHNativeWindow*。
     * @return Session 句柄；0 = 失败。
     */
    fun create(nativeWindow: COpaquePointer?, width: Int, height: Int): Long {
        val pass = HolmWgpuNative.createFullscreenPass(nativeWindow, width, height)
        if (pass == null) {
            HolmWgpuNative.markPresentable(false)
            return 0L
        }
        HolmWgpuNative.markPresentable(true)
        _passes[pass.handle] = pass
        return pass.handle
    }

    fun destroy(handle: Long) {
        if (handle == 0L) return
        _passes.remove(handle)?.destroy()
        if (_passes.isEmpty()) {
            HolmWgpuNative.markPresentable(false)
        }
    }

    fun tick(handle: Long, dtSecs: Float): Boolean =
        _passes[handle]?.tick(dtSecs) ?: false

    fun resize(handle: Long, width: Int, height: Int) {
        _passes[handle]?.resize(width, height)
    }

    fun setCustomFs(handle: Long, fs: String): Boolean =
        _passes[handle]?.setCustomFs(fs) ?: false

    fun setScene(handle: Long, scene: String) {
        _passes[handle]?.setScene(scene)
    }

    fun setLiquid(handle: Long, boost: Float, bend: Float) {
        _passes[handle]?.setLiquid(boost, bend)
    }

    fun setPlaying(handle: Long, playing: Boolean) {
        _passes[handle]?.setPlaying(playing)
    }

    fun backendId(handle: Long): String =
        _passes[handle]?.backendId() ?: "wgpu-blocked"

    fun lastErrorCode(handle: Long): String =
        _passes[handle]?.lastErrorCode().orEmpty()

    fun lastErrorMessage(handle: Long): String =
        _passes[handle]?.lastErrorMessage().orEmpty()
}
