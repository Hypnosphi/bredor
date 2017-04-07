package lib.snabbdom

import app.*
import kotlinx.html.*
import lib.snabbdom.*
import lib.xstream.*
import org.w3c.dom.events.Event

interface Child {
    fun appendTo(list: MutableList<VNode>)
}

class NodeBuilder(val selector: String = "", var text: String? = null) : Child {
    val data : VNodeData = jsObject {
        attrs = object {}
        props = object {}
    }

    fun setAttr(name: String, value: String?) {
        data.attrs[name] = value
    }

    val children = mutableListOf<Child>()

    fun addText(added: String) {
        when (text) {
            null -> text = added
            else -> text = "$text$added"
        }
    }

    fun textToChildren() {
        val t = text
        if (t != null) {
            children += NodeBuilder("span", t)
            text = null
        }
    }

    fun addChild(child: Child) {
        textToChildren()
        children += child
    }

    fun createChildren() : List<VNode> {
        textToChildren()
        val childNodes = mutableListOf<VNode>()
        children.forEach {
            it.appendTo(childNodes)
        }
        return childNodes.toList()
    }

    fun create(): VNode =
        when {
            children.isEmpty() -> lib.cycle.dom.h(selector, data, text)
            else -> {
                lib.cycle.dom.h(selector, data, createChildren().toTypedArray())
            }
        }

    override fun appendTo(list: MutableList<VNode>) {
        list.add(create())
    }
}

private class ReactiveNode : Child {
    var resolved = listOf<VNode>()

    override fun appendTo(list: MutableList<VNode>) {
        list.addAll(resolved)
    }
}

class HBuilder : TagConsumer<NodeBuilder> {
    val base = NodeBuilder()
    private val stack = mutableListOf(base)
    private val current: NodeBuilder
        get() = stack.last()
    private lateinit var lastLeaved: NodeBuilder

    var changes = of(null).persist()

    override fun onTagStart(tag: Tag) {
        val node = NodeBuilder(tag.tagName)
        tag.attributes.forEach { (name, value) ->
            node.setAttr(name, value)
        }
        stack += node
    }

    override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
        current.setAttr(attribute, value)
    }

    override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {}

    override fun onTagEnd(tag: Tag) {
        lastLeaved = stack.removeAt(stack.lastIndex)
        current.addChild(lastLeaved)
    }

    override fun onTagContent(content: CharSequence) {
        current.addText(content.toString())
    }

    override fun onTagContentEntity(entity: Entities) {
        current.addText(entity.text)
    }

    operator fun String.unaryPlus() {
        current.addText(this)
    }

    override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
        val sb = StringBuilder()
        object : Unsafe {
            override fun String.unaryPlus() {
                sb.append(this)
            }
        }.block()
        current.data.props.innerHTML = sb
    }

    override fun finalize(): NodeBuilder = lastLeaved

    operator fun <T> Stream<T>.invoke(handler: HBuilder.(T) -> Unit) {
        val node = ReactiveNode()
        current.addChild(node)
        val stream = h(this, handler)
        changes = combine(changes, stream) { _, vtree ->
            node.resolved = vtree
            null
        }
    }

    fun paver(width: Int? = null) {
        current.data.paver = PaverData(width)
    }

    var key: Int?
        get() = current.data.key
        set(value) {
            current.data.key = value
        }
}

fun h(handler: HBuilder.() -> Unit): Stream<List<VNode>> =
    with(HBuilder()) {
        handler()
        changes.map {
            base.createChildren()
        }
    }

fun <T> h(stream: Stream<T>, handler: HBuilder.(T) -> Unit): Stream<List<VNode>> =
    stream.flatMap {
        h { handler(it) }
    }

fun appDiv(className: String? = null, handler: HBuilder.() -> Unit) = h {
    div(className) { handler() }
}.map { it.first() }

