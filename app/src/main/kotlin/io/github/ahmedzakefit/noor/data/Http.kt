package io.github.ahmedzakefit.noor.data

import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal cancellable HTTP helper shared by the mosque-search sources and the
 * update checker. Plain [HttpURLConnection] on a short-lived worker thread —
 * no external dependency.
 *
 * Handles HTTP/HTTPS redirects across domains (e.g. github.com to
 * release-assets.githubusercontent.com) which standard Java HttpURLConnection
 * does not follow automatically for security boundaries.
 */
internal sealed interface HttpOutcome {
    data class Response(val code: Int, val body: String?) : HttpOutcome
    /** HEAD probe result after following redirects. */
    data class Head(val code: Int, val contentLength: Long) : HttpOutcome
}

internal object Http {
    const val USER_AGENT =
        "NoorAlSabeel/1.0.6 (open-source prayer times app; github.com/AhmedZakeFit/noor-al-sabeel)"

    /**
     * Opens an [HttpURLConnection], explicitly following 3xx redirects (up to
     * [maxRedirects]) across host boundaries while enforcing [urlValidator].
     */
    fun openWithRedirects(
        initialUrl: String,
        method: String = "GET",
        connectTimeoutMs: Int = 10_000,
        readTimeoutMs: Int = 30_000,
        maxRedirects: Int = 5,
        urlValidator: ((String) -> Boolean)? = null,
        headers: Map<String, String> = emptyMap(),
    ): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (true) {
            if (urlValidator != null && !urlValidator(currentUrl)) {
                throw SecurityException("Disallowed URL in redirect chain: $currentUrl")
            }
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                this.connectTimeout = connectTimeoutMs
                this.readTimeout = readTimeoutMs
                this.requestMethod = method
                this.instanceFollowRedirects = false
                this.setRequestProperty("User-Agent", USER_AGENT)
                for ((k, v) in headers) {
                    this.setRequestProperty(k, v)
                }
            }
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("HTTP $code redirect without Location header from $currentUrl")
                }
                val nextUrl = URL(URL(currentUrl), location).toString()
                redirects++
                if (redirects > maxRedirects) {
                    throw IOException("Too many redirects (> $maxRedirects) starting from $initialUrl")
                }
                currentUrl = nextUrl
                continue
            }
            return conn
        }
    }

    suspend fun call(
        url: String,
        method: String = "GET",
        formBody: String? = null,
        connectTimeoutMs: Int = 6_000,
        readTimeoutMs: Int = 10_000,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpOutcome = suspendCancellableCoroutine { cont ->
        var activeConn: HttpURLConnection? = null
        cont.invokeOnCancellation { runCatching { activeConn?.disconnect() } }
        thread(isDaemon = true, name = "noor-http") {
            try {
                val conn = openWithRedirects(
                    initialUrl = url,
                    method = method,
                    connectTimeoutMs = connectTimeoutMs,
                    readTimeoutMs = readTimeoutMs,
                    headers = extraHeaders,
                )
                activeConn = conn
                if (formBody != null) {
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(formBody) }
                }
                val code = conn.responseCode
                val body: String? = if (code in 200..399) {
                    runCatching { conn.inputStream.bufferedReader(Charsets.UTF_8).readText() }.getOrNull()
                } else {
                    null
                }
                if (cont.isActive) cont.resume(HttpOutcome.Response(code, body))
            } catch (t: Throwable) {
                if (cont.isActive) cont.resumeWithException(t)
            } finally {
                runCatching { activeConn?.disconnect() }
            }
        }
    }

    suspend fun finalRedirectUrl(
        url: String,
        connectTimeoutMs: Int = 6_000,
        readTimeoutMs: Int = 8_000,
    ): String? = suspendCancellableCoroutine { cont ->
        var activeConn: HttpURLConnection? = null
        cont.invokeOnCancellation { runCatching { activeConn?.disconnect() } }
        thread(isDaemon = true, name = "noor-http") {
            try {
                val conn = openWithRedirects(
                    initialUrl = url,
                    method = "HEAD",
                    connectTimeoutMs = connectTimeoutMs,
                    readTimeoutMs = readTimeoutMs,
                )
                activeConn = conn
                val code = conn.responseCode
                val finalUrl = conn.url.toString()
                if (cont.isActive) cont.resume(if (code in 200..399) finalUrl else null)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resumeWithException(t)
            } finally {
                runCatching { activeConn?.disconnect() }
            }
        }
    }

    suspend fun head(
        url: String,
        connectTimeoutMs: Int = 6_000,
        readTimeoutMs: Int = 8_000,
    ): HttpOutcome.Head = suspendCancellableCoroutine { cont ->
        var activeConn: HttpURLConnection? = null
        cont.invokeOnCancellation { runCatching { activeConn?.disconnect() } }
        thread(isDaemon = true, name = "noor-http") {
            try {
                val conn = openWithRedirects(
                    initialUrl = url,
                    method = "HEAD",
                    connectTimeoutMs = connectTimeoutMs,
                    readTimeoutMs = readTimeoutMs,
                )
                activeConn = conn
                val code = conn.responseCode
                val length = conn.contentLengthLong
                if (cont.isActive) {
                    cont.resume(HttpOutcome.Head(code, length))
                }
            } catch (t: Throwable) {
                if (cont.isActive) cont.resumeWithException(t)
            } finally {
                runCatching { activeConn?.disconnect() }
            }
        }
    }
}
