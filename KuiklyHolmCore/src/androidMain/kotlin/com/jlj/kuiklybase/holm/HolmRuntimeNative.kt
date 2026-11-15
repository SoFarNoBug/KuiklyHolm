package com.jlj.kuiklybase.holm

import android.view.Surface

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Android holm_runtime JNI Session；缺 .so / create 失败 → BLOCKED，不静默 GLES。
 */
object HolmRuntimeNative {
    @Volatile
    private var _libraryLoaded: Boolean? = null

    @Volatile
    private var _presentable: Boolean = false

    /** 尝试加载 `libholm_runtime.so`；失败不抛，返回 false。 */
    fun isLibraryLoaded(): Boolean {
        val cached = _libraryLoaded
        if (cached != null) return cached
        val ok = runCatching {
            System.loadLibrary("holm_runtime")
            true
        }.getOrDefault(false)
        _libraryLoaded = ok
        return ok
    }

    /**
     * M0 已可呈现：库已载且至少一次 Session create+tick 成功。
     */
    fun isM0Presentable(): Boolean = isLibraryLoaded() && _presentable

    /** 可宣称 `backend=wgpu-m0` 的充要条件。 */
    fun isAvailable(): Boolean = isLibraryLoaded() && isM0Presentable()

    fun blockedMessage(): String {
        return if (!isLibraryLoaded()) {
            "DualTrack B BLOCKED: libholm_runtime.so missing (run holm-runtime/build-android.ps1). No silent non-wgpu stack."
        } else if (!_presentable) {
            "DualTrack B BLOCKED: holm_runtime loaded but Session/Surface not ready. No silent GLES stand-in."
        } else {
            "DualTrack B BLOCKED: holm_runtime unavailable."
        }
    }

    fun markPresentable(ok: Boolean) {
        _presentable = ok
    }

    fun abiVersion(): Int {
        if (!isLibraryLoaded()) return 0
        return runCatching { nativeAbiVersion() }.getOrDefault(0)
    }

    fun hasWgpu(): Int {
        if (!isLibraryLoaded()) return 0
        return runCatching { nativeHasWgpu() }.getOrDefault(0)
    }

    /** 返回 Session 句柄；0 表示失败。 */
    fun createSession(surface: Surface, width: Int, height: Int): Long {
        if (!isLibraryLoaded()) return 0L
        return runCatching { nativeCreate(surface, width, height) }.getOrDefault(0L)
    }

    fun destroySession(handle: Long) {
        if (handle == 0L || !isLibraryLoaded()) return
        runCatching { nativeDestroy(handle) }
    }

    fun resize(handle: Long, width: Int, height: Int) {
        if (handle == 0L) return
        runCatching { nativeResize(handle, width, height) }
    }

    fun setScene(handle: Long, scene: String) {
        if (handle == 0L) return
        runCatching { nativeSetScene(handle, scene) }
    }

    fun setCustomFs(handle: Long, fs: String): Boolean {
        if (handle == 0L) return false
        return runCatching { nativeSetCustomFs(handle, fs) }.getOrDefault(false)
    }

    fun lastErrorCode(handle: Long): String {
        if (handle == 0L) return ""
        return runCatching { nativeLastErrorCode(handle) }.getOrDefault("")
    }

    fun lastErrorMessage(handle: Long): String {
        if (handle == 0L) return ""
        return runCatching { nativeLastErrorMessage(handle) }.getOrDefault("")
    }

    /**
     * 取出 pending 错误（含 uncaptured）；交替为 code, message, code, message…
     */
    fun drainErrors(handle: Long): List<Pair<String, String>> {
        if (handle == 0L) return emptyList()
        val flat = runCatching { nativeDrainErrors(handle) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Pair<String, String>>(flat.size / 2)
        var i = 0
        while (i + 1 < flat.size) {
            out.add(flat[i] to flat[i + 1])
            i += 2
        }
        return out
    }

    fun setLiquid(handle: Long, boost: Float, bend: Float) {
        if (handle == 0L) return
        runCatching { nativeSetLiquid(handle, boost, bend) }
    }

    fun setPlaying(handle: Long, playing: Boolean) {
        if (handle == 0L) return
        runCatching { nativeSetPlaying(handle, playing) }
    }

    fun tick(handle: Long, dtSecs: Float): Boolean {
        if (handle == 0L) return false
        return runCatching { nativeTick(handle, dtSecs) }.getOrDefault(false)
    }

    fun backendId(handle: Long): String {
        if (handle == 0L) return HolmExecutor.BACKEND_WGPU_BLOCKED
        return runCatching { nativeBackendId(handle) }.getOrDefault(HolmExecutor.BACKEND_WGPU_M0)
    }

    private external fun nativeAbiVersion(): Int
    private external fun nativeHasWgpu(): Int
    private external fun nativeCreate(surface: Surface, width: Int, height: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeResize(handle: Long, width: Int, height: Int)
    private external fun nativeSetScene(handle: Long, scene: String)
    private external fun nativeSetCustomFs(handle: Long, fs: String): Boolean
    private external fun nativeSetLiquid(handle: Long, boost: Float, bend: Float)
    private external fun nativeSetPlaying(handle: Long, playing: Boolean)
    private external fun nativeTick(handle: Long, dtSecs: Float): Boolean
    private external fun nativeBackendId(handle: Long): String
    private external fun nativeLastErrorCode(handle: Long): String
    private external fun nativeLastErrorMessage(handle: Long): String
    private external fun nativeDrainErrors(handle: Long): Array<String>
}
