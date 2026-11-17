package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: v1 Device=Session 粗粒度能力块（非逐 draw FFI）。
 * 不含 setCustomFs / setScene / liquid / playing（Holm 适配层自管）。
 */

expect fun Device.sessionTick(dtSecs: Float): Boolean

expect fun Device.sessionResize(width: Int, height: Int)

expect fun Device.sessionBackendId(): String

expect fun Device.sessionLastErrorCode(): String

expect fun Device.sessionLastErrorMessage(): String

expect fun Device.sessionDrainErrors(): List<Pair<String, String>>
