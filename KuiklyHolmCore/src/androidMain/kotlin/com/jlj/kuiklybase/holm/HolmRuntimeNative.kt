package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Android 侧 holm_runtime(wgpu) 探测；无 .so / 未接 Surface 时记 BLOCKED。
 */
object HolmRuntimeNative {
    @Volatile
    private var _libraryLoaded: Boolean? = null

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
     * M0 已可呈现清屏/三角时才为 true。
     * 本波未接 Surface/ABI 绘制 → 恒 false，避免空 .so 假通面。
     */
    fun isM0Presentable(): Boolean = false

    /** 可宣称 `backend=wgpu-m0` 的充要条件。 */
    fun isAvailable(): Boolean = isLibraryLoaded() && isM0Presentable()

    fun blockedMessage(): String {
        return if (!isLibraryLoaded()) {
            "DualTrack B BLOCKED: libholm_runtime.so missing (install rustc/cargo, build holm-runtime). No silent non-wgpu stack."
        } else {
            "DualTrack B BLOCKED: holm_runtime loaded but M0 Surface/clear not wired yet. No silent GLES stand-in."
        }
    }
}
