package app

import kotlinx.html.*
import lib.paver.paver
import lib.snabbdom.HBuilder
import lib.snabbdom.appDiv
import lib.xstream.Stream
import lib.xstream.combine
import lib.xstream.flatMap
import lib.xstream.of
import org.w3c.dom.Element
import vk.*
import vk.VK.me
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus

data class AlbumOwner(
    val uid: Int,
    val name: String,
    val photo: String,
    val albums: () -> Stream<List<AlbumVM>>
)

fun UserVM.asAlbumOwner() = AlbumOwner(user.id, user.fullName, user.photo_50) { albums }
fun GroupVM.asAlbumOwner() = AlbumOwner(group.uid, group.name, group.photo_50) { albums }

fun HBuilder.owner(owner: AlbumOwner, isSelected: Boolean = false) {
    with(owner) {
        div("owner") {
            key = uid
            id = uid.toString()
            if (isSelected) {
                classes += "selected"
            }
            img(name, photo) {
                height = "50"
                width = "50"
            }
            div("blackout")
            div("ownerName") {
                +name
            }
        }
    }
}

data class WithSelected(
    val owners: List<AlbumOwner>,
    val selected: AlbumOwner
)

fun app(sources: AppSources) : AppSinks {
    val selectOwner: Stream<Int> = sources.DOM
        .select(".owner")
        .events("click")
        .map {
            val el = it.currentTarget as Element
            el.id.toInt()
        }
        .startWith(0)
        .debug("select")

    val albumOwners: Stream<List<AlbumOwner>> = sources.VK.me
        .flatMap {
            val me = listOf(it.asAlbumOwner())
            it.groups
                .map {
                    me + it.map { it.asAlbumOwner() }
                }
                .startWith(me)
        }
        .debug("owners")

    val withSelected: Stream<WithSelected> =
        combine(selectOwner, albumOwners) { id, owners ->
            val selected = when(id){
                0 -> owners.first()
                else -> owners.find { it.uid == id }!!
            }
            WithSelected(owners, selected)
        }
        .debug("selected")

    return AppSinks(
        DOM = appDiv("app") {

            h1 {
                +"Выберите альбом, который хотите сортировать"
            }

            div("selector") {
                withSelected { (owners, selected) ->
                    div("owners") {
                        owners.forEach {
                            owner(it, it == selected)
                        }
                    }
                    div("albums") {
                        key = selected.uid
                        val albums = selected.albums()
                        paver()
                        albums {
                            when {
                                it.isNotEmpty() -> {
                                    it.forEach {
                                        with(it.album) {
                                            val thumb = sizes.find { it.type == "x" }!!
                                            img(title, thumb.src) {
                                                fun size(size: Int): String {
                                                    val realSize = if (size != 0) size else 600
                                                    return realSize.toString()
                                                }

                                                width = size(thumb.width)
                                                height = size(thumb.height)
                                            }
                                        }
                                    }
                                }
                                else -> h3 { +"Здесь пока нет альбомов" }
                            }
                        }
                    }
                }
            }
        }.debug("vtree"),
        VK = of()
    )
}
