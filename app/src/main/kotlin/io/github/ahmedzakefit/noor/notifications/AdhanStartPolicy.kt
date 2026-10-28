package io.github.ahmedzakefit.noor.notifications

/**
 * Pure, JVM-testable decision table for adhan service starts.
 *
 * Root-cause regression pin (the v1.0.16 report): the adhan service is ONE
 * reusable instance whose `preview`/`finalized` fields used to survive across
 * start commands. That made a manual test start able to hijack or shadow a
 * real prayer-time adhan on the same live instance — users heard the test
 * ("تجربة الأذان") start around/instead of the real adhan, and a real adhan
 * arriving right after a finished test could be silently swallowed by the
 * stale `finalized` flag. The service now consults THIS table before touching
 * any player or state, so:
 *
 *  - a TEST start never interrupts a REAL adhan (the reported bug becomes
 *    structurally impossible, automatic or not);
 *  - a REAL start always wins over a running test (prayer time is sacred);
 *  - a test while idle stays a normal manual test (nothing removed);
 *  - every accepted start resets the instance's finalization state, so the
 *    finished-test → prayer-time race can no longer silently kill the adhan.
 */
object AdhanStartPolicy {

    /** What is (or is about to be) playing on the service instance. */
    data class Current(val active: Boolean, val isPreview: Boolean)

    /** The start command being evaluated. */
    sealed interface Command {
        data object RealAdhan : Command       // prayer-time alarm (preview = false)
        data object PreviewAdhan : Command    // manual settings test (preview = true)
        data object Stop : Command
    }

    enum class Decision { START, IGNORE, STOP }

    fun decide(current: Current, command: Command): Decision = when (command) {
        Command.Stop -> Decision.STOP
        Command.RealAdhan -> Decision.START          // the real adhan always proceeds
        Command.PreviewAdhan ->
            if (current.active && !current.isPreview) {
                // Never let a test interrupt, replace or shadow a live real adhan.
                Decision.IGNORE
            } else {
                Decision.START
            }
    }
}
