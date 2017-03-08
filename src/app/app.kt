package app

import kotlinx.html.*
import lib.cycle.dom.DOMSource
import lib.paver.paver
import lib.snabbdom.HBuilder
import lib.snabbdom.appDiv
import lib.xstream.*
import org.w3c.dom.Element
import org.w3c.dom.events.MouseEvent
import vk.*
import vk.VK.me
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus
import kotlin.js.Math

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

            when {
                uid < 0 -> {
                    a("https://vk.com/club${-uid}", "_blank", "ownerName") {
                        +name
                    }
                }
                else -> div("ownerName") { +name }
            }
        }
    }
}

fun HBuilder.album(album: AlbumVM) {
    with(album.album) {
        div("album") {
            val thumb = sizes.find { it.type == "x" }!!
            fun size(size: Int): String {
                val realSize = if (size != 0) size else 600
                return "${realSize}px"
            }

            val width = size(thumb.width)
            val height = size(thumb.height)

            style = "width: $width; height: $height; background-image:url(${thumb.src});"

            div("description") {
                div("name") { +this@with.title }
                div("count") { +"$size фото" }
            }
        }
    }
}

data class WithSelected(
    val owners: List<AlbumOwner>,
    val selected: AlbumOwner
)

fun DOMSource.scroll() : Stream<String> =
    events("wheel")
        .map { it.asDynamic().deltaY }
        .fold(0) { a: Int, b: Int -> if (a < b) a - b else 0 }
        .map { "top: ${it}px;" }

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

    val scrollOwners: Stream<String> = sources.DOM
        .select(".owners")
        .scroll()

    val scrollAlbums: Stream<String> = sources.DOM
        .select(".albums")
        .scroll()

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
                    scrollOwners {
                        div("owners") {
                            div {
                                style = it
                                owners.forEach {
                                    owner(it, it == selected)
                                }
                            }
                        }
                    }
                    scrollAlbums {
                        div("albums") {
                            div {
                                style = it
                                key = selected.uid
                                val albums = selected.albums()
                                paver()
                                albums {
                                    when {
                                        it.isNotEmpty() -> it.forEach { album(it) }
                                        else -> h3 { +"Здесь пока нет альбомов" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.debug("vtree"),
        VK = of()
    )
}
