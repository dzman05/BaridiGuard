# Privacy Policy / سياسة الخصوصية • BaridiGuard (حارس بريدي)

**Last Updated:** October 24, 2026  
**Application Version:** 1.3.1  
**Hosting / Source:** https://github.com/dzman05/BaridiGuard  
**Live Policy Page:** https://dzman05.github.io/BaridiGuard/privacy-policy.html

---

## ⚖️ Legal Disclaimer & Non-Affiliation / إخلاء مسؤولية قانوني
BaridiGuard (حارس بريدي) is an open-source, independent cybersecurity utility designed to protect users against mobile banking trojans, malicious overlays, and OTP deletion.  
**BaridiGuard is NOT affiliated with, authorized, or officially endorsed by Algérie Poste (بريد الجزائر) or the official BaridiMob application.**

---

## 🛡️ Core Commitment: Zero Data Collection (100% Local Processing)
BaridiGuard does **not** collect, transmit, store on cloud servers, or share any personal, financial, or banking data. All operations, monitoring, and OTP retention are performed **strictly offline on the local Android device**.

---

## 1. AccessibilityService API Disclosure
In compliance with Google Play's Accessibility API policies:
- **Purpose:** BaridiGuard uses Android's `AccessibilityService` exclusively to detect when the official banking application (`ru.bpc.mobilebank.bpc`) is launched into the foreground and to detect/block unauthorized transparent or black overlay windows ("System update in progress") spawned by banking trojans.
- **Data Protection:** The service does **not** log keystrokes, does not read passwords, does not manipulate user interfaces without consent, and does not capture screen pixels or audio.
- **Prominent Disclosure & Affirmative Consent:** Users must explicitly review the in-app prominent disclosure dialog and tap affirmative consent prior to enabling the service in Android Accessibility settings.

---

## 2. SMS Protection & OTP Vault (`RECEIVE_SMS`)
- **Purpose:** BaridiGuard inspects incoming SMS messages strictly matching financial keywords or Algérie Poste OTP notifications.
- **Anti-Deletion Vault:** Extracted OTP verification codes are saved into a local secure vault (`OtpVaultStorage`) to preserve proof of transaction even if a trojan attempts to silently delete the SMS.
- **Confidentiality:** No SMS content is transmitted across the network. The user retains complete control to inspect or purge the vault.

---

## 3. Device Administration (`BIND_DEVICE_ADMIN`)
- **Purpose:** Used strictly as a self-defense mechanism (**Anti-Kill / Anti-Uninstall**) to prevent unauthorized malware from forcibly killing the guardian service or silently uninstalling the security app during an ongoing attack.

---

## 4. Emergency Network Kill-Switch
- **Purpose:** Provides a one-tap network cut to isolate the device (Wi-Fi and mobile data) when suspicious overlay activity is suspected, cutting off trojan C2 communication.

---

## 5. User Control & Data Erasure
Users may revoke permissions, clear stored vault data, or uninstall BaridiGuard at any time through standard Android settings.

---

## Contact
Open source repository: https://github.com/dzman05/BaridiGuard
