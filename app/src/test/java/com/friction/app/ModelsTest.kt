package com.friction.app

import com.friction.app.data.model.FrictionMode
import com.friction.app.data.model.InterceptionEvent
import com.friction.app.data.model.ProtectedApp
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    // ── FrictionMode ────────────────────────────────────────────────

    @Test
    fun `FrictionMode has exactly 5 values`() {
        assertEquals(5, FrictionMode.values().size)
    }

    @Test
    fun `FrictionMode contains all expected values`() {
        val expected = setOf("BREATHING", "TYPING", "MATH", "WALK", "STRICT")
        val actual = FrictionMode.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `FrictionMode valueOf round-trips correctly`() {
        FrictionMode.values().forEach { mode ->
            assertEquals(mode, FrictionMode.valueOf(mode.name))
        }
    }

    // ── ProtectedApp ────────────────────────────────────────────────

    @Test
    fun `ProtectedApp default mode is BREATHING`() {
        val app = ProtectedApp(packageName = "com.test", displayName = "Test")
        assertEquals(FrictionMode.BREATHING, app.frictionMode)
    }

    @Test
    fun `ProtectedApp default isEnabled is true`() {
        val app = ProtectedApp(packageName = "com.test", displayName = "Test")
        assertTrue(app.isEnabled)
    }

    @Test
    fun `ProtectedApp default strictModeEnabled is false`() {
        val app = ProtectedApp(packageName = "com.test", displayName = "Test")
        assertFalse(app.strictModeEnabled)
    }

    @Test
    fun `ProtectedApp copy changes mode correctly`() {
        val app = ProtectedApp(packageName = "com.test", displayName = "Test")
        val updated = app.copy(frictionMode = FrictionMode.MATH)
        assertEquals(FrictionMode.MATH, updated.frictionMode)
        assertEquals("com.test", updated.packageName) // unchanged
    }

    @Test
    fun `ProtectedApp copy changes schedule correctly`() {
        val app = ProtectedApp(packageName = "com.test", displayName = "Test")
        val updated =
                app.copy(
                        strictModeEnabled = true,
                        strictModeStartHour = 10,
                        strictModeEndHour = 18,
                        strictModeDays = "2,3,4"
                )
        assertTrue(updated.strictModeEnabled)
        assertEquals(10, updated.strictModeStartHour)
        assertEquals(18, updated.strictModeEndHour)
        assertEquals("2,3,4", updated.strictModeDays)
    }

    @Test
    fun `ProtectedApp equality by packageName`() {
        val app1 = ProtectedApp(packageName = "com.test", displayName = "Test")
        val app2 = ProtectedApp(packageName = "com.test", displayName = "Test")
        assertEquals(app1, app2)
    }

    @Test
    fun `ProtectedApp inequality with different packageName`() {
        val app1 = ProtectedApp(packageName = "com.test1", displayName = "Test")
        val app2 = ProtectedApp(packageName = "com.test2", displayName = "Test")
        assertNotEquals(app1, app2)
    }

    // ── InterceptionEvent ───────────────────────────────────────────

    @Test
    fun `InterceptionEvent default wasAllowed is false`() {
        val event = InterceptionEvent(packageName = "com.test")
        assertFalse(event.wasAllowed)
    }

    @Test
    fun `InterceptionEvent default timeSpentOnWall is 0`() {
        val event = InterceptionEvent(packageName = "com.test")
        assertEquals(0L, event.timeSpentOnWall)
    }

    @Test
    fun `InterceptionEvent default frictionMode is BREATHING`() {
        val event = InterceptionEvent(packageName = "com.test")
        assertEquals(FrictionMode.BREATHING, event.frictionMode)
    }

    @Test
    fun `InterceptionEvent timestamp is set to approximately now`() {
        val before = System.currentTimeMillis()
        val event = InterceptionEvent(packageName = "com.test")
        val after = System.currentTimeMillis()
        assertTrue(event.timestamp in before..after)
    }
}
