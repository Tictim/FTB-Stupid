package ftb.stupid.nbt

sealed class NBT

class NBTObject : NBT() {
    private val _map = mutableMapOf<String, NBT>()

    val map: Map<String, NBT>
        get() = _map

    operator fun get(key: String): NBT? = _map[key]
    operator fun set(key: String, value: Any) {
        _map[key] = objectToSNBT(value)
    }

    fun remove(key: String): NBT? = _map.remove(key)
}

sealed class NBTArray<E> : NBT(), Iterable<E>{
    abstract val size: Int
}

class NBTList : NBTArray<NBT>() {
    private val _list = mutableListOf<NBT>()
    override val size: Int
        get() = _list.size

    val list: List<NBT>
        get() = _list

    operator fun get(index: Int) = _list[index]
    operator fun set(index: Int, value: Any) {
        _list[index] = objectToSNBT(value)
    }

    operator fun plusAssign(value: Any) {
        _list.add(objectToSNBT(value))
    }

    override fun iterator() = _list.iterator()
}

class NBTByteArray(bytes: Array<Byte>) : NBTArray<Byte>() {
    private val _bytes = bytes.copyOf()
    override val size: Int
        get() = _bytes.size

    operator fun get(index: Int) = _bytes[index]
    operator fun set(index: Int, value: Byte) {
        _bytes[index] = value
    }

    override fun iterator() = _bytes.iterator()
}

class NBTIntArray(bytes: Array<Int>) : NBTArray<Int>() {
    private val _ints = bytes.copyOf()
    override val size: Int
        get() = _ints.size

    operator fun get(index: Int) = _ints[index]
    operator fun set(index: Int, value: Int) {
        _ints[index] = value
    }

    override fun iterator() = _ints.iterator()
}

class NBTLongArray(longs: Array<Long>) : NBTArray<Long>() {
    private val _longs = longs.copyOf()
    override val size: Int
        get() = _longs.size

    operator fun get(index: Int) = _longs[index]
    operator fun set(index: Int, value: Long) {
        _longs[index] = value
    }

    override fun iterator() = _longs.iterator()
}

sealed class NBTPrimitive<E> : NBT() {
    abstract val value: E
}

class NBTString(override val value: String) : NBTPrimitive<String>()
class NBTFloat(override val value: Float) : NBTPrimitive<Float>()
class NBTByte(override val value: Byte) : NBTPrimitive<Byte>()
class NBTLong(override val value: Long) : NBTPrimitive<Long>()
class NBTShort(override val value: Short) : NBTPrimitive<Short>()
class NBTInt(override val value: Int) : NBTPrimitive<Int>()
class NBTDouble(override val value: Double) : NBTPrimitive<Double>()

object NBTEnd : NBT()

val NBT.asObject: NBTObject
    get() = this as NBTObject
val NBT.asList: NBTList
    get() = this as NBTList
val NBT.asByteArray: NBTByteArray
    get() = this as NBTByteArray
val NBT.asIntArray: NBTIntArray
    get() = this as NBTIntArray
val NBT.asLongArray: NBTLongArray
    get() = this as NBTLongArray
val NBT.asString: NBTString
    get() = this as NBTString
val NBT.asFloat: NBTFloat
    get() = this as NBTFloat
val NBT.asByte: NBTByte
    get() = this as NBTByte
val NBT.asLong: NBTLong
    get() = this as NBTLong
val NBT.asShort: NBTShort
    get() = this as NBTShort
val NBT.asInt: NBTInt
    get() = this as NBTInt
val NBT.asDouble: NBTDouble
    get() = this as NBTDouble
val NBT.asEnd: NBTEnd
    get() = this as NBTEnd

fun objectToSNBT(value: Any) =
    when (value) {
        is NBT -> value
        is String -> NBTString(value)
        is Float -> NBTFloat(value)
        is Byte -> NBTByte(value)
        is Long -> NBTLong(value)
        is Short -> NBTShort(value)
        is Int -> NBTInt(value)
        is Double -> NBTDouble(value)
        else -> error("$value doesn't match any possible types of data SNBT can express")
    }