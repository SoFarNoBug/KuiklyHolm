package com.jlj.kuiklybase.holm.gltf

import com.jlj.kmp.draco.Draco
import com.jlj.kmp.draco.DracoDecodeException

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: Ohos actual：硬依赖 kmp-draco（ADR 0019）；App 须装 libkmp_draco_native.so。
 */
internal actual fun decodeDracoBitstream(bytes: ByteArray): DracoDecodedMesh {
    try {
        val mesh = Draco.decode(bytes)
        return DracoDecodedMesh(
            numPoints = mesh.numPoints,
            indices = mesh.indices,
            attributes = mesh.attributes.map { a ->
                DracoDecodedAttr(
                    uniqueId = a.uniqueId,
                    numComponents = a.numComponents,
                    values = a.values,
                )
            },
        )
    } catch (e: DracoDecodeException) {
        throw GltfParseException("KHR_draco decode failed: ${e.message}", e)
    }
}
