@file:JsModule("xstream/extra/pairwise")
package lib.xstream

@JsName("default")
external fun <T> pairwise(ins: Stream<T>): Stream<Array<T>>
