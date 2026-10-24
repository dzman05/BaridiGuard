package dz.baridiguard.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * BootReceiver
 * 
 * يستمع لإقلاع الهاتف (BOOT_COMPLETED) لضمان أن BaridiGuard يبدأ العمل فوراً
 * قبل أن يتمكن أي تروجان من تنشيط نفسه أو انتظار رسائل الـ SMS.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            val serviceIntent = Intent(context, BaridiGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
