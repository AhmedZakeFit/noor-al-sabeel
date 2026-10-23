package io.github.ahmedzakefit.noor.data

import kotlinx.coroutines.suspendCancellableCoroutine
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
 * Cancellation actually aborts the transfer: when the calling coroutine is
 * cancelled (user leaves the screen, overall deadline reached, dialog closed),
 * [HttpURLConnection.disconnect] is invoked from the cancellation handler,
 * which immediately unblocks the worker thread's socket read. This is what
 * bounds the wall-clock time of a search regardless of per-socket timeouts.
 */
internal sealed interface HttpOutcome {
    data class Response(val code: Int, val body: String?) : HttpOutcome
}

internal object Http {

    const val USER_AGENT =
        "NoorAlSabeel/1.0.5 (open-source prayer times app; github.com/AhmedZakeFit/noor-al-sabeel)"

    suspend fun call(
        url: String,
        method: String = "GET",
        formBody: String? = null,
        connectTimeoutMs: Int = 6_000,
        readTimeoutMs: Int = 10_000,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpOutcome = suspendCancellableCoroutine { cont ->
        val conn: HttpURLConnection
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                requestMethod = method
                if (formBody != null) doOutput = true
                setRequestProperty("User-Agent", USER_AGENT)
                if (formBody != null) {
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }
                for ((k, v) in extraHeaders) setRequestProperty(k, v)
            }
        } catch (t: Throwable) {
            if (cont.isActive) cont.resumeWithException(t)
            return@suspendCancellableCoroutine
        }

        // Aborting the connection is the only reliable way to interrupt a
        // blocking socket read on old-style HttpURLConnection.
        cont.invokeOnCancellation { runCatching { conn.disconnect() } }

        thread(isDaemon = true, name = "noor-http") {
            try {
                if (formBody != null) {
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
                runCatching { conn.disconnect() }
            }
        }
    }

    /**
     * Cheap HEAD probe: returns the URL a redirecting endpoint finally lands
     * on. Used by the update check to resolve "/releases/latest" without
     * touching the rate-limited API in the common "no update" case — the
     * plain website endpoint is not subject to the 60 req/h API quota.
     */
    suspend fun finalRedirectUrl(
        url: String,
        connectTimeoutMs: Int = 6_000,
        readTimeoutMs: Int = 8_000,
    ): String? = suspendCancellableCoroutine { cont ->
        val conn: HttpURLConnection
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                requestMethod = "HEAD"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
            }
        } catch (t: Throwable) {
            if (cont.isActive) cont.resumeWithException(t)
            return@suspendCancellableCoroutine
        }
        cont.invokeOnCancellation { runCatching { conn.disconnect() } }
        thread(isDaemon = true, name = "noor-http") {
            try {
                val code = conn.responseCode
                // After following redirects, conn.url is the final address.
                if (cont.isActive) cont.resume(if (code in 200..399) conn.url.toString() else null)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resumeWithException(t)
            } finally {
                runCatching { conn.disconnect() }
            }
        }
    }
}
