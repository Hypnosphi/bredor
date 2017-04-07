package app

import lib.cycle.DriversDefinition
import lib.cycle.Sinks
import lib.cycle.Sources
import lib.cycle.dom.DOMDriverOptions
import lib.cycle.dom.DOMSource
import lib.cycle.dom.makeDOMDriver
import lib.cycle.run
import lib.paver.PaverModule
import lib.snabbdom.AttrsModule
import lib.snabbdom.PropsModule
import lib.snabbdom.VNode
import lib.xstream.Stream
import vk.VKDriver
import vk.VKReq
import vk.VKSource

class AppDrivers(selector: String) : DriversDefinition {
    val DOM = makeDOMDriver(selector, DOMDriverOptions(PropsModule, AttrsModule, PaverModule))
    val VK = VKDriver
}

interface AppSources : Sources {
    val DOM: DOMSource
    val VK: VKSource
}

class AppSinks (
    val DOM: Stream<VNode>,
    val VK: Stream<VKReq<*>>
) : Sinks

fun main(args: Array<String>) {
    run(::app, AppDrivers("#app"))
}
