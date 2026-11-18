package com.jlj.kuiklybase.holm.gltf

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 程序生成最小立方体 GLB（Demo hero；无外置版权资产）。
 */
object GltfMinimalGlb {
    /**
     * 单位立方体（中心原点，边长 1），单 mesh、无贴图。
     */
    fun buildHeroCubeGlb(): ByteArray {
        // 8 vertices of cube [-0.5,0.5]^3
        val positions = floatArrayOf(
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,
        )
        val normals = floatArrayOf(
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
        )
        // expand to per-vertex matching positions (approx; faces share)
        val nExpand = FloatArray(positions.size)
        // simple outward-ish normals by position
        for (i in 0 until positions.size / 3) {
            val x = positions[i * 3]
            val y = positions[i * 3 + 1]
            val z = positions[i * 3 + 2]
            val len = kotlin.math.sqrt(x * x + y * y + z * z).coerceAtLeast(1e-6f)
            nExpand[i * 3] = x / len
            nExpand[i * 3 + 1] = y / len
            nExpand[i * 3 + 2] = z / len
        }
        val uvs = FloatArray(8 * 2) { 0f }
        val indices = intArrayOf(
            0, 1, 2, 0, 2, 3,
            1, 5, 6, 1, 6, 2,
            5, 4, 7, 5, 7, 6,
            4, 0, 3, 4, 3, 7,
            3, 2, 6, 3, 6, 7,
            4, 5, 1, 4, 1, 0,
        )

        val bin = ByteArray(
            positions.size * 4 + nExpand.size * 4 + uvs.size * 4 + indices.size * 2,
        )
        var o = 0
        fun putF(v: Float) {
            val bits = v.toRawBits()
            bin[o++] = (bits and 0xff).toByte()
            bin[o++] = ((bits shr 8) and 0xff).toByte()
            bin[o++] = ((bits shr 16) and 0xff).toByte()
            bin[o++] = ((bits shr 24) and 0xff).toByte()
        }
        fun putU16(v: Int) {
            bin[o++] = (v and 0xff).toByte()
            bin[o++] = ((v shr 8) and 0xff).toByte()
        }
        val posOff = 0
        for (v in positions) putF(v)
        val nrmOff = o
        for (v in nExpand) putF(v)
        val uvOff = o
        for (v in uvs) putF(v)
        // align indices to 4
        while (o % 4 != 0) bin[o++] = 0
        val indOff = o
        for (v in indices) putU16(v)
        while (o % 4 != 0) {
            if (o >= bin.size) break
            bin[o++] = 0
        }
        val binLen = o
        val binTrim = bin.copyOf(binLen)

        val json = """
            {"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],
            "meshes":[{"name":"hero","primitives":[{"attributes":{"POSITION":0,"NORMAL":1,"TEXCOORD_0":2},"indices":3,"material":0}]}],
            "materials":[{"pbrMetallicRoughness":{"baseColorFactor":[0.85,0.55,0.25,1],
            "metallicFactor":0,"roughnessFactor":1}}],
            "accessors":[
            {"bufferView":0,"componentType":5126,"count":8,"type":"VEC3","max":[0.5,0.5,0.5],"min":[-0.5,-0.5,-0.5]},
            {"bufferView":1,"componentType":5126,"count":8,"type":"VEC3"},
            {"bufferView":2,"componentType":5126,"count":8,"type":"VEC2"},
            {"bufferView":3,"componentType":5123,"count":36,"type":"SCALAR"}
            ],
            "bufferViews":[
            {"buffer":0,"byteOffset":$posOff,"byteLength":${positions.size * 4},"target":34962},
            {"buffer":0,"byteOffset":$nrmOff,"byteLength":${nExpand.size * 4},"target":34962},
            {"buffer":0,"byteOffset":$uvOff,"byteLength":${uvs.size * 4},"target":34962},
            {"buffer":0,"byteOffset":$indOff,"byteLength":${indices.size * 2},"target":34963}
            ],
            "buffers":[{"byteLength":$binLen}]
            }
        """.trimIndent().replace("\n", "").replace(" ", "")

        val jsonBytes = json.encodeToByteArray()
        val jsonPad = (4 - jsonBytes.size % 4) % 4
        val binPad = (4 - binTrim.size % 4) % 4
        val total = 12 + 8 + jsonBytes.size + jsonPad + 8 + binTrim.size + binPad
        val out = ByteArray(total)
        var p = 0
        fun putU32(v: Int) {
            out[p++] = (v and 0xff).toByte()
            out[p++] = ((v shr 8) and 0xff).toByte()
            out[p++] = ((v shr 16) and 0xff).toByte()
            out[p++] = ((v shr 24) and 0xff).toByte()
        }
        putU32(0x46546C67)
        putU32(2)
        putU32(total)
        putU32(jsonBytes.size + jsonPad)
        putU32(0x4E4F534A)
        jsonBytes.copyInto(out, p)
        p += jsonBytes.size
        repeat(jsonPad) { out[p++] = 0x20 }
        putU32(binTrim.size + binPad)
        putU32(0x004E4942)
        binTrim.copyInto(out, p)
        p += binTrim.size
        repeat(binPad) { out[p++] = 0 }
        return out
    }
}
