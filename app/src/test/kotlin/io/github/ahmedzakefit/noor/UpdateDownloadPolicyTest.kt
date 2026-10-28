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
 * v1.0.19 regression tests for the truncation/resume policy.
 *
 * Proven failure mode (v1.0.17 -> v1.0.18 on a real device): a mobile network
 * that closes the socket before the 24 MB body finishes delivers a truncated
 * file; the updater hashed whatever bytes arrived and reported a "SHA-256
 * mismatch" — the exact v1.0.16-era message — even though the release was
 * perfectly consistent. The fix classifies a short body as a NETWORK-family
 * truncation and resumes it with HTTP Range requests instead of hashing it.
 */
class UpdateTruncationPolicyTest {

    @Test
    fun `a stream that ends before the expected size is truncated`() {
        assertTrue(UpdateDownloadPolicy.isTruncated(bytesRead = 9_437_184, expectedTotalBytes = 25_462_681))
        assertTrue(UpdateDownloadPolicy.isTruncated(bytesRead = 1, expectedTotalBytes = 25_462_681))
    }

    @Test
    fun `a complete body is not truncated`() {
        assertFalse(UpdateDownloadPolicy.isTruncated(bytesRead = 25_462_681, expectedTotalBytes = 25_462_681))
    }

    @Test
    fun `an oversized body is not truncation - the checksum comparison decides`() {
        // More bytes than expected is corruption, not truncation; hashing and
        // the SHA-256 gate remain the integrity proof there.
        assertFalse(UpdateDownloadPolicy.isTruncated(bytesRead = 25_462_682, expectedTotalBytes = 25_462_681))
    }

    @Test
    fun `an empty body is not truncation - it is an empty download`() {
        assertFalse(UpdateDownloadPolicy.isTruncated(bytesRead = 0, expectedTotalBytes = 25_462_681))
    }

    @Test
    fun `an unknown expected total can never prove truncation`() {
        // No size from the release document and no Content-Length: the
        // checksum comparison stays the only integrity gate, as before.
        assertFalse(UpdateDownloadPolicy.isTruncated(bytesRead = 100, expectedTotalBytes = 0))
        assertFalse(UpdateDownloadPolicy.isTruncated(bytesRead = 100, expectedTotalBytes = -1))
    }

    @Test
    fun `resumes open new legs until the budget is spent`() {
        // Legs 1..MAX-1 may resume …
        assertTrue(UpdateDownloadPolicy.shouldResumeAfterTruncation(legsDone = 1))
        assertTrue(UpdateDownloadPolicy.shouldResumeAfterTruncation(legsDone = 5))
        // … the budget is bounded so a pathological network can never loop.
        assertFalse(UpdateDownloadPolicy.shouldResumeAfterTruncation(legsDone = 6))
        assertFalse(UpdateDownloadPolicy.shouldResumeAfterTruncation(legsDone = 7))
        assertEquals(6, UpdateDownloadPolicy.MAX_TRUNCATION_LEGS)
    }
}

/**
 * End-to-end (pure) simulation of the v1.0.19 resuming download over a
 * connection that keeps dropping mid-body, mirroring UpdateManager's leg
 * loop: each leg contributes a slice of the file, the running digest is
 * only ever fed the bytes that are appended, and a leg that ends before
 * the expected total consults the policy. The pinned behaviour: progress is
 * never thrown away, a dropping connection still completes the file, and
 * only a network that drops more often than the leg budget still fails —
 * with the TRUNCATED reason, never as a "SHA-256 mismatch".
 */
class UpdateResumeSimulationTest {

    /** Simulated 100-byte update file and its digest state. */
    private class Sim {
        var read = 0L
        var legs = 0
        var digested = 0L // bytes fed to the running digest
        val done: Boolean get() = read >= TOTAL

        fun step(slice: Long?) {
            legs++
            val served = slice ?: (TOTAL - read) // null leg = clean full remainder
            require(served >= 0)
            // Append + continue the digest exactly like the real loop.
            val bytes = minOf(served, TOTAL - read)
            read += bytes
            digested += bytes
        }
    }

    companion object {
        private const val TOTAL = 100L
    }

    private fun runLegs(slices: List<Long?>): Sim {
        val sim = Sim()
        for (slice in slices) {
            if (sim.done) return sim
            sim.step(slice)
            if (UpdateDownloadPolicy.isTruncated(sim.read, TOTAL)) {
                if (!UpdateDownloadPolicy.shouldResumeAfterTruncation(sim.legs)) return sim
                // else: the loop opens the next (Range) leg
            } else {
                return sim // complete (or provably corrupt) -> hash + verify
            }
        }
        return sim
    }

    @Test
    fun `dropping connection still completes the file across legs`() {
        // Four drops of 25 bytes each: every drop resumes, no progress lost.
        val sim = runLegs(listOf(25L, 25L, 25L, 25L))
        org.junit.Assert.assertEquals(4, sim.legs)
        org.junit.Assert.assertEquals(TOTAL, sim.read)
        org.junit.Assert.assertEquals(TOTAL, sim.digested) // digest saw every appended byte
    }

    @Test
    fun `a network that never delivers the file fails as TRUNCATED - never as a checksum mismatch`() {
        // Drops every 10 bytes: after the leg budget is spent the outcome is
        // truncation, which surfaces as the network-family TRUNCATED failure.
        val sim = runLegs(List(10) { 10L })
        org.junit.Assert.assertTrue(UpdateDownloadPolicy.isTruncated(sim.read, TOTAL))
        org.junit.Assert.assertFalse(UpdateDownloadPolicy.shouldResumeAfterTruncation(sim.legs))
        org.junit.Assert.assertEquals(6, sim.legs)
        org.junit.Assert.assertEquals(60L, sim.read)
    }

    @Test
    fun `one clean leg after drops finishes the job`() {
        // Two drops, then a leg that serves the whole remaining body (null =
        // clean EOF at the expected size): the file completes.
        val sim = runLegs(listOf(30L, 40L, null))
        org.junit.Assert.assertEquals(TOTAL, sim.read)
        org.junit.Assert.assertEquals(TOTAL, sim.digested)
        org.junit.Assert.assertEquals(3, sim.legs)
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
