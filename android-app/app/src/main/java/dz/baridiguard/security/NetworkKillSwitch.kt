package dz.baridiguard.security

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * NetworkKillSwitch
 * 
 * قاطع الاتصال التلقائي عند الخطر (Emergency Network Kill-Switch).
 * في أنظمة أندرويد الحديثة (Android 10 إلى 15)، تمنع جوجل التطبيقات من تعطيل الواي فاي برمجياً بدون روت.
 * لذلك نقوم بـ:
 * 1. محاولة التعطيل المباشر (للأجهزة الأقدم).
 * 2. فتح لوحة الاتصال السريع الرسمية (ACTION_INTERNET_CONNECTIVITY) في أجزاء من الثانية ليقطع المستخدم الإنترنت بضغطة زر.
 * 3. فتح إعدادات وضع الطيران (Airplane Mode) كخيار عزل فوري شامل.
 */
class NetworkKillSwitch(private val context: Context) {

    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager = 
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    /**
     * تنفيذ القطع الطارئ للشبكة وعزل الهاتف
     */
    fun triggerEmergencyNetworkIsolation(isArabic: Boolean = true): Boolean {
        var success = false

        // 1. محاولة التعطيل القديم (يعمل على أندرويد 9 فما دون)
        try {
            @Suppress("DEPRECATION")
            if (wifiManager != null && wifiManager.isWifiEnabled) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = false
                success = true
            }
        } catch (e: Exception) {
            // Android 10+ blocks this call
        }

        // 2. لأنظمة أندرويد الحديثة (Android 10 فما فوق): إطلاق نافذة قطع الإنترنت الفورية
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(panelIntent)
                success = true
            } else {
                val airplaneIntent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(airplaneIntent)
                success = true
            }
        } catch (e: Exception) {
            // Fallback to wireless settings
            try {
                val wirelessIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(wirelessIntent)
                success = true
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        val msg = if (isArabic) {
            "⚡ تم تفعيل قاطع الإنترنت! قم بإيقاف الواي فاي والبيانات فوراً لعزل التروجان."
        } else {
            "⚡ Coupe-circuit activé ! Coupez le Wi-Fi et les données pour isoler le malware."
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

        return success
    }

    /**
     * التحقق هل الهاتف متصل بالإنترنت حالياً
     */
    fun isNetworkConnected(): Boolean {
        val cm = connectivityManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
}
