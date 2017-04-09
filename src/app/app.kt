package app

import kotlinx.html.*
import lib.cycle.dom.clicks
import lib.cycle.dom.keyups
import lib.snabbdom.HBuilder
import lib.snabbdom.appDiv
import lib.xstream.*
import org.w3c.dom.Element
import org.w3c.dom.Window
import vk.*
import kotlin.browser.window

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

            css {
                width = size(thumb.width.toInt())
                height = size(thumb.height.toInt())
            }

            img(this@with.title, thumb.src, "albumImg")

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

data class RectImpl(override val width: Double, override val height: Double) : Rect

fun windowRect(w: Window) = RectImpl(795.0, w.innerHeight.toDouble())

fun app(sources: AppSources) : AppSinks {
    val selectOwner: Stream<Int> = sources.DOM
        .select(".owner")
        .clicks()
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
        .clicks()
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

    val selectPair: Stream<Pair<Photo, Photo>> = sources.VK.responses
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

    val currentPair = merge<Pair<Photo, Photo>?> (
        selectPair.toType(),
        sources.DOM.select(".back").clicks().map { null }.toType(),
        sources.DOM.select(".app").keyups("Escape").map { null }.toType()
    ).startWith(null)

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
                    of(windowRect(window)).invoke { rect ->
                        val list = it.toList();

                        fun makeFit(cont: RectImpl) = { img: Rect ->
                            when {
                                img.ratio > cont.ratio -> cont.copy(width = cont.height / img.ratio)
                                img.ratio < cont.ratio -> cont.copy(height = cont.width * img.ratio)
                                else -> cont
                            }
                        }

                        val hFit = makeFit(rect.copy(width = rect.width / 2))
                        val vFit = makeFit(rect.copy(height = rect.height / 2))
                        val hMin = list.map(hFit).map(Rect::min).min()!!
                        val vMin = list.map(vFit).map(Rect::min).min()!!

                        val horizontal = hMin > vMin
                        val fit = if (horizontal) hFit else vFit

                        div("pair") {
                            tabIndex = "0"
                            classes += if (horizontal) "horizontal" else "vertical"

                            div("button back") {
                                +"×"
                            }
                            list.forEach {
                                div("half") {
                                    img(it.text, it.photo_75, "photoBg")
                                    img(it.text, it.getForRect(fit(it)), "photo")
                                }
                            }
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
