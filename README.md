# Jokeware / Nuisanceware (Experimental Project)

Proyek aplikasi Android eksperimental berbasis **Kotlin**, **Gradle CLI**, dan **Antigravity IDE**.

---

## 📁 Struktur Project

```text
C:\joke/
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/jokeware/nuisance/
│           │       ├── MainActivity.kt
│           │       ├── PrankController.kt
│           │       └── VolumeController.kt
│           └── res/
│               ├── values/
│               │   ├── strings.xml
│               │   ├── colors.xml
│               │   └── themes.xml
│               └── raw/
│                   └── README.txt (tempat file prank_audio.*)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── prompt.txt
```

---

## 🛠️ Persyaratan Lingkungan (Toolchain)

1. **JDK (Java Development Kit)**: Versi 17 atau lebih baru.
   - Pastikan path JDK ditambahkan ke environment variable `JAVA_HOME` dan `PATH`.
   - Cek versi:
     ```powershell
     java -version
     ```
2. **Android SDK & Build Tools**:
   - Pastikan variabel `ANDROID_HOME` mengarah ke folder Android SDK Anda (misal: `C:\Users\<User>\AppData\Local\Android\Sdk`).
   - Tambahkan `platform-tools` ke `PATH` agar command `adb` dapat diakses dari terminal.
   - Cek ADB:
     ```powershell
     adb version
     ```

---

## ⚙️ Kompilasi & Build via Gradle CLI

Build APK sepenuhnya melalui terminal:

### 1. Build APK Debug
```powershell
.\gradlew.bat assembleDebug
```
*(Untuk Linux/macOS gunakan `./gradlew assembleDebug`)*

Lokasi output file APK:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Clean Project
```powershell
.\gradlew.bat clean
```

### 3. Build dengan Informasi Debug / Stacktrace
```powershell
.\gradlew.bat assembleDebug --stacktrace
```

---

## 📱 Deployment via ADB

### 1. Cek Koneksi Perangkat / Emulator
```powershell
adb devices
```

### 2. Install APK ke Perangkat
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 3. Uninstall Aplikasi (jika diperlukan)
```powershell
adb uninstall com.jokeware.nuisance
```

---

## 🔍 Debugging & Monitoring (Logcat)

Untuk memantau log aktivitas lifecycle dan controller volume:

```powershell
adb logcat -s MainActivity VolumeController PrankController
```

---

## 🧠 Penjelasan Arsitektur & Perilaku

### 1. Transparent UI (100% Transparan)
- Window tema didefinisikan menggunakan `@style/Theme.JokeApp` dengan atribut `android:windowIsTranslucent="true"`, `android:windowBackground="@android:color/transparent"`, dan `android:backgroundDimEnabled="false"`.
- Layar terlihat seperti tidak membuka aplikasi apa pun dan tampilan aplikasi di belakangnya tetap terlihat normal.

### 2. Volume Recovery & Lifecycle
- **`onResume()`**: Menangkap nilai awal media volume (`originalVolume`), lalu menaikkan media volume ke level maksimal (`maximizeMediaVolume()`) dan menjalankan audio loop.
- **`onPause()` / `onStop()` / `onDestroy()`**: Menghentikan seluruh audio, membatalkan coroutine loop, dan langsung mengembalikan media volume ke nilai awal (`restoreVolume()`).
- **Tanpa Persistensi Latar Belakang**: Tidak menggunakan Service tersembunyi, `BOOT_COMPLETED`, WorkManager, maupun background persistence. Ketika user meninggalkan activity, seluruh prank dihentikan total.
