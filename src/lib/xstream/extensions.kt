package lib.xstream

fun <T, U> Stream<T>.flatMap(project: (T) -> Stream<U>) =
    mapStream(project).flatten()

fun <T1, T2, U> combine(s1: Stream<T1>, s2: Stream<T2>, project: (T1, T2) -> U) =
    combine(s1, s2).map { values ->
        @Suppress("UNCHECKED_CAST")
        project(values[0] as T1, values[1] as T2)
    }
