package lib.xstream

external interface Stream<out T> {
    fun <U> map(project: (T) -> U): Stream<U>
    @JsName("map")
    fun <U> mapStream(project: (T) -> Stream<U>): MetaStream<U>

    fun debug(label: String? = definedExternally) : Stream<T>
    fun debug(spy: (T) -> Unit) : Stream<T>
}

external interface MetaStream<out T> : Stream<Stream<T>> {
    fun flatten(): Stream<T>
}

external interface MemoryStream<out T> : Stream<T>


