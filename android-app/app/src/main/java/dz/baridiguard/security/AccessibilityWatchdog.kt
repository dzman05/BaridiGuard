package dz.baridiguard.security

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager

/**
 * AccessibilityWatchdog
 * 
 * فاحص ومراقب خدمات إمكانية الوصول (Accessibility Services)
 * هذا المكون يفحص هل يوجد أي تطبيق غير رسمي أو مشبوه (مثل تطبيقات الأفلام أو الألعاب المهكرة)
 * تم منحه صلاحية التحكم في الشاشة واللمس، وهي الوسيلة رقم 1 لتروجانات بريدي موب.
 */
class AccessibilityWatchdog(private val context: Context) {

    data class AccessibilityRisk(
        val packageName: String,
        val appName: String,
        val isSystemApp: Boolean,
        val riskLevel: RiskLevel,
        val description: String
    )

    enum class RiskLevel {
        CRITICAL, // غير رسمي ومحمل من خارج المتجر (APK)
        WARNING,  // تطبيق عادي يطلب صلاحية وصول دون مبرر واضح
        SAFE      // خدمة نظام رسمية (Google TalkBack, Samsung Suite...)
    }

    /**
     * فحص جميع التطبيقات المفعلة حالياً في خدمات إمكانية الوصول
     */
    fun auditEnabledAccessibilityServices(): List<AccessibilityRisk> {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return emptyList()

        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val pm = context.packageManager
        val riskList = mutableListOf<AccessibilityRisk>()

        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo?.serviceInfo ?: continue
            val pkgName = serviceInfo.packageName
            val isSystem = isSystemPackage(pm, pkgName)
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkgName
            }

            val riskLevel = when {
                isSystem -> RiskLevel.SAFE
                isKnownSecurityOrUtility(pkgName) -> RiskLevel.WARNING
                else -> RiskLevel.CRITICAL // تطبيق غير معروف يستغل الـ Accessibility = خطر داهم!
            }

            riskList.add(
                AccessibilityRisk(
                    packageName = pkgName,
                    appName = appLabel,
                    isSystemApp = isSystem,
                    riskLevel = riskLevel,
                    description = if (riskLevel == RiskLevel.CRITICAL) {
                        "⚠️ تحذير: هذا التطبيق يمتلك القدرة على قراءة محتوى الشاشة وإجراء نقرات تلقائية داخل بريدي موب!"
                    } else {
                        "خدمة إمكانية وصول نشطة."
                    }
                )
            )
        }

        return riskList
    }

    private fun isSystemPackage(pm: PackageManager, packageName: String): Boolean {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
            packageName.startsWith("com.google.android.") ||
            packageName.startsWith("com.android.") ||
            packageName.startsWith("com.samsung.") ||
            packageName.startsWith("com.miui.")
        } catch (e: Exception) {
            false
        }
    }

    private fun isKnownSecurityOrUtility(packageName: String): Boolean {
        val safePrefixes = listOf(
            "com.bitdefender",
            "com.malwarebytes",
            "org.telegram.messenger",
            "com.lastpass",
            "com.bitwarden"
        )
        return safePrefixes.any { packageName.startsWith(it) }
    }
}
