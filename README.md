# ResetGuest

[![Platform](https://img.shields.io/badge/Platform-Android%2026%2B-green)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-orange)](https://developer.android.com/jetpack/compose)
[![Root](https://img.shields.io/badge/Requires-Root-red)](https://topjohnwu.github.io/Magisk/)

Aplikasi Android untuk mereset akun guest Mobile Legends via root shell dengan satu ketukan. Menghapus shared preferences ML dan menghentikan paksa proses MT Manager.

## Requirements

- Android 8.0+ (API 26)
- Root access (Magisk / KSU / SuperSU)
- Android Studio Hedgehog 2023.1.1+ atau lebih baru
- JDK 17

## Cara Build

```bash
# 1. Clone / ekstrak project
cd ResetGuest

# 2. Buka di Android Studio:
#    File → Open → pilih folder ResetGuest

# 3. Sync Gradle (otomatis saat pertama buka)

# 4. Build APK:
#    Build → Build Bundle(s) / APK(s) → Build APK(s)
#    Output: app/build/outputs/apk/debug/app-debug.apk

# 5. Install ke device:
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Struktur Project

```
ResetGuest/
├── app/
│   ├── src/main/
│   │   ├── java/com/resetguest/
│   │   │   ├── MainActivity.kt       # Entry point, Compose host
│   │   │   ├── MainViewModel.kt      # State management & business logic
│   │   │   ├── ResetGuestApp.kt      # Seluruh UI Compose
│   │   │   └── RootExecutor.kt       # Root shell execution engine
│   │   ├── res/
│   │   │   ├── drawable/             # Vector icons
│   │   │   ├── mipmap-anydpi-v26/    # Adaptive launcher icon
│   │   │   └── values/               # strings, themes
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
└── README.md
```

## Script yang Dieksekusi

```sh
rm -rf /data/user/0/com.mobile.legends/shared_prefs
rm -rf /data/user/0/com.google.android.gms/shared_prefs
sleep 0.5
am force-stop bin.mt.plus
sleep 0.5
am force-stop bin.mt.plus.canary
sleep 0.5
am force-stop bin.mt.pro
```

## Arsitektur

```
MainActivity
    └── ResetGuestApp (Compose UI)
            └── MainViewModel (StateFlow)
                    └── RootExecutor (suspend fun)
                            └── Runtime.exec("su") → shell
```

## Fitur UI

- Status root real-time (cek UID 0)
- Script preview collapsible
- Tombol eksekusi dengan animasi state
- Live execution log dengan timestamp
- Auto-scroll log ke bawah
- Refresh root check kapan saja

## Lisensi

Untuk penggunaan pribadi. Tidak untuk distribusi komersial.
