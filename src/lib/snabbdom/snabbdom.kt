package lib.snabbdom

import org.w3c.dom.*

interface VNode {
    val sel: String?
    val data: VNodeData?
    val children: Array<dynamic /* VNode | String */>?
    val elm: Element?
    val text: String?
    val key: dynamic /* String | Number */
}

data class PaverData(val width: Int? = null)

external interface VNodeData {
    var attrs: dynamic
    var props: dynamic
    var paver: PaverData?
}

abstract class Module {
    open fun pre() {}
    open fun create(emptyVNode: VNode, vNode: VNode) {}
    open fun update(oldVNode: VNode, vNode: VNode) {}
    open fun destroy(vNode: VNode) {}
    open fun remove(vNode: VNode, removeCallback: () -> Unit) {}
    open fun post() {}
}

@JsModule("snabbdom/modules/props")
external val PropsModule : Module

@JsModule("snabbdom/modules/attributes")
external val AttrsModule : Module
