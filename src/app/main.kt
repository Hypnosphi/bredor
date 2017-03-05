package app

import lib.cycle.*
import lib.cycle.dom.*
import lib.xstream.*
import vk.*

class AppDrivers(selector: String) : DriversDefinition {
    val DOM = makeDOMDriver(selector)
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
