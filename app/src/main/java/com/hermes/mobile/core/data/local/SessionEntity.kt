package com.hermes.mobile.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

const val LEGACY_ACCOUNT_SCOPE = "legacy"

@Entity(
    tableName = "sessions",
    primaryKeys = ["account_scope", "id"],
    indices = [
        Index(value = ["account_scope", "started_at"]),
        Index(value = ["account_scope", "local_last_activity_at"]),
        Index(value = ["account_scope", "last_synced_at"]),
    ],
)
data class SessionEntity(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String?,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "ended_at")
    val endedAt: Long?,
    @ColumnInfo(name = "message_count")
    val messageCount: Int,
    @ColumnInfo(name = "model")
    val model: String,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "local_last_activity_at")
    val localLastActivityAt: Long = startedAt,
    @ColumnInfo(name = "account_scope")
    val accountScope: String = LEGACY_ACCOUNT_SCOPE,
    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String? = null,
    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,
    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long? = null,
)
