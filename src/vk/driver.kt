package vk

import lib.cycle.DriverFunction
import lib.xstream.Stream
import lib.xstream.flatMap
import vk.UserVM
import vk.VK
import vk.VKReq

import lib.cycle.*
import lib.xstream.*

data class ResponseStream<T>(
    val category: String?,
    val stream: Stream<T>
)

data class VKSource (
    val responses : Stream<ResponseStream<*>>,
    val me: Stream<UserVM>
)

fun <T> VKSource.select(category: String) : Stream<T> =
    responses
        .filter {
            it.category == category
        }
        .flatMap {
            @Suppress("UNCHECKED_CAST", "UNCHECKED_CAST_TO_NATIVE_INTERFACE")
            it.stream as Stream<T>
        }


@Suppress("UNUSED_PARAMETER")
val VKDriver : DriverFunction<VKReq<*>, VKSource> = { sink, name ->
    VKSource(
        sink.map {
            ResponseStream(
                it.category,
                it.response().apply {
                    addListener()
                }
            )
        },
        VK.me
    )
}
