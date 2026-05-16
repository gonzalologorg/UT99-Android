# Unreal Tournament (UT99) Android

![Platform](https://img.shields.io/badge/platform-Android-green)
![Engine](https://img.shields.io/badge/engine-Unreal%20Engine%201-blue)
![Renderer](https://img.shields.io/badge/renderer-OpenGL%20ES%202.0-lightgrey)
![ABI](https://img.shields.io/badge/ABI-armeabi--v7a-orange)
![Controller](https://img.shields.io/badge/controller-supported-blueviolet)

**Unreal Tournament Android** is an Android port of **Unreal Tournament / UT99 (1999)** based on the classic **Unreal Engine 1** source code.

The goal of this project is to make the original Unreal Tournament playable on Android devices, including legacy Android hardware such as the **OUYA console**, while preserving the classic look and feel of the PC version.

Original Unreal Tournament game data is **not included**.  
You need a valid PC installation of Unreal Tournament / UT99 and must import your own game data.

> [!IMPORTANT]
> This project is for preservation, experimentation and personal use only.  
> Unreal Tournament, Unreal Engine and related trademarks are owned by Epic Games.  
> This project is not affiliated with or endorsed by Epic Games.

---

## ◈ Features

- Runs Unreal Tournament / UT99 on Android.
- Supports modern Android devices and older Android hardware.
- OUYA-compatible launcher entry.
- Android-side installer / preflight screen.
- Import game data from:
  - an extracted Unreal Tournament folder
  - a ZIP file containing the game data
- Automatic copy/extraction into the app's private data folder.
- OpenGL ES 2.0 rendering.
- SDL2 based native runtime.
- Controller-friendly default layout.
- Landscape fullscreen presentation.
- Legacy storage behavior kept friendly for old sideload devices.

> [!NOTE]
> This is still a work-in-progress port.  
> Expect occasional issues, especially on very old Android devices or unusual controller mappings.

---

## ▣ Requirements

- Android device with OpenGL ES 2.0 support.
- Android 4.1 / API 16 or newer.
- ARMv7 compatible device for the current build.
- Android-compatible game controller recommended.
- Original Unreal Tournament / UT99 PC game data v400.

Required game data folders:

```text
UnrealTournament/
├── System/
├── Maps/
├── Textures/
├── Sounds/
└── Music/
