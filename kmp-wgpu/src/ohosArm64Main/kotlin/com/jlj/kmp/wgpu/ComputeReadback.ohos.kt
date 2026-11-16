package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Ohos headless Compute · 本波 stub（Present 对齐单；与 ADR 0013 v1.1 产品化正交）。
 */

actual fun createHeadlessDevice(): HeadlessDevice? = null

actual fun createHeadlessDeviceAsync(onDone: (Result<HeadlessDevice>) -> Unit) {
    onDone(
        Result.failure(
            IllegalStateException("ComputeReadback: Ohos headless TBD (Present-only wave)"),
        ),
    )
}

actual fun HeadlessDevice.computeReadback(
    shaderWgsl: String,
    input: FloatArray,
    workgroupCountX: Int,
): Result<FloatArray> =
    Result.failure(IllegalStateException("ComputeReadback: Ohos headless TBD"))

actual fun HeadlessDevice.computeReadbackAsync(
    shaderWgsl: String,
    input: FloatArray,
    workgroupCountX: Int,
    onDone: (Result<FloatArray>) -> Unit,
) {
    onDone(Result.failure(IllegalStateException("ComputeReadback: Ohos headless TBD")))
}
