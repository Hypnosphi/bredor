package lib.cycle.dom

import org.w3c.dom.events.KeyboardEvent

fun DOMSource.clicks() = events("click")

fun DOMSource.keyups(key: String) = events("keyup")
    .debug("keyup")
    .filter {
        val e = it as KeyboardEvent
        e.key === key
    }
