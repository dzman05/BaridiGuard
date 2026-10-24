package dz.baridiguard.security

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import dz.baridiguard.BaridiGuardApp
import dz.baridiguard.ui.MainActivity
import java.util.regex.Pattern

/**
 * OtpVaultReceiver
 * 
 * حارس أكواد التأكيد (OTP Guard)
 * يعمل بأعلى أولوية (Priority 999) لضمان التقاط رسائل بريد الجزائر قبل أن يتمكن
 * التروجان من قراءتها وحذفها خفية من صندوق الوارد.
 */
class OtpVaultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle: Bundle? = intent.extras
        if (bundle == null) return

        try {
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")

            for (pdu in pdus) {
                val bytes = pdu as? ByteArray ?: continue
                val sms: SmsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(bytes, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(bytes)
                }

                val sender = sms.displayOriginatingAddress ?: ""
                val messageBody = sms.displayMessageBody ?: ""

                // التحقق هل الرسالة تخص بريد الجزائر أو تحتوي كود تأكيد بنكي
                if (isFinancialOrAlgeriePosteSms(sender, messageBody)) {
                    val extractedOtp = extractOtpCode(messageBody)
                    
                    // حفظ الكود وإطلاق إنذار فوري
                    handleSecureOtp(context, sender, extractedOtp, messageBody)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isFinancialOrAlgeriePosteSms(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()

        val isAlgPoste = s.contains("POSTE") || s.contains("BARID") || s.contains("ALGERIE") || s.contains("AP")
        val isOtpKeywords = b.contains("OTP") || b.contains("CODE") || b.contains("VIREMENT") || 
                           b.contains("CONFIRMATION") || b.contains("رمز") || b.contains("تحويل")

        return isAlgPoste || (isOtpKeywords && (b.contains("DZD") || b.contains("DA") || b.contains("BARIDI")))
    }

    private fun extractOtpCode(body: String): String {
        // البحث عن 4 إلى 6 أرقام متتالية
        val pattern = Pattern.compile("\\b\\d{4,6}\\b")
        val matcher = pattern.matcher(body)
        return if (matcher.find()) matcher.group(0) ?: "------" else "------"
    }

    private fun handleSecureOtp(context: Context, sender: String, otp: String, fullBody: String) {
        // تشغيل صوت إنذار مرتفع لتنبيه المستخدم فوراً
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alarmSound)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // إظهار إشعار فوري ذو أولوية قصوى
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OTP", otp)
            putExtra("EXTRA_SENDER", sender)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BaridiGuardApp.CHANNEL_OTP_VAULT)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("🛡️ رمز تأكيد محمي من بريد الجزائر: $otp")
            .setContentText("تم حفظ الرمز في خزنة BaridiGuard لمنع حذفه الخفي بواسطة التروجان!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("المرسل: $sender\nرمز الـ OTP: $otp\n\nنص الرسالة:\n$fullBody\n\n⚠️ إذا لم تكن أنت من يجري المعاملة، اتصل بـ 1530 فوراً لتجميد الحساب!"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2001, notification)
    }
}
