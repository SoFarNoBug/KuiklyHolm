@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jlj.kuiklybase.holm

import kotlinx.cinterop.COpaquePointer

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Ohos Present 门面；Fullscreen + ModelPass；壳经 @CName / dlsym，禁止直调 C Session。
 */
object HolmWgpuOhosBridge {
    private val _passes = mutableMapOf<Long, HolmFullscreenPass>()
    private val _models = mutableMapOf<Long, HolmModelPass>()
    /** 壳 HolmXcMutex 串行 OnFrame；仅本帧 setFrame 前暂存 lightDir。 */
    private val _lightDirScratch = FloatArray(3)

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
        if (_passes.isEmpty() && _models.isEmpty()) {
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
        // ADR 0015: scene removed
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

    // --- ModelPass ---

    fun createModel(nativeWindow: COpaquePointer?, width: Int, height: Int): Long {
        val pass = HolmModelPass.create(nativeWindow, width, height) ?: return 0L
        HolmWgpuNative.markPresentable(true)
        _models[pass.handle] = pass
        return pass.handle
    }

    fun destroyModel(handle: Long) {
        if (handle == 0L) return
        _models.remove(handle)?.destroy()
        if (_passes.isEmpty() && _models.isEmpty()) {
            HolmWgpuNative.markPresentable(false)
        }
    }

    fun resizeModel(handle: Long, width: Int, height: Int) {
        _models[handle]?.resize(width, height)
    }

    fun setMeshesModel(handle: Long, verts: FloatArray, indices: IntArray): Boolean =
        _models[handle]?.setMeshes(verts, indices) ?: false

    fun setFrameModel(
        handle: Long,
        mvp: FloatArray,
        color: FloatArray,
        lit: Boolean,
        lightDir: FloatArray,
    ) {
        _models[handle]?.setFrame(mvp, color, lit, lightDir)
    }

    fun setPlayingModel(handle: Long, playing: Boolean) {
        _models[handle]?.setPlaying(playing)
    }

    fun tickModel(handle: Long, dtSecs: Float): Boolean =
        _models[handle]?.tick(dtSecs) ?: false

    fun backendIdModel(handle: Long): String =
        _models[handle]?.backendId() ?: "wgpu-blocked"

    fun lastErrorCodeModel(handle: Long): String =
        _models[handle]?.lastErrorCode().orEmpty()

    fun lastErrorMessageModel(handle: Long): String =
        _models[handle]?.lastErrorMessage().orEmpty()

    fun presentModelFrame(
        handle: Long,
        sessionId: Long,
        aspect: Float,
        dtSecs: Float,
        playing: Boolean,
    ): Boolean {
        val pass = _models[handle] ?: return false
        if (!HolmOhosModelBridge.isReady(sessionId)) return false
        // 勿每帧 currentPacked()；mvp/color/light 用 Session scratch（setFrame 已拷贝进 native）。
        val mvp = HolmOhosModelBridge.fillMvp(sessionId, aspect)
        val light = HolmOhosModelBridge.lightDirIntensity(sessionId)
        val lit = light[0] > 0.5f
        val intensity = if (lit) light[4] else 1f
        val color = if (lit && intensity != 1f) {
            HolmOhosModelBridge.colorWithIntensity(sessionId, intensity)
        } else {
            HolmOhosModelBridge.colorRgba(sessionId)
        }
        _lightDirScratch[0] = light[1]
        _lightDirScratch[1] = light[2]
        _lightDirScratch[2] = light[3]
        pass.setFrame(mvp, color, lit, _lightDirScratch)
        pass.setPlaying(playing)
        return pass.tick(dtSecs)
    }

    fun uploadModelMeshes(handle: Long, sessionId: Long): Boolean {
        val snap = HolmOhosModelBridge.consumeSnapshot(sessionId)
            ?: HolmOhosModelBridge.currentPacked(sessionId)
            ?: return false
        val ok = setMeshesModel(handle, snap.verts, snap.indices)
        if (!ok) {
            HolmOhosModelBridge.requeueSnapshot(sessionId, snap)
        }
        return ok
    }
}
