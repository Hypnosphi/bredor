package app

import lib.cycle.DriversDefinition
import lib.cycle.Sinks
import lib.cycle.Sources
import lib.cycle.dom.DOMDriverOptions
import lib.cycle.dom.DOMSource
import lib.cycle.dom.makeDOMDriver
import lib.cycle.run
import lib.cycle.storage.ResponseCollection
import lib.cycle.storage.StorageRequest
import lib.cycle.storage.storageDriver
import lib.paver.PaverModule
import lib.snabbdom.AttrsModule
import lib.snabbdom.PropsModule
import lib.snabbdom.StyleModule
import lib.snabbdom.VNode
import lib.xstream.Stream
import vk.VKDriver
import vk.VKReq
import vk.VKSource

class AppDrivers(selector: String) : DriversDefinition {
    val DOM = makeDOMDriver(selector, DOMDriverOptions(PropsModule, AttrsModule, StyleModule, PaverModule))
    val VK = VKDriver
    val storage = storageDriver
}

interface AppSources : Sources {
    val DOM: DOMSource
    val VK: VKSource
    val storage: ResponseCollection
}

class AppSinks (
    val DOM: Stream<VNode>,
    val VK: Stream<VKReq<*>>,
    val storage: Stream<StorageRequest>
) : Sinks

fun main(args: Array<String>) {
    run(::app, AppDrivers("#app"))
}
