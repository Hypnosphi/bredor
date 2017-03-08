package vk

import lib.xstream.*
import kotlin.browser.window
import app.*

external interface VKError {
    val error_msg: String
}

external interface Response<out T> {
    val error: VKError?
    val response: T
}

private const val version = "5.62"
private const val root = "https://api.vk.com/method"

sealed class VKReq<T>(val ns: String, val method: String) {
    protected val params = Params(null,
        "v" to version,
        "access_token" to VK.access_token
    )

    var category: String? = null

    private val urlBase = "$root/$ns.$method"

    val url
        get() = "$urlBase?$params"

    open fun response(): Stream<T> {
        val stream: Stream<Response<T>> = jsonpStream(url)
        return stream.map {
            if (it.error != null) {
                throw Error(it.error!!.error_msg)
            }
            it.response
        }
    }

    abstract class Users<T>(method: String) : VKReq<T>("users", method) {
        class Get : Users<Array<User>>("get") {
            companion object : Builder<Get>

            var user_ids by params.IntList()
            var fields by params.PropList<User>()
            val name_case by params.EnumString<Case>()
        }
    }

    abstract class Photos<T>(method: String) : VKReq<T>("photos", method) {
        class GetAlbums : Photos<VKList<Album>>("getAlbums") {
            companion object : Builder<GetAlbums>

            var owner_id by params.Integer()
            var album_ids by params.IntList()
            var offset by params.Integer()
            var count by params.Integer()
            var need_system by params.Bool()
            var need_covers by params.Bool()
            var photo_sizes by params.Bool()
        }
    }
}

object VK {
    private val params = Params(window.location.search)

    val access_token by params
    val api_result: Response<Array<User>> by params.Json()

    val me = of(UserVM(api_result.response.first()))
}

