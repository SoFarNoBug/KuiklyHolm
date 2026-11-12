package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: 鸿蒙宿主初始化占位。
 */
object HolmOhos {
    @Volatile
    private var _resfileDir: String? = null

    @Volatile
    private var _cacheDir: String? = null

    fun init(resfileDir: String, cacheDir: String = resfileDir) {
        _resfileDir = resfileDir
        _cacheDir = cacheDir
    }
}
