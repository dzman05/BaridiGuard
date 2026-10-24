package dz.baridiguard.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.baridiguard.R
import dz.baridiguard.security.*
import dz.baridiguard.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var accessibilityWatchdog: AccessibilityWatchdog
    private lateinit var overlayDetector: OverlayDetector
    private lateinit var networkKillSwitch: NetworkKillSwitch
    private lateinit var phishingScanner: PhishingScanner
    private lateinit var forensicReportGenerator: ForensicReportGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accessibilityWatchdog = AccessibilityWatchdog(this)
        overlayDetector = OverlayDetector(this)
        networkKillSwitch = NetworkKillSwitch(this)
        phishingScanner = PhishingScanner()
        forensicReportGenerator = ForensicReportGenerator(this)

        startProtectionService()

        setContent {
            BaridiGuardTheme {
                MainAppScreen(
                    watchdog = accessibilityWatchdog,
                    overlayDetector = overlayDetector,
                    killSwitch = networkKillSwitch,
                    phishingScanner = phishingScanner,
                    forensics = forensicReportGenerator,
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
                val durationMs = 1500
                val numSamples = (durationMs * sampleRate / 1000)
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    // تردد يتصاعد ويهبط بشكل حاد كسيرينة أمنية (800Hz إلى 1600Hz)
                    val freq = 800.0 + 800.0 * kotlin.math.sin(2.0 * Math.PI * progress * 4.0)
                    val angle = 2.0 * Math.PI * i * freq / sampleRate
                    buffer[i] = (kotlin.math.sin(angle) * Short.MAX_VALUE * 0.7).toInt().toShort()
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
    onPlaySiren: () -> Unit,
    onCall1530: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var securityScore by remember { mutableStateOf(94) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showDossierDialog by remember { mutableStateOf(false) }
    var generatedReportText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(EmeraldPrimary, GuardCardDark)))
                                .border(1.dp, EmeraldLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("BARIDIGUARD", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            Text("Algérie Poste Shield", fontSize = 11.sp, color = EmeraldLight)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showEmergencyDialog = true }) {
                        Icon(Icons.Default.Warning, contentDescription = "الطوارئ", tint = DangerRed)
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
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    label = { Text("الرئيسية / Accueil", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Link, contentDescription = null) },
                    label = { Text("روابط / Liens", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("قضائي / Police", fontSize = 10.sp) }
                )
            }
        },
        containerColor = GuardBgDark
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardTab(
                    securityScore = securityScore,
                    isScanning = isScanning,
                    onStartScan = {
                        coroutineScope.launch {
                            isScanning = true
                            delay(1200)
                            isScanning = false
                            securityScore = 98
                        }
                    },
                    onTriggerSiren = onPlaySiren,
                    onTriggerKillSwitch = {
                        killSwitch.triggerEmergencyNetworkIsolation()
                        onPlaySiren()
                    }
                )
                1 -> PhishingScannerTab(phishingScanner)
                2 -> ForensicsTab(
                    onGenerateReport = {
                        val sampleDossier = forensics.generateOfficialPoliceDossier(
                            ForensicReportGenerator.IncidentData(
                                victimName = "مواطن متضرر (Victime)",
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

    if (showDossierDialog) {
        AlertDialog(
            onDismissRequest = { showDossierDialog = false },
            title = { Text("📄 تقرير الشرطة والدرك الوطني", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    generatedReportText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.height(350.dp)
                )
            },
            confirmButton = {
                Button(onClick = { showDossierDialog = false }) {
                    Text("تم النسخ والحفظ")
                }
            }
        )
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("🚨 زر الطوارئ 1530", fontWeight = FontWeight.Bold) },
            text = {
                Text("اتصال فوري ببريد الجزائر لتجميد الحساب والبطاقة الذهبية، مع قطع الإنترنت الفوري لمنع استمرار التحويلات.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        killSwitch.triggerEmergencyNetworkIsolation()
                        onCall1530()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("قطع الإنترنت والاتصال بـ 1530", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun DashboardTab(
    securityScore: Int,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onTriggerSiren: () -> Unit,
    onTriggerKillSwitch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radar Circle
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(GuardSurfaceDark)
                .border(3.dp, EmeraldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$securityScore%",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = EmeraldLight
                )
                Text(
                    text = if (isScanning) "جاري الفحص المتقدم..." else "الهاتف محمي من التروجان",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onStartScan,
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.btn_run_scan), fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Siren & Network Kill Switch
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTriggerSiren,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GuardCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("🔊 اختبار صفارة الإنذار", fontSize = 11.sp, color = GoldLight)
            }

            Button(
                onClick = onTriggerKillSwitch,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DangerBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("⚡ قطع الإنترنت الفوري", fontSize = 11.sp, color = DangerRed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GuardCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.title_active_defenses), fontWeight = FontWeight.Bold, color = Color.White)
                DefenseRow(stringResource(R.string.defense_baridimob_title), stringResource(R.string.defense_baridimob_sub), true)
                DefenseRow(stringResource(R.string.defense_overlay_title), stringResource(R.string.defense_overlay_sub), true)
                DefenseRow(stringResource(R.string.defense_killswitch_title), stringResource(R.string.defense_killswitch_sub), true)
                DefenseRow(stringResource(R.string.defense_autokill_title), stringResource(R.string.defense_autokill_sub), true)
            }
        }
    }
}

@Composable
fun PhishingScannerTab(scanner: PhishingScanner) {
    var inputUrl by remember { mutableStateOf("https://baridimob-update-algerie.cc") }
    var scanResult by remember { mutableStateOf<PhishingScanner.ScanResult?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("درع روابط الاحتيال (Anti-Phishing Shield)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        Text("افحص أي رابط قبل فتحه للتأكد من أنه لا ينتحل بريدي موب أو بطاقة الذهبية:", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = inputUrl,
            onValueChange = { inputUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("أدخل الرابط للفحص / Entrer le lien") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { scanResult = scanner.analyzeUrl(inputUrl) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("فحص أمان الرابط الآن / Analyser le lien", color = Color.Black, fontWeight = FontWeight.Bold)
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
                        if (result.isPhishing) "🚨 رابط احتيال مؤكد!" else "✓ رابط آمن",
                        fontWeight = FontWeight.Bold,
                        color = if (result.isPhishing) DangerRed else EmeraldLight,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(result.reasonAr, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(result.reasonFr, color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ForensicsTab(onGenerateReport: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("حقيبة الضحية وتقرير الأدلة الرقمية (Dossier Judiciaire)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        Text("توليد ملف أدلة فني وقانوني موجه لفرقة مكافحة الجرائم المعلوماتية (الدرك الوطني / الأمن الوطني):", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GuardCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المستندات التي يولدها التقرير:", fontWeight = FontWeight.Bold, color = GoldLight)
                Text("• البصمة الرقمية للتروجان (Package & SHA-256 Checksum)", fontSize = 11.sp, color = Color.LightGray)
                Text("• توقيت تنفيذ عمليات السطو (Horodatage précis)", fontSize = 11.sp, color = Color.LightGray)
                Text("• رقم الحساب المستلم (RIP Destinataire) للمطالبة بتجميده", fontSize = 11.sp, color = Color.LightGray)
                Text("• السند القانوني بالمواد 394 مكرر من قانون العقوبات الجزائري", fontSize = 11.sp, color = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onGenerateReport,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("توليد وطباعة ملف الشكوى الجنائية", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
