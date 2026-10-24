package dz.baridiguard.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * BaridiDeviceAdminReceiver
 * 
 * صمام الحماية القصوى ضد التعطيل (Anti-Uninstall & Anti-Kill Shield).
 * عندما يتم تفعيل هذا الخيار في BaridiGuard كمسؤول جهاز (Device Administrator):
 * 1. يقوم نظام أندرويد بتعطيل زر "إلغاء التثبيت" (Uninstall) تماماً.
 * 2. يمنع أي فيروس أو تروجان من الضغط على "فرض الإيقاف" (Force Stop).
 * 3. لا يمكن حذف التطبيق إلا بعد الدخول لكلمة سر/بصمة صاحب الهاتف وإلغاء صلاحية المسؤول عمداً.
 */
class BaridiDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "🛡️ BaridiGuard: تم تفعيل درع الحماية الذاتية (ممنوع الحذف أو التعطيل بواسطة التروجانات)",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "⚠️ تحذير أمني شديد الخطورة:\n\n" +
               "محاولة تعطيل BaridiGuard ستجعل تطبيق بريدي موب عرضة لهجمات الشاشة السوداء وسرقة الرصيد!\n" +
               "هل أنت متأكد أنك لست تحت تأثير تطبيق تجسس؟"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            "⚠️ تم إيقاف درع الحماية الذاتية. يرجى إعادة تفعيله لحماية حسابك البريدي.",
            Toast.LENGTH_LONG
        ).show()
    }
}
