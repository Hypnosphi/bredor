package app

import org.w3c.dom.url.URLSearchParams
import kotlin.browser.window
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class Params(source: String? = null, vararg initParams: Pair<String, String>) {
    private val params = URLSearchParams(source ?: "")
    override fun toString() = params.toString()

    init {
        initParams.forEach { (name, value) ->
            params.set(name, value)
        }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>) : String =
        params.get(property.name) ?: ""
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        params.set(property.name, value)
    }

    inner abstract class Delegate<T> {
        operator fun getValue(thisRef: Any, property: KProperty<*>) : T =
            deserialize(this@Params.getValue(thisRef, property))
        operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            this@Params.setValue(thisRef, property, serialize(value))
        }

        abstract fun serialize(value: T) : String
        open fun deserialize(str: String) : T {
            throw Error("readonly")
        }
    }

    inner class Json<T> : Delegate<T>() {
        override fun serialize(value: T) = JSON.stringify(value)
        override fun deserialize(str: String) : T = JSON.parse(str)
    }

    inner class Bool : Delegate<Boolean>() {
        override fun serialize(value: Boolean) = if (value) "1" else "0"
        override fun deserialize(str: String) = str == "1"
    }

    inner class Integer : Delegate<Int>() {
        override fun serialize(value: Int) = value.toString()
        override fun deserialize(str: String) = str.toInt()
    }

    inner class StringList : Delegate<List<String>>() {
        private val separator = ","

        override fun serialize(value: List<String>) = value.joinToString(separator)
        override fun deserialize(str: String): List<String> = str.split(separator)
    }

    inner class IntList : Delegate<List<Int>>() {
        override fun serialize(value: List<Int>) =
            StringList().serialize(value.map(Int::toString))
        override fun deserialize(str: String) =
            StringList().deserialize(str).map(String::toInt)
    }

    inner class PropList<T> : Delegate<List<KProperty1<T, *>>>() {
        override fun serialize(value: List<KProperty1<T, *>>) =
            StringList().serialize(value.map(KProperty<*>::name))
    }

    inner class EnumString<T: Enum<T>> : Delegate<T>() {
        override fun serialize(value: T) = value.name
    }

    inner class EnumStringList<T: Enum<T>> : Delegate<List<T>>() {
        override fun serialize(value: List<T>) =
            StringList().serialize(value.map(Enum<T>::name))
    }
}

interface Builder<T>

inline fun <reified T : Any> Builder<T>.create() : T {
    @Suppress("UNUSED_VARIABLE")
    val ctor = T::class.js
    return js("new ctor()")
}

inline operator fun <reified T : Any> Builder<T>.invoke(noinline handler: T.() -> Unit) : T =
    create().apply(handler)

fun <T> jsObject(handler: T.() -> Unit) : T {
    val obj: T = js("({})")
    return obj.apply(handler)
}

fun js(handler: dynamic.() -> Unit) = jsObject(handler)

@Suppress("NOTHING_TO_INLINE")
inline fun debugger() = js("debugger;")

external interface DefaultExport<T> {
    val default: T
}

fun setTimeout(timeout: Int = 0, handler: () -> Unit) {
    window.setTimeout(handler, timeout)
}
