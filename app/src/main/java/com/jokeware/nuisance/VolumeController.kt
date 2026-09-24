package com.jokeware.nuisance

import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * Controller yang bertanggung jawab secara khusus untuk mengelola Media Volume (STREAM_MUSIC).
 * Mengontrol pembacaan volume awal, memaksimalkan volume, dan pemulihan (recovery) volume.
 */
class VolumeController(private val context: Context) {

    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var originalVolume: Int? = null

    companion object {
        private const val TAG = "VolumeController"
    }

    /**
     * Menyimpan nilai volume media saat ini sebelum efek prank dimulai.
     * Nilai disimpan hanya satu kali per sesi aktif agar tidak ter-overwrite.
     */
    fun captureOriginalVolume() {
        audioManager?.let { am ->
            if (originalVolume == null) {
                originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                Log.d(TAG, "Captured original media volume: $originalVolume / $maxVolume")
            }
        } ?: Log.e(TAG, "AudioManager is null or unavailable")
    }

    /**
     * Memaksimalkan volume media (STREAM_MUSIC) ke level tertinggi yang diizinkan oleh sistem Android.
     * Tidak mengubah volume stream lain (ringtone, notifikasi, alarm).
     */
    fun maximizeMediaVolume() {
        audioManager?.let { am ->
            try {
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
                Log.d(TAG, "Media volume set to maximum ($maxVolume)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set media volume to maximum", e)
            }
        } ?: Log.e(TAG, "AudioManager is null or unavailable")
    }

    /**
     * Mengembalikan volume media ke level awal yang tercatat sebelum prank dimulai.
     */
    fun restoreVolume() {
        audioManager?.let { am ->
            originalVolume?.let { savedVol ->
                try {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, savedVol, 0)
                    Log.d(TAG, "Restored original media volume to $savedVol")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore original media volume", e)
                } finally {
                    originalVolume = null
                }
            }
        } ?: Log.e(TAG, "AudioManager is null or unavailable")
    }

    /**
     * Mendapatkan status volume saat ini untuk keperluan debugging / logging.
     */
    fun getCurrentVolumeInfo(): String {
        return audioManager?.let {
            val current = it.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            "Current: $current / Max: $max (Original: $originalVolume)"
        } ?: "AudioManager unavailable"
    }
}
