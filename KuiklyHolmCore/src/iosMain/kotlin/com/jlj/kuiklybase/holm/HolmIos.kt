package com.jlj.kuiklybase.holm

import kotlin.concurrent.Volatile

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: iOS 宿主初始化占位。Runtime=Metal C Session；Lite=GLES2 Shadertoy（iosApp KRHolmSurface）；viewName=KRHolmSurface。
 */
object HolmIos {
    @Volatile
    private var _cacheDir: String? = null

    fun init(cacheDir: String) {
        _cacheDir = cacheDir
    }

    fun cacheDirOrNull(): String? = _cacheDir

    /** 官方五席已删；Lite/Runtime 均由宿主 fragmentShader（GLSL / WGSL）提供。 */
    val dualTrackScenes: List<String> = emptyList()
}
