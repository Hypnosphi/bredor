package app

import kotlinx.html.div
import lib.cycle.dom.single
import lib.xstream.of

fun app(sources: AppSources) : AppSinks {
    console.log(sources)
    return AppSinks(
        DOM = single {
            div {
                +"hello "
                of("you").invoke {
                    +it
                }
            }
        }
    )
}
