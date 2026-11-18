# Rencana Pengembangan Jokeware / Nuisanceware

Dokumen ini mencatat rencana spesifikasi fitur lengkap yang telah disepakati untuk pengembangan proyek Jokeware.

---

## 📋 Daftar Fitur yang Disepakati

### 1. 📳 Sensory Enhancements (Getar, Volume & Flashlight)
- **Pola Getar Berdenyut (`VibrationController`):**
  - Menggunakan API `Vibrator` / `VibrationEffect` dengan pola denyut ritmik cepat.
  - Membutuhkan izin `android.permission.VIBRATE` (izin normal tanpa prompt pengguna).
- **Instant Volume Lock (`VolumeController` via `ContentObserver`):**
  - Memantau perubahan setting audio secara realtime.
  - Mengembalikan volume media ke 100% instan bila tombol volume fisik bawah ditekan.
- **Flashlight Strobe Effect (`FlashlightController`):**
  - Mengontrol lampu kilat (LED flash) kamera belakang menggunakan `CameraManager.setTorchMode(cameraId, true/false)`.
  - Berkedip berulang (*strobe*) selama siklus prank berlangsung.
  - Memerlukan `<uses-permission android:name="android.permission.CAMERA" />` dan `<uses-feature android:name="android.hardware.camera.flash" />`.

### 2. 🎭 Visual Gimmicks & Fake UI Overlay
- **Fake System Dialog Alert:**
  - Dialog tiruan bergaya sistem Android asli (*"System UI Not Responding"* atau *"Critical System Error"*).
  - Dilengkapi tombol interaktif palsu (*"Close App"*, *"Wait"*).
  - Perilaku prank: Tombol *"Close App"* melompat/berpindah posisi saat disentuh atau memicu respon error *"Failed to close process"*.
- **Mock Doxing Terminal Screen:**
  - Layar latar belakang bergaya terminal hacker dengan data perangkat publik:
    - Merk & Model perangkat (`Build.MANUFACTURER`, `Build.MODEL`)
    - Versi Android (`Build.VERSION.RELEASE`)
    - Persentase baterai saat ini via `BatteryManager`
  - Teks running scanning dramatis untuk ilusi doxing.

### 3. 🏷️ Camouflage & Launcher Disguise
- **Nama & Ikon Launcher:**
  - Menyamar sebagai aplikasi utilitas sistem (misal: *"System Tool"*, *"Device Care"*, atau *"Cleaner"*).
  - Icon launcher disesuaikan agar tidak mencolok.

### 4. 🛡️ Safety & Clean Recovery
- **Auto-Stop Timeout:**
  - Timer pengaman otomatis (misal 60 detik) untuk menghentikan audio, mematikan getar, mematikan flash, dan mengembalikan volume.
- **Foreground Notification Controller:**
  - Tetap menyediakan tombol "Stop" resmi di panel notifikasi agar sesi bisa dihentikan kapan saja secara aman.
- **Volume Restoration:**
  - Memulihkan volume media ke nilai awal pengguna saat aplikasi dihentikan.

---

## 🛠️ Rencana Tahapan Eksekusi
1. Sensory Update (VibrationController + FlashlightController + ContentObserver Volume)
2. Fake Dialog & Mock Doxing UI (Activity Layout & Interaction)
3. Service Orchestration & Safety Timeout (Integrasi ke PrankAudioService)
4. Camouflage Icon & Metadata Finalization
5. Build & Verifikasi Kompilasi (Gradle)
