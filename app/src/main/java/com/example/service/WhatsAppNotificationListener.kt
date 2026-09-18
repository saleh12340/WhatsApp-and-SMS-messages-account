package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.RemittanceLedgerApp
import com.example.model.TransactionSource
import com.example.parser.RemittanceParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WhatsAppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim() ?: ""

        if (text.isBlank() || title.isBlank()) return

        Log.d("WhatsAppListener", "Notification from WhatsApp: Title='$title', Text='$text'")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = RemittanceLedgerApp.instance.repository
                val isTarget = repository.isTargetWhatsAppChat(title)

                if (isTarget) {
                    val parsed = RemittanceParser.parse(
                        rawText = text,
                        senderOrChat = title,
                        source = TransactionSource.WHATSAPP
                    )
                    val entity = RemittanceParser.toEntity(
                        parsed = parsed,
                        rawMessage = text,
                        senderOrChat = title,
                        source = TransactionSource.WHATSAPP
                    )
                    val inserted = repository.saveAutomaticTransaction(entity)
                    Log.d("WhatsAppListener", "WhatsApp transaction saved: $inserted")
                } else {
                    Log.d("WhatsAppListener", "WhatsApp title '$title' did not match target filters")
                }
            } catch (e: Exception) {
                Log.e("WhatsAppListener", "Error parsing WhatsApp notification", e)
            }
        }
    }
}
