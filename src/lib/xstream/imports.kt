@file:JsModule("xstream")
@file:JsQualifier("default")
package lib.xstream

external fun <T> of(vararg items: T): Stream<T>
external fun <T> combine(vararg streams: Stream<T>) : Stream<Array<T>>
