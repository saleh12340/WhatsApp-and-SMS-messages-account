package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.RemittanceLedgerApp
import com.example.model.TransactionSource
import com.example.parser.RemittanceParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBodyBuilder = StringBuilder()
        var senderAddress = ""

        for (sms in messages) {
            if (sms != null) {
                senderAddress = sms.originatingAddress ?: senderAddress
                fullBodyBuilder.append(sms.messageBody ?: "")
            }
        }

        val fullBody = fullBodyBuilder.toString()
        if (fullBody.isBlank() || senderAddress.isBlank()) return

        Log.d("SmsReceiver", "SMS received from: $senderAddress, text: $fullBody")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = RemittanceLedgerApp.instance.repository
                val isTarget = repository.isTargetSmsSender(senderAddress)

                if (isTarget) {
                    val parsed = RemittanceParser.parse(
                        rawText = fullBody,
                        senderOrChat = senderAddress,
                        source = TransactionSource.SMS
                    )
                    val entity = RemittanceParser.toEntity(
                        parsed = parsed,
                        rawMessage = fullBody,
                        senderOrChat = senderAddress,
                        source = TransactionSource.SMS
                    )
                    val inserted = repository.saveAutomaticTransaction(entity)
                    Log.d("SmsReceiver", "Transaction parsed & saved: $inserted")
                } else {
                    Log.d("SmsReceiver", "Sender $senderAddress is not in target list")
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
