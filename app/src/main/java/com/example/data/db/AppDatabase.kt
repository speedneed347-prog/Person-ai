package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AssistantIdentity
import com.example.data.model.AutomationRoutine
import com.example.data.model.ChatMessage
import com.example.data.model.LocalMemory
import com.example.data.model.OwnerProfile
import com.example.data.model.Reminder
import com.example.data.model.SecurityLog

@Database(
    entities = [
        OwnerProfile::class,
        AssistantIdentity::class,
        ChatMessage::class,
        LocalMemory::class,
        Reminder::class,
        SecurityLog::class,
        AutomationRoutine::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun aegisDao(): AegisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aegis_ai_assistant_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
