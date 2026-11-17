package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: iOS Device Session 能力块 → cinterop（无 Holm 产品语义）。
 */

actual fun Device.sessionTick(dtSecs: Float): Boolean =
    iosPlatform().tick(handle, dtSecs)

actual fun Device.sessionResize(width: Int, height: Int) {
    iosPlatform().resize(handle, width, height)
}

actual fun Device.sessionBackendId(): String =
    iosPlatform().backendId(handle)

actual fun Device.sessionLastErrorCode(): String =
    iosPlatform().lastErrorCode(handle)

actual fun Device.sessionLastErrorMessage(): String =
    iosPlatform().lastErrorMessage(handle)

actual fun Device.sessionDrainErrors(): List<Pair<String, String>> =
    iosPlatform().drainErrors(handle)
