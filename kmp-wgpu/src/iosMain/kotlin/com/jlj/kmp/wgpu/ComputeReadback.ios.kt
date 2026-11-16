package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: iOS headless Compute · 本波 C ABI 未导出 headless；诚实失败（与 Present 正交）。
 */

actual fun createHeadlessDevice(): HeadlessDevice? = null

actual fun createHeadlessDeviceAsync(onDone: (Result<HeadlessDevice>) -> Unit) {
    onDone(
        Result.failure(
            IllegalStateException("ComputeReadback: iOS C ABI has no headless yet (Present-only wave)"),
        ),
    )
}

actual fun HeadlessDevice.computeReadback(
    shaderWgsl: String,
    input: FloatArray,
    workgroupCountX: Int,
): Result<FloatArray> =
    Result.failure(IllegalStateException("ComputeReadback: iOS C ABI has no headless yet"))

actual fun HeadlessDevice.computeReadbackAsync(
    shaderWgsl: String,
    input: FloatArray,
    workgroupCountX: Int,
    onDone: (Result<FloatArray>) -> Unit,
) {
    onDone(Result.failure(IllegalStateException("ComputeReadback: iOS C ABI has no headless yet")))
}
