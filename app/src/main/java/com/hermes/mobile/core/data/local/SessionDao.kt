package com.hermes.mobile.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE account_scope = :accountScope ORDER BY local_last_activity_at DESC, started_at DESC")
    fun getAllFlow(accountScope: String): Flow<List<SessionEntity>>

    fun getAllFlow(): Flow<List<SessionEntity>> = getAllFlow(LEGACY_ACCOUNT_SCOPE)

    @Query(
        """
        SELECT * FROM sessions
        WHERE account_scope = :accountScope
          AND (
               title LIKE '%' || :query || '%'
            OR source LIKE '%' || :query || '%'
            OR model LIKE '%' || :query || '%'
          )
        ORDER BY local_last_activity_at DESC, started_at DESC
        """,
    )
    fun searchFlow(accountScope: String, query: String): Flow<List<SessionEntity>>

    fun searchFlow(query: String): Flow<List<SessionEntity>> = searchFlow(LEGACY_ACCOUNT_SCOPE, query)

    @Query("SELECT * FROM sessions WHERE account_scope = :accountScope AND id = :sessionId")
    suspend fun getById(accountScope: String, sessionId: String): SessionEntity?

    suspend fun getById(sessionId: String): SessionEntity? = getById(LEGACY_ACCOUNT_SCOPE, sessionId)

    @Query("SELECT * FROM sessions WHERE account_scope = :accountScope ORDER BY local_last_activity_at DESC, last_synced_at DESC, started_at DESC LIMIT 1")
    suspend fun latest(accountScope: String): SessionEntity?

    suspend fun latest(): SessionEntity? = latest(LEGACY_ACCOUNT_SCOPE)

    @Query("SELECT * FROM sessions WHERE account_scope = :accountScope ORDER BY local_last_activity_at DESC, started_at DESC")
    suspend fun getByScope(accountScope: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE account_scope != :accountScope ORDER BY local_last_activity_at DESC, started_at DESC")
    suspend fun getOutsideScope(accountScope: String): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("UPDATE sessions SET unread_count = 0, last_read_at = :readAt WHERE account_scope = :accountScope AND id = :sessionId")
    suspend fun markRead(accountScope: String, sessionId: String, readAt: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("DELETE FROM sessions WHERE account_scope = :accountScope")
    suspend fun deleteByScope(accountScope: String)

    @Query("DELETE FROM sessions WHERE account_scope = :accountScope AND id = :sessionId")
    suspend fun deleteById(accountScope: String, sessionId: String)

    suspend fun deleteById(sessionId: String) = deleteById(LEGACY_ACCOUNT_SCOPE, sessionId)
}
