package app.kotatsu.plugin.sdk.core.exception

import okio.IOException

class CloudFlareBlockedException(
	val url: String,
) : IOException("Blocked by CloudFlare")
