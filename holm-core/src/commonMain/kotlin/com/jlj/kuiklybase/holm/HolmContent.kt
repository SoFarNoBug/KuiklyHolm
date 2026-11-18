package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 岛上「画什么」交付物（ADR 0016）；Surface 只挂 Content，不并列 fragmentShader。
 */
sealed interface HolmContent

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 片元内容；renderer-gl=Shadertoy 正文；renderer-wgpu=WGSL 片元段。
 */
class HolmShaderContent(
    val source: String,
    val vertexSource: String = "",
    val warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT,
    val scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED,
) : HolmContent

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 最小 glTF 英雄模型内容；真相机；可选轨道手势（ADR 0016）。
 */
class HolmModelContent private constructor(
    val assetPath: String?,
    val fileUri: String?,
    val bytes: ByteArray?,
    val albedoOverrideUri: String?,
    val solidColorRgba: FloatArray?,
    val lights: List<HolmModelLight>,
    orbitEnabled: Boolean,
    yaw: Float,
    pitch: Float,
) : HolmContent {
    private var _orbitEnabled: Boolean = orbitEnabled
    private var _yaw: Float = yaw
    private var _pitch: Float = pitch

    val orbitEnabled: Boolean get() = _orbitEnabled
    val yaw: Float get() = _yaw
    val pitch: Float get() = _pitch

    fun setOrbitEnabled(enabled: Boolean) {
        _orbitEnabled = enabled
    }

    fun setYaw(value: Float) {
        _yaw = value
    }

    fun setPitch(value: Float) {
        _pitch = value
    }

    companion object {
        const val MAX_LIGHTS: Int = 8

        fun fromAssets(
            path: String,
            albedoOverrideUri: String? = null,
            solidColorRgba: FloatArray? = null,
            lights: List<HolmModelLight> = emptyList(),
            orbitEnabled: Boolean = true,
            yaw: Float = 0f,
            pitch: Float = 0f,
        ): HolmModelContent = HolmModelContent(
            assetPath = path,
            fileUri = null,
            bytes = null,
            albedoOverrideUri = albedoOverrideUri,
            solidColorRgba = solidColorRgba,
            lights = lights.take(MAX_LIGHTS),
            orbitEnabled = orbitEnabled,
            yaw = yaw,
            pitch = pitch,
        )

        fun fromFileUri(
            uri: String,
            albedoOverrideUri: String? = null,
            solidColorRgba: FloatArray? = null,
            lights: List<HolmModelLight> = emptyList(),
            orbitEnabled: Boolean = true,
            yaw: Float = 0f,
            pitch: Float = 0f,
        ): HolmModelContent = HolmModelContent(
            assetPath = null,
            fileUri = uri,
            bytes = null,
            albedoOverrideUri = albedoOverrideUri,
            solidColorRgba = solidColorRgba,
            lights = lights.take(MAX_LIGHTS),
            orbitEnabled = orbitEnabled,
            yaw = yaw,
            pitch = pitch,
        )

        fun fromBytes(
            data: ByteArray,
            albedoOverrideUri: String? = null,
            solidColorRgba: FloatArray? = null,
            lights: List<HolmModelLight> = emptyList(),
            orbitEnabled: Boolean = true,
            yaw: Float = 0f,
            pitch: Float = 0f,
        ): HolmModelContent = HolmModelContent(
            assetPath = null,
            fileUri = null,
            bytes = data,
            albedoOverrideUri = albedoOverrideUri,
            solidColorRgba = solidColorRgba,
            lights = lights.take(MAX_LIGHTS),
            orbitEnabled = orbitEnabled,
            yaw = yaw,
            pitch = pitch,
        )
    }
}

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: Model 方向光（业务可选；不配则 unlit）。
 */
data class HolmModelLight(
    val directionX: Float,
    val directionY: Float,
    val directionZ: Float,
    val colorR: Float = 1f,
    val colorG: Float = 1f,
    val colorB: Float = 1f,
    val intensity: Float = 1f,
)
