package com.jlj.kuiklybase.holm.gltf

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 最小 GLB（glTF 2.0）解析：全静态 mesh + baseColor 因子。
 */
internal object GltfGlbParser {
    private const val MAGIC = 0x46546C67 // glTF
    private const val CHUNK_JSON = 0x4E4F534A
    private const val CHUNK_BIN = 0x004E4942

    fun parse(bytes: ByteArray): GltfDocument {
        if (bytes.size < 12) {
            throw GltfParseException("GLB too short")
        }
        val magic = readU32(bytes, 0)
        val version = readU32(bytes, 4)
        val length = readU32(bytes, 8)
        if (magic != MAGIC) {
            throw GltfParseException("Not a GLB (bad magic)")
        }
        if (version != 2) {
            throw GltfParseException("Unsupported glTF version $version")
        }
        if (length > bytes.size) {
            throw GltfParseException("GLB length exceeds buffer")
        }
        var off = 12
        var jsonText: String? = null
        var bin: ByteArray? = null
        while (off + 8 <= length) {
            val chunkLen = readU32(bytes, off)
            val chunkType = readU32(bytes, off + 4)
            off += 8
            if (off + chunkLen > length) {
                throw GltfParseException("Chunk overruns GLB")
            }
            when (chunkType) {
                CHUNK_JSON -> {
                    jsonText = bytes.decodeToString(off, off + chunkLen).trimEnd('\u0000')
                }
                CHUNK_BIN -> {
                    bin = bytes.copyOfRange(off, off + chunkLen)
                }
            }
            off += chunkLen
            // chunks are 4-byte aligned; length already includes padding in chunkLen for JSON
        }
        val json = jsonText ?: throw GltfParseException("Missing JSON chunk")
        val root = GltfJson.parseObject(json)
        return assemble(root, bin ?: ByteArray(0))
    }

    private fun assemble(root: GltfJson.Obj, bin: ByteArray): GltfDocument {
        val accessors = root.arr("accessors")
        val bufferViews = root.arr("bufferViews")
        val meshesJson = root.arr("meshes")
        val nodes = root.arr("nodes")
        val scenes = root.arr("scenes")
        val materials = root.arr("materials")

        val requiredExt = root.values("extensionsRequired").mapNotNull { (it as? GltfJson.Str)?.v }
        val needsDraco = requiredExt.any { it == "KHR_draco_mesh_compression" }

        val baseColorFactor = floatArrayOf(1f, 1f, 1f, 1f)
        if (materials.isNotEmpty()) {
            val pbr = materials[0].obj("pbrMetallicRoughness")
            val f = pbr?.values("baseColorFactor") ?: emptyList()
            if (f.size >= 3) {
                baseColorFactor[0] = f[0].asFloat(1f)
                baseColorFactor[1] = f[1].asFloat(1f)
                baseColorFactor[2] = f[2].asFloat(1f)
                if (f.size >= 4) baseColorFactor[3] = f[3].asFloat(1f)
            }
        }

        val sceneIndex = root.int("scene", 0)
        val rootNodes: List<Int> = if (scenes.isNotEmpty() && sceneIndex < scenes.size) {
            scenes[sceneIndex].ints("nodes")
        } else {
            nodes.indices.toList()
        }

        val out = mutableListOf<GltfMesh>()
        fun walk(nodeIndex: Int, parent: Mat4) {
            if (nodeIndex < 0 || nodeIndex >= nodes.size) return
            val node = nodes[nodeIndex]
            val local = nodeMatrix(node)
            val world = parent.mul(local)
            val meshIndex = node.int("mesh", -1)
            if (meshIndex >= 0 && meshIndex < meshesJson.size) {
                val mesh = meshesJson[meshIndex]
                val name = mesh.str("name", "mesh$meshIndex")
                val primitives = mesh.arr("primitives")
                for ((pi, prim) in primitives.withIndex()) {
                    val attrs = prim.obj("attributes") ?: continue
                    val posAcc = attrs.int("POSITION", -1)
                    if (posAcc < 0) continue
                    val dracoExt = prim.obj("extensions")?.obj("KHR_draco_mesh_compression")
                    val positions: FloatArray
                    var normals: FloatArray
                    val uvs: FloatArray
                    val indices: IntArray
                    val hadNormals: Boolean
                    if (dracoExt != null) {
                        val decoded = decodeDracoPrimitive(dracoExt, bufferViews, bin)
                        val attrMap = dracoExt.obj("attributes")
                        positions = floatAttrFromDraco(decoded, attrMap, "POSITION", 3)
                            ?: throw GltfParseException("KHR_draco missing POSITION")
                        val nFromDraco = floatAttrFromDraco(decoded, attrMap, "NORMAL", 3)
                        hadNormals = nFromDraco != null
                        normals = nFromDraco ?: FloatArray(positions.size)
                        uvs = floatAttrFromDraco(decoded, attrMap, "TEXCOORD_0", 2)
                            ?: FloatArray(positions.size / 3 * 2)
                        indices = if (decoded.indices.isNotEmpty()) {
                            decoded.indices
                        } else {
                            IntArray(positions.size / 3) { it }
                        }
                    } else {
                        val posView = accessors.getOrNull(posAcc)?.int("bufferView", -1) ?: -1
                        if (posView < 0) {
                            throw GltfParseException(
                                "POSITION accessor $posAcc has no bufferView and no KHR_draco",
                            )
                        }
                        positions = readFloat3Accessor(accessors, bufferViews, bin, posAcc)
                        val nAcc = attrs.int("NORMAL", -1)
                        hadNormals = nAcc >= 0
                        normals = if (nAcc >= 0) {
                            readFloat3Accessor(accessors, bufferViews, bin, nAcc)
                        } else {
                            FloatArray(positions.size)
                        }
                        val uvAcc = attrs.int("TEXCOORD_0", -1)
                        uvs = if (uvAcc >= 0) {
                            readFloat2Accessor(accessors, bufferViews, bin, uvAcc)
                        } else {
                            FloatArray(positions.size / 3 * 2)
                        }
                        val indAcc = prim.int("indices", -1)
                        indices = if (indAcc >= 0) {
                            readIndices(accessors, bufferViews, bin, indAcc)
                        } else {
                            IntArray(positions.size / 3) { it }
                        }
                    }
                    if (!hadNormals) {
                        computeFlatNormals(positions, indices, normals)
                    }
                    transformPositions(positions, world)
                    transformNormals(normals, world)
                    out += GltfMesh(
                        name = "$name#$pi",
                        positions = positions,
                        normals = normals,
                        uvs = uvs,
                        indices = indices,
                    )
                }
            }
            for (c in node.ints("children")) {
                walk(c, world)
            }
        }
        val identity = Mat4.identity()
        for (ni in rootNodes) {
            walk(ni, identity)
        }
        if (out.isEmpty()) {
            val hint = if (needsDraco) " (extensionsRequired includes KHR_draco_mesh_compression)" else ""
            throw GltfParseException("No mesh primitives found$hint")
        }
        return GltfDocument(meshes = out, baseColorFactor = baseColorFactor, baseColorImage = null)
    }

    private fun decodeDracoPrimitive(
        dracoExt: GltfJson.Obj,
        views: List<GltfJson.Obj>,
        bin: ByteArray,
    ): DracoDecodedMesh {
        val viewIndex = dracoExt.int("bufferView", -1)
        if (viewIndex < 0 || viewIndex >= views.size) {
            throw GltfParseException("KHR_draco bufferView missing or OOB: $viewIndex")
        }
        val view = views[viewIndex]
        val offset = view.int("byteOffset", 0)
        val length = view.int("byteLength", -1)
        if (length < 0 || offset < 0 || offset + length > bin.size) {
            throw GltfParseException("KHR_draco bufferView slice OOB offset=$offset length=$length")
        }
        val slice = bin.copyOfRange(offset, offset + length)
        return decodeDracoBitstream(slice)
    }

    private fun floatAttrFromDraco(
        decoded: DracoDecodedMesh,
        attrMap: GltfJson.Obj?,
        semantic: String,
        components: Int,
    ): FloatArray? {
        val uid = attrMap?.int(semantic, -1) ?: -1
        if (uid < 0) return null
        val attr = decoded.byUniqueId(uid) ?: return null
        if (attr.numComponents != components) {
            // Allow wider attrs; take leading components
            if (attr.numComponents < components) {
                throw GltfParseException(
                    "KHR_draco $semantic uniqueId=$uid comps=${attr.numComponents} < $components",
                )
            }
            val n = decoded.numPoints
            val out = FloatArray(n * components)
            val src = attr.values
            val srcComps = attr.numComponents
            for (i in 0 until n) {
                for (c in 0 until components) {
                    out[i * components + c] = src[i * srcComps + c]
                }
            }
            return out
        }
        return attr.values
    }

    private fun nodeMatrix(node: GltfJson.Obj): Mat4 {
        val m = node.values("matrix")
        if (m.size == 16) {
            val a = FloatArray(16) { i -> m[i].asFloat(0f) }
            return Mat4(a)
        }
        val t = node.values("translation")
        val r = node.values("rotation")
        val s = node.values("scale")
        val tx = if (t.size >= 3) t[0].asFloat(0f) else 0f
        val ty = if (t.size >= 3) t[1].asFloat(0f) else 0f
        val tz = if (t.size >= 3) t[2].asFloat(0f) else 0f
        val qx = if (r.size >= 4) r[0].asFloat(0f) else 0f
        val qy = if (r.size >= 4) r[1].asFloat(0f) else 0f
        val qz = if (r.size >= 4) r[2].asFloat(0f) else 0f
        val qw = if (r.size >= 4) r[3].asFloat(1f) else 1f
        val sx = if (s.size >= 3) s[0].asFloat(1f) else 1f
        val sy = if (s.size >= 3) s[1].asFloat(1f) else 1f
        val sz = if (s.size >= 3) s[2].asFloat(1f) else 1f
        return Mat4.fromTrs(tx, ty, tz, qx, qy, qz, qw, sx, sy, sz)
    }

    private fun readFloat3Accessor(
        accessors: List<GltfJson.Obj>,
        views: List<GltfJson.Obj>,
        bin: ByteArray,
        index: Int,
    ): FloatArray {
        val acc = accessors[index]
        val count = acc.int("count", 0)
        val out = FloatArray(count * 3)
        readAccessorFloats(acc, views, bin, out, 3)
        return out
    }

    private fun readFloat2Accessor(
        accessors: List<GltfJson.Obj>,
        views: List<GltfJson.Obj>,
        bin: ByteArray,
        index: Int,
    ): FloatArray {
        val acc = accessors[index]
        val count = acc.int("count", 0)
        val out = FloatArray(count * 2)
        readAccessorFloats(acc, views, bin, out, 2)
        return out
    }

    private fun readAccessorFloats(
        acc: GltfJson.Obj,
        views: List<GltfJson.Obj>,
        bin: ByteArray,
        out: FloatArray,
        comps: Int,
    ) {
        val viewIndex = acc.int("bufferView", -1)
        if (viewIndex < 0 || viewIndex >= views.size) return
        val view = views[viewIndex]
        val viewOffset = view.int("byteOffset", 0)
        val byteStride = view.int("byteStride", 0)
        val accOffset = acc.int("byteOffset", 0)
        val componentType = acc.int("componentType", 5126) // FLOAT
        val count = acc.int("count", 0)
        val elemSize = when (componentType) {
            5126 -> 4 * comps
            5121, 5120 -> comps
            5123, 5122 -> 2 * comps
            5125 -> 4 * comps
            else -> 4 * comps
        }
        val stride = if (byteStride > 0) byteStride else elemSize
        var base = viewOffset + accOffset
        for (i in 0 until count) {
            var p = base
            for (c in 0 until comps) {
                val v = when (componentType) {
                    5126 -> readF32(bin, p).also { p += 4 }
                    5121 -> (bin.getOrElse(p) { 0 }.toInt() and 0xff).toFloat().also { p += 1 }
                    5123 -> readU16(bin, p).toFloat().also { p += 2 }
                    5125 -> readU32(bin, p).toFloat().also { p += 4 }
                    else -> 0f
                }
                out[i * comps + c] = v
            }
            base += stride
        }
    }

    private fun readIndices(
        accessors: List<GltfJson.Obj>,
        views: List<GltfJson.Obj>,
        bin: ByteArray,
        index: Int,
    ): IntArray {
        val acc = accessors[index]
        val count = acc.int("count", 0)
        val viewIndex = acc.int("bufferView", -1)
        if (viewIndex < 0 || viewIndex >= views.size) return IntArray(0)
        val view = views[viewIndex]
        val base = view.int("byteOffset", 0) + acc.int("byteOffset", 0)
        val componentType = acc.int("componentType", 5123)
        val out = IntArray(count)
        when (componentType) {
            5121 -> for (i in 0 until count) out[i] = bin[base + i].toInt() and 0xff
            5123 -> for (i in 0 until count) out[i] = readU16(bin, base + i * 2)
            5125 -> for (i in 0 until count) out[i] = readU32(bin, base + i * 4)
            else -> for (i in 0 until count) out[i] = readU16(bin, base + i * 2)
        }
        return out
    }

    private fun computeFlatNormals(pos: FloatArray, indices: IntArray, normals: FloatArray) {
        var i = 0
        while (i + 2 < indices.size) {
            val i0 = indices[i] * 3
            val i1 = indices[i + 1] * 3
            val i2 = indices[i + 2] * 3
            val ax = pos[i1] - pos[i0]
            val ay = pos[i1 + 1] - pos[i0 + 1]
            val az = pos[i1 + 2] - pos[i0 + 2]
            val bx = pos[i2] - pos[i0]
            val by = pos[i2 + 1] - pos[i0 + 1]
            val bz = pos[i2 + 2] - pos[i0 + 2]
            var nx = ay * bz - az * by
            var ny = az * bx - ax * bz
            var nz = ax * by - ay * bx
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 1e-8f) {
                nx /= len
                ny /= len
                nz /= len
            }
            for (k in 0..2) {
                val vi = indices[i + k] * 3
                normals[vi] = nx
                normals[vi + 1] = ny
                normals[vi + 2] = nz
            }
            i += 3
        }
    }

    private fun transformPositions(pos: FloatArray, m: Mat4) {
        var i = 0
        while (i + 2 < pos.size) {
            val x = pos[i]
            val y = pos[i + 1]
            val z = pos[i + 2]
            pos[i] = m.a[0] * x + m.a[4] * y + m.a[8] * z + m.a[12]
            pos[i + 1] = m.a[1] * x + m.a[5] * y + m.a[9] * z + m.a[13]
            pos[i + 2] = m.a[2] * x + m.a[6] * y + m.a[10] * z + m.a[14]
            i += 3
        }
    }

    private fun transformNormals(n: FloatArray, m: Mat4) {
        // upper-left 3x3; skip translation
        var i = 0
        while (i + 2 < n.size) {
            val x = n[i]
            val y = n[i + 1]
            val z = n[i + 2]
            var nx = m.a[0] * x + m.a[4] * y + m.a[8] * z
            var ny = m.a[1] * x + m.a[5] * y + m.a[9] * z
            var nz = m.a[2] * x + m.a[6] * y + m.a[10] * z
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 1e-8f) {
                nx /= len
                ny /= len
                nz /= len
            }
            n[i] = nx
            n[i + 1] = ny
            n[i + 2] = nz
            i += 3
        }
    }

    private fun readU32(b: ByteArray, o: Int): Int {
        return (b[o].toInt() and 0xff) or
            ((b[o + 1].toInt() and 0xff) shl 8) or
            ((b[o + 2].toInt() and 0xff) shl 16) or
            ((b[o + 3].toInt() and 0xff) shl 24)
    }

    private fun readU16(b: ByteArray, o: Int): Int {
        return (b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8)
    }

    private fun readF32(b: ByteArray, o: Int): Float {
        return Float.fromBits(readU32(b, o))
    }
}

/** Column-major 4x4. */
internal class Mat4(val a: FloatArray) {
    fun mul(r: Mat4): Mat4 {
        val o = FloatArray(16)
        for (c in 0..3) {
            for (row in 0..3) {
                o[c * 4 + row] =
                    a[0 * 4 + row] * r.a[c * 4 + 0] +
                        a[1 * 4 + row] * r.a[c * 4 + 1] +
                        a[2 * 4 + row] * r.a[c * 4 + 2] +
                        a[3 * 4 + row] * r.a[c * 4 + 3]
            }
        }
        return Mat4(o)
    }

    companion object {
        fun identity(): Mat4 = Mat4(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ),
        )

        fun fromTrs(
            tx: Float, ty: Float, tz: Float,
            qx: Float, qy: Float, qz: Float, qw: Float,
            sx: Float, sy: Float, sz: Float,
        ): Mat4 {
            val x2 = qx + qx
            val y2 = qy + qy
            val z2 = qz + qz
            val xx = qx * x2
            val xy = qx * y2
            val xz = qx * z2
            val yy = qy * y2
            val yz = qy * z2
            val zz = qz * z2
            val wx = qw * x2
            val wy = qw * y2
            val wz = qw * z2
            val m = FloatArray(16)
            m[0] = (1f - (yy + zz)) * sx
            m[1] = (xy + wz) * sx
            m[2] = (xz - wy) * sx
            m[3] = 0f
            m[4] = (xy - wz) * sy
            m[5] = (1f - (xx + zz)) * sy
            m[6] = (yz + wx) * sy
            m[7] = 0f
            m[8] = (xz + wy) * sz
            m[9] = (yz - wx) * sz
            m[10] = (1f - (xx + yy)) * sz
            m[11] = 0f
            m[12] = tx
            m[13] = ty
            m[14] = tz
            m[15] = 1f
            return Mat4(m)
        }
    }
}
