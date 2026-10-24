# 🚀 دليل النشر والاعتماد على Google Play Console • BaridiGuard

هذا الدليل يشتمل على كافة النصوص والنماذج الجاهزة للنسخ واللصق (Copier-Coller) في لوحة تحكم **Google Play Console** لتجاوز المراجعة وقبول التطبيق من المرة الأولى دون أي رفض.

---

## 1. بيانات التطبيق الأساسية (Store Listing)

### اسم التطبيق (App Name - أقصى حد 30 حرف):
* **بالعربية:** `BaridiGuard: حارس بريدي للأمان` (30 حرف)
* **بالفرنسية:** `BaridiGuard: Bouclier Sécurité` (30 حرف)

### الوصف القصير (Short Description - أقصى حد 80 حرف):
* **بالعربية:** `درع أمني لحماية مستخدمي الدفع الإلكتروني من التروجان والشاشات المزيفة.` (77 حرف)
* **بالفرنسية:** `Bouclier anti-malware protégeant vos transactions financières et codes OTP.` (76 حرف)

### الوصف الكامل (Full Description):
```text
🛡️ BaridiGuard (حارس بريدي) - الدرع الأمني الشامل لحماية المعاملات المالية

BaridiGuard هو أداة أمنية سيبرانية مستقلة ومفتوحة المصدر مصممة خصيصاً لحماية المواطنين ومستخدمي تطبيقات الدفع الإلكتروني في الجزائر ضد أحدث أساليب القرصنة وهجمات أحصنة طروادة المصرفية (Banking Trojans).

⚠️ إخلاء مسؤولية قانوني:
تطبيق BaridiGuard هو مبادرة أمنية مجتمعية مستقلة غير تابعة أو معتمدة رسمياً من قِبل مؤسسة بريد الجزائر (Algérie Poste) أو تطبيق بريدي موب. الهدف من التطبيق هو تزويد المستخدم بطبقة حماية استباقية مستقلة على جهازه.

✨ الميزات الدفاعية الرئيسية:
1. 🚨 رادار إنذار تشغيل بريدي موب: إطلاق صفارة إنذار وتحذير فوري عند فتح التطبيق لكشف أي تشغيل خفي في الخلفية.
2. 📩 خزنة تأكيد الرسائل (OTP Vault): حفظ نسخة مشفرة ومستقلة من أكواد التأكيد البنكية لحمايتها من الحذف الخفي بواسطة البرمجيات الخبيثة.
3. 🔒 كاشف الشاشة السوداء والتراكب: حظر الشاشات الخادعة (مثل "جاري تحديث الهاتف...") التي تستخدمها الفيروسات لحجب الرؤية عن المستخدم أثناء التحويل غير المصرح به.
4. ⚡ قاطع الإنترنت الطارئ (Kill-Switch): عزل الهاتف فورياً عن شبكات Wi-Fi وشبكة الهاتف المحمول لقطع اتصال الفيروس بغرف التحكم.
5. 🛡️ مناعة الحماية الذاتية (Anti-Kill): منع الفيروس من فرض الإيقاف على درع الحماية أو حذفه خفية عبر صلاحية مسؤول الجهاز.
6. 🔍 درع فحص الروابط المزيفة (Anti-Phishing): الكشف الفوري عن الروابط التي تنتحل بريد الجزائر وصفحات البطاقة الذهبية.
7. 📄 حقيبة الأدلة الجنائية: توليد ملف قانوني وفني مؤصل على المادة 394 مكرر من قانون العقوبات الجزائري لتقديمه لمصالح الدرك والشرطة.

🔒 الخصوصية والأمان:
• معالجة محلية 100% (Local-Only): لا يجمع التطبيق أي بيانات ولا ينقل أي معلومة خارج هاتفك.
• كود مصدري مفتوح ومتاح للجميع للتدقيق الأمني: https://github.com/dzman05/BaridiGuard
```

---

## 2. نموذج أمان البيانات (Data Safety Form)

عند ملء قسم **App Content ➔ Data Safety** في Google Play Console:
1. **Does your app collect or share any of the required user data types?**  
   👉 اختر: **No** (التطبيق لا يجمع ولا يشارك أي بيانات مستخدم).
2. **Is all of the user data collected by your app encrypted in transit?**  
   👉 اختر: **Not applicable** (لا توجد بيانات تغادر الهاتف).
3. **Do you provide a way for users to request that their data be deleted?**  
   👉 اختر: **Yes** (عن طريق مسح بيانات التطبيق أو إلغاء تثبيته).

---

## 3. نموذج الإفصاح عن خدمة إمكانية الوصول (Accessibility API Declaration)

في قسم **App Content ➔ Accessibility Tool**:
* **Is this an accessibility tool designed for people with disabilities?**  
  👉 اختر: **No**.
* **What features in your app use the Accessibility API?**  
  👉 الصق النص التالي بالإنجليزية:
```text
BaridiGuard uses the AccessibilityService API strictly as a real-time anti-fraud and anti-malware security mechanism. Specifically:
1. Foreground App Monitoring: To immediately detect when the official banking application (ru.bpc.mobilebank.bpc) is brought to the foreground and alert the user with an audible alarm to prevent unauthorized background access.
2. Overlay Protection: To detect and block unauthorized invisible or black overlay windows ("Fake system update screen") commonly deployed by banking trojans to obscure fraudulent transactions.
The service does NOT capture keystrokes, does NOT read passwords or screen content, does NOT transmit any telemetry over the network, and operates 100% locally on the device with explicit user consent.
```
* **رابط الفيديو التوضيحي (Video URL):**  
  قم بتسجيل فيديو قصير (1 دقيقة) من شاشة الهاتف يوضح فتح التطبيق وظهور رسالة الإفصاح البارز وتفعيل إمكانية الوصول وسماع صفارة الإنذار عند فتح بريدي موب، وارفعه على YouTube كـ **Unlisted (غير مدرج)** وضع الرابط هنا.

---

## 4. نموذج الإفصاح عن إذن الرسائل النصية (`RECEIVE_SMS`)

في قسم **Sensitive Permissions ➔ SMS and Call Log**:
* **Select the Core Feature that requires SMS permission:**  
  👉 اختر: **Antivirus / Security / Financial Fraud Prevention**.
* **Provide detailed justification:**  
  👉 الصق النص التالي:
```text
BaridiGuard is a dedicated mobile security application designed to protect citizens against banking trojans targeting Algérie Poste customers. Sophisticated malware silently deletes incoming bank verification SMS messages (OTP) to prevent victims from noticing unauthorized money transfers. 
BaridiGuard intercepts incoming SMS matching bank OTP patterns with high priority, saves a protected immutable copy into a local encrypted vault (OtpVaultStorage), and sounds an immediate audio alarm. This ensures the victim retains transaction evidence even if the original SMS is deleted from the default messaging app. No SMS data is ever transmitted off the device.
```

---

## 5. رابط سياسة الخصوصية العام (Privacy Policy URL)
* ضع الرابط التالي في إعدادات التطبيق:  
  👉 **`https://dzman05.github.io/BaridiGuard/privacy-policy.html`**  
  *(أو الرابط المباشر من GitHub: `https://raw.githubusercontent.com/dzman05/BaridiGuard/main/privacy-policy.html`)*
