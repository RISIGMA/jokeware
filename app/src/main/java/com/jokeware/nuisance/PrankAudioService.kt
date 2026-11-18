package com.jokeware.nuisance

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service terisolasi untuk mengelola audio, getaran, flashlight strobe,
 * dan volume lock dengan pengaman auto-stop timeout.
 */
class PrankAudioService : Service() {

    private lateinit var volumeController: VolumeController
    private lateinit var vibrationController: VibrationController
    private lateinit var flashlightController: FlashlightController

    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var serviceScope: CoroutineScope? = null
    private var isRunning = false

    companion object {
        private const val TAG = "PrankAudioService"
        const val ACTION_START = "com.jokeware.nuisance.START"
        const val ACTION_STOP = "com.jokeware.nuisance.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "prank_audio_channel"
        private const val CHANNEL_NAME = "System Diagnostic Service"
        private const val AUTO_STOP_TIMEOUT_MS = 60_000L // 60 detik safety kill-switch
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing PrankAudioService")
        volumeController = VolumeController(this)
        vibrationController = VibrationController(this)
        flashlightController = FlashlightController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            Log.d(TAG, "onStartCommand: Received STOP action")
            stopServiceCleanly()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            isRunning = true
            Log.d(TAG, "onStartCommand: Starting foreground service components")

            val notification = buildForegroundNotification()
            startForeground(NOTIFICATION_ID, notification)

            // 1. Simpan volume awal dan jalankan instant lock observer
            volumeController.captureOriginalVolume()
            volumeController.startInstantVolumeLock()

            // 2. Jalankan audio playback
            startAudioPlayback()

            // 3. Jalankan pola getar
            vibrationController.startVibrationPattern()

            // 4. Jalankan coroutine scope untuk Flashlight Strobe dan Auto-Stop Timer
            serviceScope = CoroutineScope(Dispatchers.Main + Job())
            serviceScope?.let { scope ->
                flashlightController.startStrobe(scope)

                // Timer pengaman (Auto-stop setelah 60 detik)
                scope.launch {
                    delay(AUTO_STOP_TIMEOUT_MS)
                    if (isActive && isRunning) {
                        Log.w(TAG, "Auto-stop safety timeout reached ($AUTO_STOP_TIMEOUT_MS ms). Stopping service.")
                        stopServiceCleanly()
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun startAudioPlayback() {
        try {
            val resId = resources.getIdentifier("prank_audio", "raw", packageName)
            if (resId != 0) {
                Log.d(TAG, "Playing audio from res/raw/prank_audio (resId: $resId)")
                mediaPlayer = MediaPlayer.create(this, resId)?.apply {
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    start()
                }
            } else {
                Log.w(TAG, "res/raw/prank_audio not found, falling back to ToneGenerator")
                playToneGeneratorFallback()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer initialization error, falling back to ToneGenerator", e)
            playToneGeneratorFallback()
        }
    }

    private fun playToneGeneratorFallback() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 30_000)
            Log.d(TAG, "ToneGenerator fallback active")
        } catch (e: Exception) {
            Log.e(TAG, "ToneGenerator fallback failed", e)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val stopIntent = Intent(this, PrankAudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Service Active")
            .setContentText("Device diagnostic and background monitoring active.")
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status for background system services"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopServiceCleanly() {
        isRunning = false

        // Hentikan coroutine & auto-stop timer
        serviceScope?.cancel()
        serviceScope = null

        // Hentikan flash
        flashlightController.stopStrobe()

        // Hentikan getar
        vibrationController.stopVibration()

        // Hentikan MediaPlayer
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null

        // Hentikan ToneGenerator
        toneGenerator?.let {
            try {
                it.stopTone()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing ToneGenerator", e)
            }
        }
        toneGenerator = null

        // Kembalikan volume & copot observer
        volumeController.restoreVolume()

        // Hentikan foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        Log.d(TAG, "PrankAudioService cleanly terminated")
    }

    override fun onDestroy() {
        if (isRunning) {
            stopServiceCleanly()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
