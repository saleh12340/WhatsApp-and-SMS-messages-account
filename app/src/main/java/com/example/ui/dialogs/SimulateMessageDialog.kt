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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.TransactionSource

@Composable
fun SimulateMessageDialog(
    onDismiss: () -> Unit,
    onSimulate: (text: String, sender: String, source: TransactionSource) -> Unit
) {
    var source by remember { mutableStateOf(TransactionSource.SMS) }
    var sender by remember { mutableStateOf("AmalbwadiEX") }
    var messageText by remember {
        mutableStateOf("تم إضافة 150000 ريال يمني لحسابكم من المحول سالم باعباد. عمولة: 1000 ريال. رقم الحوالة: 78945612. رصيدكم الحالي: 1450000 ريال.")
    }

    val sampleCreditSms = "تم إضافة 150000 ريال يمني لحسابكم من المحول سالم باعباد. عمولة: 1000 ريال. رقم الحوالة: 78945612. رصيدكم الحالي: 1450000 ريال."
    val sampleDebitSms = "تم خصم 65000 ريال يمني من حسابكم للمستفيد مؤسسة النور. عمولة: 500 ريال. رقم العملية: 45612378. رصيدكم الحالي: 1384500 ريال."
    val sampleWhatsApp = "حسبنا لكم مبلغ 200000 ريال عن طريق صرافة الأمل، رقم السند: 9812456 للمستفيد خالد باوزير."
    val sampleDebitWhatsApp = "حسبنا عليكم تحويل إلى أحمد محمد بمبلغ 85000 ريال، عمولة: 400. رقم الحوالة: 332211."

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("simulate_dialog")
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
                        text = "محاكاة واختبار قراءة رسالة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "يمكنك اختبار محرك Regex المحلي لمعالجة وتصنيف رسائل الحوالات تلقائياً وحفظها في قاعدة البيانات فوراً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Source selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (source == TransactionSource.SMS) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                source = TransactionSource.SMS
                                sender = "AmalbwadiEX"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "رسالة نصية SMS",
                            color = if (source == TransactionSource.SMS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (source == TransactionSource.WHATSAPP) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                source = TransactionSource.WHATSAPP
                                sender = "صرافة الأمل"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "إشعار واتساب",
                            color = if (source == TransactionSource.WHATSAPP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sender / Chat
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("اسم المرسل أو المحادثة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick presets
                Text(
                    text = "نماذج رسائل جاهزة للاختبار:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip("دائن SMS") {
                        source = TransactionSource.SMS
                        sender = "AmalbwadiEX"
                        messageText = sampleCreditSms
                    }
                    PresetChip("مدين SMS") {
                        source = TransactionSource.SMS
                        sender = "AmalbwadiEX"
                        messageText = sampleDebitSms
                    }
                    PresetChip("دائن واتساب") {
                        source = TransactionSource.WHATSAPP
                        sender = "صرافة الأمل"
                        messageText = sampleWhatsApp
                    }
                    PresetChip("مدين واتساب") {
                        source = TransactionSource.WHATSAPP
                        sender = "صرافة الأمل"
                        messageText = sampleDebitWhatsApp
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Message Text
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("نص الرسالة") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Process button
                Button(
                    onClick = {
                        if (messageText.isNotBlank() && sender.isNotBlank()) {
                            onSimulate(messageText, sender, source)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_run_simulation"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "معالجة وتصنيف الرسالة فوراً",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
