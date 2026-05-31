package com.hermes.mobile.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE account_scope = :accountScope AND session_id = :sessionId ORDER BY timestamp ASC")
    fun getBySessionIdFlow(accountScope: String, sessionId: String): Flow<List<MessageEntity>>

    fun getBySessionIdFlow(sessionId: String): Flow<List<MessageEntity>> = getBySessionIdFlow(LEGACY_ACCOUNT_SCOPE, sessionId)

    @Query("SELECT * FROM messages WHERE account_scope = :accountScope AND session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionId(accountScope: String, sessionId: String): List<MessageEntity>

    suspend fun getBySessionId(sessionId: String): List<MessageEntity> = getBySessionId(LEGACY_ACCOUNT_SCOPE, sessionId)

    @Query("SELECT * FROM messages WHERE account_scope = :accountScope")
    suspend fun getByScope(accountScope: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE account_scope != :accountScope")
    suspend fun getOutsideScope(accountScope: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("DELETE FROM messages WHERE account_scope = :accountScope")
    suspend fun deleteByScope(accountScope: String)

    @Query("DELETE FROM messages WHERE account_scope = :accountScope AND session_id = :sessionId")
    suspend fun deleteBySessionId(accountScope: String, sessionId: String)

    suspend fun deleteBySessionId(sessionId: String) = deleteBySessionId(LEGACY_ACCOUNT_SCOPE, sessionId)

    @Query("DELETE FROM messages WHERE account_scope = :accountScope AND session_id = :sessionId AND remote_backed = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteStaleRemoteMessages(accountScope: String, sessionId: String, remoteIds: List<Long>)

    @Query("DELETE FROM messages WHERE account_scope = :accountScope AND session_id = :sessionId AND id IN (:messageIds)")
    suspend fun deleteByIds(accountScope: String, sessionId: String, messageIds: List<Long>)

    suspend fun deleteByIds(sessionId: String, messageIds: List<Long>) = deleteByIds(
        LEGACY_ACCOUNT_SCOPE,
        sessionId,
        messageIds,
    )
}
