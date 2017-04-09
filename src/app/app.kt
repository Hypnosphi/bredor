package app

import kotlinx.html.*
import lib.cycle.dom.DOMSource
import lib.snabbdom.HBuilder
import lib.snabbdom.appDiv
import lib.xstream.*
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import vk.*

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
            key = album.album.id
            id = album.album.id.toString()
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

fun DOMSource.scroll() : Stream<Int> =
    events("wheel")
        .fold(0) { y: Int, e: Event ->
            val dy: Int = e.asDynamic().deltaY
            val y1 = y + dy
            val el = e.currentTarget as Element
            val child = el.firstChild as Element
            val ymax = child.clientHeight - el.clientHeight
            when {
                ymax < 0 -> 0
                y1 < 0 -> 0
                y1 > ymax -> ymax
                else -> {
                    e.preventDefault()
                    y1
                }
            }
        }
        .debug("scroll")
        .throttle(20)
        .startWith(0)

fun DOMSource.makeScroll(): HBuilder.(String, HBuilder.() -> Unit) -> Unit = { className, block ->
    this@makeScroll.select(".$className")
        .scroll()
        .invoke {
            div(className) {
                div {
                    css {
                        position = "relative"
                        top = "${-it}px"
                    }
                    block()
                }
            }
        }
}


fun app(sources: AppSources) : AppSinks {
    val selectOwner: Stream<Int> = sources.DOM
        .select(".owner")
        .events("click")
        .map {
            val el = it.currentTarget as Element
            el.id.toInt()
        }
        .debug("select")

    val scroll = sources.DOM.makeScroll()

    val albumOwners: Stream<List<AlbumOwner>> = sources.VK.me
        .flatMap {
            val me = listOf(it.asAlbumOwner())
            it.groups
                .map {
                    me + it.map { it.asAlbumOwner() }
                }
                .startWith(me)
        }
        .remember()
        .debug("owners")

    val selectedOwner: Stream<AlbumOwner> = albumOwners
        .flatMap { owners ->
            selectOwner
                .map { id ->
                    owners.find { it.uid == id }!!
                }
                .startWith(owners.first())
        }
        .remember()
        .debug("selected")

    val currentAlbumId: Stream<Int> = sources.DOM
        .select(".album")
        .events("click")
        .map {
            val el = it.currentTarget as Element
            el.id.toInt()
        }

    val currentAlbum: Stream<AlbumVM> = selectedOwner
        .flatMap {
            it.albums()
        }
        .flatMap { albums ->
            currentAlbumId
                .map { id ->
                    albums.find { it.album.id == id }!!
                }
        }
        .debug("album")

    val currentPair: Stream<Pair<Photo, Photo>?> = sources.VK.responses
        .debug("resp")
        .filter { it.category == "pair" }
        .map { it.stream }
        .toType<Stream<VKList<Photo>>>()
        .pairwise()
        .debug("pairwise")
        .flatMap {
            combine(it.first, it.second) { fst, snd ->
                fst.first() to snd.first()
            }
        }
        .debug("photopair")
        .toNullable()
        .startWith(null)

    return AppSinks(
        DOM = appDiv("app") {
            currentPair {
                if (it == null) {
                    h1 {
                        +"Выберите альбом, который хотите сортировать"
                    }

                    div("selector") {
                        selectedOwner { selected ->
                            albumOwners { owners ->
                                scroll("owners") {
                                    owners.forEach {
                                        owner(it, it == selected)
                                    }
                                }
                            }
                            scroll("albums") {
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
                } else {
                    div {
                        it.toList().forEach {
                            img(it.text, it.photo_604)
                        }
                    }
                }
            }
        }.debug("vtree"),
        VK = currentAlbum
            .flatMap { (album) ->
                val (a, b) = randomPair(album.size)

                of(a, b)
                    .map {
                        VKReq.Photos.Get {
                            owner_id = album.owner_id
                            album_id = album.id
                            offset = it
                            count = 1
                            category = "pair"
                        }
                    }
                    .debug("req")
                    .toType<VKReq<*>>()
            }
    )
}
