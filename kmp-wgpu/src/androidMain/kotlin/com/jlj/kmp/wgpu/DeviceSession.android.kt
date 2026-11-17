package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Android Device Session 能力块 → JNI（无 Holm 产品语义）。
 */

actual fun Device.sessionTick(dtSecs: Float): Boolean =
    androidPlatform().tick(handle, dtSecs)

actual fun Device.sessionResize(width: Int, height: Int) {
    androidPlatform().resize(handle, width, height)
}

actual fun Device.sessionBackendId(): String =
    androidPlatform().backendId(handle)

actual fun Device.sessionLastErrorCode(): String =
    androidPlatform().lastErrorCode(handle)

actual fun Device.sessionLastErrorMessage(): String =
    androidPlatform().lastErrorMessage(handle)

actual fun Device.sessionDrainErrors(): List<Pair<String, String>> =
    androidPlatform().drainErrors(handle)
