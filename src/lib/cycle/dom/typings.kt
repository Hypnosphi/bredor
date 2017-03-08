package lib.cycle.dom

import lib.cycle.DriverFunction
import lib.snabbdom.*
import lib.xstream.MemoryStream
import lib.xstream.Stream
import org.w3c.dom.events.Event

external interface EventsFnOptions {
    var useCapture: Boolean?
}
external interface DOMSource {
    fun select(selector: String): DOMSource
    fun elements(): MemoryStream<dynamic /* Document | Element | Array<Element> | String */>
    fun events(eventType: String, options: EventsFnOptions? = definedExternally /* null */): Stream<Event>
}


data class DOMDriverOptions(
    val modules: Array<out Module>?
) {
    constructor(vararg modules: Module) : this(modules)
}

typealias DOMDriver = DriverFunction<VNode, DOMSource>
