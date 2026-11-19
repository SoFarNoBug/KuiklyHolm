package com.jlj.kuiklybase.holm.gltf

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: JS M0：无 kmp-draco native → fail-loud。
 */
internal actual fun decodeDracoBitstream(bytes: ByteArray): DracoDecodedMesh {
    throw GltfParseException("KHR_draco requires Android kmp-draco (M0)")
}
