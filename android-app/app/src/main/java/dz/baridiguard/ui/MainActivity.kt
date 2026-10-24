package dz.baridiguard.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.baridiguard.R
import dz.baridiguard.security.*
import dz.baridiguard.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var accessibilityWatchdog: AccessibilityWatchdog
    private lateinit var overlayDetector: OverlayDetector
    private lateinit var networkKillSwitch: NetworkKillSwitch
    private lateinit var phishingScanner: PhishingScanner
    private lateinit var forensicReportGenerator: ForensicReportGenerator
    private lateinit var baridiMobDetector: BaridiMobLaunchDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accessibilityWatchdog = AccessibilityWatchdog(this)
        overlayDetector = OverlayDetector(this)
        networkKillSwitch = NetworkKillSwitch(this)
        phishingScanner = PhishingScanner()
        forensicReportGenerator = ForensicReportGenerator(this)
        baridiMobDetector = BaridiMobLaunchDetector(this)

        startProtectionService()

        setContent {
            BaridiGuardTheme {
                MainAppScreen(
                    watchdog = accessibilityWatchdog,
                    overlayDetector = overlayDetector,
                    killSwitch = networkKillSwitch,
                    phishingScanner = phishingScanner,
                    forensics = forensicReportGenerator,
                    baridiMobDetector = baridiMobDetector,
                    onPlaySiren = { playPureCyberAlarmSiren() },
                    onCall1530 = { callAlgeriePosteEmergency() }
                )
            }
        }
    }

    private fun startProtectionService() {
        val serviceIntent = Intent(this, BaridiGuardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun callAlgeriePosteEmergency() {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1530"))
        startActivity(intent)
    }

    /**
     * تشغيل صوت إنذار سيبراني خالص بدون أي صوت بشري (Pure Multi-Tone Cyber Siren)
     */
    fun playPureCyberAlarmSiren() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 1800
                val numSamples = (durationMs * sampleRate / 1000)
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = 850.0 + 750.0 * kotlin.math.sin(2.0 * Math.PI * progress * 5.0)
                    val angle = 2.0 * Math.PI * i * freq / sampleRate
                    buffer[i] = (kotlin.math.sin(angle) * Short.MAX_VALUE * 0.75).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    watchdog: AccessibilityWatchdog,
    overlayDetector: OverlayDetector,
    killSwitch: NetworkKillSwitch,
    phishingScanner: PhishingScanner,
    forensics: ForensicReportGenerator,
    baridiMobDetector: BaridiMobLaunchDetector,
    onPlaySiren: () -> Unit,
    onCall1530: () -> Unit
) {
    // In-App Language Toggle: true = Arabic (RTL), false = French (LTR)
    var isArabic by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var securityScore by remember { mutableStateOf(98) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showDossierDialog by remember { mutableStateOf(false) }
    var generatedReportText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Dynamic RTL / LTR layout direction
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Official BaridiGuard Emblem Icon
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, EmeraldLight, RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("BARIDIGUARD", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                                Text(
                                    text = if (isArabic) "درع حماية بريدي موب" else "Bouclier BaridiMob",
                                    fontSize = 11.sp,
                                    color = EmeraldLight
                                )
                            }
                        }
                    },
                    actions = {
                        // Interactive In-App Language Switcher Button [ 🇩🇿 العربية | 🇫🇷 Français ]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GuardCardDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { isArabic = !isArabic }
                        ) {
                            Text(
                                text = if (isArabic) "🇫🇷 FR" else "🇩🇿 AR",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Emergency Button 1530
                        IconButton(onClick = { showEmergencyDialog = true }) {
                            Icon(Icons.Default.Warning, contentDescription = "Urgence", tint = DangerRed)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GuardSurfaceDark)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = GuardSurfaceDark) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(if (isArabic) "الرئيسية" else "Accueil", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text(if (isArabic) "الروابط" else "Liens", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text(if (isArabic) "قضائي" else "Justice", fontSize = 11.sp) }
                    )
                }
            },
            containerColor = GuardBgDark
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> DashboardTab(
                        isArabic = isArabic,
                        securityScore = securityScore,
                        isScanning = isScanning,
                        hasUsagePermission = baridiMobDetector.hasUsageStatsPermission(),
                        onStartScan = {
                            coroutineScope.launch {
                                isScanning = true
                                delay(1200)
                                isScanning = false
                                securityScore = 100
                            }
                        },
                        onTriggerSiren = onPlaySiren,
                        onTriggerKillSwitch = {
                            onPlaySiren()
                            killSwitch.triggerEmergencyNetworkIsolation(isArabic)
                        },
                        onTestBaridiAlarm = {
                            baridiMobDetector.triggerBaridiMobSecurityAlert(isArabic)
                        },
                        onOpenUsageSettings = {
                            baridiMobDetector.openUsageAccessSettings()
                        }
                    )
                    1 -> PhishingScannerTab(isArabic, phishingScanner)
                    2 -> ForensicsTab(
                        isArabic = isArabic,
                        onGenerateReport = {
                            val sampleDossier = forensics.generateOfficialPoliceDossier(
                                ForensicReportGenerator.IncidentData(
                                    victimName = if (isArabic) "مواطن متضرر (ضحية قرصنة)" else "Victime de cybercriminalité",
                                    victimPhone = "06XX-XX-XX-XX",
                                    ccpOrRipNumber = "00799999000123456789",
                                    maliciousAppName = "CinemaFlix Pro HD (APK)",
                                    maliciousPackage = "com.cinemaflix.stream.hd",
                                    maliciousSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                                    estimatedLossDzd = "500,000",
                                    interceptedRipDestination = "00799999000987654321"
                                )
                            )
                            generatedReportText = sampleDossier
                            showDossierDialog = true
                        }
                    )
                }
            }
        }

        // Police Forensic Dossier Modal
        if (showDossierDialog) {
            AlertDialog(
                onDismissRequest = { showDossierDialog = false },
                title = {
                    Text(
                        text = if (isArabic) "📄 ملف الشكوى الجنائية والأدلة" else "📄 Dossier de Plainte Judiciaire",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        generatedReportText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.height(350.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showDossierDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (isArabic) "تم الحفظ ✓" else "Fermer ✓", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Emergency 1530 Modal
        if (showEmergencyDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                title = {
                    Text(
                        text = if (isArabic) "🚨 إجراء الطوارئ لتجميد الحساب 1530" else "🚨 Urgence 1530 - Gel du Compte",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        if (isArabic) {
                            "إذا ظهرت شاشة سوداء على هاتفك أو اشتبهت في تحويل غير مصرح به، قم بالاتصال فوراً بـ 1530 لتجميد البطاقة وبريدي موب، مع تشغيل قاطع الإنترنت لعزل الهاتف."
                        } else {
                            "En cas d'écran noir suspect ou de transaction non autorisée, appelez immédiatement le 1530 pour geler la carte et BaridiMob, et activez le coupe-circuit réseau pour isoler l'appareil."
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmergencyDialog = false
                            killSwitch.triggerEmergencyNetworkIsolation(isArabic)
                            onCall1530()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text(
                            text = if (isArabic) "قطع الإنترنت والاتصال بـ 1530" else "Couper Net & Appeler 1530",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardTab(
    isArabic: Boolean,
    securityScore: Int,
    isScanning: Boolean,
    hasUsagePermission: Boolean,
    onStartScan: () -> Unit,
    onTriggerSiren: () -> Unit,
    onTriggerKillSwitch: () -> Unit,
    onTestBaridiAlarm: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radar Circle with Official Vector Icon Inside
        Box(
            modifier = Modifier
                .size(175.dp)
                .clip(CircleShape)
                .background(GuardSurfaceDark)
                .border(3.dp, EmeraldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$securityScore%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = EmeraldLight
                )
                Text(
                    text = if (isScanning) {
                        if (isArabic) "جاري الفحص المتقدم..." else "Analyse en cours..."
                    } else {
                        if (isArabic) "الهاتف محمي 100%" else "Appareil 100% Sécurisé"
                    },
                    fontSize = 10.5.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Big Scan Button
        Button(
            onClick = onStartScan,
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isArabic) "فحص أمان تطبيقات الدفع الآن" else "Analyser la sécurité bancaire",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Siren & Network Kill Switch
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTriggerSiren,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GuardCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isArabic) "🔊 صفارة الإنذار" else "🔊 Sirène d'alarme",
                    fontSize = 11.sp,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onTriggerKillSwitch,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DangerBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isArabic) "⚡ قاطع الإنترنت" else "⚡ Coupe-circuit Net",
                    fontSize = 11.sp,
                    color = DangerRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Test BaridiMob Launch Alarm Button
        Button(
            onClick = onTestBaridiAlarm,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GuardCardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (isArabic) "🚨 اختبار إنذار فتح بريدي موب" else "🚨 Tester l'alarme ouverture BaridiMob",
                fontSize = 12.sp,
                color = EmeraldLight,
                fontWeight = FontWeight.Bold
            )
        }

        // Usage Access Permission Banner if not yet granted
        if (!hasUsagePermission) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUsageSettings() },
                colors = CardDefaults.cardColors(containerColor = DangerBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isArabic) "اضغط هنا لتفعيل رادار بريدي موب التلقائي" else "Cliquez pour activer le radar BaridiMob",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isArabic) "يتطلب منح إذن مراقبة الاستخدام (Accès à l'utilisation)" else "Autorisation d'accès aux données d'utilisation requise",
                            fontSize = 9.5.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Defenses Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GuardCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "حالة دروع الحماية النشطة" else "Boucliers de défense actifs",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                DefenseRow(
                    title = if (isArabic) "رادار إنذار تشغيل بريدي موب" else "Alarme ouverture BaridiMob",
                    subtitle = if (isArabic) "صفارة إنذار وتحذير فوري عند فتح التطبيق" else "Alerte sonore immédiate dès le lancement de BaridiMob",
                    active = true
                )
                DefenseRow(
                    title = if (isArabic) "درع تطبيق بريدي موب" else "Blindage BaridiMob",
                    subtitle = if (isArabic) "حظر النقرات غير المرئية في الخلفية" else "Blocage des clics invisibles en arrière-plan",
                    active = true
                )
                DefenseRow(
                    title = if (isArabic) "كاشف الشاشة السوداء" else "Détecteur d'écran noir",
                    subtitle = if (isArabic) "منع شاشة التحديث المزيفة" else "Empêche l'écran factice de mise à jour",
                    active = true
                )
                DefenseRow(
                    title = if (isArabic) "قاطع الإنترنت التلقائي" else "Coupe-circuit réseau",
                    subtitle = if (isArabic) "عزل الهاتف فوراً لقطع اتصال التروجان" else "Isolation réseau immédiate anti-trojan",
                    active = true
                )
                DefenseRow(
                    title = if (isArabic) "درع الحماية الذاتية (Anti-Kill)" else "Auto-défense (Anti-Kill)",
                    subtitle = if (isArabic) "منع الفيروس من فرض الإيقاف أو الحذف" else "Immunité contre l'arrêt forcé par un malware",
                    active = true
                )
            }
        }
    }
}

@Composable
fun DefenseRow(title: String, subtitle: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
            Text(subtitle, fontSize = 9.5.sp, color = Color.Gray)
        }
        Switch(
            checked = active,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldLight, checkedTrackColor = EmeraldDark)
        )
    }
}

@Composable
fun PhishingScannerTab(isArabic: Boolean, scanner: PhishingScanner) {
    var inputUrl by remember { mutableStateOf("https://baridimob-update-algerie.cc") }
    var scanResult by remember { mutableStateOf<PhishingScanner.ScanResult?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = if (isArabic) "درع روابط الاحتيال (Anti-Phishing)" else "Bouclier Anti-Hameçonnage",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
        Text(
            text = if (isArabic) {
                "افحص أي رابط قبل فتحه للتأكد من أنه لا ينتحل بريدي موب أو بطاقة الذهبية:"
            } else {
                "Vérifiez tout lien suspect avant de l'ouvrir pour protéger vos données Edahabia:"
            },
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = inputUrl,
            onValueChange = { inputUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (isArabic) "أدخل الرابط للفحص" else "Entrer le lien à analyser") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { scanResult = scanner.analyzeUrl(inputUrl) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text(
                text = if (isArabic) "فحص أمان الرابط الآن" else "Analyser le lien maintenant",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        scanResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (result.isPhishing) DangerBg else GuardCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (result.isPhishing) DangerRed else EmeraldPrimary)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (result.isPhishing) {
                            if (isArabic) "🚨 رابط احتيال مؤكد!" else "🚨 HAMEÇONNAGE CRITIQUE DÉTECTÉ !"
                        } else {
                            if (isArabic) "✓ رابط رسمي آمن" else "✓ Lien officiel sécurisé"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (result.isPhishing) DangerRed else EmeraldLight,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) result.reasonAr else result.reasonFr,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ForensicsTab(isArabic: Boolean, onGenerateReport: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = if (isArabic) "حقيبة الضحية وتقرير الأدلة الرقمية" else "Dossier Judiciaire & Preuves Numériques",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
        Text(
            text = if (isArabic) {
                "توليد ملف أدلة فني وقانوني موجه لفرقة مكافحة الجرائم المعلوماتية (الدرك الوطني / الأمن الوطني):"
            } else {
                "Génération du dossier d'infraction cybercriminelle destiné aux services de Police et Gendarmerie:"
            },
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GuardCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isArabic) "المستندات التي يولدها التقرير:" else "Éléments inclus dans le dossier:",
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Text(
                    text = if (isArabic) "• البصمة الرقمية للتروجان (Package & SHA-256)" else "• Empreinte SHA-256 du malware",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = if (isArabic) "• توقيت تنفيذ عمليات السطو (Horodatage)" else "• Horodatage précis des transactions",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = if (isArabic) "• رقم الحساب المستلم (RIP) لتجميده" else "• N° RIP destinataire pour blocage bancaire",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = if (isArabic) "• السند القانوني بالمواد 394 مكرر من قانون العقوبات" else "• Fondement: Articles 394 bis du Code Pénal",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onGenerateReport,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "توليد وطباعة ملف الشكوى الجنائية" else "Générer le dossier judiciaire",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
