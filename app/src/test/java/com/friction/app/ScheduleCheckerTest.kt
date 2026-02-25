package com.friction.app

import com.friction.app.data.model.ProtectedApp
import com.friction.app.utils.ScheduleChecker
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScheduleCheckerTest {

    private lateinit var checker: ScheduleChecker

    @Before
    fun setUp() {
        checker = ScheduleChecker()
    }

    @Test
    fun `returns false when strictModeEnabled is false`() {
        val app =
                ProtectedApp(
                        packageName = "com.test",
                        displayName = "Test",
                        strictModeEnabled = false,
                        strictModeStartHour = 0,
                        strictModeEndHour = 23,
                        strictModeDays = "1,2,3,4,5,6,7"
                )
        assertFalse(checker.isStrictModeActive(app))
    }

    @Test
    fun `returns true during active hours on active day`() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentDay = cal.get(Calendar.DAY_OF_WEEK).toString()

        val app =
                ProtectedApp(
                        packageName = "com.test",
                        displayName = "Test",
                        strictModeEnabled = true,
                        strictModeStartHour = currentHour,
                        strictModeEndHour = currentHour + 1,
                        strictModeDays = currentDay
                )
        assertTrue(checker.isStrictModeActive(app))
    }

    @Test
    fun `returns false outside time window`() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentDay = cal.get(Calendar.DAY_OF_WEEK).toString()

        // Set window to an hour that is NOT now
        val startHour = (currentHour + 2) % 24
        val endHour = (currentHour + 4) % 24

        // Only valid if startHour < endHour (non-wrapping window)
        if (startHour < endHour) {
            val app =
                    ProtectedApp(
                            packageName = "com.test",
                            displayName = "Test",
                            strictModeEnabled = true,
                            strictModeStartHour = startHour,
                            strictModeEndHour = endHour,
                            strictModeDays = currentDay
                    )
            assertFalse(checker.isStrictModeActive(app))
        }
    }

    @Test
    fun `returns false on inactive day`() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)

        // Pick a day that is NOT today
        val otherDay = if (currentDay == 1) "2" else "1"

        val app =
                ProtectedApp(
                        packageName = "com.test",
                        displayName = "Test",
                        strictModeEnabled = true,
                        strictModeStartHour = currentHour,
                        strictModeEndHour = currentHour + 1,
                        strictModeDays = otherDay
                )
        assertFalse(checker.isStrictModeActive(app))
    }

    @Test
    fun `returns false when start equals end hour`() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentDay = cal.get(Calendar.DAY_OF_WEEK).toString()

        val app =
                ProtectedApp(
                        packageName = "com.test",
                        displayName = "Test",
                        strictModeEnabled = true,
                        strictModeStartHour = currentHour,
                        strictModeEndHour = currentHour, // same hour — window is zero-width
                        strictModeDays = currentDay
                )
        assertFalse(checker.isStrictModeActive(app))
    }

    @Test
    fun `default ProtectedApp schedule is Mon-Fri 9 to 17`() {
        val app = ProtectedApp(packageName = "com.test", displayName = "Test")
        assertEquals(9, app.strictModeStartHour)
        assertEquals(17, app.strictModeEndHour)
        assertEquals("1,2,3,4,5", app.strictModeDays)
        assertFalse(app.strictModeEnabled)
    }
}
