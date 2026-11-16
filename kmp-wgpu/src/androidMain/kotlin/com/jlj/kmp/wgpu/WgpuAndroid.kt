package com.jlj.kmp.wgpu

import android.view.Surface

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Android kmp-wgpu 绑定；JNI 进 libholm_runtime（kmp_wgpu_native 产物名兼容）。
 * 粗粒度能力块：create / tick / resize / destroy；无逐 draw FFI。
 */

actual typealias WgpuHandle = Long

object AndroidWgpuPlatform : WgpuPlatform {
    @Volatile
    private var _libraryLoaded: Boolean? = null

    override fun isLibraryLoaded(): Boolean {
        val cached = _libraryLoaded
        if (cached != null) return cached
        val ok = runCatching {
            System.loadLibrary("holm_runtime")
            true
        }.getOrDefault(false)
        _libraryLoaded = ok
        return ok
    }

    override fun isAvailable(): Boolean = isLibraryLoaded() && runCatching { nativeHasWgpu() }.getOrDefault(0) == 1

    override fun blockedMessage(): String {
        return if (!isLibraryLoaded()) {
            "kmp-wgpu BLOCKED: libholm_runtime.so missing (run holm-runtime/build-android.ps1). No silent non-wgpu stack."
        } else {
            "kmp-wgpu BLOCKED: holm_runtime loaded but has_wgpu=0. No silent GLES stand-in."
        }
    }

    override fun destroyDevice(handle: WgpuHandle) {
        if (handle == 0L || !isLibraryLoaded()) return
        runCatching { nativeDestroy(handle) }
    }

    fun createDeviceFromSurface(surface: Surface, width: Int, height: Int): Device? {
        if (!isLibraryLoaded()) return null
        val handle = runCatching {
            nativeCreate(surface, width.coerceAtLeast(1), height.coerceAtLeast(1))
        }.getOrDefault(0L)
        if (handle == 0L) return null
        return Device(handle, this)
    }

    fun abiVersion(): Int {
        if (!isLibraryLoaded()) return 0
        return runCatching { nativeAbiVersion() }.getOrDefault(0)
    }

    fun lastCreateError(): String {
        if (!isLibraryLoaded()) return ""
        return runCatching { nativeLastCreateError() }.getOrDefault("")
    }

    fun resize(handle: WgpuHandle, width: Int, height: Int) {
        if (handle == 0L) return
        runCatching { nativeResize(handle, width, height) }
    }

    fun tick(handle: WgpuHandle, dtSecs: Float): Boolean {
        if (handle == 0L) return false
        return runCatching { nativeTick(handle, dtSecs) }.getOrDefault(false)
    }

    fun setCustomFs(handle: WgpuHandle, fs: String): Boolean {
        if (handle == 0L) return false
        return runCatching { nativeSetCustomFs(handle, fs) }.getOrDefault(false)
    }

    fun setScene(handle: WgpuHandle, scene: String) {
        if (handle == 0L) return
        runCatching { nativeSetScene(handle, scene) }
    }

    fun setLiquid(handle: WgpuHandle, boost: Float, bend: Float) {
        if (handle == 0L) return
        runCatching { nativeSetLiquid(handle, boost, bend) }
    }

    fun setPlaying(handle: WgpuHandle, playing: Boolean) {
        if (handle == 0L) return
        runCatching { nativeSetPlaying(handle, playing) }
    }

    fun backendId(handle: WgpuHandle): String {
        if (handle == 0L) return "wgpu-blocked"
        return runCatching { nativeBackendId(handle) }.getOrDefault("wgpu-m0")
    }

    fun lastErrorCode(handle: WgpuHandle): String {
        if (handle == 0L) return ""
        return runCatching { nativeLastErrorCode(handle) }.getOrDefault("")
    }

    fun lastErrorMessage(handle: WgpuHandle): String {
        if (handle == 0L) return ""
        return runCatching { nativeLastErrorMessage(handle) }.getOrDefault("")
    }

    fun drainErrors(handle: WgpuHandle): List<Pair<String, String>> {
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

    private external fun nativeAbiVersion(): Int
    private external fun nativeHasWgpu(): Int
    private external fun nativeCreate(surface: Surface, width: Int, height: Int): Long
    private external fun nativeLastCreateError(): String
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

actual object WgpuRuntime {
    actual fun createInstance(): Instance = Instance(AndroidWgpuPlatform)

    actual fun isLibraryLoaded(): Boolean = AndroidWgpuPlatform.isLibraryLoaded()

    actual fun isAvailable(): Boolean = AndroidWgpuPlatform.isAvailable()

    actual fun blockedMessage(): String = AndroidWgpuPlatform.blockedMessage()
}

fun androidPlatform(): AndroidWgpuPlatform = AndroidWgpuPlatform
