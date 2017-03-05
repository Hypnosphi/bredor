package vk

import lib.xstream.Stream
import app.*

external interface VKStruct
interface VKStructVM<T: VKStruct>

external interface User : VKStruct {
    val id: Int
    val first_name: String
    val last_name: String
    val photo_50: String
}
data class UserVM(val user: User) : VKStructVM<User> {
    val albums: Stream<List<AlbumVM>> by lazy {
        VKReq.Photos.GetAlbums {
            owner_id = user.id
        }.response().map {
            it.map(::AlbumVM)
        }
    }
}

enum class Case {
    nom, gen, dat, acc, ins, abl
}

val User.fullName get() = "$first_name $last_name"

external interface Album : VKStruct {
    val id: Long
    val thumb_id: Long
    val owner_id: Long
    val title: String
    val description: String
    val created: Int
    val updated: Int
    val size: Int
    val can_upload: Int
    val privacy_view: Array<Any>
    val privacy_comment: Array<Any>
    val upload_by_admins_only: Int
    val comments_disabled: Int
    val thumb_src: String
}
class AlbumVM(val album: Album) : VKStructVM<Album>
