package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: iOS 宿主初始化占位。Lite 渲染由 iosApp `KRHolmSurface`（GLES2）承担；viewName=KRHolmSurface。
 */
object HolmIos {
    @Volatile
    private var _cacheDir: String? = null

    fun init(cacheDir: String) {
        _cacheDir = cacheDir
    }

    fun cacheDirOrNull(): String? = _cacheDir

    /** 官方五席已删；iOS 壳须宿主片元（本波暂未接 fragmentShader Attr，空则清屏+error）。 */
    val dualTrackScenes: List<String> = emptyList()
}
