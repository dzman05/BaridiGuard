package dz.baridiguard.security

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationCompat
import dz.baridiguard.BaridiGuardApp
import dz.baridiguard.R
import dz.baridiguard.ui.MainActivity

/**
 * BaridiMobLaunchDetector
 * 
 * رادار الكشف اللحظي عن تشغيل تطبيق بريدي موب (BaridiMob Launch Guardian).
 * عندما يفتح المستخدم (أو التروجان الخبيث خلسة وراء الشاشة السوداء) تطبيق بريدي موب:
 * 1. يطلق فوراً صفارة إنذار أمنية قوية لتنبيه الضحية.
 * 2. يظهر إشعاراً عاجلاً في قمة الشاشة (Heads-up Notification):
 *    "إذا لم تكن أنت من فتح بريدي موب حالياً، فأنت تتعرض لسرقة خفية! اقطع الإنترنت وأطفئ الهاتف فوراً!"
 */
class BaridiMobLaunchDetector(private val context: Context) {

    // قائمة حزم بريد الجزائر المستهدفة بالهجمات
    val targetBankingPackages = setOf(
        "dz.poste.baridimob",
        "com.baridimob",
        "dz.poste.eccp",
        "dz.poste.baridinet"
    )

    companion object {
        private var wasBaridiMobForeground = false
        private var lastAlertTime: Long = 0
        private const val alertCooldownMs: Long = 20000 // 20 ثانية كحد أدنى
    }

    /**
     * فحص هل إذن مراقبة الاستخدام متاح
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * فتح شاشة إعدادات إذن مراقبة الاستخدام
     */
    fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * فحص هل تطبيق بريدي موب يعمل حالياً في الواجهة وإطلاق الإنذار
     */
    fun checkAndTriggerIfBaridiMobActive(isArabic: Boolean = true): Boolean {
        val isBaridiMobForeground = isBaridiMobInForeground()

        if (isBaridiMobForeground) {
            // انطلاق جديد فقط! (New Launch Transition)
            if (!wasBaridiMobForeground) {
                wasBaridiMobForeground = true
                val now = System.currentTimeMillis()
                if (now - lastAlertTime > alertCooldownMs) {
                    lastAlertTime = now
                    triggerBaridiMobSecurityAlert(isArabic)
                    return true
                }
            }
        } else {
            // المستخدم غادر بريدي موب -> إعادة ضبط التبديل
            wasBaridiMobForeground = false
        }
        return false
    }

    /**
     * التحقق هل الحزمة نشطة في الواجهة
     */
    fun isBaridiMobInForeground(): Boolean {
        try {
            // 1. عبر UsageEvents (الأدق والأحدث لرصد التغييرات في أقل من ثانية)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usm != null) {
                    val now = System.currentTimeMillis()
                    // فحص الأحداث خلال آخر 4 ثوانٍ
                    val events = usm.queryEvents(now - 4000, now)
                    val event = UsageEvents.Event()
                    var lastForegroundPackage: String? = null

                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                            lastForegroundPackage = event.packageName
                        }
                    }

                    if (lastForegroundPackage != null && targetBankingPackages.contains(lastForegroundPackage)) {
                        return true
                    }
                }
            }

            // 2. فحص تكميلي عبر ActivityManager
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am != null) {
                val runningProcesses = am.runningAppProcesses ?: return false
                for (processInfo in runningProcesses) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (activePkg in processInfo.pkgList) {
                            if (targetBankingPackages.contains(activePkg)) {
                                return true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * إطلاق الإنذار الصوتي والإشعار التحذيري العاجل
     */
    fun triggerBaridiMobSecurityAlert(isArabic: Boolean = true) {
        // 1. تشغيل صفارة الإنذار الفائقة متعددة القنوات فوراً (ToneGenerator + Ringtone + Siren)
        EmergencyAlarmPlayer.playAudibleAlarm(context)

        // 2. إظهار الإشعار ذو الأولوية القصوى Heads-Up
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isArabic) {
            "🚨 تحذير أمني: تطبيق بريدي موب فُتح الآن!"
        } else {
            "🚨 ALERTE SÉCURITÉ : BaridiMob vient de s'ouvrir !"
        }

        val body = if (isArabic) {
            "⚠️ إذا لم تكن أنت من قام بفتح بريدي موب، فهناك فيروس يتلاعب به خلف الشاشة! اقطع النت فوراً واضغط هنا لعزل الهاتف."
        } else {
            "⚠️ Si vous n'avez pas ouvert BaridiMob vous-même, un malware opère en douce ! Coupez Internet immédiatement."
        }

        val notification = NotificationCompat.Builder(context, BaridiGuardApp.CHANNEL_THREAT_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3001, notification)
    }

    private fun playChimeSiren() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 1500
                val numSamples = (durationMs * sampleRate / 1000)
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    // نغمة إنذار استنفار حادة 1000Hz إلى 1600Hz
                    val freq = 1000.0 + 600.0 * kotlin.math.sin(2.0 * Math.PI * progress * 7.0)
                    val angle = 2.0 * Math.PI * i * freq / sampleRate
                    buffer[i] = (kotlin.math.sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
