@file:JsModule("@cycle/run")
package lib.cycle

external fun <So: Sources, Si: Sinks> run(main: (sources: So) -> Si, drivers: DriversDefinition): () -> Unit
