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
 * Foreground Service untuk mengelola pemutaran media dan kontrol volume di latar belakang.
 * Sesuai standar Android 10+, service ini menampilkan notifikasi sistem aktif di status bar.
 */
class PrankAudioService : Service() {

    private lateinit var volumeController: VolumeController
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
        private const val CHANNEL_NAME = "Media Playback Service"
        private const val VOLUME_INTERVAL_MS = 1000L
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing PrankAudioService")
        volumeController = VolumeController(this)
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
            Log.d(TAG, "onStartCommand: Starting foreground service")

            val notification = buildForegroundNotification()
            startForeground(NOTIFICATION_ID, notification)

            // Simpan volume awal dan maksimalkan volume
            volumeController.captureOriginalVolume()
            volumeController.maximizeMediaVolume()

            // Mulai audio
            startAudioPlayback()

            // Loop untuk menjaga volume selama service aktif
            serviceScope = CoroutineScope(Dispatchers.Main + Job())
            serviceScope?.launch {
                while (isActive && isRunning) {
                    volumeController.maximizeMediaVolume()
                    delay(VOLUME_INTERVAL_MS)
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
                Log.d(TAG, "MediaPlayer started successfully: isPlaying = ${mediaPlayer?.isPlaying}")
            } else {
                Log.d(TAG, "Playing tone fallback")
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio in service", e)
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, -1)
            } catch (ex: Exception) {
                Log.e(TAG, "Error fallback tone", ex)
            }
        }
    }

    private fun stopAudioPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null

            toneGenerator?.let {
                it.stopTone()
                it.release()
            }
            toneGenerator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio in service", e)
        }
    }

    private fun stopServiceCleanly() {
        isRunning = false
        serviceScope?.cancel()
        serviceScope = null

        stopAudioPlayback()
        volumeController.restoreVolume()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status pemutaran audio latar belakang"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
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
            .setContentTitle("Audio Service Running")
            .setContentText("Background media playback is active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: Activity swiped away, service keeps playing in background")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up service")
        stopServiceCleanly()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
