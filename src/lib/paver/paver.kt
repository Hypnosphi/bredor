package lib.paver

import lib.paver.PaverModule.paver
import lib.snabbdom.HBuilder
import lib.snabbdom.Module
import lib.snabbdom.NodeBuilder
import lib.snabbdom.VNode
import org.w3c.dom.*

data class Tile (
    val width: Int,
    val height: Int,
    val elm: Element
) {
    constructor(elm: Element) : this(elm.clientWidth, elm.clientHeight, elm)
}

data class PaverOptions (
    val renderTile: (Tile) -> Node
)

external interface PaverLayout {
    fun render()
}

external fun Paver(dataSource: Array<Tile>, width: Int, options: PaverOptions? = definedExternally) : PaverLayout

object PaverModule : Module() {
    fun paver(vNode: VNode) {
        val paver = vNode.data?.paver
        paver ?: return

        val width = paver.width ?: vNode.elm?.clientWidth
        width ?: return

        val children = vNode.children
        children ?: return
        val tiles = children
            .filterIsInstance<VNode>()
            .filter { it.elm != null }
            .map { Tile(it.elm!!) }
            .toTypedArray()

        val options = PaverOptions(Tile::elm)

        val layout = Paver(tiles, width, options)
        layout.render()
    }

    override fun create(emptyVNode: VNode, vNode: VNode) {
        paver(vNode)
    }

    override fun update(oldVNode: VNode, vNode: VNode) {
        paver(vNode)
    }
}
