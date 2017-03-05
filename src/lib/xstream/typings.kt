package lib.xstream

external interface Listener<T> {
    var next: (x: T) -> Unit
    var error: (err: Any) -> Unit
    var complete: () -> Unit
}

external interface Producer<T> {
    var start: (listener: Listener<T>) -> Unit
    var stop: () -> Unit
}

external interface Stream<T> {
    fun addListener(listener: Listener<T>)

    fun <U> map(project: (T) -> U): Stream<U>
    @JsName("map")
    fun <U> mapStream(project: (T) -> Stream<U>): MetaStream<U>

    fun filter(passes: (t: T) -> Boolean): Stream<T>

    fun debug(label: String? = definedExternally) : Stream<T>
    fun debug(spy: (T) -> Unit) : Stream<T>
}

external interface MetaStream<T> : Stream<Stream<T>> {
    fun flatten(): Stream<T>
}

external interface MemoryStream<T> : Stream<T>


