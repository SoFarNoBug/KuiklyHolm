package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.HolmOhos

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: 鸿蒙壳侧初始化占位。
 */
object HolmOhosNative {
    fun configure(resfileDir: String, cacheDir: String) {
        HolmOhos.init(resfileDir, cacheDir)
    }
}
