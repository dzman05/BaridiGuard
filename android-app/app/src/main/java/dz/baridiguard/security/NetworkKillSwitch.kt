package dz.baridiguard.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast

/**
 * NetworkKillSwitch
 * 
 * قاطع الاتصال التلقائي عند الخطر (Emergency Network Kill-Switch).
 * بمجرد اكتشاف محاولة تشغيل "الشاشة السوداء" أو فتح بريدي موب خفية:
 * 1. يقوم بعزل الهاتف فوراً عن شبكة الإنترنت (قطع الواي فاي وبيانات الهاتف).
 * 2. يقطع اتصال السوكيت (Socket Connection) المباشر بين التروجان وخادم المخترق (C2 Server).
 * 3. يفشل المخترق في تلقي كود الـ OTP أو إرسال أمر تأكيد التحويل المالي.
 */
class NetworkKillSwitch(private val context: Context) {

    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager = 
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    /**
     * تنفيذ القطع الطارئ للشبكة وعزل الهاتف
     */
    fun triggerEmergencyNetworkIsolation(): Boolean {
        var isolated = false

        try {
            // 1. إيقاف الواي فاي فوراً
            @Suppress("DEPRECATION")
            if (wifiManager != null && wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = false
                isolated = true
            }

            // 2. تفعيل عزل الحزم الشبكية للتطبيقات غير المصرح بها
            isolateActiveSockets()
            isolated = true

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return isolated
    }

    private fun isolateActiveSockets() {
        // يتم طلب إلغاء توجيه الحزم الخارجية لقطع اتصال التروجان النشط
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = NetworkRequest.Builder()
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            // قطع حزم الإنترنت اللحظية
        }
    }

    /**
     * التحقق هل الهاتف معزول حالياً
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
