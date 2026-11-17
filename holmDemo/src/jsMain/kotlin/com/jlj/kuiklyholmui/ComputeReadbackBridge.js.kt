package com.jlj.kuiklyholmui

import com.jlj.kmp.wgpu.ComputeReadbackSample

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: H5 headless Compute 冒烟 → kmp-wgpu（异步）。
 */
actual fun runComputeReadbackSample(onDone: (Result<String>) -> Unit) {
    ComputeReadbackSample.run { result ->
        onDone(
            result.map { out ->
                "ok size=${out.size} sample=${out.take(8).joinToString(",")}"
            },
        )
    }
}
