@file:JsModule("xstream/extra/flattenConcurrently")
package lib.xstream

@JsName("default")
external fun <T> flattenConcurrently(ins: Stream<Stream<T>>): Stream<T>
