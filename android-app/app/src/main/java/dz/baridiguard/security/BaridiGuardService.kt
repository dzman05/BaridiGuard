package dz.baridiguard.security

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dz.baridiguard.BaridiGuardApp
import dz.baridiguard.R
import dz.baridiguard.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * BaridiGuardService
 * 
 * خدمة حماية تعمل في الخلفية لمراقبة الأمان اللحظي،
 * رصد تشغيل تطبيق بريدي موب لإطلاق إنذار فوري، كشف الشاشات السوداء، وفحص الـ Accessibility الدوري.
 */
class BaridiGuardService : Service() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var accessibilityWatchdog: AccessibilityWatchdog
    private lateinit var overlayDetector: OverlayDetector
    private lateinit var baridiMobDetector: BaridiMobLaunchDetector

    override fun onCreate() {
        super.onCreate()
        accessibilityWatchdog = AccessibilityWatchdog(this)
        overlayDetector = OverlayDetector(this)
        baridiMobDetector = BaridiMobLaunchDetector(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        startForeground(1001, notification)

        startSecurityLoop()
        return START_STICKY
    }

    private fun startSecurityLoop() {
        scope.launch {
            while (isActive) {
                // 1. مراقبة فتح تطبيق بريدي موب في الواجهة (لإطلاق الإنذار فوراً)
                baridiMobDetector.checkAndTriggerIfBaridiMobActive()

                // 2. فحص خدمات إمكانية الوصول المشبوهة
                val dangerousServices = accessibilityWatchdog.auditEnabledAccessibilityServices()
                    .filter { it.riskLevel == AccessibilityWatchdog.RiskLevel.CRITICAL }

                if (dangerousServices.isNotEmpty()) {
                    broadcastThreatDetected(dangerousServices.first().appName)
                }

                delay(2000) // فحص سريع كل ثانيتين
            }
        }
    }

    private fun broadcastThreatDetected(threatAppName: String) {
        val intent = Intent("dz.baridiguard.THREAT_ALERT").apply {
            putExtra("THREAT_APP", threatAppName)
        }
        sendBroadcast(intent)
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BaridiGuardApp.CHANNEL_GUARD_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("BaridiGuard: درع الحماية نشط")
            .setContentText("رادار مراقبة بريدي موب ومكافحة التروجانات يعمل بنجاح")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
