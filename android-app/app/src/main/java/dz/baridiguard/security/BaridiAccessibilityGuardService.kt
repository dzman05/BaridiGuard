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
        "ru.bpc.mobilebank.bpc",
        "dz.poste.baridimob",
        "com.baridimob",
        "dz.poste.eccp",
        "dz.poste.baridinet"
    )

    private var isInsideBankingApp = false
    private var lastTriggerTime: Long = 0
    private val cooldownMs: Long = 25000 // 25 ثانية لمنع التكرار

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return
            val detector = BaridiMobLaunchDetector(applicationContext)

            if (detector.isBankingPackage(pkgName)) {
                // انطلاق جديد فقط لأول مرة يدخل فيها التطبيق
                if (!isInsideBankingApp) {
                    isInsideBankingApp = true
                    val now = System.currentTimeMillis()
                    if (now - lastTriggerTime > cooldownMs) {
                        lastTriggerTime = now
                        // إرسال الإشعار وتشغيل الإنذار المنضبط (لمدة 3.5 ثانية فقط)
                        detector.triggerBaridiMobSecurityAlert(true)
                    }
                }
            } else {
                // المستخدم خرج من التطبيق البنكي إلى تطبيق آخر
                isInsideBankingApp = false
            }
        }
    }

    override fun onInterrupt() {
        // لا شيء
    }
}
