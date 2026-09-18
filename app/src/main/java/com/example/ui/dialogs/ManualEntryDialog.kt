package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenBg
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedBg
import com.example.ui.util.FormatUtils

@Composable
fun ManualEntryDialog(
    initialTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amount: Double,
        commission: Double,
        partyName: String,
        referenceNumber: String,
        notes: String
    ) -> Unit
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.CREDIT) }
    var amountText by remember { mutableStateOf(initialTransaction?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var commissionText by remember { mutableStateOf(initialTransaction?.commission?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var partyName by remember { mutableStateOf(initialTransaction?.partyName ?: "") }
    var referenceNumber by remember { mutableStateOf(initialTransaction?.referenceNumber ?: "") }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val commissionValue = commissionText.toDoubleOrNull() ?: 0.0
    val totalValue = amountValue + commissionValue

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("manual_entry_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTransaction == null) "إدخال حركة محاسبية يدوية" else "تعديل العملية اليدوية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type Toggle: Debit vs Credit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Credit (دائن / إضافة / لكم)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == TransactionType.CREDIT) CreditGreen else Color.Transparent)
                            .clickable { type = TransactionType.CREDIT }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "دائن (لكم / إضافة)",
                            color = if (type == TransactionType.CREDIT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Debit (مدين / خصم / عليكم)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == TransactionType.DEBIT) DebitRed else Color.Transparent)
                            .clickable { type = TransactionType.DEBIT }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "مدين (عليكم / خصم)",
                            color = if (type == TransactionType.DEBIT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("المبلغ (ر.ي)*") },
                    placeholder = { Text("مثال: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Commission
                OutlinedTextField(
                    value = commissionText,
                    onValueChange = { commissionText = it },
                    label = { Text("العمولة إن وجدت (ر.ي)") },
                    placeholder = { Text("مثال: 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_commission")
                )

                // Total Amount preview card
                if (amountValue > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (type == TransactionType.CREDIT) CreditGreenBg else DebitRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبلغ الإجمالي المحتسب:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = FormatUtils.formatMoney(totalValue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (type == TransactionType.CREDIT) CreditGreen else DebitRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Party Name
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("اسم المستفيد / الطرف الآخر*") },
                    placeholder = { Text("مثال: أحمد محمد علي") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_party_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Reference / Phone
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("رقم مرجعي أو هاتف الحوالة (اختياري)") },
                    placeholder = { Text("مثال: 9482716") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reference")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes / Reason
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات أو سبب الإدخال اليدوي") },
                    placeholder = { Text("مثال: تسليم كاش / تأخر إشعار البنك") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("input_notes")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = DebitRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0) {
                            errorMessage = "يرجى كتابة مبلغ صحيح أكبر من الصفر"
                            return@Button
                        }
                        if (partyName.isBlank()) {
                            errorMessage = "يرجى كتابة اسم الطرف الآخر أو المستفيد"
                            return@Button
                        }
                        val parsedComm = commissionText.toDoubleOrNull() ?: 0.0
                        onSave(
                            type,
                            parsedAmount,
                            parsedComm,
                            partyName.trim(),
                            referenceNumber.trim(),
                            notes.trim()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_save_manual"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.CREDIT) CreditGreen else DebitRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (initialTransaction == null) "حفظ وإضافة إلى كشف الحساب" else "حفظ التعديلات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
