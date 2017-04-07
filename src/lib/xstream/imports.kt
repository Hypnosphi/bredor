@file:JsModule("xstream")
@file:JsQualifier("default")
package lib.xstream

external fun <T> create(producer: Producer<T>? = definedExternally /* null */): Stream<T>
external fun <T> of(vararg items: T): Stream<T>
external fun <T> merge(vararg streams: Stream<T>) : Stream<T>
external fun combine(vararg streams: Stream<*>) : Stream<Array<dynamic>>
external fun <T> never(): Stream<T>
