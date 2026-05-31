package com.hermes.mobile.core.di

import android.content.Context
import androidx.room.Room
import com.hermes.mobile.core.data.local.HermesDatabase
import com.hermes.mobile.core.data.local.MessageDao
import com.hermes.mobile.core.data.local.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HermesDatabase {
        return Room.databaseBuilder(context, HermesDatabase::class.java, "hermes.db")
            .addMigrations(
                HermesDatabase.MIGRATION_1_2,
                HermesDatabase.MIGRATION_2_3,
                HermesDatabase.MIGRATION_3_4,
                HermesDatabase.MIGRATION_4_5,
                HermesDatabase.MIGRATION_5_6,
                HermesDatabase.MIGRATION_6_7,
                HermesDatabase.MIGRATION_7_8,
            )
            .build()
    }

    @Provides
    fun provideSessionDao(database: HermesDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideMessageDao(database: HermesDatabase): MessageDao = database.messageDao()
}
