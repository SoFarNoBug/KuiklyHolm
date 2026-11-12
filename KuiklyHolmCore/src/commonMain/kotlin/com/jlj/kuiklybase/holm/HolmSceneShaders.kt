package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: scene → 同源 GLES2/WebGL1 shader 路由（未知 scene 回退 frost）。
 */
object HolmSceneShaders {
    const val SCENE_FROST: String = "frost"
    const val SCENE_GLOW: String = "glow"
    const val SCENE_LIQUID_APPROX: String = "liquid_approx"
    const val SCENE_GRADIENT_BG: String = "gradient_bg"
    const val SCENE_FULLSCREEN_WARP: String = "fullscreen_warp"

    fun gles2Vertex(): String = FrostShaderSource.gles2Vertex

    fun gles2Fragment(scene: String): String = when (scene) {
        SCENE_GLOW -> GlowShaderSource.gles2Fragment
        SCENE_LIQUID_APPROX -> LiquidApproxShaderSource.gles2Fragment
        SCENE_GRADIENT_BG -> GradientBgShaderSource.gles2Fragment
        SCENE_FULLSCREEN_WARP -> FullscreenWarpShaderSource.gles2Fragment
        else -> FrostShaderSource.gles2Fragment
    }
}
