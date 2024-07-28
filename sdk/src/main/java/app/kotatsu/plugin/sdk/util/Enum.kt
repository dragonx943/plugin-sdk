@file:JvmName("EnumUtils")

package app.kotatsu.plugin.sdk.util

import kotlin.enums.EnumEntries

fun <E : Enum<E>> EnumEntries<E>.names() = Array(size) { i ->
	get(i).name
}

fun <E : Enum<E>> EnumEntries<E>.find(name: String?): E? {
	if (name == null) {
		return null
	}
	return find { x -> x.name == name }
}
