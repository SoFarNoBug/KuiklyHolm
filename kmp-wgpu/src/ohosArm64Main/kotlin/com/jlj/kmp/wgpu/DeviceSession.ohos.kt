package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Ohos Device Session 能力块 → cinterop（无 Holm 产品语义）。
 */

actual fun Device.sessionTick(dtSecs: Float): Boolean =
    ohosPlatform().tick(handle, dtSecs)

actual fun Device.sessionResize(width: Int, height: Int) {
    ohosPlatform().resize(handle, width, height)
}

actual fun Device.sessionBackendId(): String =
    ohosPlatform().backendId(handle)

actual fun Device.sessionLastErrorCode(): String =
    ohosPlatform().lastErrorCode(handle)

actual fun Device.sessionLastErrorMessage(): String =
    ohosPlatform().lastErrorMessage(handle)

actual fun Device.sessionDrainErrors(): List<Pair<String, String>> =
    ohosPlatform().drainErrors(handle)
