package app

import kotlinx.html.div
import lib.cycle.dom.DOMSource
import lib.snabbdom.HBuilder
import lib.xstream.Stream
import lib.xstream.fold
import lib.xstream.throttle
import org.w3c.dom.Element
import org.w3c.dom.events.Event

fun DOMSource.scroll() : Stream<Int> =
    events("wheel")
        .fold(0) { y: Int, e: Event ->
            e.preventDefault()
            val dy: Int = e.asDynamic().deltaY
            val y1 = y + dy
            val el = e.currentTarget as Element
            val child = el.firstChild as Element
            val ymax = child.clientHeight - el.clientHeight
            when {
                ymax < 0 -> 0
                y1 < 0 -> 0
                y1 > ymax -> ymax
                else -> y1
            }
        }
        .debug("scroll")
        .throttle(20)
        .startWith(0)

fun DOMSource.makeScroll(): HBuilder.(String, HBuilder.() -> Unit) -> Unit = { className, block ->
    this@makeScroll.select(".$className")
        .scroll()
        .invoke {
            div(className) {
                div {
                    css {
                        position = "relative"
                        top = "${-it}px"
                    }
                    block()
                }
            }
        }
}
