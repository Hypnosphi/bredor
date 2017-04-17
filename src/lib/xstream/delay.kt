@file:JsModule("xstream/extra/delay")
package lib.xstream

@JsName("default")
external fun <T> delay(period: Int) : Operator<T, T>
