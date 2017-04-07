@file:JsModule("xstream/extra/throttle")
package lib.xstream

@JsName("default")
external fun <T> throttle(period: Int) : Operator<T, T>
