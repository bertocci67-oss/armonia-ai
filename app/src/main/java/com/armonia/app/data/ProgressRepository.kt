package com.armonia.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.armonia.app.domain.model.ProgressStats
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.domain.model.Skill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val kind: String,
    val refId: String,
    val title: String,
    val skill: String,
    val accuracy: Float,
    val xp: Int,
    val durationSec: Int
)

@Dao
interface ProgressDao {

    @Insert
    suspend fun insert(session: PracticeSession): Long

    @Query("SELECT * FROM practice_sessions ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PracticeSession>>

    @Query("SELECT COUNT(*) FROM practice_sessions WHERE refId = :refId AND kind = :kind")
    suspend fun countRef(refId: String, kind: String): Int
}

@Database(
    entities = [PracticeSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "armonia.db"
                ).build().also { instance = it }
            }
    }
}

class ProgressRepository(private val dao: ProgressDao) {

    val sessions: Flow<List<PracticeSession>> = dao.observeRecent(200)

    val stats: Flow<ProgressStats> = sessions.map { computeProgressStats(it) }

    suspend fun log(
        kind: SessionKind,
        refId: String,
        title: String,
        skill: Skill,
        accuracy: Float,
        xp: Int,
        durationSec: Int
    ) {
        dao.insert(
            PracticeSession(
                createdAt = System.currentTimeMillis(),
                kind = kind.name,
                refId = refId,
                title = title,
                skill = skill.name,
                accuracy = accuracy.coerceIn(0f, 1f),
                xp = xp.coerceAtLeast(0),
                durationSec = durationSec.coerceAtLeast(0)
            )
        )
    }

    suspend fun wasCompleted(kind: SessionKind, refId: String): Boolean =
        dao.countRef(refId, kind.name) > 0
}

fun computeProgressStats(sessions: List<PracticeSession>): ProgressStats {
    if (sessions.isEmpty()) return ProgressStats()

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val byDay = sessions.groupBy {
        Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
    }

    var streak = 0
    var cursor = if (byDay.containsKey(today)) today else today.minusDays(1)
    while (byDay.containsKey(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }

    val weekMinutes = (6 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        val seconds = byDay[day].orEmpty().sumOf { it.durationSec }
        (seconds / 60.0).roundToInt()
    }

    val skillBuckets = mutableMapOf<Skill, MutableList<Float>>()
    sessions.forEach { session ->
        val skill = runCatching { Skill.valueOf(session.skill) }.getOrNull()
        if (skill != null) {
            skillBuckets.getOrPut(skill) { mutableListOf() }.add(session.accuracy)
        }
    }

    val totalSeconds = sessions.sumOf { it.durationSec }
    val averageAccuracy = sessions.map { it.accuracy }.average().toFloat()

    return ProgressStats(
        totalXp = sessions.sumOf { it.xp },
        currentStreak = streak,
        sessionsCount = sessions.size,
        totalMinutes = (totalSeconds / 60.0).roundToInt(),
        weekMinutes = weekMinutes,
        skillAccuracy = skillBuckets.mapValues { (_, values) -> values.average().toFloat() },
        averageAccuracy = averageAccuracy
    )
}
