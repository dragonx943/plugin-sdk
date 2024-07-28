package app.kotatsu.plugin.sdk.util

import app.kotatsu.plugin.sdk.core.ContentRating
import app.kotatsu.plugin.sdk.core.MangaParser
import app.kotatsu.plugin.sdk.core.MangaState
import app.kotatsu.plugin.sdk.core.MangaTag
import app.kotatsu.plugin.sdk.core.exception.ErrorMessages
import app.kotatsu.plugin.sdk.core.exception.ParseException
import org.jsoup.nodes.Element

/**
 * Create a unique id for [Manga]/[MangaChapter]/[MangaPage].
 * @param url must be relative url, without a domain
 * @see [Manga.id]
 * @see [MangaChapter.id]
 * @see [MangaPage.id]
 */
fun MangaParser.generateUid(url: String): Long {
	var h = 1125899906842597L
	authority.forEach { c ->
		h = 31 * h + c.code
	}
	url.forEach { c ->
		h = 31 * h + c.code
	}
	return h
}

/**
 * Create a unique id for [Manga]/[MangaChapter]/[MangaPage].
 * @param id an internal identifier
 * @see [Manga.id]
 * @see [MangaChapter.id]
 * @see [MangaPage.id]
 */
fun MangaParser.generateUid(id: Long): Long {
	var h = 1125899906842597L
	authority.forEach { c ->
		h = 31 * h + c.code
	}
	h = 31 * h + id
	return h
}

fun Element.parseFailed(message: String? = null): Nothing {
	throw ParseException(message, ownerDocument()?.location() ?: baseUri(), null)
}

fun Set<MangaTag>?.oneOrThrowIfMany(): MangaTag? {
	return when {
		isNullOrEmpty() -> null
		size == 1 -> first()
		else -> throw IllegalArgumentException(ErrorMessages.FILTER_MULTIPLE_GENRES_NOT_SUPPORTED)
	}
}

fun Set<MangaState>?.oneOrThrowIfMany(): MangaState? {
	return when {
		isNullOrEmpty() -> null
		size == 1 -> first()
		else -> throw IllegalArgumentException(ErrorMessages.FILTER_MULTIPLE_STATES_NOT_SUPPORTED)
	}
}

fun Set<ContentRating>?.oneOrThrowIfMany(): ContentRating? {
	return when {
		isNullOrEmpty() -> null
		size == 1 -> first()
		else -> throw IllegalArgumentException(ErrorMessages.FILTER_MULTIPLE_CONTENT_RATING_NOT_SUPPORTED)
	}
}
