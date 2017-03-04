package lib.cycle.dom

import lib.cycle.DriverFunction
import lib.xstream.MemoryStream
import lib.xstream.Stream
import org.w3c.dom.events.Event

external interface EventsFnOptions {
    var useCapture: Boolean?
}
external interface DOMSource {
    fun <S : DOMSource> select(selector: String): S
    fun elements(): MemoryStream<dynamic /* Document | Element | Array<Element> | String */>
    fun events(eventType: String, options: EventsFnOptions? = definedExternally /* null */): Stream<Event>
}

external interface VNode

external interface VNodeData {
    var attrs: dynamic
    var props: dynamic
}

external interface Module

external interface DOMDriverOptions {
    var modules: Array<Module>?
}

typealias DOMDriver = DriverFunction<VNode, DOMSource>
