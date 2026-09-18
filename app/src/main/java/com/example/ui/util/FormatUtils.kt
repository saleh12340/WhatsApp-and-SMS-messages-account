package com.example.ui.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    private val numberFormat = DecimalFormat("#,##0.##")

    fun formatMoney(amount: Double, currency: String = "YER"): String {
        val formatted = numberFormat.format(amount)
        val curSymbol = when (currency.uppercase(Locale.ROOT)) {
            "YER", "ر.ي" -> "ر.ي"
            "SAR", "ر.س" -> "ر.س"
            "USD", "$" -> "$"
            else -> currency
        }
        return "$formatted $curSymbol"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.forLanguageTag("ar"))
        return sdf.format(Date(timestamp))
    }
}
