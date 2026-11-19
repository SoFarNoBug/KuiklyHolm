@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jlj.kuiklybase.holm

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.toCPointer

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: ObjC 可见 Runtime Present 门面；Fullscreen + ModelPass；禁止壳直调 C Session。
 */
object HolmWgpuIosBridge {
    private val _passes = mutableMapOf<Long, HolmFullscreenPass>()
    private val _models = mutableMapOf<Long, HolmModelPass>()

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
     * @param metalLayerPtr CAMetalLayer* 的整型位型（ObjC：`(int64_t)(__bridge void *)layer`）。
     * @return Session 句柄；0 = 失败。
     */
    fun create(metalLayerPtr: Long, width: Int, height: Int): Long {
        val layer: COpaquePointer? =
            if (metalLayerPtr == 0L) null else metalLayerPtr.toCPointer()
        val pass = HolmWgpuNative.createFullscreenPass(layer, width, height)
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

    fun createModel(metalLayerPtr: Long, width: Int, height: Int): Long {
        val layer: COpaquePointer? =
            if (metalLayerPtr == 0L) null else metalLayerPtr.toCPointer()
        val pass = HolmModelPass.create(layer, width, height)
        if (pass == null) {
            return 0L
        }
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

    /**
     * 本帧 MVP/灯光 + tick（mesh 须先灌；[sessionId] 对应 [HolmIosModelBridge] 岛会话）。
     */
    fun presentModelFrame(
        handle: Long,
        sessionId: Long,
        aspect: Float,
        dtSecs: Float,
        playing: Boolean,
    ): Boolean {
        val pass = _models[handle] ?: return false
        if (!HolmIosModelBridge.isReady(sessionId)) return false
        val snap = HolmIosModelBridge.currentPacked(sessionId) ?: return false
        val mvp = HolmIosModelBridge.fillMvp(sessionId, aspect)
        val light = HolmIosModelBridge.lightDirIntensity(sessionId)
        val lit = light[0] > 0.5f
        val intensity = if (lit) light[4] else 1f
        val color = if (lit && intensity != 1f) {
            floatArrayOf(
                snap.colorR * intensity,
                snap.colorG * intensity,
                snap.colorB * intensity,
                snap.colorA,
            )
        } else {
            floatArrayOf(snap.colorR, snap.colorG, snap.colorB, snap.colorA)
        }
        pass.setFrame(mvp, color, lit, floatArrayOf(light[1], light[2], light[3]))
        pass.setPlaying(playing)
        return pass.tick(dtSecs)
    }

    fun uploadModelMeshes(handle: Long, sessionId: Long): Boolean {
        val snap = HolmIosModelBridge.consumeSnapshot(sessionId)
            ?: HolmIosModelBridge.currentPacked(sessionId)
            ?: return false
        return setMeshesModel(handle, snap.verts, snap.indices)
    }
}
