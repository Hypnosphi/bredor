package lib.paver

import app.setTimeout
import lib.snabbdom.*
import org.w3c.dom.*
import kotlin.browser.window

data class Tile (
    val width: Int,
    val height: Int,
    val elm: Element
) {
    constructor(elm: Element) : this(elm.clientWidth, elm.clientHeight, elm)
}

external interface TileData {
    val data: Tile
}

data class PaverOptions (
    val margin: Int = 2,
    val renderTile: (TileData) -> Node
)

external interface PaverLayout {
    fun render(element: Element)
}

external fun Paver(dataSource: Array<Tile>, width: Int, options: PaverOptions? = definedExternally) : PaverLayout

fun paver(vNode: VNode) {
    val paver = vNode.data?.paver
    val elm = vNode.elm
    val children = vNode.children
    paver ?: return
    elm ?: return
    children ?: return

    elm.asDynamic().style.visibility = "hidden"
    setTimeout {
        val width = paver.width ?: elm.clientWidth

        val vNodeChildren: List<VNode> = children.filter { it !is String }

        val tiles = vNodeChildren
            .filter { it.elm != null }
            .map { Tile(it.elm!!) }
            .toTypedArray()

        val options = PaverOptions(0) { it.data.elm }

        val layout = Paver(tiles, width, options)
        layout.render(elm)
        elm.asDynamic().style.visibility = "visible"
    }
}

object PaverModule : ModuleImpl() {
    override fun create(emptyVNode: VNode, vNode: VNode) {
        paver(vNode)
    }

    override fun update(oldVNode: VNode, vNode: VNode) {
        // TODO perform diff checking
        //paver(vNode)
    }
}
