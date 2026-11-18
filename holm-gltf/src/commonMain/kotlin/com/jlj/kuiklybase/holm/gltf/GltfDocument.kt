package com.jlj.kuiklybase.holm.gltf

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 最小 glTF 2.0 / GLB 解析结果（全静态 mesh；ADR 0016）。
 */
data class GltfDocument(
    val meshes: List<GltfMesh>,
    val baseColorFactor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val baseColorImage: GltfImage? = null,
) {
    companion object {
        /**
         * 解析 `.glb`（或嵌入 JSON+BIN 的 GLB）。失败抛 [GltfParseException]。
         */
        fun parse(bytes: ByteArray): GltfDocument = GltfGlbParser.parse(bytes)
    }
}

data class GltfMesh(
    val name: String,
    val positions: FloatArray,
    val normals: FloatArray,
    val uvs: FloatArray,
    val indices: IntArray,
)

data class GltfImage(
    val width: Int,
    val height: Int,
    /** RGBA8888 行优先 */
    val rgba: ByteArray,
)

class GltfParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
