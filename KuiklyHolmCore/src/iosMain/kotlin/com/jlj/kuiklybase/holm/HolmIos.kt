package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: iOS 宿主初始化占位。
 */
object HolmIos {
    @Volatile
    private var _cacheDir: String? = null

    fun init(cacheDir: String) {
        _cacheDir = cacheDir
    }

    fun cacheDirOrNull(): String? = _cacheDir
}
