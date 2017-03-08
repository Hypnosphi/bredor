package app

import kotlinx.html.div
import kotlinx.html.img
import lib.snabbdom.appDiv
import lib.xstream.of
import vk.VK
import vk.fullName

fun app(sources: AppSources) : AppSinks {

    return AppSinks(
        DOM = appDiv {
            +"Выберите альбом, который хотите сортировать"

            div("selector") {
                sources.VK.me {
                    div("owners") {
                        with(it.user) {
                            img(fullName, photo_50)
                        }
                    }
                    div("albums") {
                        paver()
                        it.albums {
                            it.forEach {
                                with(it.album) {
                                    img(title, thumb_src)
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
