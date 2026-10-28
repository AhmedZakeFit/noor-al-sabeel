package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.UpdateDownloadPolicy
import io.github.ahmedzakefit.noor.data.UpdateVerifier
import io.github.ahmedzakefit.noor.data.UpdateVerifier.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.0.17 regression tests for the update-download retry policy.
 *
 * Proven failure mode (v1.0.16): the release was re-published with different
 * APK bytes ~19 minutes after its first publish while users were mid-update;
 * the in-app updater fetched the published checksum and the APK in two
 * separate requests, verified two different builds against each other, and
 * failed with a false "SHA-256 mismatch". UpdateDownloadPolicy is the pure
 * decision core of the fix; these tests pin both its positive paths (a
 * re-publish race gets exactly one re-verify pass) and its negative paths
 * (a genuinely bad file still fails, and non-checksum verdicts never retry).
 */
class UpdateDownloadPolicyTest {

    @Test
    fun `checksum-family verdicts retry exactly once`() {
        // First attempt failed -> one retry is allowed …
        assertTrue(UpdateDownloadPolicy.shouldRetry(Verdict.CHECKSUM_MISMATCH, attemptsDone = 1))
        assertTrue(UpdateDownloadPolicy.shouldRetry(Verdict.CHECKSUM_UNKNOWN, attemptsDone = 1))
        // … but never a third pass.
        assertFalse(UpdateDownloadPolicy.shouldRetry(Verdict.CHECKSUM_MISMATCH, attemptsDone = 2))
        assertFalse(UpdateDownloadPolicy.shouldRetry(Verdict.CHECKSUM_UNKNOWN, attemptsDone = 2))
        assertFalse(UpdateDownloadPolicy.shouldRetry(Verdict.CHECKSUM_MISMATCH, attemptsDone = 3))
        assertEquals(2, UpdateDownloadPolicy.MAX_ATTEMPTS)
    }

    @Test
    fun `package version and signature verdicts never retry`() {
        // These describe the bytes themselves, not a fetch race; re-fetching
        // the checksum cannot change them, so retrying would only burn data.
        assertFalse(UpdateDownloadPolicy.shouldRetry(Verdict.PACKAGE_MISMATCH, attemptsDone = 1))
        assertFalse(UpdateDownloadPolicy.shouldRetry(Verdict.NOT_NEWER, attemptsDone = 1))
        assertFalse(UpdateDownloadPolicy.shouldRetry(Verdict.SIGNATURE_MISMATCH, attemptsDone = 1))
        // OK never retries (there is nothing to fix).
        assertFalse(UpdateDownloadPolicy.shouldRetry(Verdict.OK, attemptsDone = 1))
    }

    @Test
    fun `served size different from the release asset proves an asset swap`() {
        // The release document says 24554033 bytes; the server answering a
        // different size is provably NOT the release the check resolved.
        assertTrue(UpdateDownloadPolicy.sizeProvesAssetSwap(servedContentLength = 24554073, releaseAssetSizeBytes = 24554033))
        assertTrue(UpdateDownloadPolicy.sizeProvesAssetSwap(servedContentLength = 24554033, releaseAssetSizeBytes = 24554073))
    }

    @Test
    fun `unknown sizes prove nothing`() {
        // Chunked transfer (no Content-Length) and releases without a size
        // (API-free fallback path) must never trigger the early abort.
        assertFalse(UpdateDownloadPolicy.sizeProvesAssetSwap(servedContentLength = -1, releaseAssetSizeBytes = 24554033))
        assertFalse(UpdateDownloadPolicy.sizeProvesAssetSwap(servedContentLength = 24554033, releaseAssetSizeBytes = 0))
        assertFalse(UpdateDownloadPolicy.sizeProvesAssetSwap(servedContentLength = -1, releaseAssetSizeBytes = 0))
    }

    @Test
    fun `matching size proves nothing either`() {
        // Same size does NOT mean same bytes — the real proof is always the
        // SHA-256 comparison; the size check only detects provable swaps.
        assertFalse(UpdateDownloadPolicy.sizeProvesAssetSwap(servedContentLength = 24554033, releaseAssetSizeBytes = 24554033))
    }
}

/**
 * End-to-end (pure) simulation of the v1.0.16 failure: a release re-published
 * between the updater's checksum request and its APK download. The pinned
 * behaviour: one re-fetch of the published checksum resolves the race WITHOUT
 * re-downloading the APK (the bytes on disk match what the release publishes
 * NOW), while a genuine tamper still fails permanently.
 *
 * The network legs are stubbed with queues, mirroring UpdateManager's
 * attempt loop: on each attempt the caller (a) reads the currently published
 * checksum, (b) may reuse the file already on disk when it matches, else
 * downloads, (c) asks UpdateVerifier for a verdict, (d) consults
 * UpdateDownloadPolicy whether to loop again.
 */
class UpdateSwapRaceSimulationTest {

    /** Verifiable update as published for real: checksum matches bytes. */
    private val goodSha = "11".repeat(32)
    private val installed = "io.github.ahmedzakefit.noor"
    private val facts = UpdateVerifier.ArchiveFacts(installed, 18, goodSha)

    private fun loop(
        publishedChecksums: MutableList<String?>,   // what each checksum fetch returns (consumed per attempt)
        servedApkHashes: MutableList<String?>,       // what each APK download yields (consumed per attempt)
    ): Pair<Verdict, Int> {
        var attempts = 0
        var verdict: Verdict = Verdict.CHECKSUM_UNKNOWN
        var onDisk: String? = null
        while (true) {
            attempts++
            val published = publishedChecksums.removeAt(0)
            if (published == null) {
                verdict = Verdict.CHECKSUM_UNKNOWN
                if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) break
                continue
            }
            if (onDisk != null && onDisk == published) {
                // The release was swapped between our two requests; the file
                // we already hold IS what the release publishes now.
                verdict = UpdateVerifier.verdict(published, onDisk, facts, installed, 17, goodSha)
                if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) break
                continue
            }
            onDisk = servedApkHashes.removeAt(0)
            verdict = UpdateVerifier.verdict(published, onDisk, facts, installed, 17, goodSha)
            if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) break
        }
        return verdict to attempts
    }

    @Test
    fun `positive - release re-published mid-update resolves via one checksum re-fetch`() {
        // v1.0.16 exactly: attempt 1 fetched the FIRST build's checksum, then
        // the CDN served the SECOND build's APK. Re-fetching the (now updated)
        // published checksum matches the file already on disk: verified.
        val verdict = loop(
            publishedChecksums = mutableListOf(
                "17cddeb5b54660063479abb5c7b36fc41f47bd3223fbec1720440529ebe0dac7",
                "370a4deb6dd8d13df834070ece224cf1dacf0f3092069e7e3441c39150bebcff",
            ),
            servedApkHashes = mutableListOf(
                "370a4deb6dd8d13df834070ece224cf1dacf0f3092069e7e3441c39150bebcff",
            ),
        )
        assertEquals(Verdict.OK, verdict.first)
        assertEquals("no re-download needed — the checksum re-fetch resolved it", 2, verdict.second)
    }

    @Test
    fun `positive - served bytes swap under a stale checksum resolve via one re-download`() {
        // Mirror race: checksum fetched AFTER the swap (new), APK served from
        // a cache BEFORE it (old). The disk file disagrees with the new
        // checksum, so the retry re-downloads and then verifies.
        val verdict = loop(
            publishedChecksums = mutableListOf(goodSha, goodSha),
            servedApkHashes = mutableListOf("ff".repeat(32), goodSha),
        )
        assertEquals(Verdict.OK, verdict.first)
        assertEquals(2, verdict.second)
    }

    @Test
    fun `negative - a genuinely corrupted download fails permanently after one retry`() {
        // Both the published checksum and every served byte disagree — a real
        // mismatch (tampered or broken file), NOT a publish race: still a
        // failure, exactly like before the hardening.
        val verdict = loop(
            publishedChecksums = mutableListOf(goodSha, goodSha, goodSha),
            servedApkHashes = mutableListOf("ff".repeat(32), "ee".repeat(32)),
        )
        assertEquals(Verdict.CHECKSUM_MISMATCH, verdict.first)
        assertEquals(2, verdict.second)
    }

    @Test
    fun `negative - unavailable checksum retries once then reports unavailable`() {
        // Half-published release (checksum asset not uploaded yet).
        val verdict = loop(
            publishedChecksums = mutableListOf(null, null, null),
            servedApkHashes = mutableListOf(goodSha),
        )
        assertEquals(Verdict.CHECKSUM_UNKNOWN, verdict.first)
        assertEquals(2, verdict.second)
    }
}
