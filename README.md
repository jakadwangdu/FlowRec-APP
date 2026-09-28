# FlowRec Android 📱

> Minimal screen recorder with powerful editing features — built for creators.

[![GitHub Release](https://img.shields.io/github/v/release/jakadwangdu/FlowRec-APP?style=flat-square&color=black)](https://github.com/jakadwangdu/FlowRec-APP/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/jakadwangdu/FlowRec-APP/build-apk.yml?branch=main&style=flat-square)](https://github.com/jakadwangdu/FlowRec-APP/actions)
[![Platform](https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android)](https://www.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

---

## ℹ️ About
FlowRec Android is a modern, high-performance native screen recorder and studio video editor built with **Kotlin** and **Jetpack Compose Material 3**. Inspired by minimalist design aesthetics, FlowRec gives content creators, educators, and developers the tools to capture high-framerate screen recordings, apply smart zoom and cursor highlights, customize keyframe timelines, and export crisp MP4 videos.

### 📥 Download APK (Latest Release)

[![Download APK](https://img.shields.io/badge/Download-APK%20(Latest)-000000?style=for-the-badge&logo=android&logoColor=white)](https://github.com/jakadwangdu/FlowRec-APP/releases/latest)

* **Releases & Download:** [**GitHub Releases (Latest APK)**](https://github.com/jakadwangdu/FlowRec-APP/releases/latest)

---

## ✨ Key Features

- 🌓 **Dynamic System Theme (Auto Dark / Light):** Follows your Android device's system appearance by default. If your phone is set to Dark Mode, the app seamlessly runs in high-contrast OLED Dark; if in Light Mode, it runs in clean, minimalist Light. Can also be manually switched in Settings.
- 🔴 **High-Fidelity Screen Capture:** Native `MediaProjection` and hardware `MediaCodec` pipeline supporting 720p, 1080p, and native resolutions up to 60 FPS.
- 🎙️ **Dual Audio Recording:** Capture high-definition microphone commentary alongside internal device audio.
- ⏱️ **Non-Intrusive Floating HUD:** Minimal top REC pill (`• REC 02:17`) and bottom capsule dock (Pause, Stop, Hide) that stays out of your recording content.
- 🎞️ **Multi-Track Timeline Editor:** Visual keyframe editor with tracks for Video, Cursor, Zoom, Effects, and real-time audio waveform visualization.
- 🎯 **Cursor & Zoom FX:** Customizable cursor styles, highlight halos, and click-triggered zooms with smooth easing transitions.
- 🚀 **Fast Deterministic Export:** Export straight to MP4 with customizable bitrate, presets (YouTube 1080p, Instagram Reel, Twitter/X HD), and hardware acceleration.

---

## 📱 13 Screen Layouts

1. **Home Screen** — Hero card ("Record. Edit. Share."), "+ New Recording" CTA, recent projects list, floating capsule dock.
2. **New Recording** — Capture target cards (Screen, Window, Tab), resolution, framerate, and audio selectors.
3. **Screen Selection** — Display preview mockup with source selector options (This Screen, Front Camera, Specific App).
4. **Countdown** — Sleek dark ambient 3-2-1 timer with circular progress and "Get Ready".
5. **Recording HUD** — Top floating status pill and bottom floating capsule controller.
6. **Project Library** — Category filters (All, Videos, Projects), search bar, and recordings list.
7. **Project Details** — Video preview player, metadata specs, quick action buttons (Favorite, Share, Duplicate, Edit).
8. **Editor** — Video canvas preview, playhead timecode, zoom controls, and multi-track waveform.
9. **Effects Panel** — Cursor style & size, Click Zoom duration & easing, and Motion Blur controls.
10. **Timeline Screen** — Thumbnail filmstrip, marker insertion, clip splitting, and track management.
11. **Export Video** — Social media presets, format dropdowns, and quality options.
12. **Settings** — Theme selector (System Default / Light / Dark), defaults, and storage manager.
13. **Success Screen ("Video Ready!")** — Radiating checkmark badge, exported file card, Open File & Share buttons.

---

## 🛠️ Building From Source

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK 36 (minSdk 24)

### Clone & Build
```bash
git clone https://github.com/jakadwangdu/FlowRec-APP.git
cd FlowRec-APP

# Build debug APK locally
./gradlew :app:assembleDebug
```

The compiled APK will be output at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
Created for creators with ❤️ by Jakad Wangdu.
