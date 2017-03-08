package vk

import lib.xstream.Stream
import app.*

external interface VKStruct
interface VKStructVM<T: VKStruct>

external interface VKList<T : VKStruct> {
    val count: Int
    val items: Array<T>
}

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

external interface Thumb {
    val src: String
    val width: Int
    val height: Int
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
class AlbumVM(val album: Album) : VKStructVM<Album>

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
