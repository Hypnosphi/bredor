package app

import kotlinx.html.img
import lib.cycle.dom.appDiv
import lib.xstream.of
import vk.VK
import vk.fullName

fun app(sources: AppSources) : AppSinks {

    return AppSinks(
        DOM = appDiv {
            +"Выберите альбом, который хотите сортировать"

            sources.VK.me {
                with(it.user) {
                    img(fullName, photo_50)
                }
                it.albums {
                    it.forEach {
                        with(it.album) {
                            img(title, thumb_src)
                        }
                    }
                }
            }
        }.debug("vtree"),
        VK = of()
    )
}
