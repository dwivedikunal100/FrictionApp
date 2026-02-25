package com.friction.app.data.repository

import android.content.Context
import com.friction.app.data.db.FrictionDatabase
import com.friction.app.data.model.FrictionMode
import com.friction.app.data.model.InterceptionEvent
import com.friction.app.data.model.ProtectedApp
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class AppRepository private constructor(context: Context) {

    private val db = FrictionDatabase.getInstance(context)
    private val appDao = db.protectedAppDao()
    private val eventDao = db.interceptionEventDao()

    // ── Protected Apps ──────────────────────────────────────────────────────

    fun getAllApps(): Flow<List<ProtectedApp>> = appDao.getAll()

    suspend fun getProtectedApp(packageName: String): ProtectedApp? =
        appDao.getByPackage(packageName)

    suspend fun addApp(app: ProtectedApp) = appDao.upsert(app)

    suspend fun removeApp(app: ProtectedApp) = appDao.delete(app)

    suspend fun toggleApp(packageName: String, enabled: Boolean) =
        appDao.setEnabled(packageName, enabled)

    suspend fun updateFrictionMode(packageName: String, mode: FrictionMode) {
        val app = appDao.getByPackage(packageName) ?: return
        appDao.upsert(app.copy(frictionMode = mode))
    }

    // ── Interception Events ─────────────────────────────────────────────────

    suspend fun recordInterception(event: InterceptionEvent) = eventDao.insert(event)

    suspend fun getOpensToday(packageName: String): Int {
        val startOfDay = System.currentTimeMillis().let {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.timeInMillis
        }
        return eventDao.countOpens(packageName, startOfDay)
    }

    suspend fun getOpensInLastHour(packageName: String): Int {
        val oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        return eventDao.countOpens(packageName, oneHourAgo)
    }

    /** Returns time saved today in milliseconds */
    suspend fun getTimeSavedToday(): Long {
        val startOfDay = System.currentTimeMillis().let {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.timeInMillis
        }
        return eventDao.getTotalTimeSaved(startOfDay)
    }

    /** Returns daily stats for the past 7 days for the analytics chart */
    suspend fun getWeeklyStats(): List<DailyStat> {
        val now = System.currentTimeMillis()
        return (6 downTo 0).map { daysAgo ->
            val dayStart = now - TimeUnit.DAYS.toMillis(daysAgo.toLong())
            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
            val events = eventDao.getEventsSince(dayStart)
                .filter { it.timestamp < dayEnd }
            DailyStat(
                daysAgo = daysAgo,
                openCount = events.size,
                timeSavedMs = events.filter { !it.wasAllowed }.sumOf { it.timeSpentOnWall }
            )
        }
    }

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun getInstance(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                AppRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}

data class DailyStat(
    val daysAgo: Int,
    val openCount: Int,
    val timeSavedMs: Long
)
