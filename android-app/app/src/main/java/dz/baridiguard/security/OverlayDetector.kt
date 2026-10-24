package dz.baridiguard.security

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * OverlayDetector
 * 
 * كاشف هجمات النوافذ العائمة والشاشات السوداء المزيفة (Overlay Attacks).
 * التروجان يقوم برسم شاشة سوداء كاملة ليمنع المستخدم من رؤية ما يحدث أثناء سرقة بريدي موب.
 */
class OverlayDetector(private val context: Context) {

    data class OverlayApp(
        val packageName: String,
        val appName: String,
        val isSystem: Boolean,
        val isSuspicious: Boolean
    )

    /**
     * فحص جميع التطبيقات التي تمتلك إذن "الظهور فوق التطبيقات الأخرى" (SYSTEM_ALERT_WINDOW)
     */
    fun getAppsWithOverlayPermission(): List<OverlayApp> {
        val pm = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager 
            ?: return emptyList()

        val installedPackages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val result = mutableListOf<OverlayApp>()

        for (pkg in installedPackages) {
            val permissions = pkg.requestedPermissions ?: continue
            val hasOverlayDeclared = permissions.contains(android.Manifest.permission.SYSTEM_ALERT_WINDOW)
            
            if (!hasOverlayDeclared) continue

            val appInfo = pkg.applicationInfo ?: continue
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    appInfo.uid,
                    pkg.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    appInfo.uid,
                    pkg.packageName
                )
            }

            val isAllowed = (mode == AppOpsManager.MODE_ALLOWED)
            if (isAllowed) {
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                
                // تطبيق غير نظامي + ليس من التطبيقات المعروفة كأدوات تواصل أو خرائط
                val isSuspicious = !isSystem && isHighRiskOverlayApp(pkg.packageName)

                result.add(
                    OverlayApp(
                        packageName = pkg.packageName,
                        appName = appLabel,
                        isSystem = isSystem,
                        isSuspicious = isSuspicious
                    )
                )
            }
        }

        return result
    }

    /**
     * التحقق مما إذا كان تطبيق معين يشكل خطراً عند فتح النوافذ العائمة
     */
    private fun isHighRiskOverlayApp(packageName: String): Boolean {
        val benignOverlays = listOf(
            "com.facebook.orca", // Messenger chat heads
            "com.google.android.apps.maps",
            "com.whatsapp",
            "dz.baridiguard"
        )
        return !benignOverlays.contains(packageName)
    }

    /**
     * توجيه المستخدم مباشرة لشاشة الإعدادات لسحب الإذن من التطبيق الخبيث بنقرة واحدة
     */
    fun createManageOverlayIntent(packageName: String): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
