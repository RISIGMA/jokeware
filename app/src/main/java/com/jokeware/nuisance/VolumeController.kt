package com.jokeware.nuisance

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

/**
 * Controller yang bertanggung jawab mengelola Media Volume (STREAM_MUSIC).
 * Menggunakan ContentObserver untuk mengunci volume secara instan (real-time enforcement)
 * saat pengguna menekan tombol volume fisik turun.
 */
class VolumeController(private val context: Context) {

    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var originalVolume: Int? = null
    private var volumeObserver: ContentObserver? = null
    private var isLocking = false

    companion object {
        private const val TAG = "VolumeController"
    }

    /**
     * Menyimpan nilai volume media saat ini sebelum prank dimulai.
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
     * Memaksimalkan volume media (STREAM_MUSIC) ke level tertinggi.
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
     * Mengaktifkan ContentObserver agar setiap perubahan volume media
     * langsung dikembalikan ke level maksimal seketika.
     */
    fun startInstantVolumeLock() {
        if (isLocking) return
        isLocking = true
        maximizeMediaVolume()

        val handler = Handler(Looper.getMainLooper())
        volumeObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (isLocking) {
                    audioManager?.let { am ->
                        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        if (current < max) {
                            Log.d(TAG, "Volume reduced to $current. Re-enforcing max volume!")
                            maximizeMediaVolume()
                        }
                    }
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver!!
            )
            Log.d(TAG, "Volume ContentObserver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register volume ContentObserver", e)
        }
    }

    /**
     * Menghapus ContentObserver.
     */
    fun stopVolumeLock() {
        isLocking = false
        volumeObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
                Log.d(TAG, "Volume ContentObserver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister volume ContentObserver", e)
            }
        }
        volumeObserver = null
    }

    /**
     * Mengembalikan volume media ke level awal yang tercatat.
     */
    fun restoreVolume() {
        stopVolumeLock()
        audioManager?.let { am ->
            originalVolume?.let { savedVol ->
                try {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, savedVol, 0)
                    Log.d(TAG, "Restored original media volume to $savedVol")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore original media volume", e)
                }
            } ?: Log.w(TAG, "No original volume recorded, skipping restore")
        } ?: Log.e(TAG, "AudioManager is null or unavailable")
    }
}
