package com.hermes.mobile.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY started_at DESC")
    fun getAllFlow(): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT * FROM sessions
        WHERE title LIKE '%' || :query || '%'
           OR source LIKE '%' || :query || '%'
           OR model LIKE '%' || :query || '%'
        ORDER BY started_at DESC
        """,
    )
    fun searchFlow(query: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY last_synced_at DESC, started_at DESC LIMIT 1")
    suspend fun latest(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)
}
