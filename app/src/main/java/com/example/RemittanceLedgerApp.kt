package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.SyncEngine
import com.example.data.TransactionRepository

class RemittanceLedgerApp : Application() {

    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val database = AppDatabase.getInstance(this)
        val syncEngine = SyncEngine(this, database.transactionDao())
        repository = TransactionRepository(
            transactionDao = database.transactionDao(),
            targetConfigDao = database.targetConfigDao(),
            syncEngine = syncEngine
        )
    }

    companion object {
        lateinit var instance: RemittanceLedgerApp
            private set
    }
}
