package cn.edu.pku.openrunner.feature.records.domain

import cn.edu.pku.openrunner.core.network.UserStatusDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunGoalProgressTest {
    @Test
    fun creditsBonusMileageAndKeepsProgressAboveOneLap() {
        val goal = RunGoalProgress(UserStatusDto(current = 29_400, bonus = 9_300, target = 30_000))

        assertEquals(38_700L, goal.distanceMeters)
        assertEquals(129.0, goal.percent)
    }

    @Test
    fun keepsFractionalProgressJustBeforeCompletingTheTarget() {
        val goal = RunGoalProgress(UserStatusDto(current = 29_970, target = 30_000))

        assertEquals(99.9, goal.percent)
    }

    @Test
    fun acceptsExactLapBoundariesAndMultipleLaps() {
        assertEquals(0.0, RunGoalProgress(UserStatusDto(target = 30_000)).percent)
        assertEquals(100.0, RunGoalProgress(UserStatusDto(current = 30_000, target = 30_000)).percent)
        assertEquals(200.0, RunGoalProgress(UserStatusDto(current = 60_000, target = 30_000)).percent)
        assertEquals(201.0, RunGoalProgress(UserStatusDto(current = 60_300, target = 30_000)).percent)
    }

    @Test
    fun showsCreditedDistanceWhenNoTargetIsAvailable() {
        val goal = RunGoalProgress(UserStatusDto(current = 3_500, bonus = 500, target = 0))

        assertEquals(4_000L, goal.distanceMeters)
        assertNull(goal.percent)
        assertNull(RunGoalProgress(UserStatusDto(target = -1)).percent)
    }

    @Test
    fun addsMileageWithoutIntegerOverflow() {
        val goal = RunGoalProgress(
            UserStatusDto(current = Int.MAX_VALUE, bonus = Int.MAX_VALUE, target = Int.MAX_VALUE)
        )

        assertEquals(4_294_967_294L, goal.distanceMeters)
        assertEquals(200.0, goal.percent)
    }
}
