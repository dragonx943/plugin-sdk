package app.kotatsu.plugin.sdk.core.exception

import okhttp3.Headers
import okio.IOException

class CloudFlareProtectedException(
	val url: String,
	@Transient val headers: Headers,
) : IOException("Protected by CloudFlare")
