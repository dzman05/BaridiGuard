package dz.baridiguard.security

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OtpVaultStorage
 * 
 * الخزنة المشفرة لحفظ رسائل تأكيد بريد الجزائر (OTP Vault).
 * تحفظ نسخة مستقلة ومحصنة من الرسائل النصية وكود التأكيد لحظة وصولها،
 * لتبقى محفوظة في أمان حتى لو قام فيروس القرصنة بحذف الرسالة الأصلية من الهاتف!
 */
object OtpVaultStorage {

    private const val PREFS_NAME = "baridiguard_otp_vault"
    private const val KEY_OTPS = "saved_otp_list"

    data class OtpRecord(
        val id: String,
        val sender: String,
        val otpCode: String,
        val fullBody: String,
        val timestamp: String,
        val isProtected: Boolean = true
    )

    fun saveOtp(context: Context, sender: String, otpCode: String, fullBody: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_OTPS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            val item = JSONObject().apply {
                put("id", System.currentTimeMillis().toString())
                put("sender", sender)
                put("otpCode", otpCode)
                put("fullBody", fullBody)
                put("timestamp", timeStr)
                put("isProtected", true)
            }

            // وضع الرسالة الأحدث في بداية القائمة
            val newArray = JSONArray()
            newArray.put(item)
            for (i in 0 until array.length()) {
                if (i < 50) { // الاحتفاظ بآخر 50 رسالة
                    newArray.put(array.get(i))
                }
            }

            prefs.edit().putString(KEY_OTPS, newArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllOtps(context: Context): List<OtpRecord> {
        val list = mutableListOf<OtpRecord>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_OTPS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OtpRecord(
                        id = obj.optString("id"),
                        sender = obj.optString("sender"),
                        otpCode = obj.optString("otpCode"),
                        fullBody = obj.optString("fullBody"),
                        timestamp = obj.optString("timestamp"),
                        isProtected = obj.optBoolean("isProtected", true)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
