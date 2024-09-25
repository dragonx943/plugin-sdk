package app.kotatsu.plugin.sdk.core

public enum class ContentType {

	/**
	 * Standard manga, manhua, webtoons, etc
	 */
	MANGA,

	MANHWA,

	MANHUA,

	/**
	 * Use this if the source provides mostly nsfw content.
	 */
	HENTAI,

	/**
	 * Western comics
	 */
	COMICS,

	NOVEL,

	/**
	 * Use this type if no other suits your needs. For example, for an indie manga
	 */
	OTHER,
	ONE_SHOT,
}
