package com.example.importer

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.data.TransactionRepository
import com.example.model.TransactionSource
import com.example.parser.RemittanceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportStats(
    val totalScanned: Int = 0,
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
    val skippedCount: Int = 0,
    val errorMessage: String? = null
)

object SmsImporter {

    private const val TAG = "SmsImporter"

    /**
     * استيراد جميع الرسائل السابقة من صندوق الوارد (SMS Inbox)
     * @param context سياق التطبيق للوصول إلى ContentResolver
     * @param repository مستودع البيانات للحفظ والتحقق من الأهداف
     * @param targetSenderOnly إذا كان true يستورد فقط للمرسلين المحددين في الإعدادات، وإذا كان false يستورد كل الحوالات المالية
     */
    suspend fun importHistoricalSms(
        context: Context,
        repository: TransactionRepository,
        targetSenderOnly: Boolean = false
    ): ImportStats = withContext(Dispatchers.IO) {
        var totalScanned = 0
        var importedCount = 0
        var duplicateCount = 0
        var skippedCount = 0

        val contentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    totalScanned++
                    val address = if (addressIdx != -1) c.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                    val dateMs = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isBlank() || address.isBlank()) {
                        skippedCount++
                        continue
                    }

                    // التحقق من الهدف
                    val isTarget = if (targetSenderOnly) {
                        repository.isTargetSmsSender(address)
                    } else {
                        // إذا كان المستهدف أو إذا كانت رسالة تحتوي على مؤشرات حوالة صريحة
                        repository.isTargetSmsSender(address) || RemittanceParser.isRemittanceMessage(body)
                    }

                    if (!isTarget) {
                        skippedCount++
                        continue
                    }

                    // التحقق من أنها رسالة حوالة / مالية
                    if (!RemittanceParser.isRemittanceMessage(body)) {
                        skippedCount++
                        continue
                    }

                    try {
                        val parsed = RemittanceParser.parse(
                            rawText = body,
                            senderOrChat = address,
                            source = TransactionSource.SMS
                        )

                        if (parsed.amount <= 0.0) {
                            skippedCount++
                            continue
                        }

                        val entity = RemittanceParser.toEntity(
                            parsed = parsed,
                            rawMessage = body,
                            senderOrChat = address,
                            source = TransactionSource.SMS,
                            timestamp = dateMs
                        )

                        val saved = repository.saveAutomaticTransaction(entity)
                        if (saved) {
                            importedCount++
                        } else {
                            duplicateCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing historical SMS from $address", e)
                        skippedCount++
                    }
                }
            }

            // تشغيل مزامنة سحابية بعد انتهاء الاستيراد
            if (importedCount > 0) {
                repository.syncPending()
            }

            Log.d(TAG, "Import completed: Scanned=$totalScanned, Imported=$importedCount, Duplicates=$duplicateCount, Skipped=$skippedCount")
            ImportStats(
                totalScanned = totalScanned,
                importedCount = importedCount,
                duplicateCount = duplicateCount,
                skippedCount = skippedCount
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: READ_SMS permission not granted", e)
            ImportStats(
                totalScanned = totalScanned,
                importedCount = importedCount,
                duplicateCount = duplicateCount,
                skippedCount = skippedCount,
                errorMessage = "لم يتم منح إذن قراءة الرسائل (READ_SMS) بعد."
            )
        } catch (e: Exception) {
            Log.e(TAG, "General exception during SMS import", e)
            ImportStats(
                totalScanned = totalScanned,
                importedCount = importedCount,
                duplicateCount = duplicateCount,
                skippedCount = skippedCount,
                errorMessage = e.localizedMessage ?: "حدث خطأ غير متوقع أثناء استيراد الرسائل"
            )
        }
    }
}
