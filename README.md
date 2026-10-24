# 🛡️ BaridiGuard - حارس بريدي (Android Anti-Banking-Trojan Guardian)

> **درع الأمان الاستباقي لمستخدمي بريدي موب والدفع الإلكتروني في الجزائر**

مشروع أندرويد متكامل تم بناؤه استناداً لأحدث معايير هندسة وتأمين تطبيقات الهاتف (وفق مهارات `mobile-developer` و `mobile-security-coder`) للتصدي لتروجانات أندرويد التي تستهدف تطبيق **BaridiMob**، وتعتمد على حيل:
1. **الشاشة السوداء المزيفة (Overlay Attacks)** ورسالة "يتم تحديث الهاتف".
2. **استغلال خدمات إمكانية الوصول (Accessibility Abuse)** للنقر وإجراء تحويلات مالية خفية.
3. **اعتراض وحذف رسائل الـ OTP** قبل أن يراها المستخدم.

---

## 📁 محتويات المشروع (Project Structure)

```
D:\BaridiGuard\
├── app-icon.svg                  # أيقونة التطبيق الرسمية فائقة الدقة (Google Play Ready)
├── logo.svg                      # الشعار الكامل الأفقي (العربية والإنجليزية)
├── index.html                    # المعاينة التفاعلية الكاملة ومحاكي الهاتف الحي (تشغيل فوري في المتصفح)
└── android-app\                  # كود مشروع أندرويد الكامل الجاهز للفتح في Android Studio
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── app\
        ├── build.gradle.kts      # Jetpack Compose + Material 3 + Coroutines + Crypto
        └── src\main\
            ├── AndroidManifest.xml
            ├── java\dz\baridiguard\
            │   ├── BaridiGuardApp.kt              # تهيئة قنوات الإشعارات عالية الأولوية
            │   ├── security\
            │   │   ├── AccessibilityWatchdog.kt   # فاحص خدمات الوصول المشبوهة
            │   │   ├── OverlayDetector.kt         # كاشف النوافذ العائمة والشاشة السوداء
            │   │   ├── OtpVaultReceiver.kt        # لاقط أكواد الـ OTP بالأولوية 999
            │   │   └── BaridiGuardService.kt      # خدمة المراقبة الدائمة في الخلفية
            │   └── ui\
            │       ├── MainActivity.kt            # واجهات Jetpack Compose العصرية
            │       └── theme\                     # ألوان وهوية التطبيق الرسمية
            └── res\
```

---

## 🚀 كيفية فتح وتشغيل المشروع

### 1. تجربة المحاكي التفاعلي الفوري:
- افتح ملف `D:\BaridiGuard\index.html` في متصفحك مباشرة.
- ستتمكن من:
  - الضغط على **"بدء فحص الهاتف"** لرؤية تدقيق الأذونات.
  - الضغط على **"محاكاة هجوم الشاشة السوداء"** لاختبار تصدي الدرع للتروجان مع أصوات الإنذار التفاعلية.
  - محاكاة استقبال كود OTP مشفر من بريد الجزائر.

### 2. فتح المشروع في Android Studio:
1. افتح **Android Studio**.
2. اختر **Open** ثم حدد المجلد: `D:\BaridiGuard\android-app`.
3. سيبدأ Gradle بمزامنة الاعتماديات تلقائياً (Kotlin 1.9+, Java 17+, Android SDK 35).
4. اضغط على **Run ▶️** لتشغيله على أي هاتف حقيقي أو محاكي أندرويد.
