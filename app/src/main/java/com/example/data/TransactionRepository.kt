package com.example.data

import com.example.model.LedgerSummary
import com.example.model.SyncStatus
import com.example.model.TargetConfigEntity
import com.example.model.TransactionEntity
import com.example.model.TransactionSource
import com.example.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val targetConfigDao: TargetConfigDao,
    val syncEngine: SyncEngine
) {

    fun getFilteredTransactions(
        query: String?,
        source: TransactionSource?,
        type: TransactionType?
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getFilteredTransactions(query, source, type)
    }

    val ledgerSummaryFlow: Flow<LedgerSummary> = combine(
        transactionDao.getTotalDebitFlow(),
        transactionDao.getTotalCreditFlow(),
        transactionDao.getCountFlow()
    ) { debit, credit, count ->
        val net = credit - debit
        LedgerSummary(
            totalDebit = debit,
            totalCredit = credit,
            netBalance = net,
            totalTransactions = count
        )
    }

    suspend fun saveAutomaticTransaction(transaction: TransactionEntity): Boolean {
        // Prevent duplicates if referenceNumber is present
        if (transaction.referenceNumber.isNotBlank()) {
            val existing = transactionDao.findByReference(transaction.referenceNumber)
            if (existing != null) {
                // Already recorded
                return false
            }
        }
        transactionDao.insert(transaction)
        return true
    }

    suspend fun addManualTransaction(
        type: TransactionType,
        amount: Double,
        commission: Double,
        partyName: String,
        referenceNumber: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val total = amount + commission
        val ref = if (referenceNumber.isBlank()) {
            "MAN-${System.currentTimeMillis() % 1000000}"
        } else {
            referenceNumber.trim()
        }

        val entity = TransactionEntity(
            type = type,
            source = TransactionSource.MANUAL,
            senderOrChat = "إدخال يدوي",
            partyName = partyName.ifBlank { "طرف غير محدد" },
            amount = amount,
            commission = commission,
            totalAmount = total,
            currency = "YER",
            referenceNumber = ref,
            remainingBalance = null,
            notes = notes,
            rawMessage = null,
            timestamp = timestamp,
            syncStatus = SyncStatus.PENDING
        )
        return transactionDao.insert(entity)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.update(
            transaction.copy(
                totalAmount = transaction.amount + transaction.commission,
                syncStatus = SyncStatus.PENDING
            )
        )
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.softDelete(id)
    }

    // Target configuration
    val allTargetsFlow: Flow<List<TargetConfigEntity>> = targetConfigDao.getAllTargetsFlow()

    suspend fun addTarget(type: String, identifier: String): Long {
        return targetConfigDao.insert(
            TargetConfigEntity(targetType = type, identifier = identifier.trim(), isEnabled = true)
        )
    }

    suspend fun toggleTarget(target: TargetConfigEntity) {
        targetConfigDao.update(target.copy(isEnabled = !target.isEnabled))
    }

    suspend fun removeTarget(id: Long) {
        targetConfigDao.delete(id)
    }

    suspend fun isTargetSmsSender(sender: String): Boolean {
        val active = targetConfigDao.getActiveTargets()
            .filter { it.targetType.equals("SMS", ignoreCase = true) }
        val cleanSender = sender.trim().lowercase()
        return active.any {
            cleanSender.contains(it.identifier.trim().lowercase()) ||
            it.identifier.trim().lowercase().contains(cleanSender)
        }
    }

    suspend fun isTargetWhatsAppChat(title: String): Boolean {
        val active = targetConfigDao.getActiveTargets()
            .filter { it.targetType.equals("WHATSAPP", ignoreCase = true) }
        val cleanTitle = title.trim().lowercase()
        return active.any {
            cleanTitle.contains(it.identifier.trim().lowercase())
        }
    }

    suspend fun syncPending() = syncEngine.performSync()
}
