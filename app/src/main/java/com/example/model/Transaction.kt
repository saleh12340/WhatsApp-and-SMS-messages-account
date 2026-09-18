package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    DEBIT,  // مدين (خصم / عليكم)
    CREDIT  // دائن (إضافة / لكم)
}

enum class TransactionSource {
    SMS,       // رسالة نصية قصيرة
    WHATSAPP,  // إشعار واتساب
    MANUAL     // إدخال يدوي
}

enum class SyncStatus {
    PENDING,   // بانتظار المزامنة
    SYNCED,    // تم المزامنة سحابياً
    FAILED     // تعذر المزامنة مؤقتاً
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["referenceNumber"], unique = false),
        Index(value = ["timestamp"]),
        Index(value = ["source"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val source: TransactionSource,
    val senderOrChat: String,          // اسم المرسل أو المحادثة (مثل AmalbwadiEX أو اسم محادثة واتساب)
    val partyName: String,             // الطرف الآخر: المستفيد أو المحول
    val amount: Double,                // المبلغ
    val commission: Double = 0.0,      // العمولة / أجور التحويل
    val totalAmount: Double = amount + commission, // المبلغ الإجمالي
    val currency: String = "YER",       // العملة الافتراضية
    val referenceNumber: String = "",   // رقم الحوالة أو السند
    val remainingBalance: Double? = null, // الرصيد المتبقي بعد العملية إن وجد
    val notes: String = "",            // ملاحظات أو سبب الإدخال اليدوي
    val rawMessage: String? = null,    // النص الأصلي للرسالة إن وُجد
    val timestamp: Long = System.currentTimeMillis(), // تاريخ ووقت العملية
    val syncStatus: SyncStatus = SyncStatus.PENDING,  // حالة المزامنة
    val syncedAt: Long? = null,        // وقت المزامنة السحابية
    val isDeleted: Boolean = false     // Soft delete for sync tracking
)

data class LedgerSummary(
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double,
    val totalTransactions: Int
)

data class TargetRule(
    val smsSenders: List<String> = listOf("AmalbwadiEX", "ALAMAL", "ALQASIMI", "ALNAJM"),
    val whatsappChats: List<String> = listOf("حوالات", "صرافة الأمل", "AmalbwadiEX")
)
