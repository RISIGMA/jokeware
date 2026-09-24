package com.jokeware.nuisance

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * MainActivity:
 * - 100% transparan secara visual.
 * - Menggunakan touch pass-through (FLAG_NOT_TOUCHABLE dan FLAG_NOT_FOCUSABLE)
 *   sehingga semua sentuhan layar langsung tembus ke launcher / aplikasi lain.
 * - Memulai PrankAudioService (Foreground Service) untuk memutar audio di latar belakang.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi Window: 100% Transparan + Tembus Sentuhan (Click-Through)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        Log.d(TAG, "onCreate: Initialized transparent touch-through window")

        // Memulai Foreground Audio Service
        val serviceIntent = Intent(this, PrankAudioService::class.java).apply {
            action = PrankAudioService.ACTION_START
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        // Tutup dummy launcher activity secara mulus sehingga tidak ada task card di recent apps
        finishAndRemoveTask()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity active")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Activity destroyed")
    }
}
