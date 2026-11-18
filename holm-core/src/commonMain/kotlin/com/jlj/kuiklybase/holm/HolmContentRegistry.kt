package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: Content 过 NVI 用 contentId；须在 Kuikly 线程访问（ADR 0016）。
 */
object HolmContentRegistry {
    private var _seq: Long = 0L
    private val _map = mutableMapOf<String, HolmContent>()

    fun register(content: HolmContent): String {
        _seq += 1L
        val id = "hc-$_seq"
        _map[id] = content
        return id
    }

    fun get(id: String): HolmContent? {
        if (id.isEmpty()) return null
        return _map[id]
    }

    fun unregister(id: String) {
        if (id.isEmpty()) return
        _map.remove(id)
    }

    fun replace(oldId: String?, content: HolmContent?): String {
        if (oldId != null) {
            unregister(oldId)
        }
        if (content == null) {
            return ""
        }
        return register(content)
    }
}
