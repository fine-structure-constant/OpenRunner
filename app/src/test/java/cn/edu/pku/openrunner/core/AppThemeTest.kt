package cn.edu.pku.openrunner.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {
    @Test
    fun `persisted theme values are stable and unknown values follow system`() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromPersistedValue(null))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromPersistedValue("unknown"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromPersistedValue("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromPersistedValue("dark"))
    }
}
