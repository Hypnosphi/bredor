package lib.cycle.storage

import lib.xstream.MemoryStream

external interface StorageRequest {
    val target: String
    val action: String
    val key: String
    val value: String
}

data class Save (override val key: String, val objValue: Any): StorageRequest {
    override val value = objValue.toString()
    override val target = "local"
    override val action = "setItem"
}

external interface Storage {
    fun getItem(key: String): MemoryStream<String>
}

external interface ResponseCollection {
    val local: Storage
}
