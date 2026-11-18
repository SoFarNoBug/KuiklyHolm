package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: 双渲染后端执行器常量；`renderer-gl`→holm-renderer-gl，`renderer-wgpu`→holm-renderer-wgpu（经 Provider）。
 */
object HolmExecutor {
    const val GL: String = "renderer-gl"
    const val WGPU: String = "renderer-wgpu"

    const val BACKEND_GLES2: String = "gles2"
    const val BACKEND_WEBGL: String = "webgl"
    const val BACKEND_WGPU_M0: String = "wgpu-m0"
    const val BACKEND_WGPU_BLOCKED: String = "wgpu-blocked"

    const val ERROR_BLOCKED: String = "BLOCKED"
    /** 非 Android 端尚未实现 Model Present（ADR 0016）。 */
    const val ERROR_MODEL_UNSUPPORTED: String = "MODEL_UNSUPPORTED"

    fun normalize(raw: String?): String {
        val v = raw?.trim()?.lowercase().orEmpty()
        return if (v == WGPU) WGPU else GL
    }

    fun isWgpu(raw: String?): Boolean = normalize(raw) == WGPU
}
