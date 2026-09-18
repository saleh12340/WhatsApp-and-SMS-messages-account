package com.example.importer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.TransactionRepository
import com.example.model.TransactionSource
import com.example.parser.RemittanceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

object WhatsAppChatImporter {

    private const val TAG = "WhatsAppChatImporter"

    /**
     * استيراد الرسائل من ملف نصي مُصدَّر من واتساب (Exported Chat .txt)
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        repository: TransactionRepository,
        defaultSenderName: String = "محادثة واتساب"
    ): ImportStats = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ImportStats(errorMessage = "تعذر فتح ملف المحادثة")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val fullText = reader.use { it.readText() }
            importFromText(fullText, repository, defaultSenderName)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WhatsApp chat file", e)
            ImportStats(errorMessage = "خطأ في قراءة ملف الدردشة: ${e.localizedMessage}")
        }
    }

    /**
     * استيراد الرسائل من نص محادثة واتساب كامل (سطر بسطر أو رسائل متعددة)
     */
    suspend fun importFromText(
        chatText: String,
        repository: TransactionRepository,
        defaultSenderName: String = "محادثة واتساب"
    ): ImportStats = withContext(Dispatchers.IO) {
        var totalScanned = 0
        var importedCount = 0
        var duplicateCount = 0
        var skippedCount = 0

        val lines = chatText.lines()
        val messageChunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        // نمط بداية سطر رسالة واتساب القياسي: "18/09/2026, 10:15 - " أو "[18/09/2026, 10:15:30]"
        val waHeaderPattern = Pattern.compile("""^(?:\[?\d{1,4}[/\-.]\d{1,2}[/\-.]\d{1,4}[,\s]+\d{1,2}:\d{1,2}(?::\d{1,2})?(?:\s*[APap][Mm])?\]?\s*[-:]?\s*)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (waHeaderPattern.matcher(trimmed).find()) {
                if (currentChunk.isNotEmpty()) {
                    messageChunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
                currentChunk.append(trimmed)
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append("\n").append(trimmed)
                } else {
                    currentChunk.append(trimmed)
                }
            }
        }
        if (currentChunk.isNotEmpty()) {
            messageChunks.add(currentChunk.toString())
        }

        // معالجة كل رسالة
        for (rawChunk in messageChunks) {
            totalScanned++

            // استخراج اسم المرسل إن وجد في هيدر واتساب: "18/09/2026, 10:15 - Amalbwadi: النص..."
            var senderName = defaultSenderName
            var cleanBody = rawChunk

            val senderExtractPattern = Pattern.compile("""^(?:\[?.*?\]?\s*[-:]?\s*)([^:]+):\s*(.*)$""", Pattern.DOTALL)
            val matcher = senderExtractPattern.matcher(rawChunk)
            if (matcher.find()) {
                val extractedSender = matcher.group(1)?.trim() ?: ""
                val extractedBody = matcher.group(2)?.trim() ?: ""
                if (extractedSender.isNotBlank() && extractedBody.isNotBlank()) {
                    senderName = extractedSender
                    cleanBody = extractedBody
                }
            }

            if (!RemittanceParser.isRemittanceMessage(cleanBody)) {
                skippedCount++
                continue
            }

            try {
                val parsed = RemittanceParser.parse(
                    rawText = cleanBody,
                    senderOrChat = senderName,
                    source = TransactionSource.WHATSAPP
                )

                if (parsed.amount <= 0.0) {
                    skippedCount++
                    continue
                }

                val entity = RemittanceParser.toEntity(
                    parsed = parsed,
                    rawMessage = cleanBody,
                    senderOrChat = senderName,
                    source = TransactionSource.WHATSAPP,
                    timestamp = System.currentTimeMillis()
                )

                val saved = repository.saveAutomaticTransaction(entity)
                if (saved) {
                    importedCount++
                } else {
                    duplicateCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing WhatsApp line", e)
                skippedCount++
            }
        }

        if (importedCount > 0) {
            repository.syncPending()
        }

        ImportStats(
            totalScanned = totalScanned,
            importedCount = importedCount,
            duplicateCount = duplicateCount,
            skippedCount = skippedCount
        )
    }
}
