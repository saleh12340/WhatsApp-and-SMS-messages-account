package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.SyncStatus
import com.example.model.TransactionEntity
import com.example.model.TransactionSource
import com.example.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        AND (:source IS NULL OR source = :source)
        AND (:type IS NULL OR type = :type)
        AND (
            :query IS NULL OR :query = '' 
            OR partyName LIKE '%' || :query || '%' 
            OR referenceNumber LIKE '%' || :query || '%' 
            OR notes LIKE '%' || :query || '%'
            OR senderOrChat LIKE '%' || :query || '%'
        )
        ORDER BY timestamp DESC
    """)
    fun getFilteredTransactions(
        query: String?,
        source: TransactionSource?,
        type: TransactionType?
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE referenceNumber = :ref AND referenceNumber != '' LIMIT 1")
    suspend fun findByReference(ref: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncStatus = :status, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, syncedAt: Long)

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM transactions WHERE type = 'DEBIT' AND isDeleted = 0")
    fun getTotalDebitFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM transactions WHERE type = 'CREDIT' AND isDeleted = 0")
    fun getTotalCreditFlow(): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0")
    fun getCountFlow(): Flow<Int>
}
