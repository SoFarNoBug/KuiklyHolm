package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: GL 族 Provider（`executor=renderer-gl` → gles2 / webgl）。
 */
object GlBackendProvider : HolmBackendProvider {
    const val ID: String = "gl"

    override val id: String get() = ID
    override val executorKey: String get() = HolmExecutor.GL
}
