package com.jokeware.nuisance

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controller utama yang mengoordinasikan efek prank:
 * - Mengatur siklus hidup prank (Start & Stop)
 * - Mengatur audio playback (MediaPlayer dari raw resource atau ToneGenerator fallback)
 * - Mengontrol loop simulasi penguncian volume maksimum selama Activity berada di foreground
 * - Melakukan pembersihan resource secara total saat Activity tidak aktif
 */
class PrankController(private val context: Context) {

    private val volumeController = VolumeController(context)
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var prankScope: CoroutineScope? = null
    private var isPrankActive = false

    companion object {
        private const val TAG = "PrankController"
        private const val VOLUME_CHECK_INTERVAL_MS = 500L
    }

    /**
     * Memulai efek prank:
     * 1. Simpan volume media awal.
     * 2. Naikkan volume media ke batas maksimum.
     * 3. Mainkan audio prank dari resource lokal.
     * 4. Jalankan periodic loop untuk menjaga volume tetap maksimum selama sesi foreground.
     */
    fun startPrank() {
        if (isPrankActive) return
        isPrankActive = true
        Log.d(TAG, "Starting prank session...")

        // 1. Catat volume media awal sebelum diubah
        volumeController.captureOriginalVolume()

        // 2. Maksimalkan volume media
        volumeController.maximizeMediaVolume()

        // 3. Putar audio
        playAudio()

        // 4. Simulasi lock volume: lakukan periodic re-maximize selama Activity aktif di foreground
        prankScope = CoroutineScope(Dispatchers.Main + Job())
        prankScope?.launch {
            while (isActive && isPrankActive) {
                volumeController.maximizeMediaVolume()
                delay(VOLUME_CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * Menghentikan efek prank dan melakukan cleanup:
     * 1. Hentikan periodic loop coroutine.
     * 2. Hentikan dan lepaskan resource audio player.
     * 3. Kembalikan volume media ke nilai awal (Recovery).
     */
    fun stopPrank() {
        if (!isPrankActive) return
        Log.d(TAG, "Stopping prank session and cleaning up resources...")
        isPrankActive = false

        // 1. Batalkan loop
        prankScope?.cancel()
        prankScope = null

        // 2. Hentikan audio
        stopAudio()

        // 3. Restorasi volume ke kondisi awal
        volumeController.restoreVolume()
    }

    private fun playAudio() {
        try {
            // Periksa apakah ada resource raw/prank_audio yang tersedia di package
            val resId = context.resources.getIdentifier("prank_audio", "raw", context.packageName)
            if (resId != 0) {
                Log.d(TAG, "Playing audio from res/raw/prank_audio")
                mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    start()
                }
            } else {
                // Fallback otomatis menggunakan sistem ToneGenerator jika tidak ada file audio eksternal
                Log.d(TAG, "No raw audio found; falling back to ToneGenerator")
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing or playing prank audio", e)
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            toneGenerator?.let {
                it.stopTone()
                it.release()
            }
            toneGenerator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio resources", e)
        }
    }
}
