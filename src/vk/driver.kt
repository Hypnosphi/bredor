package vk

import lib.cycle.DriverFunction
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
            it.stream.toType<T>()
        }


val VKDriver : DriverFunction<VKReq<*>, VKSource> = {
    val responses = it
        .limitBandwidth(1000, 3)
        .map {
            ResponseStream(
                it.category,
                it.response().remember().apply {
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
