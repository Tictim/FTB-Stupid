package ftb.stupid.nbt

private val SIMPLE_VALUE = Regex("[A-Za-z0-9._+-]+")

fun nbtToString(nbt: Nbt, indent: Int = 0): String =
    SNBTBuilder(indent).apply { nbtToString(nbt) }.toString()

class SNBTBuilder(var indent: Int = 0) {
    private val stb = StringBuilder()

    fun append(o: Any?): SNBTBuilder {
        stb.append(o)
        return this
    }
    fun append(o: String?): SNBTBuilder {
        stb.append(o)
        return this
    }
    fun append(o: Char): SNBTBuilder {
        stb.append(o)
        return this
    }
    fun append(o: Int): SNBTBuilder {
        stb.append(o)
        return this
    }
    fun append(o: Long): SNBTBuilder {
        stb.append(o)
        return this
    }
    fun append(o: Float): SNBTBuilder {
        stb.append(o)
        return this
    }
    fun append(o: Double): SNBTBuilder {
        stb.append(o)
        return this
    }

    fun newLineAndIndent(): SNBTBuilder{
        stb.append('\n')
        for(i in 0 until indent) stb.append('\t')
        return this
    }

    override fun toString() = stb.toString()
}

private fun SNBTBuilder.nbtToString(nbt: Nbt) {
    when (nbt) {
        is NBTObject -> {
            if (nbt.map.isEmpty()) append("{}")
            else {
                append('{')
                indent++

                var first = true
                for ((k, v) in nbt.map) {
                    if (first) first = false
                    else append(',')
                    newLineAndIndent()
                    if (SIMPLE_VALUE.matches(k)) append(k)
                    else quoteAndString(k)
                    append(": ").nbtToString(v)
                }

                indent--
                newLineAndIndent().append('}')
            }
        }
        is NBTList -> array(nbt, "") { n, i ->
            nbtToString(n[i])
        }
        is NBTByteArray -> array(nbt, "B;") { n, i ->
            append(n[i]).append('b')
        }
        is NBTIntArray -> array(nbt, "I;") { n, i ->
            append(n[i])
        }
        is NBTLongArray -> array(nbt, "L;") { n, i ->
            append(n[i]).append('L')
        }
        is NBTPrimitive<*> -> {
            when (nbt) {
                is NBTString -> {
                    quoteAndString(nbt.value)
                }
                is NBTFloat -> append(nbt.value).append('f')
                is NBTByte -> append(nbt.value).append('b')
                is NBTLong -> append(nbt.value).append('L')
                is NBTShort -> append(nbt.value).append('s')
                is NBTInt -> append(nbt.value)
                is NBTDouble -> append(nbt.value).append('d')
            }
        }
        is NBTEnd -> append("END")
    }
}

private inline fun <reified E, reified ARRAY : NBTArray<E>> SNBTBuilder.array(
    snbt: ARRAY,
    starting: String,
    appending: SNBTBuilder.(n: ARRAY, i: Int) -> Unit
) {
    when (snbt.size) {
        0 -> append('[').append(starting).append(']')
        1 -> {
            append('[').append(starting)
            appending(snbt, 0)
            append(']')
        }
        else -> {
            append('[').append(starting)
            indent++

            for(index in 0 until snbt.size){
                if(index>0) append(',')
                newLineAndIndent()
                appending(snbt, index)
            }

            indent--
            newLineAndIndent().append(']')
        }
    }
}

private fun SNBTBuilder.quoteAndString(s: String){
    append('"')

    for (c in s) {
        if (c == '\\' || c == '"') append('\\')
        append(c)
    }

    append('"')
}