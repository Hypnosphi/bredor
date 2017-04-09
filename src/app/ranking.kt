package app

import kotlin.js.Math

class Ranking(val albumId: Int, serialized: String?) {
    val map = HashMap<Int, Double>()

    init {
        if (serialized != null) {
            val parsed: dynamic = JSON.parse(serialized)
            val keys: Array<String> = js("Object").keys(parsed)
            for (key in keys) {
                if (parsed.hasOwnProperty(key)) {
                    map[key.toInt()] = parsed[key] as Double
                }
            }
        }
    }

    fun serialize(): String {
        val obj = js {
            for (key in map.keys) {
                this[key] = map[key]
            }
        }
        return JSON.stringify(obj)
    }

    fun pair(winner: Int, loser: Int) {
        val r1 = map[winner] ?: 0.0
        val r2 = map[loser] ?: 0.0
        val d = 1 / (1 + Math.pow(2.0, r1 - r2))
        map[winner] = r1 + d
        map[loser] = r2 - d
    }

}
