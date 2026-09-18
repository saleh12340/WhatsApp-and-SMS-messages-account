package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncUiState
import com.example.model.LedgerSummary
import com.example.model.TransactionEntity
import com.example.model.TransactionSource
import com.example.model.TransactionType
import com.example.ui.components.LedgerSummaryCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.dialogs.ManualEntryDialog
import com.example.ui.dialogs.SimulateMessageDialog
import com.example.ui.dialogs.TransactionDetailDialog
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed
import com.example.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: TransactionViewModel,
    transactions: List<TransactionEntity>,
    summary: LedgerSummary,
    syncState: SyncUiState,
    searchQuery: String,
    selectedSource: TransactionSource?,
    selectedType: TransactionType?,
    onNavigateToSettings: () -> Unit
) {
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSimulateDialog by remember { mutableStateOf(false) }
    var deleteCandidateId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "سجل الحوالات والمحاسبة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إدارة وقراءة الحوالات أوفلاين والمزامنة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    // Test Message simulation button
                    IconButton(
                        onClick = { showSimulateDialog = true },
                        modifier = Modifier.testTag("btn_open_simulate")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "اختبار رسالة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Cloud Sync action
                    IconButton(
                        onClick = { viewModel.triggerSync() },
                        modifier = Modifier.testTag("btn_quick_sync")
                    ) {
                        Icon(
                            imageVector = if (syncState is SyncUiState.Syncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                            contentDescription = "مزامنة سحابية",
                            tint = if (syncState is SyncUiState.Syncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings action
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات والأذونات"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTransaction = null
                    showManualEntryDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_manual")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إدخال يدوي",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("transactions_list"),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary Card
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    LedgerSummaryCard(summary = summary)
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("بحث برقم الحوالة، اسم المستفيد، أو الملاحظات...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field")
                    )

                    // Type Filter Chips (الكل / دائن / مدين)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { viewModel.onTypeFilterSelect(null) },
                            label = { Text("كل الحركات") },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.CREDIT,
                            onClick = { viewModel.onTypeFilterSelect(TransactionType.CREDIT) },
                            label = { Text("دائن (لكم)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CreditGreen.copy(alpha = 0.15f),
                                selectedLabelColor = CreditGreen
                            )
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.DEBIT,
                            onClick = { viewModel.onTypeFilterSelect(TransactionType.DEBIT) },
                            label = { Text("مدين (عليكم)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DebitRed.copy(alpha = 0.15f),
                                selectedLabelColor = DebitRed
                            )
                        )
                    }

                    // Source Filter Chips (الكل / SMS / واتساب / يدوي)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSource == null,
                            onClick = { viewModel.onSourceFilterSelect(null) },
                            label = { Text("كافة المصادر") }
                        )
                        FilterChip(
                            selected = selectedSource == TransactionSource.SMS,
                            onClick = { viewModel.onSourceFilterSelect(TransactionSource.SMS) },
                            label = { Text("رسائل SMS") }
                        )
                        FilterChip(
                            selected = selectedSource == TransactionSource.WHATSAPP,
                            onClick = { viewModel.onSourceFilterSelect(TransactionSource.WHATSAPP) },
                            label = { Text("إشعارات واتساب") }
                        )
                        FilterChip(
                            selected = selectedSource == TransactionSource.MANUAL,
                            onClick = { viewModel.onSourceFilterSelect(TransactionSource.MANUAL) },
                            label = { Text("إدخال يدوي (كاش)") }
                        )
                    }
                }
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "كشف الحساب وسجل الحركات",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transactions.size} حركة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Empty State
            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedSource != null || selectedType != null) {
                                "لا توجد حركات تطابق معايير الفلترة والبحث"
                            } else {
                                "لا توجد أي حركات مسجلة حتى الآن"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يمكنك إضافة حركة يدوياً عبر زر '+ إدخال يدوي'، أو تجربة زر 'اختبار رسالة' بأعلى الشاشة لمعاينة قراءة رسائل الحوالات فورياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    items = transactions,
                    key = { it.id }
                ) { tx ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TransactionItemCard(
                            transaction = tx,
                            onClick = { viewingTransaction = tx },
                            onEdit = if (tx.source == TransactionSource.MANUAL) {
                                {
                                    editingTransaction = tx
                                    showManualEntryDialog = true
                                }
                            } else null,
                            onDelete = if (tx.source == TransactionSource.MANUAL) {
                                { deleteCandidateId = tx.id }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Manual Entry Dialog (Add or Edit)
    if (showManualEntryDialog) {
        ManualEntryDialog(
            initialTransaction = editingTransaction,
            onDismiss = {
                showManualEntryDialog = false
                editingTransaction = null
            },
            onSave = { type, amount, commission, partyName, reference, notes ->
                if (editingTransaction == null) {
                    viewModel.addManualTransaction(
                        type = type,
                        amount = amount,
                        commission = commission,
                        partyName = partyName,
                        referenceNumber = reference,
                        notes = notes
                    )
                } else {
                    viewModel.updateTransaction(
                        editingTransaction!!.copy(
                            type = type,
                            amount = amount,
                            commission = commission,
                            partyName = partyName,
                            referenceNumber = reference,
                            notes = notes
                        )
                    )
                }
                showManualEntryDialog = false
                editingTransaction = null
            }
        )
    }

    // Transaction Details Dialog
    viewingTransaction?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onDismiss = { viewingTransaction = null }
        )
    }

    // Simulate Message Dialog
    if (showSimulateDialog) {
        SimulateMessageDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { text, sender, source ->
                viewModel.simulateMessage(text, sender, source) {
                    showSimulateDialog = false
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteCandidateId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text("تأكيد حذف الحركة اليدوية") },
            text = { Text("هل أنت متأكد من حذف هذه الحركة اليدوية من كشف الحساب؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(id)
                        deleteCandidateId = null
                    }
                ) {
                    Text("حذف", color = DebitRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
