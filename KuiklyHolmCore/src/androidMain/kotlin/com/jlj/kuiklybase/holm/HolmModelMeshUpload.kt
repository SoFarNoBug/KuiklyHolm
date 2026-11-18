package com.jlj.kuiklybase.holm

import android.opengl.Matrix
import com.jlj.kuiklybase.holm.gltf.GltfDocument
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: glTF → 交错顶点/索引 + orbit MVP（GL/wgpu Model 共用）。
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
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, center[0], center[1], center[2], 0f, 1f, 0f)
        Matrix.perspectiveM(proj, 0, 45f, aspect.coerceAtLeast(0.01f), 0.05f, eyeDist * 8f)
        Matrix.multiplyMM(out, 0, proj, 0, view, 0)
    }
}
