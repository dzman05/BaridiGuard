package dz.baridiguard.security

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.concurrent.thread

/**
 * EmergencyAlarmPlayer
 * 
 * مشغل الإنذار الأمني الفائق (Triple-Redundancy Cyber Alarm).
 * يضمن إطلاق الصوت بأعلى شدة ممكنة (100% Volume) مهما كانت إعدادات هاتف المستخدم:
 * 1. ToneGenerator: صفارات الطوارئ من نظام أندرويد مباشرة (تخترق وضع الصامت).
 * 2. RingtoneManager / MediaPlayer: نغمة إنذار الهاتف الرسمية بأقصى مستوى.
 * 3. AudioTrack: مولّد الصفارة الرقمية ثنائية التردد (Cyber Siren Synthesizer).
 * 4. Vibrator: نمط اهتزاز استنفار قوي لشد انتباه الضحية.
 */
object EmergencyAlarmPlayer {

    fun playAudibleAlarm(context: Context, durationMs: Long = 3000) {
        thread(start = true, isDaemon = true) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                // ضبط صوت المنبه على أقصى درجة في لحظة الخطر
                audioManager?.let { am ->
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                }

                // 1. تشغيل ToneGenerator (الأضمن على الإطلاق ويعمل على كل هواتف أندرويد)
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1800)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. تشغيل نغمة إنذار الهاتف الرسمية
                try {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, alertUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone?.isLooping = false
                    }
                    ringtone?.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. هزاز الاستنفار القوي
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 400, 200, 400, 200, 600),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 4. صفارة الترددات السيبرانية الحادة (Synthesized Cyber Siren)
                playSynthesizedSiren()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playSynthesizedSiren() {
        try {
            val sampleRate = 44100
            val durationMs = 1500
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val numSamples = (durationMs * sampleRate / 1000).toInt()
            val totalSize = kotlin.math.max(numSamples, minBuf)
            val buffer = ShortArray(totalSize)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                // موجة تصاعدية وتنازلية حادة بين 1100Hz و 1800Hz
                val freq = 1100.0 + 700.0 * kotlin.math.sin(2.0 * Math.PI * progress * 8.0)
                val angle = 2.0 * Math.PI * i * freq / sampleRate
                buffer[i] = (kotlin.math.sin(angle) * Short.MAX_VALUE * 0.9).toInt().toShort()
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
            track.setVolume(1.0f)
            track.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
