package com.jlj.kuiklybase.holm

import kotlin.concurrent.Volatile

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: iOS 宿主初始化占位。Runtime 由 iosApp `KRHolmSurface` + holm-runtime C Session（ADR 0009）承担；viewName=KRHolmSurface。
 */
object HolmIos {
    @Volatile
    private var _cacheDir: String? = null

    fun init(cacheDir: String) {
        _cacheDir = cacheDir
    }

    fun cacheDirOrNull(): String? = _cacheDir

    /** 官方五席已删；Lite 片元后置（本波 Lite=LITE_PENDING）；Runtime 走宿主 WGSL。 */
    val dualTrackScenes: List<String> = emptyList()
}
