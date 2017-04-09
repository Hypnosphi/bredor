package lib.xstream

import app.jsObject
import lib.jsonp.JsonpOptions
import lib.jsonp.jsonp

fun <T, U> Stream<T>.flatMap(project: (T) -> Stream<U>) =
    mapStream(project).flatten()

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
    listener: Listener<T>.() -> Unit = {
        next = {}
        error = {}
        complete = {}
    }
) {
    addListener(jsObject(listener))
}

fun <T, R> Stream<T>.fold(seed: R, accumulate: (acc: R, t: T) -> R) = fold(accumulate, seed)

fun <T> Stream<T>.persist() = merge(this, never())

@Suppress("UNCHECKED_CAST", "UNCHECKED_CAST_TO_NATIVE_INTERFACE")
fun <T> Stream<*>.toType() = this as Stream<T>

fun <T> Stream<T>.toNullable() = toType<T?>()

fun <T> Stream<T>.throttle(period: Int) = lib.xstream.throttle<T>(period)(this)

fun <T> Stream<T>.pairwise(): Stream<Pair<T, T>> =
    lib.xstream.pairwise(this).map { it[0] to it[1] }

fun <T> Stream<T?>.filterNotNull() = filter { it != null }.toType<T>()