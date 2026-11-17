package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: wgpu Present Provider（`executor=renderer-wgpu` → wgpu-m0*）。
 */
object WgpuBackendProvider : HolmBackendProvider {
    const val ID: String = "wgpu"

    override val id: String get() = ID
    override val executorKey: String get() = HolmExecutor.WGPU
}
