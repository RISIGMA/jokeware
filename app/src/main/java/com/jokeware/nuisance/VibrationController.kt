package com.jokeware.nuisance

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Controller untuk mengelola getaran (haptic feedback) berpola intensif selama prank aktif.
 */
class VibrationController(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var isVibrating = false

    companion object {
        private const val TAG = "VibrationController"
        // Pola denyut getaran: jeda 100ms, getar 350ms, jeda 150ms, getar 500ms
        private val VIBRATE_PATTERN = longArrayOf(100, 350, 150, 500)
        // Amplitudo (intensitas): 0 (silent), 255 (max), 0 (silent), 255 (max)
        private val VIBRATE_AMPLITUDE = intArrayOf(0, 255, 0, 255)
    }

    /**
     * Memulai pola getar berulang (looping).
     */
    fun startVibrationPattern() {
        vibrator?.let { vib ->
            if (!vib.hasVibrator()) {
                Log.w(TAG, "Device does not have vibration hardware")
                return
            }
            if (isVibrating) return

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(VIBRATE_PATTERN, VIBRATE_AMPLITUDE, 0) // 0 = repeat index
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(VIBRATE_PATTERN, 0)
                }
                isVibrating = true
                Log.d(TAG, "Vibration pattern started (looping)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start vibration pattern", e)
            }
        } ?: Log.e(TAG, "Vibrator service not available")
    }

    /**
     * Menghentikan semua getaran.
     */
    fun stopVibration() {
        if (!isVibrating) return
        try {
            vibrator?.cancel()
            isVibrating = false
            Log.d(TAG, "Vibration cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop vibration", e)
        }
    }
}
