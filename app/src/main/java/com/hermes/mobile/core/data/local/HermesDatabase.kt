package com.hermes.mobile.core.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class HermesDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN image_uris_json TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sessions_new (
                        id TEXT NOT NULL,
                        title TEXT,
                        source TEXT NOT NULL,
                        started_at INTEGER NOT NULL,
                        ended_at INTEGER,
                        message_count INTEGER NOT NULL,
                        model TEXT NOT NULL,
                        last_synced_at INTEGER NOT NULL,
                        account_scope TEXT NOT NULL DEFAULT 'legacy',
                        PRIMARY KEY(account_scope, id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO sessions_new (
                        id, title, source, started_at, ended_at, message_count, model, last_synced_at, account_scope
                    )
                    SELECT id, title, source, started_at, ended_at, message_count, model, last_synced_at, 'legacy'
                    FROM sessions
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS messages_new (
                        id INTEGER NOT NULL,
                        session_id TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        image_uris_json TEXT NOT NULL DEFAULT '[]',
                        account_scope TEXT NOT NULL DEFAULT 'legacy',
                        remote_backed INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(account_scope, id),
                        FOREIGN KEY(account_scope, session_id) REFERENCES sessions(account_scope, id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO messages_new (
                        id, session_id, role, content, timestamp, image_uris_json, account_scope, remote_backed
                    )
                    SELECT id, session_id, role, content, timestamp, image_uris_json, 'legacy', 1
                    FROM messages
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE messages")
                db.execSQL("DROP TABLE sessions")
                db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")
                db.execSQL("ALTER TABLE messages_new RENAME TO messages")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_account_scope_started_at ON sessions(account_scope, started_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_account_scope_last_synced_at ON sessions(account_scope, last_synced_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_account_scope_session_id ON messages(account_scope, session_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_account_scope_timestamp ON messages(account_scope, timestamp)")
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN local_last_activity_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE sessions SET local_last_activity_at = started_at WHERE local_last_activity_at = 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_account_scope_local_last_activity_at ON sessions(account_scope, local_last_activity_at)")
            }
        }
    }
}
