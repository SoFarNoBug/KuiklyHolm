package com.jlj.kuiklybase.holm

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: Model Content 过 NVI 的编解码（H5 双包 Registry 不可达；对齐 Shader bridgeFragment；非业务公开契约）。
 */
object HolmModelNviCodec {

    /** fromBytes 过 NVI 上限（Base64）；更大资产请走 asset/fileUri。 */
    const val MAX_BYTES_B64_CHARS: Int = 6_000_000

    fun encodeLights(lights: List<HolmModelLight>): String {
        if (lights.isEmpty()) return ""
        return lights.joinToString(";") { light ->
            listOf(
                light.directionX,
                light.directionY,
                light.directionZ,
                light.colorR,
                light.colorG,
                light.colorB,
                light.intensity,
            ).joinToString(",")
        }
    }

    fun decodeLights(raw: String): List<HolmModelLight> {
        if (raw.isBlank()) return emptyList()
        return raw.split(';')
            .mapNotNull { part ->
                val p = part.split(',')
                if (p.size < 7) return@mapNotNull null
                val nums = FloatArray(7)
                for (i in 0 until 7) {
                    nums[i] = p[i].toFloatOrNull() ?: return@mapNotNull null
                }
                HolmModelLight(
                    directionX = nums[0],
                    directionY = nums[1],
                    directionZ = nums[2],
                    colorR = nums[3],
                    colorG = nums[4],
                    colorB = nums[5],
                    intensity = nums[6],
                )
            }
            .take(HolmModelContent.MAX_LIGHTS)
    }

    fun encodeColor(rgba: FloatArray?): String {
        if (rgba == null || rgba.size < 4) return ""
        return "${rgba[0]},${rgba[1]},${rgba[2]},${rgba[3]}"
    }

    fun decodeColor(raw: String): FloatArray? {
        if (raw.isBlank()) return null
        val p = raw.split(',')
        if (p.size < 4) return null
        val out = FloatArray(4)
        for (i in 0 until 4) {
            out[i] = p[i].toFloatOrNull() ?: return null
        }
        return out
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encodeBytesB64(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return ""
        val encoded = Base64.encode(bytes)
        if (encoded.length > MAX_BYTES_B64_CHARS) return ""
        return encoded
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBytesB64(raw: String): ByteArray? {
        if (raw.isBlank()) return null
        return runCatching { Base64.decode(raw) }.getOrNull()
    }
}
