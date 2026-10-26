package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.OverpassClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Regression tests for the "mosque search fails after a short loading period"
 * report (v1.0.2): the client must retry official mirror endpoints on
 * transient failures and classify every failure into a reason the UI can
 * explain and offer a retry for.
 */
class OverpassFailoverTest {

    @Test
    fun `at least one mirror endpoint follows the primary instance`() {
        assertTrue(OverpassClient.ENDPOINTS.size >= 2)
        assertTrue(OverpassClient.ENDPOINTS.all { it.startsWith("https://") })
        // All endpoints distinct — a retry must never hit the same instance.
        assertEquals(OverpassClient.ENDPOINTS.size, OverpassClient.ENDPOINTS.toSet().size)
    }

    @Test
    fun `rate limiting and overload are retryable server-busy failures`() {
        assertEquals(OverpassClient.Reason.SERVER_BUSY, OverpassClient.classifyHttp(429))
        assertEquals(OverpassClient.Reason.SERVER_BUSY, OverpassClient.classifyHttp(500))
        assertEquals(OverpassClient.Reason.SERVER_BUSY, OverpassClient.classifyHttp(504))
        assertTrue(OverpassClient.isRetryableHttp(429))
        assertTrue(OverpassClient.isRetryableHttp(504))
    }

    @Test
    fun `client errors fail fast because mirrors would answer the same`() {
        assertEquals(OverpassClient.Reason.OTHER, OverpassClient.classifyHttp(400))
        assertEquals(OverpassClient.Reason.OTHER, OverpassClient.classifyHttp(404))
        assertFalse(OverpassClient.isRetryableHttp(400))
        assertFalse(OverpassClient.isRetryableHttp(404))
    }

    @Test
    fun `network exceptions classify into actionable reasons`() {
        assertEquals(OverpassClient.Reason.NO_NETWORK, OverpassClient.classifyException(UnknownHostException("host")))
        assertEquals(OverpassClient.Reason.NO_NETWORK, OverpassClient.classifyException(ConnectException("refused")))
        assertEquals(OverpassClient.Reason.TIMEOUT, OverpassClient.classifyException(SocketTimeoutException("timeout")))
        assertEquals(OverpassClient.Reason.OTHER, OverpassClient.classifyException(IllegalStateException("boom")))
    }
}
