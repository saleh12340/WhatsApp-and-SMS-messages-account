package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.RemittanceLedgerApp
import com.example.data.SyncUiState
import com.example.data.TransactionRepository
import com.example.importer.ImportStats
import com.example.importer.SmsImporter
import com.example.importer.WhatsAppChatImporter
import com.example.model.LedgerSummary
import com.example.model.TargetConfigEntity
import com.example.model.TransactionEntity
import com.example.model.TransactionSource
import com.example.model.TransactionType
import com.example.parser.RemittanceParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSourceFilter = MutableStateFlow<TransactionSource?>(null)
    val selectedSourceFilter: StateFlow<TransactionSource?> = _selectedSourceFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    // Combined query trigger
    private val _filterTrigger = MutableStateFlow(Triple("", null as TransactionSource?, null as TransactionType?))

    val transactions: StateFlow<List<TransactionEntity>> = _filterTrigger.flatMapLatest { (q, src, type) ->
        repository.getFilteredTransactions(
            query = q.ifBlank { null },
            source = src,
            type = type
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val summary: StateFlow<LedgerSummary> = repository.ledgerSummaryFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerSummary(0.0, 0.0, 0.0, 0)
    )

    val syncState: StateFlow<SyncUiState> = repository.syncEngine.syncState

    val targets: StateFlow<List<TargetConfigEntity>> = repository.allTargetsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _lastImportStats = MutableStateFlow<ImportStats?>(null)
    val lastImportStats: StateFlow<ImportStats?> = _lastImportStats.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        updateTrigger()
    }

    fun onSourceFilterSelect(source: TransactionSource?) {
        _selectedSourceFilter.value = source
        updateTrigger()
    }

    fun onTypeFilterSelect(type: TransactionType?) {
        _selectedTypeFilter.value = type
        updateTrigger()
    }

    private fun updateTrigger() {
        _filterTrigger.value = Triple(
            _searchQuery.value,
            _selectedSourceFilter.value,
            _selectedTypeFilter.value
        )
    }

    fun addManualTransaction(
        type: TransactionType,
        amount: Double,
        commission: Double,
        partyName: String,
        referenceNumber: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addManualTransaction(
                type = type,
                amount = amount,
                commission = commission,
                partyName = partyName,
                referenceNumber = referenceNumber,
                notes = notes,
                timestamp = timestamp
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun addTarget(type: String, identifier: String) {
        if (identifier.isBlank()) return
        viewModelScope.launch {
            repository.addTarget(type, identifier)
        }
    }

    fun toggleTarget(target: TargetConfigEntity) {
        viewModelScope.launch {
            repository.toggleTarget(target)
        }
    }

    fun removeTarget(id: Long) {
        viewModelScope.launch {
            repository.removeTarget(id)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncPending()
        }
    }

    /**
     * محاكاة وصول رسالة لاختبار الفرز والتصنيف والاستخراج
     */
    fun simulateMessage(text: String, sender: String, source: TransactionSource, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val parsed = RemittanceParser.parse(text, sender, source)
            val entity = RemittanceParser.toEntity(parsed, text, sender, source)
            val saved = repository.saveAutomaticTransaction(entity)
            onComplete(saved)
        }
    }

    fun clearLastImportStats() {
        _lastImportStats.value = null
    }

    /**
     * استيراد جميع الرسائل السابقة من صندوق الوارد (SMS Inbox)
     */
    fun importHistoricalSms(
        context: Context,
        targetSenderOnly: Boolean = false,
        onComplete: (ImportStats) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            val stats = SmsImporter.importHistoricalSms(context, repository, targetSenderOnly)
            _isImporting.value = false
            _lastImportStats.value = stats
            onComplete(stats)
        }
    }

    /**
     * استيراد محادثة واتساب كاملة من ملف نصي (تصدير الدردشة .txt)
     */
    fun importWhatsAppChatFile(
        context: Context,
        uri: Uri,
        defaultSenderName: String = "محادثة واتساب",
        onComplete: (ImportStats) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            val stats = WhatsAppChatImporter.importFromUri(context, uri, repository, defaultSenderName)
            _isImporting.value = false
            _lastImportStats.value = stats
            onComplete(stats)
        }
    }

    /**
     * استيراد محادثة واتساب من نص مكتوب أو منسوخ
     */
    fun importWhatsAppChatText(
        chatText: String,
        senderName: String = "محادثة واتساب",
        onComplete: (ImportStats) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            val stats = WhatsAppChatImporter.importFromText(chatText, repository, senderName)
            _isImporting.value = false
            _lastImportStats.value = stats
            onComplete(stats)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = RemittanceLedgerApp.instance
                return TransactionViewModel(app.repository) as T
            }
        }
    }
}
