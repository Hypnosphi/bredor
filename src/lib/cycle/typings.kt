package lib.cycle

import lib.xstream.Stream

external interface Sinks

external interface Sources

typealias DriverFunction<T, S> = (Stream<T>, String) -> S

external interface DriversDefinition
