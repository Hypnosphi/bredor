package app

import lib.cycle.DriversDefinition
import lib.cycle.Sinks
import lib.cycle.Sources
import lib.cycle.dom.DOMSource
import lib.cycle.dom.VNode
import lib.cycle.dom.makeDOMDriver
import lib.cycle.run
import lib.xstream.Stream

class AppDrivers(selector: String) : DriversDefinition {
    val DOM = makeDOMDriver(selector)
}

interface AppSources : Sources {
    val DOM : DOMSource
}

class AppSinks (
    val DOM: Stream<VNode>
) : Sinks

fun main(args: Array<String>) {
    run(::app, AppDrivers("#app"))
}
