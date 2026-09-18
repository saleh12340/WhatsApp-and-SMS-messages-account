package com.example.parser

import com.example.model.TransactionEntity
import com.example.model.TransactionSource
import com.example.model.TransactionType
import java.util.regex.Pattern

data class ParsedRemittance(
    val type: TransactionType,
    val amount: Double,
    val commission: Double = 0.0,
    val totalAmount: Double = if (type == TransactionType.DEBIT && commission > 0) amount + commission else amount,
    val currency: String = "YER",
    val partyName: String = "",
    val referenceNumber: String = "",
    val remainingBalance: Double? = null,
    val notes: String = ""
)

object RemittanceParser {

    // كلمات تدل بشكل قاطع على أن الرسالة هي إشعار مالي أو حوالة
    private val REMITTANCE_INDICATORS = listOf(
        "حوالة", "حواله", "قيد لكم", "قيد عليكم", "حسبنا لكم", "حسبنا عليكم",
        "تم إيداع", "تم ايداع", "تم خصم", "تم تحويل", "تم إضافة", "تم اضافة",
        "مبلغ", "عمولة", "عموله", "رصيدكم", "رصيدك", "الرصيد", "سند", "المرجع"
    )

    // كلمات دلالية للعمليات الدائنة (إضافة / لكم / وارد)
    private val CREDIT_KEYWORDS = listOf(
        "حسبنا لكم", "قيد لكم", "تم إيداع", "تم ايداع", "تم إضافة", "تم اضافة",
        "حوالة واردة", "حواله وارده", "استلام حوالة", "استلام حواله",
        "إيداع", "ايداع", "إضافة", "اضافة", "وارد", "واردة", "وارده", "دائن",
        "قبض", "تغذية", "استرداد", "لكم"
    )

    // كلمات دلالية للعمليات المدينة (خصم / عليكم / صادر)
    private val DEBIT_KEYWORDS = listOf(
        "حسبنا عليكم", "قيد عليكم", "تم خصم", "تم سحب", "تم تحويل",
        "حوالة صادرة", "حواله صادره", "إرسال حوالة", "ارسال حواله", "ارسال حوالة",
        "خصم", "سحب", "تحويل إلى", "تحويل الى", "دفع", "سداد", "مدين", "صرف",
        "مشتريات", "صادرة", "صادره", "عليكم"
    )

    /**
     * التحقق مما إذا كانت الرسالة تحتوي على مؤشرات حوالة أو عملية مالية
     */
    fun isRemittanceMessage(text: String): Boolean {
        val normalized = normalizeArabicNumbers(text).lowercase()
        return REMITTANCE_INDICATORS.any { normalized.contains(it.lowercase()) }
    }

    /**
     * تحويل الأرقام العربية المشرقية (٠١٢٣٤٥٦٧٨٩) والفواصل العشرية (٫) إلى أرقام قياسية
     */
    fun normalizeArabicNumbers(input: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], ('0' + i))
        }
        return result.replace('٫', '.')
    }

    /**
     * تحديد النوع المحاسبي بناءً على الكلمات المفتاحية وأوزانها
     */
    fun detectTransactionType(text: String): TransactionType {
        val lowerText = text.lowercase()

        var creditScore = 0
        var debitScore = 0

        for (kw in CREDIT_KEYWORDS) {
            if (lowerText.contains(kw.lowercase())) {
                val weight = when (kw) {
                    "حسبنا لكم", "قيد لكم", "تم إيداع", "حوالة واردة", "استلام حوالة" -> 3
                    "لكم", "إيداع", "ايداع", "تم إضافة" -> 2
                    else -> 1
                }
                creditScore += weight
            }
        }

        for (kw in DEBIT_KEYWORDS) {
            if (lowerText.contains(kw.lowercase())) {
                val weight = when (kw) {
                    "حسبنا عليكم", "قيد عليكم", "تم خصم", "حوالة صادرة", "إرسال حوالة" -> 3
                    "عليكم", "خصم", "تم تحويل", "تحويل إلى", "تحويل الى" -> 2
                    else -> 1
                }
                debitScore += weight
            }
        }

        return if (creditScore > debitScore) {
            TransactionType.CREDIT
        } else {
            TransactionType.DEBIT
        }
    }

    /**
     * تفكيك واستخراج بيانات الحوالة بدقة تامة (المبلغ، العمولة، الرصيد، رقم الحوالة، المستفيد، العملة)
     */
    fun parse(rawText: String, senderOrChat: String, source: TransactionSource): ParsedRemittance {
        val normalized = normalizeArabicNumbers(rawText)
        val type = detectTransactionType(normalized)

        // 1. استخراج رقم الحوالة / العملية / المرجع (Reference Number)
        var reference = ""
        val refPatterns = listOf(
            Pattern.compile("""(?:رقم\s*الحوالة|حوالة\s*رقم|حواله\s*رقم|كود\s*الحوالة|رمز\s*الحوالة|رقم\s*العملية|عملية\s*رقم|رقم\s*السند|سند\s*رقم|المرجع|مرجع|Ref\s*No|Ref|No|رقم)\s*[:#=\-]?\s*([A-Za-z0-9\-]+)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:حوالة|حواله)\s+([0-9]{6,12})\b""", Pattern.CASE_INSENSITIVE)
        )
        for (pattern in refPatterns) {
            val matcher = pattern.matcher(normalized)
            if (matcher.find()) {
                val group = matcher.group(1)?.trim() ?: ""
                if (group.isNotBlank() && !group.equals("مبلغ", ignoreCase = true) && !group.equals("ريال", ignoreCase = true)) {
                    reference = group
                    break
                }
            }
        }
        if (reference.isBlank()) {
            // محاولة التقاط كود رقمي عام من 6 إلى 12 رقماً إذا لم يكن مسبوقاً بمبلغ أو رصيد
            val generalNumberPattern = Pattern.compile("""\b(\d{6,12})\b""")
            val genMatcher = generalNumberPattern.matcher(normalized)
            while (genMatcher.find()) {
                val candidate = genMatcher.group(1)?.trim() ?: ""
                if (candidate.isNotBlank()) {
                    reference = candidate
                    break
                }
            }
        }

        // 2. استخراج العمولة / أجور التحويل (Commission / Fee)
        var commission = 0.0
        val commissionPatterns = listOf(
            Pattern.compile("""(?:عمولة\s*التحويل|عمولة\s*الحوالة|عمولة\s*الإرسال|عمولة\s*الارسال|عمولة\s*الحواله|أجور\s*التحويل|اجور\s*التحويل|أجور\s*الإرسال|اجور\s*الارسال|أجور\s*الحوالة|اجور\s*الحوالة|أجور\s*الحواله|اجور\s*الحواله|رسوم\s*التحويل|رسوم\s*الحوالة|العمولة|العموله|عمولة|عموله|أجور|اجور|الرسوم|رسوم)\s*[:#=\-]?\s*([0-9,]+(?:\.[0-9]+)?)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""\+?\s*([0-9,]+(?:\.[0-9]+)?)\s*(?:أجور|اجور|عمولة|عموله|رسوم)""", Pattern.CASE_INSENSITIVE)
        )
        for (p in commissionPatterns) {
            val commMatcher = p.matcher(normalized)
            if (commMatcher.find()) {
                val commStr = commMatcher.group(1)?.replace(",", "") ?: "0"
                val parsedComm = commStr.toDoubleOrNull() ?: 0.0
                if (parsedComm > 0) {
                    commission = parsedComm
                    break
                }
            }
        }

        // 3. استخراج الرصيد المتبقي / الإجمالي (Remaining / Running / Total Balance)
        var balance: Double? = null
        val balancePatterns = listOf(
            Pattern.compile("""(?:الرصيد\s*الإجمالي|الرصيد\s*الاجمالي|إجمالي\s*الرصيد|اجمالي\s*الرصيد|الرصيد\s*بعد\s*العملية|الرصيد\s*بعد\s*التحويل|الرصيد\s*الحالي|رصيدك\s*الحالي|رصيدكم\s*الحالي|الرصيد\s*المتاح|رصيدك\s*المتاح|رصيدكم\s*المتاح|الرصيد\s*المتبقي|رصيدك\s*المتبقي|رصيدكم\s*المتبقي|الرصيد\s*الكلي|رصيد\s*الحساب|رصيدكم|رصيدك|الرصيد|رصيد|balance|avail\s*bal)\s*[:#=\-]?\s*([0-9,]+(?:\.[0-9]+)?)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([0-9,]+(?:\.[0-9]+)?)\s*(?:ريال|ر\.ي|YER|SAR|USD)?\s*(?:الرصيد\s*المتبقي|رصيدك\s*الحالي|الرصيد)""", Pattern.CASE_INSENSITIVE)
        )
        for (p in balancePatterns) {
            val balMatcher = p.matcher(normalized)
            if (balMatcher.find()) {
                val balStr = balMatcher.group(1)?.replace(",", "") ?: ""
                val candidateBal = balStr.toDoubleOrNull()
                if (candidateBal != null && candidateBal != commission) {
                    balance = candidateBal
                    break
                }
            }
        }

        // 4. استخراج المبلغ الصافي والعملة (Amount & Currency)
        var amount = 0.0
        var currency = "YER"

        val amountPatterns = listOf(
            Pattern.compile("""(?:مبلغ\s*الحوالة|مبلغ\s*الحواله|مبلغ\s*التحويل|المبلغ\s*الصافي|مبلغ|المبلغ|مبلغا\s*وقدره|مبلغاً\s*وقدره|بقيمة|قيمة|قيد\s*لكم\s*مبلغ|قيد\s*عليكم\s*مبلغ|حسبنا\s*لكم\s*مبلغ|حسبنا\s*عليكم\s*مبلغ|تم\s*خصم\s*مبلغ|تم\s*خصم|تم\s*إيداع\s*مبلغ|تم\s*ايداع\s*مبلغ|تم\s*إيداع|تم\s*ايداع|تم\s*إضافة\s*مبلغ|تم\s*اضافة\s*مبلغ|تم\s*إضافة|تم\s*اضافة|تم\s*تحويل\s*مبلغ|تم\s*تحويل|استلام\s*حوالة|ارسال\s*حوالة|إرسال\s*حوالة|لكم\s*مبلغ|عليكم\s*مبلغ|لكم|عليكم)\s*[:#=\-]?\s*([0-9,]+(?:\.[0-9]+)?)\s*([A-Za-z\u0600-\u06FF]{2,10})?""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:حوالة|حواله)\s*[:#=\-]?\s*([0-9,]+(?:\.[0-9]+)?)\s*([A-Za-z\u0600-\u06FF]{2,10})?""", Pattern.CASE_INSENSITIVE)
        )

        for (p in amountPatterns) {
            val amtMatcher = p.matcher(normalized)
            if (amtMatcher.find()) {
                val amtStr = amtMatcher.group(1)?.replace(",", "") ?: "0"
                val parsedAmt = amtStr.toDoubleOrNull() ?: 0.0
                // تأكد أن الرقم ليس هو الرصيد أو العمولة أو رقم الحوالة
                val refDouble = reference.toDoubleOrNull()
                if (parsedAmt > 0 && parsedAmt != balance && parsedAmt != commission && parsedAmt != refDouble) {
                    amount = parsedAmt
                    val curGroup = amtMatcher.group(2)?.trim()
                    if (!curGroup.isNullOrBlank()) {
                        currency = extractCurrency(curGroup, normalized)
                    }
                    break
                }
            }
        }

        // في حال لم يتم التقاط المبلغ بالكلمات الصريحة، ابحث عن الأرقام المستقلة
        if (amount <= 0.0) {
            val standaloneRegex = Pattern.compile("""\b([0-9]{3,}(?:,[0-9]{3})*(?:\.[0-9]+)?)\b""")
            val stMatcher = standaloneRegex.matcher(normalized)
            val refDouble = reference.toDoubleOrNull()
            val candidates = mutableListOf<Double>()
            while (stMatcher.find()) {
                val candidateStr = stMatcher.group(1)?.replace(",", "") ?: "0"
                val candidate = candidateStr.toDoubleOrNull() ?: 0.0
                if (candidate > 0 && candidate != balance && candidate != commission && candidate != refDouble) {
                    candidates.add(candidate)
                }
            }
            if (candidates.isNotEmpty()) {
                // إذا كان هناك رصيد، المبلغ عادة يكون أصغر من الرصيد التراكمي
                amount = candidates.first()
            }
        }

        // استخراج العملة العامة إن لم تكن محددة
        currency = extractCurrency("", normalized)

        // 5. استخراج اسم الطرف الآخر (المستفيد / المحول / العميل / الطرف المقابل)
        var partyName = ""
        val partyPatterns = listOf(
            Pattern.compile("""(?:للمستفيد|المستفيد|إلى\s*المستفيد|الى\s*المستفيد|المحول\s*له|إلى|الى|من\s*المحول|من\s*العميل|من|المحول|المرسل|المودع|الطرف\s*الآخر|العميل|باسم|بإسم|حساب)\s*[:#=\-]?\s*([\u0600-\u06FFA-Za-z\s]{3,35})(?=[,\n\r.\-;]|رقم|مبلغ|عمولة|رصيد|حوالة|$)""", Pattern.CASE_INSENSITIVE)
        )
        for (p in partyPatterns) {
            val partyMatcher = p.matcher(normalized)
            if (partyMatcher.find()) {
                val candidate = partyMatcher.group(1)?.trim()?.replace("\n", " ") ?: ""
                if (candidate.length >= 3 && !candidate.equals("غير محدد", ignoreCase = true)) {
                    partyName = candidate
                    break
                }
            }
        }

        if (partyName.isBlank()) {
            partyName = if (type == TransactionType.CREDIT) "محول غير محدد" else "مستفيد غير محدد"
        }

        // حساب المبلغ الإجمالي
        val totalAmount = when (type) {
            TransactionType.DEBIT -> if (commission > 0) amount + commission else amount
            TransactionType.CREDIT -> amount
        }

        return ParsedRemittance(
            type = type,
            amount = amount,
            commission = commission,
            totalAmount = totalAmount,
            currency = currency,
            partyName = partyName,
            referenceNumber = reference,
            remainingBalance = balance,
            notes = "معالجة من $senderOrChat"
        )
    }

    private fun extractCurrency(currencyHint: String, fullText: String): String {
        val combined = "$currencyHint $fullText".uppercase()
        return when {
            combined.contains("سعودي") || combined.contains("SAR") || combined.contains("ر.س") -> "SAR"
            combined.contains("دولار") || combined.contains("USD") || combined.contains("$") -> "USD"
            combined.contains("درهم") || combined.contains("AED") -> "AED"
            combined.contains("يمني") || combined.contains("YER") || combined.contains("ر.ي") || combined.contains("ريال") -> "YER"
            else -> "YER"
        }
    }

    /**
     * تحويل النتيجة المفككة إلى Entity قابل للحفظ في قاعدة البيانات
     */
    fun toEntity(
        parsed: ParsedRemittance,
        rawMessage: String,
        senderOrChat: String,
        source: TransactionSource,
        timestamp: Long = System.currentTimeMillis()
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
            timestamp = timestamp
        )
    }
}
