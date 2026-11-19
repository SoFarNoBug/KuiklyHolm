package com.jlj.kuiklybase.holm.gltf

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: KHR_draco 比特流解码钩子（expect；Android → kmp-draco）。
 */
internal expect fun decodeDracoBitstream(bytes: ByteArray): DracoDecodedMesh

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 解析层用的解压网格（与 kmp-draco DecodedMesh 对齐字段）。
 */
internal class DracoDecodedMesh(
    val numPoints: Int,
    val indices: IntArray,
    val attributes: List<DracoDecodedAttr>,
) {
    fun byUniqueId(uniqueId: Int): DracoDecodedAttr? =
        attributes.firstOrNull { it.uniqueId == uniqueId }
}

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 单属性（uniqueId = KHR_draco attributes 映射值）。
 */
internal class DracoDecodedAttr(
    val uniqueId: Int,
    val numComponents: Int,
    val values: FloatArray,
)
