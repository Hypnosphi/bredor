package lib.xstream

import app.debugger
import app.jsObject
import lib.jsonp.*

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
