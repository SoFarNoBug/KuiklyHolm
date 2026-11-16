package com.jlj.kuiklyholmui

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: iOS 未纳入 ADR 0013 v1.1 范围。
 */
actual fun runComputeReadbackSample(onDone: (Result<String>) -> Unit) {
    onDone(Result.failure(IllegalStateException("ComputeReadback v1.1: Android+H5 only (iOS TBD)")))
}
