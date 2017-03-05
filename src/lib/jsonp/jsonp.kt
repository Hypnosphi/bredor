package lib.jsonp

external interface JsonpOptions {
    val param: String
    val timeout: Int
    val prefix: String
    val name: String
}

@JsModule("jsonp")
external fun <T> jsonp(url: String, opts: JsonpOptions? = definedExternally, fn: (Error?, T) -> Unit) : () -> Unit
