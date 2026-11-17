package com.jlj.kuiklyholmui

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: ADR 0013 v1.1 Demo 桥：Android/H5 走 kmp-wgpu；其它端诚实不可用。
 */
expect fun runComputeReadbackSample(onDone: (Result<String>) -> Unit)
