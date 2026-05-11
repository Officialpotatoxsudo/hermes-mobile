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
        return Room.databaseBuilder(context, HermesDatabase::class.java, "hermes.db").build()
    }

    @Provides
    fun provideSessionDao(database: HermesDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideMessageDao(database: HermesDatabase): MessageDao = database.messageDao()
}
