package cn.edu.pku.openrunner.feature.run.ui

import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunWorkflowTest {
    @Test
    fun saving_clearsTheSharedMapRouteButKeepsFinishedMetrics() {
        val location = TrackPoint(116.31, 39.99)
        val finished = RunUiState(
            status = RunStatus.FINISHED,
            durationSeconds = 300,
            distanceMeters = 1000,
            paceSecondsPerKm = 300,
            stepCount = 1234,
            pointCount = 2,
            points = listOf(location, TrackPoint(116.32, 39.99)),
            currentPoint = location,
            usedVirtualLocation = true
        )
        val saved = finished.withSavedRecord("local-1")

        assertTrue(saved.points.isEmpty())
        assertEquals(RecordSaveStatus.SAVED, saved.recordSaveStatus)
        assertEquals("local-1", saved.savedRecordId)
        assertEquals(300, saved.durationSeconds)
        assertEquals(1000, saved.distanceMeters)
        assertEquals(300, saved.paceSecondsPerKm)
        assertEquals(1234, saved.stepCount)
        assertEquals(2, saved.pointCount)
        assertEquals(location, saved.currentPoint)
        assertTrue(saved.usedVirtualLocation)
        assertEquals(RunPrimaryAction.START, saved.primaryAction)
    }

    @Test
    fun laterLocationUpdates_cannotRestoreSavedRouteOrEraseFinishedSummary() {
        val saved = RunUiState(
            status = RunStatus.FINISHED,
            distanceMeters = 1000,
            pointCount = 2,
            paceSecondsPerKm = 300
        ).withSavedRecord("local-1")
        val nextPoint = TrackPoint(116.32, 39.99)
        val updated = saved.withLocation(nextPoint, 5, listOf(nextPoint))

        assertTrue(updated.points.isEmpty())
        assertEquals(1000, updated.distanceMeters)
        assertEquals(2, updated.pointCount)
        assertEquals(300, updated.paceSecondsPerKm)
        assertEquals(nextPoint, updated.currentPoint)
    }

    @Test
    fun activeLocationUpdates_stillBuildTrackAndComputeDistance() {
        val track = listOf(TrackPoint(116.31, 39.99), TrackPoint(116.32, 39.99))
        val state = RunUiState(status = RunStatus.RUNNING, durationSeconds = 300)
            .withLocation(track.last(), 5, track)

        assertEquals(track, state.points)
        assertEquals(2, state.pointCount)
        assertTrue(state.distanceMeters > 800)
        assertTrue(state.paceSecondsPerKm != null)
    }

    @Test
    fun virtualMode_doesNotResetRunMetricsAndItsProvenanceCannotBeCleared() {
        val running = RunUiState(
            status = RunStatus.RUNNING,
            durationSeconds = 120,
            distanceMeters = 300,
            stepCount = 42
        )
        val enabled = running.withVirtualLocationMode(true)
        val disabled = enabled.withVirtualLocationMode(false)

        assertTrue(enabled.usedVirtualLocation)
        assertTrue(disabled.usedVirtualLocation)
        assertFalse(disabled.virtualLocationEnabled)
        assertEquals(RunStatus.RUNNING, disabled.status)
        assertEquals(120, disabled.durationSeconds)
        assertEquals(300, disabled.distanceMeters)
        assertEquals(42, disabled.stepCount)
    }

    @Test
    fun changingVirtualMode_afterFinishingDoesNotRelabelThePreviousRun() {
        val finished = RunUiState(status = RunStatus.FINISHED)
            .withVirtualLocationMode(true)

        assertFalse(finished.usedVirtualLocation)
        assertEquals(RunPrimaryAction.SAVE, finished.primaryAction)
    }

    @Test
    fun finishedUnsavedRun_requiresSaveInsteadOfStartingAnotherRun() {
        val state = RunUiState(status = RunStatus.FINISHED)

        assertTrue(state.hasPendingRecord)
        assertEquals(RunPrimaryAction.SAVE, state.primaryAction)
    }

    @Test
    fun savingOrSaveFailure_keepsRecordPending() {
        val saving = RunUiState(
            status = RunStatus.FINISHED,
            recordSaveStatus = RecordSaveStatus.SAVING
        )
        val failed = saving.copy(recordSaveStatus = RecordSaveStatus.ERROR)

        assertTrue(saving.hasPendingRecord)
        assertTrue(saving.isSavingRecord)
        assertEquals(RunPrimaryAction.SAVE, saving.primaryAction)
        assertTrue(failed.hasPendingRecord)
        assertEquals(RunPrimaryAction.SAVE, failed.primaryAction)
    }

    @Test
    fun savedOrDiscardedRun_allowsStartingAgain() {
        val saved = RunUiState(
            status = RunStatus.FINISHED,
            recordSaveStatus = RecordSaveStatus.SAVED
        )

        assertFalse(saved.hasPendingRecord)
        assertEquals(RunPrimaryAction.START, saved.primaryAction)
        assertEquals(RunPrimaryAction.START, RunUiState().primaryAction)
        assertEquals(
            RunPrimaryAction.STOP,
            RunUiState(status = RunStatus.RUNNING).primaryAction
        )
    }
}
