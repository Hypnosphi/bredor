@file:JsModule("xstream/extra/flattenSequentially")
package lib.xstream

@JsName("default")
external fun <T> fromDiagram(diagram: String, options: FromDiagramOptions = definedExternally): Stream<T>
