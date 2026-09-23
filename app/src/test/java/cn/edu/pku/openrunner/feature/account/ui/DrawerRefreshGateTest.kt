package cn.edu.pku.openrunner.feature.account.ui

import cn.edu.pku.openrunner.feature.account.ui.DrawerRefreshGate.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the drawer refresh throttling. These rules are what stops the menu from querying the
 * server on every open, so they are worth pinning down independently of the ViewModel (which
 * needs a Context and cannot run as a plain JUnit test here).
 */
class DrawerRefreshGateTest {
    private val gate = DrawerRefreshGate(CACHE_MILLIS, MIN_INTERVAL_MILLIS)

    @Test
    fun `a refresh with nothing cached goes out`() {
        assertEquals(Decision.FETCH, gate.decide(force = false, now = NOW))
    }

    @Test
    fun `a successful fetch is reused for the whole cache window`() {
        gate.recordAttempt(NOW)
        gate.recordSuccess(NOW)

        assertEquals(Decision.SKIP, gate.decide(force = false, now = NOW + CACHE_MILLIS - 1))
        assertEquals(Decision.FETCH, gate.decide(force = false, now = NOW + CACHE_MILLIS))
    }

    @Test
    fun `forcing skips the cache but still respects the floor`() {
        gate.recordAttempt(NOW)
        gate.recordSuccess(NOW)

        // The cached value is still trusted, so an opportunistic refresh does nothing...
        assertEquals(Decision.SKIP, gate.decide(force = false, now = NOW + 1_000))
        // ...while a forced one wants to go out, but the floor holds it back.
        assertEquals(Decision.DEFER, gate.decide(force = true, now = NOW + 1_000))
        // Once the floor has passed the two still differ: only force overrides the cache.
        assertEquals(Decision.FETCH, gate.decide(force = true, now = NOW + MIN_INTERVAL_MILLIS))
        assertEquals(Decision.SKIP, gate.decide(force = false, now = NOW + MIN_INTERVAL_MILLIS))
    }

    @Test
    fun `an opportunistic refresh inside the floor is dropped rather than queued`() {
        gate.recordAttempt(NOW)

        // Nothing is on screen yet, so the cache does not apply, but the floor still does.
        // Dropping is fine: the next drawer open or resume will ask again.
        assertEquals(Decision.SKIP, gate.decide(force = false, now = NOW + 1))
        assertEquals(Decision.FETCH, gate.decide(force = false, now = NOW + MIN_INTERVAL_MILLIS))
    }

    @Test
    fun `a success is what creates the cache, not an attempt`() {
        gate.recordAttempt(NOW)
        // Attempted ten seconds ago but never succeeded: there is still nothing to show, so
        // opening the drawer again has to retry rather than report the empty cache as fresh.
        assertEquals(Decision.FETCH, gate.decide(force = false, now = NOW + 10_000))

        gate.recordSuccess(NOW + 10_000)
        assertEquals(Decision.SKIP, gate.decide(force = false, now = NOW + 10_001))
    }

    @Test
    fun `deferral counts down and never goes negative`() {
        gate.recordAttempt(NOW)

        assertEquals(MIN_INTERVAL_MILLIS, gate.deferralMillis(NOW))
        assertEquals(1_000L, gate.deferralMillis(NOW + MIN_INTERVAL_MILLIS - 1_000))
        assertEquals(0L, gate.deferralMillis(NOW + MIN_INTERVAL_MILLIS))
        assertEquals(0L, gate.deferralMillis(NOW + 10 * MIN_INTERVAL_MILLIS))
    }

    @Test
    fun `deferral with no attempt yet is zero`() {
        assertEquals(0L, gate.deferralMillis(NOW))
    }

    @Test
    fun `a session change forgets both the cache and the floor`() {
        gate.recordAttempt(NOW)
        gate.recordSuccess(NOW)
        gate.reset()

        assertEquals(Decision.FETCH, gate.decide(force = false, now = NOW + 1))
        assertEquals(0L, gate.deferralMillis(NOW + 1))
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val CACHE_MILLIS = 30_000L
        const val MIN_INTERVAL_MILLIS = 5_000L
    }
}
