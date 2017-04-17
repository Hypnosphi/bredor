package app

import kotlinx.html.*
import lib.cycle.dom.clicks
import lib.cycle.dom.cls
import lib.cycle.dom.keyups
import lib.cycle.storage.Save
import lib.snabbdom.HBuilder
import lib.snabbdom.appDiv
import lib.xstream.*
import org.w3c.dom.Element
import org.w3c.dom.Window
import org.w3c.dom.events.Event
import vk.*
import kotlin.browser.window
import kotlin.js.Date

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

fun albumKey(id: Int) = "rankings_$id"

const val helpKey = "help_seen"

fun app(sources: AppSources) : AppSinks {
//    sources.DOM.clicks().limitBandwidth(1000, 3).addListener {
//        next = {
//            console.log(Date().getTime())
//        }
//    }

    sources.DOM
        .select(".btn:not(.choose)")
        .clicks()
        .addListener {
            next = {
                it.stopPropagation()
            }
        }

    val selectOwner: Stream<Int> = sources.DOM
        .cls("owner")
        .clicks()
        .map {
            val el = it.currentTarget as Element
            el.id.toInt()
        }
        .debug("select")

    val scroll = sources.DOM.makeScroll()

    val albumOwners: Stream<List<AlbumOwner>> = sources.VK.me
        .switchMap {
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
        .switchMap { owners ->
            selectOwner
                .map { id ->
                    owners.find { it.uid == id }!!
                }
                .startWith(owners.first())
        }
        .remember()
        .debug("selected")

    val currentAlbumId: Stream<Int> = sources.DOM
        .cls("album")
        .clicks()
        .map {
            val el = it.currentTarget as Element
            el.id.toInt()
        }

    val currentAlbum: Stream<AlbumVM> = selectedOwner
        .switchMap {
            it.albums()
        }
        .switchMap { albums ->
            currentAlbumId
                .map { id ->
                    albums.find { it.album.id == id }!!
                }
        }
        .debug("album")

    val selectPair/*: Stream<Pair<Photo, Photo>>*/ = sources.VK.responses
        .debug {
            console.log("resp ${Date().getTime()} $it")
        }
        .filter { it.category == "pair" }
        .map { it.stream }
        .toType<Stream<VKList<Photo>>>()
        .couples()
        .debug("zipped")
        .switchMap {
            combine(it.first.debug("fst"), it.second.debug("snd")) { fst, snd ->
                fst.first() to snd.first()
            }
        }
        .debug("photopair")

    val currentPair = merge<Pair<Photo, Photo>?> (
        selectPair.toType(),
        sources.DOM.cls("back").clicks().map {
            null
        }.toType(),
        sources.DOM.cls("pair").keyups("Escape").map { null }.toType()
    ).startWith(null)

    val rankings: Stream<Ranking> = currentAlbum
        .switchMap {
            val album = it.album
            sources.storage.local.getItem(albumKey(album.id))
                .map { Ranking(album, it) }
        }
        .remember()

    val selectWinner: Stream<Pair<Int, Int>> = selectPair
        .switchMap { (first, second) ->
            val firstWon = first.id to second.id
            val secondWon = second.id to first.id
            merge(
                sources.DOM.cls("half").clicks().map {
                    val el = it.currentTarget as Element
                    when (el.id.toInt()) {
                        first.id -> firstWon
                        else -> secondWon
                    }
                },
                sources.DOM.cls("vertical").keyups("ArrowUp").map { firstWon },
                sources.DOM.cls("vertical").keyups("ArrowDown").map { secondWon },
                sources.DOM.cls("horizontal").keyups("ArrowLeft").map { firstWon },
                sources.DOM.cls("horizontal").keyups("ArrowRight").map { secondWon }
            )
        }

    val selectedWinner: Stream<Int> = rankings
        .switchMap { ranking ->
            selectWinner.map { (winner, loser) ->
                ranking.pair(winner, loser)
                winner
            }
        }
        .debug("winner")

    val justSelected: Stream<Int> = merge(
        selectWinner.map { it.first },
        currentPair.map { -1 }
    ).remember()

    val pairRequests: Stream<VKReq<*>> = currentAlbum
        .switchMap { (album) ->
            selectWinner
                .toNullable()
                .startWith(null)
                .switchMap {

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
                }
        }.toType()

    val sortAlbum: Stream<Event> = sources.DOM
        .cls("sort")
        .clicks()

    val sortRequests: Stream<VKReq<*>> = rankings
        .switchMap { ranking ->
            sortAlbum.switchMap {
                from(ranking.sort().toTypedArray())
                    .map {
                        VKReq.Photos.ReorderPhotos {
                            owner_id = ranking.album.owner_id
                            photo_id = it.key
                            category = "sort"
                            // place first
                            before = it.key
                        }
                    }
            }
        }.toType()

    val sortResponses: Stream<Int> = sources.VK
        .select("sort")

    val reqCount = merge(
        sortRequests.map { 1 },
        sortResponses.debug("sortResp").map { -1 }
    ).fold(0) { a, b -> a + b }.debug("reqCount")

    val complete = rankings
        .toNullable()
        .startWith(null)
        .switchMap {
            reqCount
                .drop(1)
                .filter { it == 0 }
                .map { true }
                .startWith(false)
        }
        .remember()
        .debug("complete")

    val showHelp: Stream<Boolean> = sources.storage.local
        .getItem(helpKey)
        .map { it !== "true" }

    val hideHelp = merge(
        sources.DOM.cls("paranja").clicks(),
        sources.DOM.cls("confirm").clicks(),
        sources.DOM.cls("dialog").keyups("Enter")
    )

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
                    showHelp {
                        div("dialog") {
                            tabIndex = "0"

                            if(!it) {
                                css {
                                    display = "none"
                                }
                            }

                            div("paranja")
                            div("help") {
                                +"Выбирайте более удачное фото кликом по нему или стрелочкой клавиатуры. Когда надоест или просто решите, что достаточно, нажмите кнопку ✓. Это запустит сортировку фотографий в альбоме по рассчитанному рейтингу."
                                button(type = ButtonType.button, classes = "confirm") {
                                    +"Ладно"
                                }
                                span("close") {
                                    +"×"
                                }
                            }
                        }
                    }
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
                                +"‹"
                                title = "Назад"
                            }

                            combine(reqCount, complete).invoke {
                                val count: Int = it[0]
                                val ok: Boolean = it[1]
                                div("button snd") {
                                    if (count > 0) {
                                        classes += "loading"
                                        span {
                                            +"◌"
                                        }
                                    } else {
                                        classes += "sort"
                                        if (ok) {
                                            classes += "ok"
                                        }
                                        +"✓"
                                    }
                                    title = "Запустить сортировку"
                                }
                            }

                            a(list.first().albumUrl, "_blank", "button view") {
                                title = "Перейти к альбому"
                                +"👁"
                            }

                            list.forEach {
                                div("half") {
                                    id = it.id.toString()
                                    key = it.id
                                    img(it.text, it.photo_75, "photoBg")
                                    img(it.text, it.getForRect(fit(it)), "photo")
                                    justSelected { winner ->
                                        div("button choose") {
                                            if (winner == it.id) {
                                                classes += "ok"
                                                +"✓"
                                            } else {
                                                +"▲"
                                                title = "Выберите это фото кликом по нему или стрелочкой клавиатуры"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.debug("vtree"),
        VK = merge(pairRequests, sortRequests),
        storage = merge(
            rankings.switchMap { ranking ->
                selectedWinner.map {
                    Save(albumKey(ranking.album.id), ranking.serialize())
                }
            },
            hideHelp.map {
                Save(helpKey, "true")
            }
        ).debug("save").toType()
    )
}
