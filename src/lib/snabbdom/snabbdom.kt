package lib.snabbdom

import app.DefaultExport
import org.w3c.dom.*

external interface VNode {
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
    var key: Int?
}

external interface Module {
    fun pre()
    fun create(emptyVNode: VNode, vNode: VNode)
    fun update(oldVNode: VNode, vNode: VNode)
    fun destroy(vNode: VNode)
    fun remove(vNode: VNode, removeCallback: () -> Unit)
    fun post()
}

abstract class ModuleImpl : Module {
    override fun pre() {}
    override fun create(emptyVNode: VNode, vNode: VNode) {}
    override fun update(oldVNode: VNode, vNode: VNode) {}
    override fun destroy(vNode: VNode) {}
    override fun remove(vNode: VNode, removeCallback: () -> Unit) {
        removeCallback()
    }
    override fun post() {}
}

@JsModule("snabbdom/modules/props")
external val PropsModuleExport : DefaultExport<Module>
val PropsModule = PropsModuleExport.default

@JsModule("snabbdom/modules/attributes")
external val AttrsModuleExport : DefaultExport<Module>
val AttrsModule = AttrsModuleExport.default
