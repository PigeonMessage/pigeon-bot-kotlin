package io.github.furka.pigeon.config

import io.ktor.http.*

const val DEFAULT_BASE_URL = "http://localhost:8080"
const val DEFAULT_API_PREFIX = "/api/v1"
const val DEFAULT_WS_PATH = "/ws"

data class ClientConfig(
    val token: String,
    val baseUrl: String? = null,
    val wsUrl: String? = null,
    val autoReconnect: Boolean = true,
    val reconnectIntervalMs: Long = 5000
)


fun resolveBaseUrl(config: ClientConfig): String {
    return (config.baseUrl ?: DEFAULT_BASE_URL).trimEnd('/')
}

fun resolveApiUrl(config: ClientConfig, path: String): String {
    val base = resolveBaseUrl(config)
    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return "$base$DEFAULT_API_PREFIX$normalizedPath"
}

fun resolveWsUrl(config: ClientConfig): String {
    if (config.wsUrl != null) {
        return config.wsUrl
    }

    val httpBase = resolveBaseUrl(config)
    val parsed = URLBuilder(httpBase).build()
    val protocol = if (parsed.protocol.isSecure()) "wss" else "ws"
    val origin = "$protocol://${parsed.hostWithPort}"
    return "$origin$DEFAULT_WS_PATH"
}