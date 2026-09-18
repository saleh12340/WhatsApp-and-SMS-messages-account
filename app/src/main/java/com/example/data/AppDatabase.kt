package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.TargetConfigEntity
import com.example.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, TargetConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun targetConfigDao(): TargetConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remittance_ledger.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default targets in background
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).targetConfigDao()
                                dao.insert(TargetConfigEntity(targetType = "SMS", identifier = "AmalbwadiEX"))
                                dao.insert(TargetConfigEntity(targetType = "SMS", identifier = "ALAMAL"))
                                dao.insert(TargetConfigEntity(targetType = "SMS", identifier = "الكريمي"))
                                dao.insert(TargetConfigEntity(targetType = "WHATSAPP", identifier = "AmalbwadiEX"))
                                dao.insert(TargetConfigEntity(targetType = "WHATSAPP", identifier = "صرافة الأمل"))
                                dao.insert(TargetConfigEntity(targetType = "WHATSAPP", identifier = "حوالات"))
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
