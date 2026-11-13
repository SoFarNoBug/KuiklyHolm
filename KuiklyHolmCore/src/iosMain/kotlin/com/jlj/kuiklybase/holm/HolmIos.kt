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

    /** DualTrack A：iOS 本波验收 scene 集合（非五席全量）。 */
    val dualTrackScenes: List<String> = listOf(
        HolmSceneShaders.SCENE_FROST,
        HolmSceneShaders.SCENE_LIQUID_APPROX,
    )
}
