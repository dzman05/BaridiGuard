package dz.baridiguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BaridiGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_GUARD_SERVICE,
                "حارس الحماية الدائمة (BaridiGuard)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تشغيل مراقبة الهجمات والتطبيقات الخبيثة في الخلفية"
            }

            val threatChannel = NotificationChannel(
                CHANNEL_THREAT_ALERTS,
                "إنذار أمني عاجل (Threat Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية عند اكتشاف شاشة سوداء أو محاولة اختراق"
                enableVibration(true)
            }

            val otpChannel = NotificationChannel(
                CHANNEL_OTP_VAULT,
                "خزنة رموز التأكيد (OTP Vault)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه فوري بوصول رمز تأكيد بنكي من بريد الجزائر"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(threatChannel)
            manager.createNotificationChannel(otpChannel)
        }
    }

    companion object {
        const val CHANNEL_GUARD_SERVICE = "channel_guard_service"
        const val CHANNEL_THREAT_ALERTS = "channel_threat_alerts"
        const val CHANNEL_OTP_VAULT = "channel_otp_vault"
    }
}
