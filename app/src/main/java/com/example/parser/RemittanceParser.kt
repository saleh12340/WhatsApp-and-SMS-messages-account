package com.example.parser

import com.example.model.TransactionEntity
import com.example.model.TransactionSource
import com.example.model.TransactionType
import java.util.regex.Pattern

data class ParsedRemittance(
    val type: TransactionType,
    val amount: Double,
    val commission: Double = 0.0,
    val totalAmount: Double = amount + commission,
    val currency: String = "YER",
    val partyName: String = "",
    val referenceNumber: String = "",
    val remainingBalance: Double? = null,
    val notes: String = ""
)

object RemittanceParser {

    // قائمة الكلمات الدلالية للمدين (Debit)
    private val DEBIT_KEYWORDS = listOf(
        "خصم", "عليكم", "حسبنا عليكم", "قيد عليكم", "سحب",
        "تحويل إلى", "تحويل الى", "دفع", "مدين", "سداد", "ارسال حوالة", "صرف"
    )

    // قائمة الكلمات الدلالية للدائن (Credit)
    private val CREDIT_KEYWORDS = listOf(
        "إضافة", "اضافة", "لكم", "لك", "حسبنا لكم", "قيد لكم",
        "تم إيداع", "تم ايداع", "استلام", "وارد", "دائن", "إيداع", "ايداع", "حوالة واردة", "قبض"
    )

    /**
     * تحويل الأرقام العربية المشرقية والفواصل إلى أرقام غربية
     */
    fun normalizeArabicNumbers(input: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], ('0' + i))
        }
        // توحيد الفواصل العشرية
        return result.replace('٫', '.')
    }

    /**
     * تحديد النوع المحاسبي بناءً على الكلمات المفتاحية
     */
    fun detectTransactionType(text: String): TransactionType {
        val lowerText = text.lowercase()

        var creditScore = 0
        var debitScore = 0

        for (kw in CREDIT_KEYWORDS) {
            if (lowerText.contains(kw.lowercase())) {
                creditScore += if (kw in listOf("حسبنا لكم", "قيد لكم", "تم إيداع", "لكم")) 2 else 1
            }
        }

        for (kw in DEBIT_KEYWORDS) {
            if (lowerText.contains(kw.lowercase())) {
                debitScore += if (kw in listOf("حسبنا عليكم", "قيد عليكم", "خصم", "عليكم")) 2 else 1
            }
        }

        return if (creditScore >= debitScore && creditScore > 0) {
            TransactionType.CREDIT
        } else {
            // الافتراضي في حال وجود كلمات خصم أو تعادل ترجيح المدين
            TransactionType.DEBIT
        }
    }

    /**
     * تفكيك واستخراج بيانات الحوالة باستخدام Regex
     */
    fun parse(rawText: String, senderOrChat: String, source: TransactionSource): ParsedRemittance {
        val normalized = normalizeArabicNumbers(rawText)

        val type = detectTransactionType(normalized)

        // 1. استخراج رقم الحوالة / العملية
        val refPattern = Pattern.compile(
            """(?:رقم\s*الحوالة|حوالة\s*رقم|رقم\s*العملية|عملية\s*رقم|رقم\s*السند|سند\s*رقم|المرجع|مرجع|Ref|No|رقم)\s*[:#=\-]?\s*([A-Za-z0-9\-]+)""",
            Pattern.CASE_INSENSITIVE
        )
        val refMatcher = refPattern.matcher(normalized)
        var reference = ""
        if (refMatcher.find()) {
            reference = refMatcher.group(1)?.trim() ?: ""
        }
        if (reference.isBlank()) {
            // محاولة استخراج كود رقمي من 6 أرقام فأكثر
            val generalNumberPattern = Pattern.compile("""\b(\d{6,12})\b""")
            val genMatcher = generalNumberPattern.matcher(normalized)
            if (genMatcher.find()) {
                reference = genMatcher.group(1)?.trim() ?: ""
            }
        }

        // 2. استخراج العمولة
        val commissionPattern = Pattern.compile(
            """(?:عمولة|العمولة|أجور|اجور|رسوم|الرسوم)\s*[:#=\-]?\s*([0-9,]+(?:\.[0-9]+)?)""",
            Pattern.CASE_INSENSITIVE
        )
        val commMatcher = commissionPattern.matcher(normalized)
        var commission = 0.0
        if (commMatcher.find()) {
            val commStr = commMatcher.group(1)?.replace(",", "") ?: "0"
            commission = commStr.toDoubleOrNull() ?: 0.0
        }

        // 3. استخراج الرصيد المتبقي
        val balancePattern = Pattern.compile(
            """(?:رصيدكم|الرصيد|رصيدك|المتبقي)(?:\s*(?:الحالي|المتبقي))?\s*[:#=\-]?\s*([0-9,]+(?:\.[0-9]+)?)""",
            Pattern.CASE_INSENSITIVE
        )
        val balMatcher = balancePattern.matcher(normalized)
        var balance: Double? = null
        if (balMatcher.find()) {
            val balStr = balMatcher.group(1)?.replace(",", "") ?: ""
            balance = balStr.toDoubleOrNull()
        }

        // 4. استخراج المبلغ والعملة
        val amountPattern = Pattern.compile(
            """(?:مبلغ|المبلغ|مبلغا\s*وقدره|بقيمة|تم\s*خصم|تم\s*تحويل|تم\s*إيداع|تم\s*ايداع|تم\s*إضافة|تم\s*اضافة|إضافة|اضافة|إيداع|ايداع|حوالة|عليكم|لكم)\s*[:#=\-]?\s*([0-9,]+(?:\.[0-9]+)?)\s*([A-Za-z\u0600-\u06FF]{2,10})?""",
            Pattern.CASE_INSENSITIVE
        )
        val amtMatcher = amountPattern.matcher(normalized)
        var amount = 0.0
        var currency = "YER"
        if (amtMatcher.find()) {
            val amtStr = amtMatcher.group(1)?.replace(",", "") ?: "0"
            amount = amtStr.toDoubleOrNull() ?: 0.0
            val curGroup = amtMatcher.group(2)?.trim()
            if (!curGroup.isNullOrBlank() && !curGroup.contains("ريال") && curGroup.length <= 5) {
                currency = curGroup
            } else if (curGroup?.contains("ريال") == true || normalized.contains("ريال") || normalized.contains("ر.ي")) {
                currency = if (normalized.contains("سعودي") || normalized.contains("SAR")) "SAR" else "YER"
            } else if (normalized.contains("دولار") || normalized.contains("USD") || normalized.contains("$")) {
                currency = "USD"
            }
        } else {
            // محاولة التقاط أكبر رقم مالي إذا لم يتم العثور على الكلمة التمهيدية
            val standaloneAmount = Pattern.compile("""\b([0-9]{3,}(?:,[0-9]{3})*(?:\.[0-9]+)?)\b""")
            val stMatcher = standaloneAmount.matcher(normalized)
            val refNum = reference.toDoubleOrNull()
            while (stMatcher.find()) {
                val candidateStr = stMatcher.group(1)?.replace(",", "") ?: "0"
                val candidate = candidateStr.toDoubleOrNull() ?: 0.0
                // استبعاد أرقام الحوالات أو الأرقام المطابقة للرصيد والعمولة
                if (candidate != balance && candidate != commission && candidate != refNum && candidate > amount) {
                    amount = candidate
                }
            }
        }

        // 5. استخراج اسم الطرف الآخر (المستفيد / المحول / العميل)
        val partyPattern = Pattern.compile(
            """(?:للمستفيد|المستفيد|إلى|الى|من|المحول|الطرف\s*الآخر|العميل|المودع)\s*[:#=\-]?\s*([\u0600-\u06FFA-Za-z\s]{3,35})(?=[,\n\r.\-;]|رقم|مبلغ|رصيد|$)""",
            Pattern.CASE_INSENSITIVE
        )
        val partyMatcher = partyPattern.matcher(normalized)
        var partyName = ""
        if (partyMatcher.find()) {
            partyName = partyMatcher.group(1)?.trim()?.replace("\n", " ") ?: ""
        }
        if (partyName.isBlank()) {
            partyName = if (type == TransactionType.CREDIT) "محول غير محدد" else "مستفيد غير محدد"
        }

        val totalAmount = if (commission > 0) amount + commission else amount

        return ParsedRemittance(
            type = type,
            amount = amount,
            commission = commission,
            totalAmount = totalAmount,
            currency = currency,
            partyName = partyName,
            referenceNumber = reference,
            remainingBalance = balance,
            notes = "معالجة تلقائية من $senderOrChat"
        )
    }

    /**
     * تحويل النتيجة المفككة إلى Entity قابل للحفظ في قاعدة البيانات
     */
    fun toEntity(
        parsed: ParsedRemittance,
        rawMessage: String,
        senderOrChat: String,
        source: TransactionSource
    ): TransactionEntity {
        return TransactionEntity(
            type = parsed.type,
            source = source,
            senderOrChat = senderOrChat,
            partyName = parsed.partyName,
            amount = parsed.amount,
            commission = parsed.commission,
            totalAmount = parsed.totalAmount,
            currency = parsed.currency,
            referenceNumber = parsed.referenceNumber,
            remainingBalance = parsed.remainingBalance,
            notes = parsed.notes,
            rawMessage = rawMessage,
            timestamp = System.currentTimeMillis()
        )
    }
}
