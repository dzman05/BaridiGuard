package dz.baridiguard.security

import android.net.Uri
import java.util.Locale

/**
 * PhishingScanner
 * 
 * درع روابط الاحتيال الموجهة ضد الجزائريين (Algerian Banking Anti-Phishing Shield).
 * يفحص الروابط المنتشرة في SMS وفيسبوك وتيليغرام ويكتشف الصفحات المزيفة التي تنتحل:
 * - بريدي موب (BaridiMob)
 * - البطاقة الذهبية وتعبئة الرصيد (Edahabia / Poste)
 * - منحة البطالة والمساعدات الوهمية (Minha / ANEM)
 */
class PhishingScanner {

    data class ScanResult(
        val url: String,
        val isPhishing: Boolean,
        val riskScore: Int, // 0 to 100
        val targetSpoofed: String?,
        val reasonAr: String,
        val reasonFr: String
    )

    // الكلمات المفتاحية للبنوك والمؤسسات الجزائرية المستهدفة بالاحتيال
    private val algerianFinancialKeywords = listOf(
        "baridimob", "baridi", "edahabia", "poste-dz", "algerieposte", 
        "algerie-poste", "eccp", "monetique", "minha", "chomage", "carte-or"
    )

    // النطاقات الرسمية الموثوقة 100% لبريد الجزائر والمؤسسات الوطنية
    private val officialTrustedDomains = listOf(
        "poste.dz",
        "baridinet.poste.dz",
        "eccp.poste.dz",
        "anem.dz",
        "algerieposte.dz"
    )

    // امتدادات مشبوهة ورخيصة يستعملها المحتالون عادة
    private val suspiciousTlds = listOf(
        ".top", ".xyz", ".cc", ".tk", ".ml", ".ga", ".cf", ".work", ".click", ".live", ".support"
    )

    /**
     * فحص رابط معين والتأكد من سلامته
     */
    fun analyzeUrl(rawUrl: String): ScanResult {
        val cleanUrl = rawUrl.trim()
        val lowerUrl = cleanUrl.lowercase(Locale.ROOT)

        var uri: Uri? = null
        try {
            uri = Uri.parse(if (!cleanUrl.startsWith("http")) "https://$cleanUrl" else cleanUrl)
        } catch (e: Exception) {
            // invalid URL
        }

        val host = uri?.host?.lowercase(Locale.ROOT) ?: lowerUrl

        // 1. هل هو نطاق رسمي موثوق؟
        val isOfficial = officialTrustedDomains.any { host == it || host.endsWith(".$it") }
        if (isOfficial) {
            return ScanResult(
                url = cleanUrl,
                isPhishing = false,
                riskScore = 0,
                targetSpoofed = null,
                reasonAr = "رابط رسمي وموثوق تابع لبريد الجزائر / المؤسسات الحكومية.",
                reasonFr = "Lien officiel et sécurisé appartenant à Algérie Poste / institutions étatiques."
            )
        }

        // 2. هل ينتحل اسماً بنكياً أو بريدياً جزائرياً؟
        val spoofedKeyword = algerianFinancialKeywords.find { host.contains(it) || lowerUrl.contains(it) }

        if (spoofedKeyword != null) {
            // انتحال اسم بريد الجزائر خارج النطاقات الرسمية = احتيال مؤكد 100%!
            return ScanResult(
                url = cleanUrl,
                isPhishing = true,
                riskScore = 98,
                targetSpoofed = getBrandName(spoofedKeyword),
                reasonAr = "🚨 احتيال خطير: هذا الموقع ينتحل صفحة ${getBrandName(spoofedKeyword)} لسرقة بيانات بطاقتك الذهبية ورصيدك!",
                reasonFr = "🚨 Hameçonnage critique: Ce site usurpe l\'identité de ${getBrandName(spoofedKeyword)} pour voler vos identifiants bancaires!"
            )
        }

        // 3. فحص الـ TLDs والـ IP Addresses المباشرة
        val isDirectIp = host.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))
        val hasSuspiciousTld = suspiciousTlds.any { host.endsWith(it) }

        if (isDirectIp || hasSuspiciousTld) {
            return ScanResult(
                url = cleanUrl,
                isPhishing = true,
                riskScore = 75,
                targetSpoofed = "موقع مجهول / Inconnu",
                reasonAr = "⚠️ نطاق عالي الخطورة ومشبوه لا يتبع لأي جهة معتمدة في الجزائر.",
                reasonFr = "⚠️ Domaine à haut risque non affilié aux autorités certifiées."
            )
        }

        return ScanResult(
            url = cleanUrl,
            isPhishing = false,
            riskScore = 10,
            targetSpoofed = null,
            reasonAr = "الرابط لا يبدو أنه ينتحل بريدي موب أو بطاقة الذهبية.",
            reasonFr = "Le lien ne semble pas cibler BaridiMob ou la carte Edahabia."
        )
    }

    private fun getBrandName(keyword: String): String {
        return when (keyword) {
            "baridimob", "baridi" -> "بريدي موب (BaridiMob)"
            "edahabia", "carte-or" -> "البطاقة الذهبية (Edahabia)"
            "minha", "chomage" -> "منحة البطالة (Minha)"
            else -> "بريد الجزائر (Algérie Poste)"
        }
    }
}
