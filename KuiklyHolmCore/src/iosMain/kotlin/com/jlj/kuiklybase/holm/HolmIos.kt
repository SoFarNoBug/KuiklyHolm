package com.jlj.kuiklybase.holm

import kotlin.concurrent.Volatile

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: iOS 宿主初始化；umbrella Provider 兜底；Runtime Present 经 HolmRuntimeIosBridge。
 */
object HolmIos {
    @Volatile
    private var _cacheDir: String? = null

    fun init(cacheDir: String) {
        _cacheDir = cacheDir
        HolmUmbrellaBootstrap.ensure()
    }

    fun cacheDirOrNull(): String? = _cacheDir

    /** 官方五席已删；Lite/Runtime 均由宿主 fragmentShader（GLSL / WGSL）提供。 */
    val dualTrackScenes: List<String> = emptyList()
}
