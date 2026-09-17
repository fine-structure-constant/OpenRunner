package cn.edu.pku.openrunner.feature.run.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunWorkflowTest {
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
