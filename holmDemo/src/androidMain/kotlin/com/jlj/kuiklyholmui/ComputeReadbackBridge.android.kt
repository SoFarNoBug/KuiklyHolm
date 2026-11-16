package com.jlj.kuiklyholmui

import android.os.Handler
import android.os.Looper
import com.jlj.kmp.wgpu.ComputeReadbackSample

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Android headless Compute 冒烟 → kmp-wgpu；Async 在后台，回主线程再 onDone。
 */
actual fun runComputeReadbackSample(onDone: (Result<String>) -> Unit) {
    val main = Handler(Looper.getMainLooper())
    ComputeReadbackSample.run { result ->
        val mapped = result.map { out ->
            "ok size=${out.size} sample=${out.take(8).joinToString(",")}"
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onDone(mapped)
        } else {
            main.post { onDone(mapped) }
        }
    }
}
