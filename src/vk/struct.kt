package vk

import app.invoke
import lib.xstream.Stream

external interface VKStruct
interface VKStructVM<T: VKStruct>

external interface VKList<T : VKStruct> {
    val count: Int
    val items: Array<T>
}

fun <T : VKStruct> VKList<T>.toList() = items.toList()
fun <T : VKStruct> VKList<T>.first() = items.first()

external interface User : VKStruct {
    val id: Int
    val first_name: String
    val last_name: String
    val photo_50: String
}
data class UserVM(val user: User) : VKStructVM<User> {
    val albums: Stream<List<AlbumVM>> by lazy {
        getAlbums(user.id).remember()
    }
    val groups: Stream<List<GroupVM>> by lazy {
        VKReq.Groups
            .Get {
                user_id = user.id
                extended = true
                filter = listOf(GroupFilter.editor)
                fields = listOf(Group::photo_50, Group::main_album_id)
                count = 1000
            }
            .response()
            .map {
                it.items
                    .filter { it.main_album_id != null }
                    .map(::GroupVM)
            }
            .remember()
    }
}

enum class Case {
    nom, gen, dat, acc, ins, abl
}

val User.fullName get() = "$first_name $last_name"

external interface Rect {
    val width: Double
    val height: Double
}

val Rect.ratio : Double get() = height / width
val Rect.min: Double get() = minOf(width, height)
val Rect.max: Double get() = maxOf(width, height)

external interface Thumb: Rect {
    val src: String
    val type: String
}

external interface Album : VKStruct {
    val id: Int
    val thumb_id: Int
    val owner_id: Int
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
    val sizes: Array<Thumb>
}
data class AlbumVM(val album: Album) : VKStructVM<Album>

val Album.url get() = "https://vk.com/album${owner_id}_$id"

external interface Photo : VKStruct, Rect {
    val id: Int
    val album_id: Int
    val owner_id: Int
    val user_id: Int
    val text: String
    val date: Int
    val photo_75: String
    val photo_130: String?
    val photo_604: String?
    val photo_807: String?
    val photo_1280: String?
    val photo_2560: String?
}

val Photo.albumUrl get() = "https://vk.com/album${owner_id}_${album_id}"

fun Photo.getForRect(rect: Rect): String = when {
    rect.height > 1080.0 || rect.width > 1024.0 && photo_2560 != null -> photo_2560!!
    rect.max > 807 && photo_1280 != null -> photo_1280!!
    rect.max > 604 && photo_807 != null -> photo_807!!
    rect.max > 130 && photo_604 != null -> photo_604!!
    rect.max > 75 && photo_130 != null -> photo_130!!
    else -> photo_75
}

external interface Group : VKStruct {
    val id: Int
    val name: String
    val screen_name: String
    val is_closed: Int
    val deactivated: String
    val is_admin: String
    val admin_level: Int
    val is_member: Int
    val invited_by: Int
    val type: String
    val has_photo: Int
    val photo_50: String
    val photo_100: String
    val photo_200: String
    val main_album_id: Int?
}

val Group.uid get() = -id

class GroupVM(val group: Group) : VKStructVM<Group> {
    val albums: Stream<List<AlbumVM>> by lazy {
        getAlbums(group.uid)
            .map {
                it.filter { it.album.can_upload == 1 }
            }
            .remember()
    }
}

enum class GroupFilter {
    admin, editor, moder, groups, publics, events
}

enum class FeedType {
    post, photo, photo_tag, wall_photo, friend, note, audio, video
}
