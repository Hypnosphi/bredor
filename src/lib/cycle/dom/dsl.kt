package lib.cycle.dom

import kotlinx.html.*
import lib.xstream.Stream
import lib.xstream.combine
import lib.xstream.flatMap
import lib.xstream.of
import org.w3c.dom.events.Event

interface Child {
    fun appendTo(list: MutableList<VNode>)
}

class Node(val selector: String = "", var text: String? = null) : Child {
    val data = object : VNodeData {
        override var attrs = object {}
        override var props = object {}
    }

    fun setAttr(name: String, value: String?) {
        data.attrs[name] = value
    }

    val children = mutableListOf<Child>()

    fun addText(added: String) {
        when (text) {
            null -> text = added
            else -> text += added
        }
    }

    fun textToChildren() {
        val t = text
        if (t != null) {
            children += Node("span", t)
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
            children.isEmpty() -> h(selector, data, text)
            else -> {
                h(selector, data, createChildren().toTypedArray())
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

class HBuilder : TagConsumer<Node> {
    val base = Node()
    private val stack = mutableListOf(base)
    private val current: Node
        get() = stack.last()
    private lateinit var lastLeaved: Node

    var changes = of(Unit)

    override fun onTagStart(tag: Tag) {
        val node = Node(tag.tagName)
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

    override fun finalize(): Node = lastLeaved

    operator fun <T> Stream<T>.invoke(handler: HBuilder.(T) -> Unit) {
        val node = ReactiveNode()
        current.addChild(node)
        val stream = h(this, handler)
        changes = combine(changes, stream) { _, vtree ->
            node.resolved = vtree
        }
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

fun appDiv(handler: HBuilder.() -> Unit) = h {
    div { handler() }
}.map { it.first() }

