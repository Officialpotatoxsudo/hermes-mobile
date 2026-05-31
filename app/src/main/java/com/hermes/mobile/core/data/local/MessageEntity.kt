package com.hermes.mobile.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "messages",
    primaryKeys = ["account_scope", "session_id", "id"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["account_scope", "id"],
            childColumns = ["account_scope", "session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["account_scope", "session_id"]),
        Index(value = ["account_scope", "timestamp"]),
    ],
)
data class MessageEntity(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "reasoning")
    val reasoning: String = "",
    @ColumnInfo(name = "image_uris_json")
    val imageUrisJson: String = "[]",
    @ColumnInfo(name = "received_attachments_json")
    val receivedAttachmentsJson: String = "[]",
    @ColumnInfo(name = "account_scope")
    val accountScope: String = LEGACY_ACCOUNT_SCOPE,
    @ColumnInfo(name = "remote_backed")
    val remoteBacked: Boolean = true,
)
