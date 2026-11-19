package com.jlj.kuiklybase.holm

import kotlin.concurrent.Volatile

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: 鸿蒙宿主初始化；触发 umbrella Provider 注册。
 */
object HolmOhos {
    @Volatile
    private var _resfileDir: String? = null

    @Volatile
    private var _cacheDir: String? = null

    fun init(resfileDir: String, cacheDir: String = resfileDir) {
        HolmUmbrellaBootstrap.ensure()
        _resfileDir = resfileDir
        _cacheDir = cacheDir
    }

    /** Ability 注入的 resourceDir；可读性探测用。 */
    fun resfileDir(): String = _resfileDir.orEmpty()

    /** Ability 注入的 cacheDir；Demo 将 rawfile models 拷贝至此供 Model 读。 */
    fun cacheDir(): String = _cacheDir.orEmpty()
}
