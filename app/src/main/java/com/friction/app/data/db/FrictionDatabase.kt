package com.friction.app.data.db

import android.content.Context
import androidx.room.*
import com.friction.app.data.model.FrictionMode
import com.friction.app.data.model.InterceptionEvent
import com.friction.app.data.model.ProtectedApp
import kotlinx.coroutines.flow.Flow

// ─── Type Converters ────────────────────────────────────────────────────────

class Converters {
    @TypeConverter fun fromFrictionMode(mode: FrictionMode): String = mode.name
    @TypeConverter fun toFrictionMode(name: String): FrictionMode = FrictionMode.valueOf(name)
}

// ─── DAOs ───────────────────────────────────────────────────────────────────

@Dao
interface ProtectedAppDao {
    @Query("SELECT * FROM protected_apps WHERE isEnabled = 1")
    fun getAllEnabled(): Flow<List<ProtectedApp>>

    @Query("SELECT * FROM protected_apps")
    fun getAll(): Flow<List<ProtectedApp>>

    @Query("SELECT * FROM protected_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): ProtectedApp?

    @Upsert
    suspend fun upsert(app: ProtectedApp)

    @Delete
    suspend fun delete(app: ProtectedApp)

    @Query("UPDATE protected_apps SET isEnabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)
}

@Dao
interface InterceptionEventDao {
    @Insert
    suspend fun insert(event: InterceptionEvent)

    @Query("""
        SELECT * FROM interception_events
        WHERE timestamp > :since
        ORDER BY timestamp DESC
    """)
    suspend fun getEventsSince(since: Long): List<InterceptionEvent>

    @Query("""
        SELECT COUNT(*) FROM interception_events
        WHERE packageName = :pkg AND timestamp > :since
    """)
    suspend fun countOpens(pkg: String, since: Long): Int

    // Total time saved: sum of timeSpentOnWall for sessions where wasAllowed = false
    @Query("""
        SELECT COALESCE(SUM(timeSpentOnWall), 0) FROM interception_events
        WHERE wasAllowed = 0 AND timestamp > :since
    """)
    suspend fun getTotalTimeSaved(since: Long): Long

    @Query("DELETE FROM interception_events WHERE timestamp < :before")
    suspend fun purgeOlderThan(before: Long)
}

// ─── Database ───────────────────────────────────────────────────────────────

@Database(
    entities = [ProtectedApp::class, InterceptionEvent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FrictionDatabase : RoomDatabase() {
    abstract fun protectedAppDao(): ProtectedAppDao
    abstract fun interceptionEventDao(): InterceptionEventDao

    companion object {
        @Volatile private var INSTANCE: FrictionDatabase? = null

        fun getInstance(context: Context): FrictionDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FrictionDatabase::class.java,
                    "friction_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
