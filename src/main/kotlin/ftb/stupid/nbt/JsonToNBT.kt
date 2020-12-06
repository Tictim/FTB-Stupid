package ftb.stupid.nbt

import java.util.regex.Pattern

open class JsonToNBT internal constructor(
    private val string: String
) {
    private var cursor = 0

    @Throws(NBTException::class)
    fun readSingleStruct(): NBTObject {
        val o = readObj()
        skipWhitespace()
        if (this.canRead()) {
            ++cursor
            throw exception("Trailing data found")
        } else return o
    }

    @Throws(NBTException::class)
    protected fun readKey(): String {
        skipWhitespace()
        if (!this.canRead()) throw exception("Expected key")
        else {
            val peek = peek()
            if (isQuotedStringStart(peek)) {
                return readQuotedString()
            } else return readString()
        }
    }

    private fun exception(message: String) =
        NBTException(message, string, cursor)

    @Throws(NBTException::class)
    protected fun readTypedValue(): Nbt {
        skipWhitespace()
        if (isQuotedStringStart(peek())) return NBTString(readQuotedString())
        val s = readString()
        if (s.isEmpty()) throw exception("Expected value")
        else return type(s)
    }

    private fun type(stringIn: String): Nbt {
        try {
            when {
                FLOAT_PATTERN.matcher(stringIn).matches() -> return NBTFloat(
                    stringIn.substring(0, stringIn.length - 1).toFloat()
                )
                BYTE_PATTERN.matcher(stringIn).matches() -> return NBTByte(
                    stringIn.substring(0, stringIn.length - 1).toByte()
                )
                LONG_PATTERN.matcher(stringIn).matches() -> return NBTLong(
                    stringIn.substring(0, stringIn.length - 1).toLong()
                )
                SHORT_PATTERN.matcher(stringIn).matches() -> return NBTShort(
                    stringIn.substring(0, stringIn.length - 1).toShort()
                )
                INT_PATTERN.matcher(stringIn).matches() -> return NBTInt(stringIn.toInt())
                DOUBLE_PATTERN.matcher(stringIn).matches() -> return NBTDouble(
                    stringIn.substring(
                        0,
                        stringIn.length - 1
                    ).toDouble()
                )
                DOUBLE_PATTERN_NOSUFFIX.matcher(stringIn).matches() -> return NBTDouble(stringIn.toDouble())
                "true".equals(stringIn, ignoreCase = true) -> return NBTByte(1.toByte())
                "false".equals(stringIn, ignoreCase = true) -> return NBTByte(0.toByte())
            }
        } catch (ignored: NumberFormatException) {
        }
        return NBTString(stringIn)
    }

    @Throws(NBTException::class)
    private fun readQuotedString(): String {
        if (!canRead()) return ""

        val start = peek()
        if (!isQuotedStringStart(start)) exception("Expected start of string")

        val i = ++cursor
        var stb: StringBuilder? = null
        var escape = false

        while (this.canRead()) {
            val c = read()
            if (escape) {
                if (c != '\\' && c != start) throw exception("Invalid escape of '$c'")
                escape = false
            } else {
                if (c == '\\') {
                    escape = true
                    if (stb == null) stb = StringBuilder(string.substring(i, cursor - 1))
                    continue
                }
                if (c == start) return stb?.toString() ?: string.substring(i, cursor - 1)
            }
            stb?.append(c)
        }
        throw exception("Missing termination quote")
    }

    private fun readString(): String {
        val i = cursor
        while (this.canRead() && isAllowedInKey(peek())) ++cursor
        return string.substring(i, cursor)
    }

    @Throws(NBTException::class)
    protected fun readValue(): Nbt {
        skipWhitespace()
        if (!this.canRead()) {
            throw exception("Expected value")
        } else return when (peek()) {
            '{' -> readObj()
            '[' -> readList()
            else -> readTypedValue()
        }
    }

    @Throws(NBTException::class)
    protected fun readList(): Nbt =
        if (this.canRead(2) && peek(1) != '"' && peek(2) == ';')
            readArrayTag()
        else readListTag()

    @Throws(NBTException::class)
    protected fun readObj(): NBTObject {
        expect('{')
        val SNBTcompound = NBTObject()
        skipWhitespace()
        while (this.canRead() && peek() != '}') {
            val s = readKey()
            if (s.isEmpty()) {
                throw exception("Expected non-empty key")
            }
            expect(':')
            SNBTcompound[s] = readValue()
            if (!hasElementSeparator()) {
                break
            }
            if (!this.canRead()) {
                throw exception("Expected key")
            }
        }
        expect('}')
        return SNBTcompound
    }

    @Throws(NBTException::class)
    private fun readListTag(): Nbt {
        expect('[')
        skipWhitespace()
        if (!this.canRead()) throw exception("Expected value")
        val list = NBTList()
        var type: Class<Nbt>? = null
        while (peek() != ']') {
            val snbt = readValue()
            val ctype: Class<Nbt> = snbt.javaClass
            if (type == null) {
                type = ctype
            } else if (type != ctype) {
                throw exception(
                    "Unable to insert ${ctype.name} into ListTag of type ${type.name}"
                )
            }
            list += snbt
            if (!hasElementSeparator()) break
            if (!this.canRead()) throw exception("Expected value")
        }
        expect(']')
        return list
    }

    @Throws(NBTException::class)
    private fun readArrayTag(): Nbt {
        expect('[')
        val type = read()
        read()
        skipWhitespace()
        return when {
            !this.canRead() -> throw exception("Expected value")
            type == 'B' -> NBTByteArray(readArray<NBTByteArray, NBTByte, Byte>())
            type == 'L' -> NBTLongArray(readArray<NBTLongArray, NBTLong, Long>())
            type == 'I' -> NBTIntArray(readArray<NBTIntArray, NBTInt, Int>())
            else -> throw exception("Invalid array type '$type' found")
        }
    }

    @Throws(NBTException::class)
    private inline fun <
            reified TYPE : Nbt,
            reified E : NBTPrimitive<DATA>,
            reified DATA : Number>
            readArray(): Array<DATA> {
        val list = mutableListOf<DATA>()
        while (true) {
            if (peek() != ']') {
                val snbt = readValue()
                if (snbt !is E) {
                    throw exception(
                        "Unable to insert ${snbt.javaClass.name} into ${TYPE::class.java.name}"
                    )
                }
                list.add(snbt.value)
                if (hasElementSeparator()) {
                    if (!this.canRead()) throw exception("Expected value")
                    continue
                }
            }
            expect(']')
            return list.toTypedArray()
        }
    }

    private fun skipWhitespace() {
        while (this.canRead() && Character.isWhitespace(peek())) ++cursor
    }

    private fun hasElementSeparator(): Boolean {
        skipWhitespace()
        return if (this.canRead() && peek() == ',') {
            ++cursor
            skipWhitespace()
            true
        } else false
    }

    @Throws(NBTException::class)
    private fun expect(expected: Char) {
        skipWhitespace()
        val flag = this.canRead()
        if (flag && peek() == expected) {
            ++cursor
        } else throw NBTException(
            "Expected '" + expected + "' but got '" + (if (flag) peek() else "<EOF>") + "'",
            string,
            cursor + 1
        )
    }

    protected fun isAllowedInKey(charIn: Char): Boolean {
        return charIn in '0'..'9' ||
                charIn in 'A'..'Z' ||
                charIn in 'a'..'z' ||
                charIn == '_' ||
                charIn == '-' ||
                charIn == '.' ||
                charIn == '+'
    }

    private fun canRead(offset: Int = 0): Boolean =
        cursor + offset < string.length

    private fun peek(offset: Int = 0) = string[cursor + offset]
    private fun read() = string[cursor++]

    private fun skip() {
        cursor++
    }

    private fun isQuotedStringStart(c: Char) = c == '"' || c == '\''

    companion object {
        private val DOUBLE_PATTERN_NOSUFFIX = Pattern.compile("[-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?", 2)
        private val DOUBLE_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?d", 2)
        private val FLOAT_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?f", 2)
        private val BYTE_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)b", 2)
        private val LONG_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)l", 2)
        private val SHORT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)s", 2)
        private val INT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)")

        @Throws(NBTException::class)
        fun getTagFromJson(jsonString: String): NBTObject =
            JsonToNBT(jsonString).readSingleStruct()
    }
}