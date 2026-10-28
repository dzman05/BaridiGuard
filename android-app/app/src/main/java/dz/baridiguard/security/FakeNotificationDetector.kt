package dz.baridiguard.security

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dz.baridiguard.BaridiGuardApp
import dz.baridiguard.R
import dz.baridiguard.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FakeNotificationDetector
 * 
 * رادار كشف إشعارات الخصم المزيفة (Fake BaridiMob Deduction Notification Radar).
 * يرصد ويفضح أي تطبيق خبيث (مثل تطبيقات المباريات أو الأفلام المقرصنة)
 * يحاول إظهار إشعار كاذب باسم بريدي موب لإثارة الرعب لدى الضحية واستدراجها لفتح الحساب.
 */
object FakeNotificationDetector {

    private const val PREFS_NAME = "baridiguard_fake_notifications"
    private const val KEY_ALERTS = "intercepted_fake_notifications"

    data class FakeAlertRecord(
        val id: String,
        val maliciousPackage: String,
        val notificationTitle: String,
        val notificationText: String,
        val timestamp: String,
        val fakeAmount: String
    )

    fun isSuspiciousSpoofedNotification(pkgName: String, text: String): Boolean {
        val legitimatePackages = setOf(
            "ru.bpc.mobilebank.bpc",
            "dz.poste.baridimob",
            "com.baridimob",
            "dz.poste.eccp",
            "dz.baridiguard",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging"
        )

        // إذا كان الإشعار صادر من التطبيق الرسمي، فهو ليس مزيفاً
        if (legitimatePackages.contains(pkgName.lowercase())) return false

        val lower = text.lowercase()
        val containsBrand = lower.contains("baridimob") || lower.contains("baridi") || 
                           lower.contains("poste") || lower.contains("بريدي") || 
                           lower.contains("بريد الجزائر") || lower.contains("الذهبية")

        val containsFinancialAlert = lower.contains("virement") || lower.contains("خصم") || 
                                     lower.contains("اقتطاع") || lower.contains("débit") || 
                                     lower.contains("transfert") || lower.contains("retrait") || 
                                     lower.contains("سحب") || lower.contains("dzd") || lower.contains("da")

        return containsBrand && containsFinancialAlert
    }

    fun handleFakeNotification(context: Context, pkgName: String, title: String, text: String) {
        val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val extractedAmount = RipBlackboxStorage.extractAmount(text) ?: "مبلغ مجهول"

        // 1. تسجيل المحاولة الخبيثة في قاعدة البيانات المحلية
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_ALERTS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            val item = JSONObject().apply {
                put("id", System.currentTimeMillis().toString())
                put("maliciousPackage", pkgName)
                put("notificationTitle", title)
                put("notificationText", text)
                put("timestamp", timeStr)
                put("fakeAmount", extractedAmount)
            }

            val newArray = JSONArray()
            newArray.put(item)
            for (i in 0 until array.length()) {
                if (i < 50) newArray.put(array.get(i))
            }
            prefs.edit().putString(KEY_ALERTS, newArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. تشغيل صفارة الإنذار الأمني الفورية
        EmergencyAlarmPlayer.playAudibleAlarm(context, 3500)

        // 3. إظهار إشعار أمني تحذيري فوري
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "FAKE_NOTIF")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BaridiGuardApp.CHANNEL_THREAT_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("تحذير أمني: رصد إشعار خصم مزيف ينتحل بريدي موب")
            .setContentText("التطبيق المشبوه ($pkgName) أرسل إشعاراً كاذباً لاستدراجك لفتح حسابك")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "محاولة تصيد احتيالي نشطة:\n\n" +
                    "التطبيق المشبوه: $pkgName\n" +
                    "نص الإشعار المزيف: $text\n\n" +
                    "تنبيه: لم يتم خصم أي أموال من حسابك. هذا فخ لإثارة الذعر ودفعك لفتح بريدي موب بينما البرمجية الخبيثة تنشط في الخلفية. لا تفتح بريدي موب الآن."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3002, notification)
    }

    fun getAllAlerts(context: Context): List<FakeAlertRecord> {
        val list = mutableListOf<FakeAlertRecord>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_ALERTS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FakeAlertRecord(
                        id = obj.optString("id"),
                        maliciousPackage = obj.optString("maliciousPackage"),
                        notificationTitle = obj.optString("notificationTitle"),
                        notificationText = obj.optString("notificationText"),
                        timestamp = obj.optString("timestamp"),
                        fakeAmount = obj.optString("fakeAmount", "مبلغ مجهول")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
