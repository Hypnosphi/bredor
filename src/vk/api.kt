package vk

import app.Builder
import app.Params
import app.invoke
import lib.xstream.Stream
import lib.xstream.jsonpStream
import lib.xstream.of
import kotlin.browser.window

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

        class Get: Photos<VKList<Photo>>("get") {
            companion object : Builder<Get>

            var owner_id by params.Integer()
            var album_id by params.Integer()
            var photo_ids by params.IntList()
            var rev by params.Bool()
            var extended by params.Bool()
            var feed_type by params.EnumString<FeedType>()
            var feed by params.Integer()
            var photo_sizes by params.Bool()
            var offset by params.Integer()
            var count by params.Integer()
        }
    }

    abstract class Groups<T>(method: String): VKReq<T>("groups", method) {
        class Get: Groups<VKList<Group>>("get") {
            companion object : Builder<Get>

            var user_id by params.Integer()
            var extended by params.Bool()
            var filter by params.EnumStringList<GroupFilter>()
            var fields by params.PropList<Group>()
            var offset by params.Integer()
            var count by params.Integer()
        }
    }
}

fun getAlbums(id: Int): Stream<List<AlbumVM>> {
    return VKReq.Photos
        .GetAlbums {
            owner_id = id
            need_covers = true
            photo_sizes = true
        }
        .response()
        .map {
            it.items
                .filter { it.size > 1 }
                .map(::AlbumVM)
        }
}

object VK {
    private val params = Params(window.location.search)

    val access_token by params
    val api_result: Response<Array<User>> by params.Json()

    val me = of(UserVM(api_result.response.first()))
}

