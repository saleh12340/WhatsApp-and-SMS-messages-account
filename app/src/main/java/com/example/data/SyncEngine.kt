package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.model.SyncStatus
import com.example.model.TransactionEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SyncUiState {
    object Idle : SyncUiState
    data class Syncing(val pendingCount: Int) : SyncUiState
    data class Success(val syncedCount: Int, val timestamp: Long) : SyncUiState
    data class Offline(val pendingCount: Int) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

class SyncEngine(
    private val context: Context,
    private val transactionDao: TransactionDao
) {
    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun performSync(): Result<Int> {
        val pendingList = transactionDao.getPendingSyncTransactions()
        if (pendingList.isEmpty()) {
            _syncState.value = SyncUiState.Success(syncedCount = 0, timestamp = System.currentTimeMillis())
            return Result.success(0)
        }

        if (!isOnline()) {
            _syncState.value = SyncUiState.Offline(pendingCount = pendingList.size)
            return Result.failure(Exception("الجهاز غير متصل بالإنترنت حالياً"))
        }

        _syncState.value = SyncUiState.Syncing(pendingCount = pendingList.size)

        return try {
            // Simulated robust cloud sync (e.g. Firebase Firestore / Cloud API)
            // with simulated latency and batch verification
            delay(1200)

            val now = System.currentTimeMillis()
            var count = 0
            for (tx in pendingList) {
                // Idempotent sync: update status in Room
                transactionDao.updateSyncStatus(tx.id, SyncStatus.SYNCED, now)
                count++
            }

            _syncState.value = SyncUiState.Success(syncedCount = count, timestamp = now)
            Result.success(count)
        } catch (e: Exception) {
            _syncState.value = SyncUiState.Error(e.localizedMessage ?: "فشل المزامنة")
            Result.failure(e)
        }
    }
}
