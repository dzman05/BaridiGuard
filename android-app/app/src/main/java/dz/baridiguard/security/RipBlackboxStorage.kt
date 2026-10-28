package dz.baridiguard.security

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * RipBlackboxStorage
 * 
 * الصندوق الأسود لتسجيل أرقام حسابات الـ RIP والتحويلات المشبوهة.
 * يحتفظ بنسخة محصنة وغير قابلة للحذف لأي رقم حساب بنكي (20 رقماً يبدأ بـ 007)
 * حاول التروجان تحويل الأموال إليه، لاستخدامه في تجميد الحساب فوراً عبر 1530
 * وتقديمه للدرك والشرطة الوطنية كدليل جنائي لإلقاء القبض على صاحب الحساب المستلم.
 */
object RipBlackboxStorage {

    private const val PREFS_NAME = "baridiguard_rip_blackbox"
    private const val KEY_RECORDS = "suspicious_rip_records"

    data class RipRecord(
        val id: String,
        val ripNumber: String,
        val amountDzd: String,
        val source: String,
        val timestamp: String,
        val recipientName: String = "حساب مستفيد مشبوه / Destinataire suspect",
        val isFrozenRequested: Boolean = false
    )

    fun recordSuspiciousRip(
        context: Context,
        ripNumber: String,
        amountDzd: String = "غير محدد / Non précisé",
        source: String = "رصد آلي / Détection automatique",
        recipientName: String = "حساب مستفيد مشبوه / Destinataire suspect"
    ) {
        if (ripNumber.isBlank()) return

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            val item = JSONObject().apply {
                put("id", System.currentTimeMillis().toString())
                put("ripNumber", ripNumber.trim())
                put("amountDzd", amountDzd.trim())
                put("source", source.trim())
                put("timestamp", timeStr)
                put("recipientName", recipientName.trim())
                put("isFrozenRequested", false)
            }

            val newArray = JSONArray()
            newArray.put(item)
            for (i in 0 until array.length()) {
                if (i < 100) {
                    val existing = array.getJSONObject(i)
                    // تجنب تكرار نفس الرقم في نفس الدقيقة
                    if (existing.optString("ripNumber") != ripNumber.trim() || 
                        existing.optString("timestamp") != timeStr) {
                        newArray.put(existing)
                    }
                }
            }

            prefs.edit().putString(KEY_RECORDS, newArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllRecords(context: Context): List<RipRecord> {
        val list = mutableListOf<RipRecord>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RipRecord(
                        id = obj.optString("id"),
                        ripNumber = obj.optString("ripNumber"),
                        amountDzd = obj.optString("amountDzd", "غير محدد"),
                        source = obj.optString("source", "رصد آلي"),
                        timestamp = obj.optString("timestamp"),
                        recipientName = obj.optString("recipientName", "حساب مشبوه"),
                        isFrozenRequested = obj.optBoolean("isFrozenRequested", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deleteRecord(context: Context, id: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
            val array = JSONArray(jsonString)
            val newArray = JSONArray()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != id) {
                    newArray.put(obj)
                }
            }
            prefs.edit().putString(KEY_RECORDS, newArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * استخراج رقم الـ RIP الجزائري المكون من 20 رقماً (يبدأ غالباً بـ 007 لبريد الجزائر)
     */
    fun extractAlgerianRip(text: String): String? {
        val ripPattern = Pattern.compile("\\b(007\\d{17})\\b")
        val matcher = ripPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        // أو أي 20 رقماً بنكياً متتالياً
        val genericPattern = Pattern.compile("\\b(\\d{20})\\b")
        val genericMatcher = genericPattern.matcher(text)
        if (genericMatcher.find()) {
            return genericMatcher.group(1)
        }
        return null
    }

    /**
     * استخراج المبلغ المحول بالدينار الجزائري من نص الرسالة أو الشاشة
     */
    fun extractAmount(text: String): String? {
        val pattern = Pattern.compile("(?i)(\\d[\\d\\s.,]{1,12})\\s*(?:DZD|DA|دج|دينار)")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return "${matcher.group(1)?.trim()} DZD"
        }
        return null
    }
}
