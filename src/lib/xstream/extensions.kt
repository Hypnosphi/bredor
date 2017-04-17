package lib.xstream

import app.jsObject
import app.setTimeout
import app.toJs
import lib.jsonp.JsonpOptions
import lib.jsonp.jsonp

fun <T, U> Stream<T>.switchMap(project: (T) -> Stream<U>) =
    mapStream(project).flatten()

fun <T, U> Stream<T>.bind(project: (T) -> Stream<U>) =
    take(1)
        .persist()
        .mapStream(project)
        .flatten()

fun <T1, T2, U> combine(s1: Stream<T1>, s2: Stream<T2>, project: (T1, T2) -> U) =
    combine(s1, s2).map { values ->
        project(values[0] as T1, values[1] as T2)
    }

fun <T> create(producer: Producer<T>.() -> Unit) =
    create(jsObject(producer))

fun <T> jsonpStream(url: String, opts: JsonpOptions? = null) : Stream<T> =
    create {
        var cancel: (() -> Unit)? = null
        start = { listener ->
            cancel = jsonp<T>(url, opts) { err, data ->
                when {
                    err != null -> listener.error(err)
                    else -> {
                        listener.next(data)
                    }
                }
            }
        }
        stop = {
            cancel?.invoke()
        }
    }

fun <T> Stream<T>.addListener(
    listener: Listener<T>.() -> Unit = {}
) {
    addListener(jsObject {
        next = {}
        error = {}
        complete = {}

        listener()
    })
}

fun <T, R> Stream<T>.fold(seed: R, accumulate: (acc: R, t: T) -> R) = fold(accumulate, seed)

fun <T> Stream<T>.persist() = merge(this, never())

@Suppress("UNCHECKED_CAST", "UNCHECKED_CAST_TO_NATIVE_INTERFACE")
fun <T> Stream<*>.toType() = this as Stream<T>

fun <T> Stream<T>.toNullable() = toType<T?>()

fun <T> Stream<T>.throttle(period: Int) = lib.xstream.throttle<T>(period)(this)

fun <T> Stream<T>.pairwise(): Stream<Pair<T, T>> =
    pairwise(this).map { it[0] to it[1] }

fun <T> Stream<T?>.filterNotNull() = filter { it != null }.toType<T>()

fun <T, U> Stream<T>.concatMap(project: (T) -> Stream<U>) =
    flattenSequentially(mapStream(project))

fun <T, U> Stream<T>.flatMap(project: (T) -> Stream<U>) =
    flattenConcurrently(mapStream(project))

fun <T> Stream<T>.delay(period: Int) = lib.xstream.delay<T>(period)(this)

fun <T> Stream<T>.sample(period: Int) = concatMap { of(it).delay(period) }

fun <T> Stream<T>.couples(): Stream<Pair<T, T>> =
    fold<T, Pair<T?, Pair<T, T>?>>(null to null) { (fst), next ->
        when (fst) {
            null -> next to null
            else -> null to (fst to next)
        }
    }
    .map { it.second }
    .filterNotNull()

fun <T> diagramOptions(
    unit: Int? = null,
    error: Throwable? = null,
    valuesBuilder: (MutableMap<Char, T>.() -> Unit)? = null
): FromDiagramOptions = jsObject {
    unit?.let { timeUnit = it }
    error?.let { errorValue = it }
    valuesBuilder?.let {
        values = mutableMapOf<Char, T>().apply(valuesBuilder).toJs()
    }
}

fun diagram(diagram: String, unit: Int? = null, errorValue: Throwable? = null): Stream<String>
    = fromDiagram(diagram, diagramOptions<Unit>(unit, errorValue))

fun <T> diagram(
    diagram: String,
    unit: Int? = null,
    errorValue: Throwable? = null,
    valuesBuilder: MutableMap<Char, T>.() -> Unit
): Stream<T>
    = fromDiagram(diagram, diagramOptions(unit, errorValue, valuesBuilder))

fun reserve(period: Int): Stream<(Int) -> Int> =
    diagram<(Int) -> Int>("io|", period) {
        put('i') { it + 1 }
        put('o') { it - 1 }
    }

data class BandwithData<T>(
    val events: Stream<(Int) -> Int> = never(),
    val count: MemoryStream<Int> = of(0).remember(),
    val out: Stream<T> = never()
)

fun <T> Stream<(T) -> T>.apply(init: T) = fold(init) { value, fn -> fn(value) }

class Buffer(val period: Int, val size: Int) {
    private var count: Int = 0
    private val queue = arrayOf<() -> Unit>()

    fun run(cb: () -> Unit) {
        count++
        cb()
        setTimeout(period) {
            if (--count < size && queue.isNotEmpty()) {
                run(queue.asDynamic().shift())
            }
        }
    }

    fun add(cb: () -> Unit) {
        if (count < size) {
            run(cb)
        } else {
            queue.asDynamic().push(cb)
        }
    }
}

fun <T> Stream<T>.limitBandwidth(period: Int, bufferSize: Int = 1): Stream<T> =
    create {
        val buffer = Buffer(period, bufferSize)

        var il: Listener<T> = jsObject {}
        start = { listener ->
            il = jsObject {
                next = {
                    buffer.add {
                        console.log("wow")
                        listener.next(it)
                    }
                }
            }
            addListener(il)
        }
        stop = {
            removeListener(il)
        }
    }
