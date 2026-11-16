package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: DualTrack 执行器常量；lite→holm-renderer-gl，runtime→holm-renderer-wgpu（经 Provider）。
 */
object HolmExecutor {
    const val LITE: String = "lite"
    const val RUNTIME: String = "runtime"

    const val BACKEND_GLES2: String = "gles2"
    const val BACKEND_WEBGL: String = "webgl"
    const val BACKEND_WGPU_M0: String = "wgpu-m0"
    const val BACKEND_WGPU_BLOCKED: String = "wgpu-blocked"

    const val ERROR_BLOCKED: String = "BLOCKED"

    fun normalize(raw: String?): String {
        val v = raw?.trim()?.lowercase().orEmpty()
        return if (v == RUNTIME) RUNTIME else LITE
    }

    fun isRuntime(raw: String?): Boolean = normalize(raw) == RUNTIME
}
