package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.HolmIos

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: 壳侧调用入口；转发 HolmIos.init（Swift 可见）。
 */
object HolmIosBridge {
    fun configure(cacheDir: String) {
        HolmIos.init(cacheDir)
    }
}
