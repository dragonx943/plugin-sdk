@file:JvmName("EnumUtils")

package app.kotatsu.plugin.sdk.util

import kotlin.enums.EnumEntries

public fun <E : Enum<E>> EnumEntries<E>.names(): Array<String> = Array(size) { i ->
    get(i).name
}

public fun <E : Enum<E>> EnumEntries<E>.find(name: String?): E? {
    if (name == null) {
        return null
    }
    return find { x -> x.name == name }
}

public fun <E : Enum<E>> Collection<E>.find(name: String?): E? {
    if (name == null) {
        return null
    }
    return find { x -> x.name == name }
}


internal fun <E : Enum<E>> Iterable<E>.namesToString() = joinToString(",") { it.name }