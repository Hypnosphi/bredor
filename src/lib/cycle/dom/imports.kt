@file:JsModule("@cycle/dom")
package lib.cycle.dom

import lib.snabbdom.VNode
import lib.snabbdom.VNodeData
import org.w3c.dom.Node

external fun makeDOMDriver(container: String, options: DOMDriverOptions? = definedExternally) : DOMDriver
external fun makeDOMDriver(container: Node, options: DOMDriverOptions? = definedExternally) : DOMDriver

external fun h(sel: String, data: VNodeData? = definedExternally, text: String? = definedExternally): VNode
external fun h(sel: String, text: String): VNode
external fun h(sel: String, data: VNodeData, children: Array<VNode>): VNode
external fun h(sel: String, children: Array<VNode>): VNode
