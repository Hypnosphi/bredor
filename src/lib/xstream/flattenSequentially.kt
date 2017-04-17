@file:JsModule("xstream/extra/flattenSequentially")
package lib.xstream

@JsName("default")
external fun <T> flattenSequentially(ins: Stream<Stream<T>>): Stream<T>
