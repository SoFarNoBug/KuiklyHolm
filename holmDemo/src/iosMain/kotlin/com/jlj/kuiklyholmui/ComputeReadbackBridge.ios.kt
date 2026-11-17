package com.jlj.kuiklyholmui

import com.jlj.kmp.wgpu.ComputeReadbackSample

/**
 * @CreateDate: 2026/11/17
 * @Author: jlj
 * @Description: iOS headless Compute 冒烟 → kmp-wgpu C ABI（后台创建/回读，主线程 onDone）。
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
