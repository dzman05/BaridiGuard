package dz.baridiguard.security

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * BaridiAccessibilityGuardService
 * 
 * خدمة حراسة إمكانية الوصول فائقة السرعة (Ultra-Fast Event-Driven Guard).
 * تمنح حارس بريدي قدرة رصد فورية (Zero-Latency) عند فتح تطبيق بريدي موب في أي جزء من الثانية،
 * بدون أي استهلاك للبطارية وبدون الحاجة لـ Polling.
 */
class BaridiAccessibilityGuardService : AccessibilityService() {

    private val targetBankingPackages = setOf(
        "dz.poste.baridimob",
        "com.baridimob",
        "dz.poste.eccp",
        "dz.poste.baridinet"
    )

    private var lastTriggerTime: Long = 0
    private val cooldownMs: Long = 8000 // 8 ثوانٍ بين كل إنذار

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return

            if (targetBankingPackages.contains(pkgName)) {
                val now = System.currentTimeMillis()
                if (now - lastTriggerTime > cooldownMs) {
                    lastTriggerTime = now
                    // إطلاق صفارة الإنذار الشاملة فوراً
                    EmergencyAlarmPlayer.playAudibleAlarm(applicationContext)

                    // إرسال الإشعار العاجل
                    BaridiMobLaunchDetector(applicationContext).triggerBaridiMobSecurityAlert(true)
                }
            }
        }
    }

    override fun onInterrupt() {
        // لا شيء
    }
}
