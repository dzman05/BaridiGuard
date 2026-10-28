package dz.baridiguard.security

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * BaridiAccessibilityGuardService
 * 
 * خدمة حراسة إمكانية الوصول الشاملة (Event-Driven Security Engine).
 * 1. رصد فوري للفتح الأمامي لتطبيق بريدي موب الرسمي (ru.bpc.mobilebank.bpc).
 * 2. كشف إشعارات الخصم المزيفة الصادرة من تطبيقات خبيثة تنتحل بريد الجزائر.
 * 3. التقاط وتوثيق أرقام الـ RIP (20 رقماً) والمبالغ المحولة داخل الصندوق الأسود.
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

        val pkgName = event.packageName?.toString() ?: ""

        when (event.eventType) {
            // 1. مراقبة حركة النوافذ والتطبيقات النشطة في الواجهة
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val detector = BaridiMobLaunchDetector(applicationContext)

                if (detector.isBankingPackage(pkgName)) {
                    if (!isInsideBankingApp) {
                        isInsideBankingApp = true
                        val now = System.currentTimeMillis()
                        if (now - lastTriggerTime > cooldownMs) {
                            lastTriggerTime = now
                            detector.triggerBaridiMobSecurityAlert(true)
                        }
                    }
                    // محاولة فحص شاشة بريدي موب لرصد أي رقم RIP مدخل
                    inspectScreenForRip(rootInActiveWindow)
                } else {
                    isInsideBankingApp = false
                }
            }

            // 2. فحص محتوى الشاشة أثناء تفاعل التروجان أو المستخدم
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (isInsideBankingApp) {
                    inspectScreenForRip(rootInActiveWindow)
                }
            }

            // 3. رادار كشف إشعارات الخصم المزيفة المنبثقة من تطبيقات أخرى
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val notifText = event.text.joinToString(" ")
                if (FakeNotificationDetector.isSuspiciousSpoofedNotification(pkgName, notifText)) {
                    FakeNotificationDetector.handleFakeNotification(
                        context = applicationContext,
                        pkgName = pkgName,
                        title = "إشعار منتحل لبريدي موب",
                        text = notifText
                    )
                }
            }
        }
    }

    /**
     * فحص تسلسل الشجرة البرمجية لواجهة بريدي موب بحثاً عن أرقام RIP (20 رقماً)
     */
    private fun inspectScreenForRip(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        try {
            traverseAndExtractRip(rootNode)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun traverseAndExtractRip(node: AccessibilityNodeInfo) {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val combined = "$text $desc"

        val rip = RipBlackboxStorage.extractAlgerianRip(combined)
        if (rip != null) {
            val amount = RipBlackboxStorage.extractAmount(combined) ?: "قيد المراقبة"
            RipBlackboxStorage.recordSuspiciousRip(
                context = applicationContext,
                ripNumber = rip,
                amountDzd = amount,
                source = "شاشة بريدي موب الحقيقية (Capture Écran)"
            )
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { traverseAndExtractRip(it) }
        }
    }

    override fun onInterrupt() {
        // لا شيء
    }
}
