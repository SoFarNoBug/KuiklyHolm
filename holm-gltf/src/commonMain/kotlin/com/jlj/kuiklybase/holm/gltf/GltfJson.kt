package com.jlj.kuiklybase.holm.gltf

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: 极简 JSON 对象读取（仅 glTF 所需子集，无外部依赖）。
 */
internal object GltfJson {
    sealed class Value {
        fun asFloat(default: Float): Float = when (this) {
            is Num -> v.toFloat()
            is Str -> v.toFloatOrNull() ?: default
            else -> default
        }

        fun asInt(default: Int): Int = when (this) {
            is Num -> v.toInt()
            is Str -> v.toIntOrNull() ?: default
            else -> default
        }
    }

    class Num(val v: Double) : Value()
    class Str(val v: String) : Value()
    class Bool(val v: Boolean) : Value()
    object Null : Value()
    class Arr(val items: List<Value>) : Value()
    class Obj(val map: Map<String, Value>) : Value() {
        fun values(key: String): List<Value> {
            val a = map[key] as? Arr ?: return emptyList()
            return a.items
        }

        fun arr(key: String): List<Obj> = values(key).mapNotNull { it as? Obj }

        fun ints(key: String): List<Int> = values(key).map { it.asInt(0) }

        fun obj(key: String): Obj? = map[key] as? Obj

        fun str(key: String, default: String): String {
            val v = map[key] ?: return default
            return when (v) {
                is Str -> v.v
                else -> default
            }
        }

        fun int(key: String, default: Int): Int {
            val v = map[key] ?: return default
            return v.asInt(default)
        }
    }

    fun parseObject(text: String): Obj {
        val p = Parser(text)
        val v = p.parseValue()
        return v as? Obj ?: throw GltfParseException("JSON root must be object")
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun parseValue(): Value {
            skipWs()
            if (i >= s.length) throw GltfParseException("Unexpected end of JSON")
            return when (s[i]) {
                '{' -> parseObj()
                '[' -> parseArr()
                '"' -> Str(parseString())
                't' -> {
                    expect("true")
                    Bool(true)
                }
                'f' -> {
                    expect("false")
                    Bool(false)
                }
                'n' -> {
                    expect("null")
                    Null
                }
                else -> Num(parseNumber())
            }
        }

        private fun parseObj(): Obj {
            i++ // {
            val map = linkedMapOf<String, Value>()
            skipWs()
            if (peek('}')) {
                i++
                return Obj(map)
            }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expectChar(':')
                val value = parseValue()
                map[key] = value
                skipWs()
                when {
                    peek('}') -> {
                        i++
                        return Obj(map)
                    }
                    peek(',') -> i++
                    else -> throw GltfParseException("Expected , or } at $i")
                }
            }
        }

        private fun parseArr(): Arr {
            i++ // [
            val items = mutableListOf<Value>()
            skipWs()
            if (peek('}')) {
                // wrong
            }
            if (peek(']')) {
                i++
                return Arr(items)
            }
            while (true) {
                items += parseValue()
                skipWs()
                when {
                    peek(']') -> {
                        i++
                        return Arr(items)
                    }
                    peek(',') -> i++
                    else -> throw GltfParseException("Expected , or ] at $i")
                }
            }
        }

        private fun parseString(): String {
            expectChar('"')
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= s.length) throw GltfParseException("Bad escape")
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000c')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw GltfParseException("Bad unicode")
                                val hex = s.substring(i, i + 4).toInt(16)
                                sb.append(hex.toChar())
                                i += 4
                            }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw GltfParseException("Unterminated string")
        }

        private fun parseNumber(): Double {
            val start = i
            if (peek('-')) i++
            while (i < s.length && s[i].isDigit()) i++
            if (peek('.')) {
                i++
                while (i < s.length && s[i].isDigit()) i++
            }
            if (peek('e') || peek('E')) {
                i++
                if (peek('+') || peek('-')) i++
                while (i < s.length && s[i].isDigit()) i++
            }
            return s.substring(start, i).toDouble()
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun peek(c: Char): Boolean = i < s.length && s[i] == c

        private fun expectChar(c: Char) {
            skipWs()
            if (i >= s.length || s[i] != c) {
                throw GltfParseException("Expected '$c' at $i")
            }
            i++
        }

        private fun expect(lit: String) {
            if (!s.startsWith(lit, i)) throw GltfParseException("Expected $lit at $i")
            i += lit.length
        }
    }
}
