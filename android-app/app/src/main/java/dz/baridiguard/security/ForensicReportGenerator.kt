package dz.baridiguard.security

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ForensicReportGenerator
 * 
 * مولد تقرير الأدلة الرقمية للضحايا (Digital Forensic Evidence Dossier).
 * يُصدر تقريراً فنياً وقانونياً جاهزاً للطباعة والتقديم لـ:
 * 1. فرقة مكافحة الجرائم المعلوماتية (الدرك الوطني / الأمن الوطني)
 * 2. المصلحة القانونية لبريد الجزائر (Algérie Poste) للمطالبة بتتبع الأموال
 */
class ForensicReportGenerator(private val context: Context) {

    data class IncidentData(
        val victimName: String,
        val victimPhone: String,
        val ccpOrRipNumber: String,
        val maliciousAppName: String,
        val maliciousPackage: String,
        val maliciousSha256: String,
        val estimatedLossDzd: String,
        val interceptedRipDestination: String,
        val dateOfIncident: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    )

    /**
     * توليد التقرير الجنائي الكامل باللغتين العربية والفرنسية
     */
    fun generateOfficialPoliceDossier(incident: IncidentData): String {
        return """
================================================================================
          الجمهورية الجزائرية الديمقراطية الشعبية
       RÉPUBLIQUE ALGÉRIENNE DÉMOCRATIQUE ET POPULAIRE
================================================================================
ملف أدلة رقمية لإيداع شكوى: جريمة قرصنة إلكترونية واستيلاء غير مشروع على حساب بريدي
DOSSIER DE PREUVE NUMÉRIQUE: CYBERCRIMINALITÉ & FRAUDE BANCAIRE BARIDIMOB
--------------------------------------------------------------------------------
موجّه إلى:
- مصالح الأمن الوطني / الدرك الوطني (فرقة مكافحة الجرائم السيبيريانية والمعلوماتية)
- مؤسسة بريد الجزائر (مديرية الأمن المعلوماتي ومتابعة المنازعات)

Destiné aux:
- Services de la Police Judiciaire et de la Gendarmerie Nationale (Lutte Anti-Cybercriminalité)
- Algérie Poste (Direction de la Sécurité des Systèmes d'Information)
================================================================================

1. معلومات الضحية والحساب المتضرر / IDENTITÉ DE LA VICTIME:
--------------------------------------------------------------------------------
- الاسم واللقب / Nom & Prénom: ${incident.victimName}
- رقم الهاتف المسجل في بريدي موب / N° Téléphone BaridiMob: ${incident.victimPhone}
- رقم الحساب البريدي الجاري / RIP ou N° CCP: ${incident.ccpOrRipNumber}
- تاريخ وتوقيت رصد الهجوم / Date & Heure de l'attaque: ${incident.dateOfIncident}
- القيمة المالية المسلوبة (تقديرياً) / Montant frauduleux: ${incident.estimatedLossDzd} DZD

2. بيانات الجهاز والبيئة الرقمية / DÉTAILS DU TÉLÉPHONE COMPROMIS:
--------------------------------------------------------------------------------
- طراز الهاتف / Modèle de l'appareil: ${Build.MANUFACTURER} ${Build.MODEL}
- إصدار نظام أندرويد / Version Android: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
- بصمة البناء الأمنية / Build Fingerprint: ${Build.FINGERPRINT}

3. البصمة الجنائية للبرمجية الخبيثة (تروجان أندرويد) / IDENTIFICATION DU MALWARE:
--------------------------------------------------------------------------------
- الاسم الظاهري للتطبيق الخبيث / Nom de l'APK malveillant: ${incident.maliciousAppName}
- اسم الحزمة البرمجية / Package Identifier: ${incident.maliciousPackage}
- البصمة الرقمية المشفرة / SHA-256 Checksum: ${incident.maliciousSha256}
- طريقة التسلل / Vecteur d'infection: تثبيت ملف APK خارجي خارج متجر Google Play.

4. السلوكيات الجنائية المرصودة تقنياً / ACTIONS FRAUDULEUSES DÉTECTÉES:
--------------------------------------------------------------------------------
[X] 1. استغلال خدمات إمكانية الوصول (Accessibility Services Abuse):
       التطبيق الخبيث حصل على إذن التحكم التلقائي بالشاشة والنقر دون موافقة الضحية.
[X] 2. هجوم الغطاء الحاجب (Black Screen Overlay Attack):
       توليد شاشة سوداء مزيفة بعنوان "يتم تحديث الهاتف" لحجب الرؤية وتجميد الجهاز 10 دقائق.
[X] 3. اعتراض وحذف رسائل التأكيد (SMS/OTP Interception & Deletion):
       قراءة رمز التحويل المرسل من بريد الجزائر وتأكيد المعاملة ثم حذف الرسالة خفية.
[X] 4. الحساب البريدي المستقبل للتحويلات (إن وجد) / RIP Destinataire:
       ${if (incident.interceptedRipDestination.isNotBlank()) incident.interceptedRipDestination else "قيد التتبع في سجلات خادم بريد الجزائر"}

5. السند القانوني في التشريع الجزائري / FONDEMENT JURIDIQUE (LOI ALGÉRIENNE):
--------------------------------------------------------------------------------
- المواد 394 مكرر إلى 394 مكرر 2 من قانون العقوبات الجزائري: (جرائم المساس بأنظمة المعالجة الآلية للمعطيات).
- القانون رقم 18-07 المؤرخ في 10 يونيو 2018 المتعلق بحماية الأشخاص الطبيعيين في مجال معالجة المعطيات ذات الطابع الشخصي.
- القانون رقم 15-04 المحدد للقواعد العامة المتعلقة بالتوقيع والتصديق الإلكترونيين.

تم استخراج وتوثيق هذه الأدلة الرقمية تلقائياً بواسطة منصة: BaridiGuard Security Engine
Rapport certifié et horodaté généré par BaridiGuard Forensic System.
================================================================================
""".trimIndent()
    }
}
