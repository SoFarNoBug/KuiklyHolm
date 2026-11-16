package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Lite GL 族 Provider（`executor=lite` → gles2 / webgl）。
 */
object GlBackendProvider : HolmBackendProvider {
    const val ID: String = "gl"

    override val id: String get() = ID
    override val executorKey: String get() = HolmExecutor.LITE
}
