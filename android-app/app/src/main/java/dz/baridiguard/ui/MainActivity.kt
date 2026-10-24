package dz.baridiguard.ui

import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

        // طلب إذن الإشعارات لأندرويد 13 فما فوق لضمان عمل الخدمة بدون انقطاع
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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
                    hasSmsPermission = hasSmsPermission(),
                    onRequestSmsPermission = { requestSmsPermission() },
                    isDeviceAdminActive = isDeviceAdminActive(),
                    onRequestDeviceAdmin = { requestDeviceAdmin() },
                    onOpenAppDetails = { openAppDetailsSettings() },
                    onPlaySiren = { playPureCyberAlarmSiren() },
                    onCall1530 = { callAlgeriePosteEmergency() },
                    onLaunchBaridiMob = { launchBaridiMobApp() }
                )
            }
        }
    }

    fun hasSmsPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun requestSmsPermission() {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_SMS
            ),
            102
        )
    }

    fun isDeviceAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val componentName = ComponentName(this, BaridiDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(componentName)
    }

    fun requestDeviceAdmin() {
        val componentName = ComponentName(this, BaridiDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "حماية BaridiGuard من الإيقاف القسري أو الإلغاء بواسطة الفيروسات والتروجانات أثناء الهجوم على بريدي موب."
            )
        }
        startActivity(intent)
    }

    fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchBaridiMobApp() {
        try {
            val pm = packageManager
            val intent = pm.getLaunchIntentForPackage("ru.bpc.mobilebank.bpc")
                ?: pm.getLaunchIntentForPackage("dz.poste.baridimob")
                ?: pm.getLaunchIntentForPackage("com.baridimob")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=ru.bpc.mobilebank.bpc"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
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

    fun playPureCyberAlarmSiren() {
        EmergencyAlarmPlayer.playAudibleAlarm(this)
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
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    isDeviceAdminActive: Boolean,
    onRequestDeviceAdmin: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onPlaySiren: () -> Unit,
    onCall1530: () -> Unit,
    onLaunchBaridiMob: () -> Unit
) {
    // In-App Language Toggle: true = Arabic (RTL), false = French (LTR)
    var isArabic by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var isAlarmPlaying by remember { mutableStateOf(false) }
    var securityScore by remember { mutableStateOf(98) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showDossierDialog by remember { mutableStateOf(false) }
    var generatedReportText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("BARIDIGUARD", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmeraldDark
                                    ) {
                                        Text(
                                            "v1.3.0",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldLight
                                        )
                                    }
                                }
                                Text(
                                    text = if (isArabic) "حارس بريدي • الدرع الشامل" else "Bouclier BaridiMob • v1.3.0",
                                    fontSize = 10.5.sp,
                                    color = EmeraldLight
                                )
                            }
                        }
                    },
                    actions = {
                        // Language Switcher
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

                        // Emergency 1530 Call
                        IconButton(onClick = { showEmergencyDialog = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.fa_triangle_exclamation),
                                contentDescription = "Urgence",
                                tint = DangerRed,
                                modifier = Modifier.size(20.dp)
                            )
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
                        icon = { 
                            Icon(
                                painter = painterResource(id = R.drawable.fa_house),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selectedTab == 0) EmeraldLight else Color.Gray
                            ) 
                        },
                        label = { Text(if (isArabic) "الرئيسية" else "Accueil", fontSize = 10.5.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { 
                            Icon(
                                painter = painterResource(id = R.drawable.fa_envelope),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selectedTab == 1) EmeraldLight else Color.Gray
                            ) 
                        },
                        label = { Text(if (isArabic) "خزنة OTP" else "Coffre OTP", fontSize = 10.5.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { 
                            Icon(
                                painter = painterResource(id = R.drawable.fa_magnifying_glass),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selectedTab == 2) EmeraldLight else Color.Gray
                            ) 
                        },
                        label = { Text(if (isArabic) "الروابط" else "Liens", fontSize = 10.5.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { 
                            Icon(
                                painter = painterResource(id = R.drawable.fa_scale_balanced),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selectedTab == 3) EmeraldLight else Color.Gray
                            ) 
                        },
                        label = { Text(if (isArabic) "قضائي" else "Justice", fontSize = 10.5.sp) }
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
                        isAlarmPlaying = isAlarmPlaying,
                        isAccessibilityActive = baridiMobDetector.isAccessibilityServiceEnabled(),
                        hasUsagePermission = baridiMobDetector.hasUsageStatsPermission(),
                        hasSmsPermission = hasSmsPermission,
                        isDeviceAdminActive = isDeviceAdminActive,
                        onRequestSmsPermission = onRequestSmsPermission,
                        onRequestDeviceAdmin = onRequestDeviceAdmin,
                        onOpenAppDetails = onOpenAppDetails,
                        onStartScan = {
                            coroutineScope.launch {
                                isScanning = true
                                delay(1200)
                                isScanning = false
                                securityScore = 100
                            }
                        },
                        onTriggerSiren = {
                            if (isAlarmPlaying) {
                                EmergencyAlarmPlayer.stopAlarm()
                                isAlarmPlaying = false
                            } else {
                                isAlarmPlaying = true
                                onPlaySiren()
                                coroutineScope.launch {
                                    delay(3700)
                                    isAlarmPlaying = false
                                }
                            }
                        },
                        onStopAlarm = {
                            EmergencyAlarmPlayer.stopAlarm()
                            isAlarmPlaying = false
                        },
                        onTriggerKillSwitch = {
                            onPlaySiren()
                            killSwitch.triggerEmergencyNetworkIsolation(isArabic)
                        },
                        onTestBaridiAlarm = {
                            if (isAlarmPlaying) {
                                EmergencyAlarmPlayer.stopAlarm()
                                isAlarmPlaying = false
                            } else {
                                isAlarmPlaying = true
                                baridiMobDetector.triggerBaridiMobSecurityAlert(isArabic)
                                coroutineScope.launch {
                                    delay(3700)
                                    isAlarmPlaying = false
                                }
                            }
                        },
                        onOpenAccessibility = {
                            baridiMobDetector.openAccessibilitySettings()
                        },
                        onLaunchBaridiMob = onLaunchBaridiMob
                    )
                    1 -> OtpVaultTab(
                        isArabic = isArabic,
                        hasSmsPermission = hasSmsPermission,
                        onRequestSmsPermission = onRequestSmsPermission
                    )
                    2 -> PhishingScannerTab(isArabic, phishingScanner)
                    3 -> ForensicsTab(
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
    isAlarmPlaying: Boolean,
    isAccessibilityActive: Boolean,
    hasUsagePermission: Boolean,
    hasSmsPermission: Boolean,
    isDeviceAdminActive: Boolean,
    onRequestSmsPermission: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onStartScan: () -> Unit,
    onTriggerSiren: () -> Unit,
    onStopAlarm: () -> Unit,
    onTriggerKillSwitch: () -> Unit,
    onTestBaridiAlarm: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onLaunchBaridiMob: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radar Circle with Official Emblem Inside
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(GuardSurfaceDark)
                .border(3.dp, EmeraldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$securityScore%",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = EmeraldLight
                )
                Text(
                    text = if (isScanning) {
                        if (isArabic) "جاري الفحص المتقدم..." else "Analyse en cours..."
                    } else {
                        if (isArabic) "الهاتف محمي 100%" else "Appareil 100% Sécurisé"
                    },
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big Scan Button
        Button(
            onClick = onStartScan,
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isArabic) "فحص أمان تطبيقات الدفع الآن" else "Analyser la sécurité bancaire",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Big Prominent STOP ALARM Button when sound is active
        if (isAlarmPlaying) {
            Button(
                onClick = onStopAlarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_bell),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "🛑 إيقاف صفارة الإنذار فوراً (Arrêter)" else "🛑 Arrêter l'alarme sonore",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Action Buttons: Test BaridiMob Alarm & Launch Official BaridiMob
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTestBaridiAlarm,
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GuardCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_triangle_exclamation),
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "🚨 تجربة الإنذار" else "🚨 Test Alarme",
                        fontSize = 11.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onLaunchBaridiMob,
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_house),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "📱 فتح بريدي موب" else "📱 Ouvrir BaridiMob",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Manual Siren & Network Kill Switch
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTriggerSiren,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GuardCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_bell),
                        contentDescription = null,
                        tint = GoldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "صفارة الإنذار" else "Sirène d'alarme",
                        fontSize = 11.sp,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onTriggerKillSwitch,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DangerBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_bolt),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "قاطع الإنترنت" else "Coupe-circuit",
                        fontSize = 11.sp,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ══════════════════════════════════════════════════════════════════
        // 🚀 معالج التفعيل السريع للدروع (One-Tap Shield Activator 1-2-3-4)
        // ══════════════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GuardCardDark),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_shield_halved),
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "⚡ معالج التفعيل والتسطيب السريع (Guide 1-2-3)" else "⚡ Guide d'Activation Rapide (1-2-3)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GoldLight
                    )
                }

                Text(
                    text = if (isArabic) {
                        "لضمان أمان أموالك 100% ضد تروجان بريدي موب والشاشة السوداء وسرقة الـ OTP، يرجى تفعيل الصلاحيات التالية:"
                    } else {
                        "Pour garantir la protection totale de vos fonds contre le trojan BaridiMob et le vol d'OTP, activez les modules ci-dessous :"
                    },
                    fontSize = 10.5.sp,
                    color = Color.LightGray
                )

                HorizontalDivider(color = GuardSurfaceDark, thickness = 1.dp)

                // الخطوة 1: حل مشكلة الإعدادات المقيدة والشفافة (Paramètres restreints Android 13/14)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isArabic) "1. 🔓 حل القفل الشفاف (Paramètres Restreints):" else "1. 🔓 Débloquer les paramètres restreints :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (isArabic) {
                            "إذا كان خيار إمكانية الوصول رمادياً ولا يفتح: اضغط الزر أدناه، ثم من القائمة العلوية (⋮) في الزاوية اختر: Autoriser les paramètres restreints."
                        } else {
                            "Si l'option Accessibilité est grisée: cliquez ci-dessous, puis appuyez sur les 3 points (⋮) en haut à droite et choisissez 'Autoriser les paramètres restreints'."
                        },
                        fontSize = 9.5.sp,
                        color = Color.Gray
                    )
                    OutlinedButton(
                        onClick = onOpenAppDetails,
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "⚙️ فتح معلومات التطبيق (Infos sur l'application)" else "⚙️ Ouvrir Infos Application",
                            fontSize = 10.5.sp,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = GuardSurfaceDark, thickness = 1.dp)

                // الخطوة 2: رادار إمكانية الوصول لتطبيق بريدي موب
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "2. 🛡️ رادار بريدي موب (Accessibilité):" else "2. 🛡️ Radar BaridiMob (Accessibilité) :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isAccessibilityActive) "✅ مفعل" else "❌ معطل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAccessibilityActive) EmeraldLight else DangerRed
                        )
                    }
                    if (!isAccessibilityActive) {
                        Button(
                            onClick = onOpenAccessibility,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "👉 تفعيل رادار الحراسة الآن (Accessibilité)" else "👉 Activer dans Accessibilité",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = GuardSurfaceDark, thickness = 1.dp)

                // الخطوة 3: حماية رسائل OTP من السرقة والحذف
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "3. 📩 درع حفظ رسائل OTP (Anti-Suppression):" else "3. 📩 Coffre OTP (Anti-Suppression) :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (hasSmsPermission) "✅ مفعل" else "❌ معطل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSmsPermission) EmeraldLight else DangerRed
                        )
                    }
                    if (!hasSmsPermission) {
                        Button(
                            onClick = onRequestSmsPermission,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "📩 منح إذن حماية رسائل التأكيد (SMS OTP)" else "📩 Autoriser la protection SMS OTP",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = GuardSurfaceDark, thickness = 1.dp)

                // الخطوة 4: الحماية الذاتية ضد الإغلاق والحذف (Device Admin)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "4. 🔒 مناعة ضد الإلغاء والإيقاف (Anti-Kill):" else "4. 🔒 Immunité Anti-Désinstallation :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isDeviceAdminActive) "✅ محصن" else "❌ غير محصن",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeviceAdminActive) EmeraldLight else DangerRed
                        )
                    }
                    if (!isDeviceAdminActive) {
                        OutlinedButton(
                            onClick = onRequestDeviceAdmin,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "🛡️ تحصين التطبيق كمسؤول جهاز (Device Admin)" else "🛡️ Activer l'immunité Device Admin",
                                color = EmeraldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ══════════════════════════════════════════════════════════════════
        // 🛡️ دروع الحماية الـ 5 الأساسية (Les 5 Piliers Fondamentaux)
        // ══════════════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GuardCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "حالة دروع الأمان الأساسية الـ 5" else "Les 5 Boucliers Fondamentaux de Sécurité",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                DefenseRow(
                    iconRes = R.drawable.fa_bell,
                    title = if (isArabic) "1. رادار تشغيل بريدي موب الرسمي" else "1. Radar BaridiMob (ru.bpc.mobilebank.bpc)",
                    subtitle = if (isArabic) "صفارة إنذار فورية عند فتح التطبيق من جوجل بلاي" else "Alerte sonore immédiate à l'ouverture de l'app",
                    active = isAccessibilityActive
                )
                DefenseRow(
                    iconRes = R.drawable.fa_envelope,
                    title = if (isArabic) "2. خزنة تأكيد الرسائل (OTP Vault)" else "2. Coffre OTP & Anti-Suppression",
                    subtitle = if (isArabic) "أسبقية قصوى (Priority 999) لحفظ كود التأكيد ومنع حذفه" else "Sauvegarde ultra-prioritaire anti-suppression malware",
                    active = hasSmsPermission
                )
                DefenseRow(
                    iconRes = R.drawable.fa_lock,
                    title = if (isArabic) "3. كاشف الشاشة السوداء والتراكب" else "3. Bloqueur d'écran noir & overlays",
                    subtitle = if (isArabic) "منع شاشة التحديث المزيفة (جاري تحديث الهاتف...)" else "Empêche l'écran factice de mise à jour système",
                    active = true
                )
                DefenseRow(
                    iconRes = R.drawable.fa_wifi,
                    title = if (isArabic) "4. قاطع الإنترنت التلقائي (Kill-Switch)" else "4. Coupe-circuit réseau instantané",
                    subtitle = if (isArabic) "عزل فوري للشبكة وقطع اتصال التروجان بالخادم" else "Isolation réseau pour couper la communication trojan",
                    active = true
                )
                DefenseRow(
                    iconRes = R.drawable.fa_shield_virus,
                    title = if (isArabic) "5. درع الحماية الذاتية (Anti-Kill)" else "5. Auto-défense & Anti-Désinstallation",
                    subtitle = if (isArabic) "منع الفيروس من فرض الإيقاف أو حذف التطبيق" else "Immunité contre la fermeture et suppression forcée",
                    active = isDeviceAdminActive
                )
            }
        }
    }
}

@Composable
fun DefenseRow(iconRes: Int, title: String, subtitle: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GuardSurfaceDark)
                    .border(1.dp, EmeraldLight.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (active) EmeraldLight else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                Text(subtitle, fontSize = 9.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = active,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldLight,
                checkedTrackColor = EmeraldDark,
                uncheckedThumbColor = Color.DarkGray,
                uncheckedTrackColor = GuardSurfaceDark
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// 📩 خزنة OTP المشفرة (Coffre-Fort des Codes OTP)
// ══════════════════════════════════════════════════════════════════════════
@Composable
fun OtpVaultTab(
    isArabic: Boolean,
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit
) {
    val context = LocalContext.current
    var otpList by remember { mutableStateOf(OtpVaultStorage.getAllOtps(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isArabic) "خزنة تأكيد الرسائل (OTP Vault)" else "Coffre-Fort des Codes OTP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = if (isArabic) {
                        "النسخة المستقلة المحصنة ضد الحذف الخفي بواسطة التروجان"
                    } else {
                        "Copie sécurisée immunisée contre la suppression discrète par les malwares"
                    },
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )
            }

            // زر تحديث القائمة
            IconButton(onClick = { otpList = OtpVaultStorage.getAllOtps(context) }) {
                Icon(
                    painter = painterResource(id = R.drawable.fa_envelope),
                    contentDescription = "Refresh",
                    tint = EmeraldLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // تنبيه في حال عدم منح إذن الرسائل
        if (!hasSmsPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DangerBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isArabic) "⚠️ إذن حماية الرسائل غير ممنوح!" else "⚠️ Permission SMS requise !",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isArabic) {
                            "لكي يتمكن التطبيق من التقاط رمز الـ OTP بأسبقية 999 وحفظه قبل أن يحذفه الفيروس، يرجى منح الإذن:"
                        } else {
                            "Pour intercepter les SMS de confirmation avant leur suppression par le malware, accordez la permission :"
                        },
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                    Button(
                        onClick = onRequestSmsPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text(
                            text = if (isArabic) "منح إذن الرسائل الآن (Autoriser SMS)" else "Autoriser l'accès SMS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // زر تجربة محاكاة رسالة تأكيد وهمية للاختبار الفوري
        OutlinedButton(
            onClick = {
                OtpVaultStorage.saveOtp(
                    context,
                    sender = "ALGERIE POSTE",
                    otpCode = (100000..999999).random().toString(),
                    fullBody = "ALGERIE POSTE: Votre code de confirmation pour virement BaridiMob est: ${(100000..999999).random()}. Ne le communiquez a personne."
                )
                otpList = OtpVaultStorage.getAllOtps(context)
                Toast.makeText(context, if (isArabic) "تمت إضافة رسالة تجريبية للخزنة ✓" else "Code OTP test ajouté ✓", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(38.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.fa_envelope),
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "🧪 تجربة حفظ رسالة OTP تجريبية" else "🧪 Tester l'enregistrement d'un OTP",
                    fontSize = 10.5.sp,
                    color = EmeraldLight,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (otpList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_lock),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) "الخزنة فارغة حالياً" else "Aucun message intercepté",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isArabic) {
                            "بمجرد وصول أي رسالة OTP من بريد الجزائر، ستظهر نسختها المحمية هنا فوراً حتى لو حُذفت من هاتفك."
                        } else {
                            "Dès la réception d'un code OTP d'Algérie Poste, sa copie protégée apparaîtra ici."
                        },
                        color = Color.Gray,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(otpList) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GuardCardDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldDark
                                    ) {
                                        Text(
                                            text = record.sender,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldLight
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✓ محمي ضد الحذف",
                                        fontSize = 9.sp,
                                        color = EmeraldLight
                                    )
                                }
                                Text(
                                    text = record.timestamp,
                                    fontSize = 9.5.sp,
                                    color = Color.Gray
                                )
                            }

                            // عرض كود التأكيد بحجم كبير جداً مع زر النسخ
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GuardSurfaceDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabic) "رمز التأكيد (Code OTP):" else "Code de confirmation :",
                                        fontSize = 10.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = record.otpCode,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldLight,
                                        letterSpacing = 2.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("OTP", record.otpCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isArabic) "تم نسخ الكود: ${record.otpCode}" else "Code copié !", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "نسخ الكود" else "Copier",
                                        fontSize = 11.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // نص الرسالة الأصلي الكامل
                            Text(
                                text = record.fullBody,
                                fontSize = 10.5.sp,
                                color = Color.White,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
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
                "Vérifiez tout lien suspect avant de l'ouvrir pour protéger vos données Edahabia :"
            },
            fontSize = 11.sp,
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

        // Quick test URLs: Official BaridiWeb vs Scam Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    inputUrl = "https://baridiweb.poste.dz/"
                    scanResult = scanner.analyzeUrl("https://baridiweb.poste.dz/")
                },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_globe),
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("baridiweb.poste.dz", fontSize = 9.5.sp, color = EmeraldLight)
                }
            }

            OutlinedButton(
                onClick = {
                    inputUrl = "https://baridimob-update-algerie.cc"
                    scanResult = scanner.analyzeUrl("https://baridimob-update-algerie.cc")
                },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fa_triangle_exclamation),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "رابط احتيالي وهمي" else "Lien pirate test", fontSize = 9.5.sp, color = DangerRed)
                }
            }
        }

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
                "Génération du dossier d'infraction cybercriminelle destiné aux services de Police et Gendarmerie :"
            },
            fontSize = 11.5.sp,
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
                    text = if (isArabic) "المستندات التي يولدها التقرير:" else "Éléments inclus dans le dossier :",
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
                    text = if (isArabic) "• السند القانوني بالمواد 394 مكرر من قانون العقوبات" else "• Fondement : Articles 394 bis du Code Pénal",
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
                Icon(
                    painter = painterResource(id = R.drawable.fa_scale_balanced),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
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
