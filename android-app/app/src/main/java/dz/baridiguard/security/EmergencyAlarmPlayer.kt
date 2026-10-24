package dz.baridiguard.security

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.concurrent.thread

/**
 * EmergencyAlarmPlayer
 * 
 * مشغل الإنذار الأمني الذكي (Smart Auto-Stopping Alarm).
 * يضمن إطلاق إنذار قوي ومسموع لمدة محددة (3.5 ثوانٍ تلقائياً) ثم يتوقف فوراً،
 * مع إمكانية الإيقاف اليدوي الفوري بنقرة واحدة (Stop Alarm).
 */
object EmergencyAlarmPlayer {

    @Volatile
    var isAlarmPlaying: Boolean = false
        private set

    private var activeRingtone: Ringtone? = null
    private var activeToneGen: ToneGenerator? = null
    private var activeAudioTrack: AudioTrack? = null
    private var activeVibrator: Vibrator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    /**
     * تشغيل الإنذار الأمني لمدة محددة (افتراضياً 3.5 ثانية) ثم التوقف التلقائي
     */
    @Synchronized
    fun playAudibleAlarm(context: Context, durationMs: Long = 3500) {
        // إيقاف أي صوت سابق أولاً
        stopAlarm()

        isAlarmPlaying = true

        thread(start = true, isDaemon = true) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                // رفع مستوى الصوت لحالة الخطر
                audioManager?.let { am ->
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val targetVol = (maxVol * 0.85).toInt().coerceAtLeast(1)
                    am.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                }

                // 1. تشغيل ToneGenerator الرسمي للأجهزة
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 90)
                    activeToneGen = toneGen
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, durationMs.toInt())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. تشغيل نغمة المنبه مع حفظ المرجع لإيقافها
                try {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val ringtone = RingtoneManager.getRingtone(context, alertUri)
                    activeRingtone = ringtone
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone?.isLooping = false
                    }
                    ringtone?.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. هزاز متقطع لشد الانتباه
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    activeVibrator = vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 300, 200, 300, 200, 400),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(longArrayOf(0, 300, 200, 300, 200, 400), -1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 4. جدولة الإيقاف التلقائي بعد انتهاء المدة (Auto-Stop Timer)
                mainHandler.post {
                    autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
                    autoStopRunnable = Runnable {
                        stopAlarm()
                    }
                    mainHandler.postDelayed(autoStopRunnable!!, durationMs)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                stopAlarm()
            }
        }
    }

    /**
     * إيقاف الإنذار فوراً وإسكات كافة الأصوات والهزازات
     */
    @Synchronized
    fun stopAlarm() {
        try {
            autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
            autoStopRunnable = null

            // إيقاف النغمة
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            activeRingtone = null

            // إيقاف ToneGenerator
            activeToneGen?.let {
                it.stopTone()
                it.release()
            }
            activeToneGen = null

            // إيقاف AudioTrack
            activeAudioTrack?.let {
                it.stop()
                it.release()
            }
            activeAudioTrack = null

            // إيقاف الهزاز
            activeVibrator?.cancel()
            activeVibrator = null

            isAlarmPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
            isAlarmPlaying = false
        }
    }
}
