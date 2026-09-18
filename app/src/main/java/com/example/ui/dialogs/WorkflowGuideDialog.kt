package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed

@Composable
fun WorkflowGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("workflow_guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Heart/Workflow Theme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DebitRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = DebitRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ملف سير العمل (علاقة حب)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تناغم الأتمتة والمحاسبة وراحة البال",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Intro card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "فلسفة التطبيق: تحويل فوضى إشعارات الحوالات إلى علاقة تناغم وطمأنينة مالية. يرصد النظام الرسائل، يفرزها، يحللها محاسبياً، ويحفظها محلياً دون أي مجهود يدوي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Step 1
                WorkflowStepItem(
                    stepNumber = "1",
                    icon = Icons.Default.MarkEmailRead,
                    title = "الاستقبال والاستماع التلقائي",
                    description = "استقبال فوري عبر SMS Receiver وخدمة استماع إشعارات واتساب، أو تسجيل الحركات النقدية يدوياً.",
                    accentColor = MaterialTheme.colorScheme.primary
                )

                // Step 2
                WorkflowStepItem(
                    stepNumber = "2",
                    icon = Icons.Default.FilterAlt,
                    title = "بوابة الفرز وحماية الخصوصية",
                    description = "فلترة صارمة تضمن معالجة الرسائل القادمة من أرقام الصرافين والمحادثات المعتمدة فقط، وتجاهل الباقي تماماً.",
                    accentColor = Color(0xFFE65100)
                )

                // Step 3
                WorkflowStepItem(
                    stepNumber = "3",
                    icon = Icons.Default.Psychology,
                    title = "التحليل الذكي والتصنيف المحاسبي",
                    description = "استخراج المبلغ، العمولة، رقم الحوالة، والمستفيد، وتصنيف الحركة بدقة: دائن لكم (+) أو مدين عليكم (-).",
                    accentColor = CreditGreen
                )

                // Step 4
                WorkflowStepItem(
                    stepNumber = "4",
                    icon = Icons.Default.Storage,
                    title = "التخزين المحلي الآمن (Room DB)",
                    description = "تخزين فوري محلي 100%، منع التكرار بناءً على رقم الحوالة، وتحديث الرصيد الصافي والإجماليات فوراً.",
                    accentColor = Color(0xFF5E35B1)
                )

                // Step 5
                WorkflowStepItem(
                    stepNumber = "5",
                    icon = Icons.Default.CloudSync,
                    title = "المزامنة السحابية عند الاتصال",
                    description = "يعمل النظام أوفلاين أولاً بكل كفاءة، وعند توفر الإنترنت يرفع الحركات للسحابة بأمان وثقة تامة.",
                    accentColor = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider()

                // Accounting Matrix Summary
                Text(
                    text = "مصفوفة التأثير على الرصيد الصافي:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CreditGreen.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "حركة دائنة (لكم)", color = CreditGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(text = "+ زيادة في رصيدك (إيداع / وارد)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DebitRed.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "حركة مدينة (عليكم)", color = DebitRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(text = "- خصم من رصيدك (تحويل / صرف)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تم الفهم والاستيعاب", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WorkflowStepItem(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "خطوة $stepNumber",
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
