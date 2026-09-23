package cn.edu.pku.openrunner.feature.account.ui

/**
 * Decides whether a drawer-summary refresh should hit the network.
 *
 * Pulled out of [DrawerSummaryViewModel] on purpose: the part worth testing is pure arithmetic
 * over timestamps, while the ViewModel itself needs a `Context` for `SessionStore` and this
 * module has no Robolectric. Keeping the policy here means the throttling rules — the thing
 * users actually notice — are covered by plain JUnit.
 */
internal class DrawerRefreshGate(
    /** How long a successful fetch is trusted when the caller does not force a refresh. */
    private val cacheMillis: Long,
    /** Floor between two attempts, forced ones included. */
    private val minIntervalMillis: Long
) {
    // Null rather than 0 as the "never happened" sentinel: with 0 the elapsed time would be
    // `now - 0`, which only reads as "a long time ago" because wall-clock timestamps are huge.
    private var lastSuccessAt: Long? = null
    private var lastAttemptAt: Long? = null

    enum class Decision {
        /** Query the server now. */
        FETCH,

        /** Nothing to do — either the cached value is still trusted, or a retry is pointless. */
        SKIP,

        /** Too soon for another attempt, but the caller asked for a real change. Run it later. */
        DEFER
    }

    fun decide(force: Boolean, now: Long): Decision {
        val age = lastSuccessAt?.let { now - it } ?: Long.MAX_VALUE
        // A forced refresh distrusts the cache, but it still cannot beat the floor: a burst of
        // "something changed" notifications must not turn into a burst of requests.
        if (!force && age < cacheMillis) return Decision.SKIP
        val sinceAttempt = lastAttemptAt?.let { now - it } ?: Long.MAX_VALUE
        if (sinceAttempt < minIntervalMillis) {
            // Only a forced refresh is worth remembering. A skipped opportunistic refresh will
            // come round again on the next drawer open or resume.
            return if (force) Decision.DEFER else Decision.SKIP
        }
        return Decision.FETCH
    }

    /** Milliseconds still to wait before a deferred refresh may run. Never negative. */
    fun deferralMillis(now: Long): Long = lastAttemptAt
        ?.let { (minIntervalMillis - (now - it)).coerceAtLeast(0L) }
        ?: 0L

    fun recordAttempt(now: Long) {
        lastAttemptAt = now
    }

    fun recordSuccess(now: Long) {
        lastSuccessAt = now
    }

    /** Called when the session changes: nothing fetched for the previous user is reusable. */
    fun reset() {
        lastSuccessAt = null
        lastAttemptAt = null
    }
}
