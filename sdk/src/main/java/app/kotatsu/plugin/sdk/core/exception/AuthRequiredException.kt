package app.kotatsu.plugin.sdk.core.exception

/**
 * Authorization is required for access to the requested content
 */
class AuthRequiredException @JvmOverloads constructor(
	cause: Throwable? = null,
) : RuntimeException("Authorization required", cause)
