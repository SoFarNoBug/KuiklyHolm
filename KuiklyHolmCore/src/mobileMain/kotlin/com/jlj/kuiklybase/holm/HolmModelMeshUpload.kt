package com.jlj.kuiklybase.holm

import com.jlj.kuiklybase.holm.gltf.GltfDocument
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: glTF → 交错顶点/索引 + orbit MVP（GL/wgpu Model 共用；无 Android Matrix 依赖）。
 */
internal object HolmModelMeshUpload {
    data class Packed(
        val verts: FloatArray,
        val indices: IntArray,
        val center: FloatArray,
        val radius: Float,
        val color: FloatArray,
    )

    fun pack(doc: GltfDocument, colorOverride: FloatArray?): Packed {
        var vertCount = 0
        var indexCount = 0
        for (m in doc.meshes) {
            vertCount += m.positions.size / 3
            indexCount += m.indices.size
        }
        val verts = FloatArray(vertCount * 6)
        val indices = IntArray(indexCount)
        var vo = 0
        var io = 0
        var base = 0
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (m in doc.meshes) {
            val vc = m.positions.size / 3
            for (i in 0 until vc) {
                val x = m.positions[i * 3]
                val y = m.positions[i * 3 + 1]
                val z = m.positions[i * 3 + 2]
                verts[vo++] = x
                verts[vo++] = y
                verts[vo++] = z
                verts[vo++] = m.normals.getOrElse(i * 3) { 0f }
                verts[vo++] = m.normals.getOrElse(i * 3 + 1) { 1f }
                verts[vo++] = m.normals.getOrElse(i * 3 + 2) { 0f }
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (z < minZ) minZ = z
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                if (z > maxZ) maxZ = z
            }
            for (idx in m.indices) {
                indices[io++] = idx + base
            }
            base += vc
        }
        val center = floatArrayOf(
            (minX + maxX) * 0.5f,
            (minY + maxY) * 0.5f,
            (minZ + maxZ) * 0.5f,
        )
        val radius = max(maxX - minX, max(maxY - minY, maxZ - minZ)).coerceAtLeast(0.001f) * 0.6f
        val factor = doc.baseColorFactor
        val color = colorOverride?.copyOf(4)
            ?: floatArrayOf(factor[0], factor[1], factor[2], factor.getOrElse(3) { 1f })
        return Packed(verts, indices, center, radius, color)
    }

    fun mvp(
        out: FloatArray,
        center: FloatArray,
        radius: Float,
        yaw: Float,
        pitch: Float,
        aspect: Float,
    ) {
        val view = FloatArray(16)
        val proj = FloatArray(16)
        val eyeDist = radius * 3.2f
        val cy = cos(pitch.toDouble()).toFloat()
        val eyeX = center[0] + eyeDist * cy * sin(yaw.toDouble()).toFloat()
        val eyeY = center[1] + eyeDist * sin(pitch.toDouble()).toFloat()
        val eyeZ = center[2] + eyeDist * cy * cos(yaw.toDouble()).toFloat()
        setLookAtM(view, eyeX, eyeY, eyeZ, center[0], center[1], center[2], 0f, 1f, 0f)
        val zNear = (eyeDist * 0.05f).coerceAtLeast(0.01f)
        val zFar = (eyeDist * 10f).coerceAtLeast(zNear * 100f)
        perspectiveM(proj, 45f, aspect.coerceAtLeast(0.01f), zNear, zFar)
        multiplyMM(out, proj, view)
    }

    private fun setLookAtM(
        rm: FloatArray,
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
    ) {
        var fx = centerX - eyeX
        var fy = centerY - eyeY
        var fz = centerZ - eyeZ
        val rlf = 1f / kotlin.math.sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-8f)
        fx *= rlf
        fy *= rlf
        fz *= rlf
        var sx = fy * upZ - fz * upY
        var sy = fz * upX - fx * upZ
        var sz = fx * upY - fy * upX
        val rls = 1f / kotlin.math.sqrt(sx * sx + sy * sy + sz * sz).coerceAtLeast(1e-8f)
        sx *= rls
        sy *= rls
        sz *= rls
        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx
        rm[0] = sx
        rm[1] = ux
        rm[2] = -fx
        rm[3] = 0f
        rm[4] = sy
        rm[5] = uy
        rm[6] = -fy
        rm[7] = 0f
        rm[8] = sz
        rm[9] = uz
        rm[10] = -fz
        rm[11] = 0f
        rm[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ)
        rm[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ)
        rm[14] = fx * eyeX + fy * eyeY + fz * eyeZ
        rm[15] = 1f
    }

    private fun perspectiveM(
        m: FloatArray,
        fovyDeg: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float,
    ) {
        val f = 1f / tan((fovyDeg * (PI / 180.0) / 2.0)).toFloat()
        val rangeInv = 1f / (zNear - zFar)
        m[0] = f / aspect
        m[1] = 0f
        m[2] = 0f
        m[3] = 0f
        m[4] = 0f
        m[5] = f
        m[6] = 0f
        m[7] = 0f
        m[8] = 0f
        m[9] = 0f
        m[10] = (zFar + zNear) * rangeInv
        m[11] = -1f
        m[12] = 0f
        m[13] = 0f
        m[14] = 2f * zFar * zNear * rangeInv
        m[15] = 0f
    }

    private fun multiplyMM(result: FloatArray, lhs: FloatArray, rhs: FloatArray) {
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                result[c * 4 + r] =
                    lhs[r] * rhs[c * 4] +
                        lhs[4 + r] * rhs[c * 4 + 1] +
                        lhs[8 + r] * rhs[c * 4 + 2] +
                        lhs[12 + r] * rhs[c * 4 + 3]
            }
        }
    }
}
