package com.example

import com.example.model.TransactionSource
import com.example.model.TransactionType
import com.example.parser.RemittanceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemittanceParserTest {

    @Test
    fun testCreditSmsParsing() {
        val message = "تم إضافة 150000 ريال يمني لحسابكم من المحول سالم باعباد. عمولة: 1000 ريال. رقم الحوالة: 78945612. رصيدكم الحالي: 1450000 ريال."
        val parsed = RemittanceParser.parse(message, "AmalbwadiEX", TransactionSource.SMS)

        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals(150000.0, parsed.amount, 0.01)
        assertEquals(1000.0, parsed.commission, 0.01)
        assertEquals(151000.0, parsed.totalAmount, 0.01)
        assertEquals("78945612", parsed.referenceNumber)
        assertEquals(1450000.0, parsed.remainingBalance ?: 0.0, 0.01)
        assertTrue(parsed.partyName.contains("سالم"))
    }

    @Test
    fun testDebitSmsParsing() {
        val message = "تم خصم 75000 ريال من حسابكم للمستفيد مؤسسة النور. عمولة: 500 ريال. رقم العملية: 45612378. رصيدكم: 1374500 ريال."
        val parsed = RemittanceParser.parse(message, "AmalbwadiEX", TransactionSource.SMS)

        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(75000.0, parsed.amount, 0.01)
        assertEquals(500.0, parsed.commission, 0.01)
        assertEquals(75500.0, parsed.totalAmount, 0.01)
        assertEquals("45612378", parsed.referenceNumber)
        assertTrue(parsed.partyName.contains("مؤسسة النور"))
    }

    @Test
    fun testWhatsAppAccountingClassification() {
        val creditMessage = "حسبنا لكم مبلغ 200000 ريال عن طريق صرافة الأمل، رقم السند: 9812456 للمستفيد خالد باوزير."
        val debitMessage = "حسبنا عليكم تحويل إلى أحمد محمد بمبلغ 85000 ريال، عمولة: 400. رقم الحوالة: 332211."

        val parsedCredit = RemittanceParser.parse(creditMessage, "صرافة الأمل", TransactionSource.WHATSAPP)
        val parsedDebit = RemittanceParser.parse(debitMessage, "صرافة الأمل", TransactionSource.WHATSAPP)

        assertEquals(TransactionType.CREDIT, parsedCredit.type)
        assertEquals(TransactionType.DEBIT, parsedDebit.type)
    }

    @Test
    fun testArabicDigitsNormalization() {
        val rawWithEasternDigits = "تم إضافة ١٥٠٠٠٠ ريال"
        val normalized = RemittanceParser.normalizeArabicNumbers(rawWithEasternDigits)
        assertTrue(normalized.contains("150000"))
    }
}
