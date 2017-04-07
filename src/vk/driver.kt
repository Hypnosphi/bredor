package vk

import lib.cycle.DriverFunction
import lib.xstream.Stream
import lib.xstream.addListener
import lib.xstream.flatMap
import lib.xstream.toType

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
            it.stream.toType<T>()
        }


@Suppress("UNUSED_PARAMETER")
val VKDriver : DriverFunction<VKReq<*>, VKSource> = { sink, name ->
    val responses = sink.map {
        ResponseStream(
            it.category,
            it.response().apply {
                addListener()
            }
        )
    }
    responses.addListener()
    VKSource(
        responses,
        VK.me
    )
}
