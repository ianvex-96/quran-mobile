# Qur'an Mobile

**Qur'an Mobile** (`com.quranmobile.app`) is a Quran application for Android, developed by **Ilman Nafi'an** as a fork of [Quran for Android](https://github.com/quran/quran_android) by quran.com.

## Features

- Complete Qur'an (114 surah) with Arabic text
- Indonesian translation (Kemenag), bundled offline and enabled by default
- Indonesian tafsir (Al-Mukhtashar), bundled offline
- Per-ayah audio recitation with streaming and offline download
- Multiple qari/reciter support
- Juz navigation and memorization helpers (ayah/range repeat and playback controls)
- Bookmarks and last-read position
- Search
- Dark mode

## Developer

- **Ilman Nafi'an**
- Facebook: https://www.facebook.com/ianvex.id

## Credits & Attribution

This project is based on [quran/quran_android](https://github.com/quran/quran_android) and includes work from its many contributors. Please keep the original contributors in your prayers.

### Translation & Tafsir Data

- **Terjemahan Al-Qur'an Bahasa Indonesia (Kemenag)** — translation data provided by the Ministry of Religious Affairs of the Republic of Indonesia (Kementerian Agama RI), bundled for offline use.
- **Tafsir Al-Mukhtashar (Bahasa Indonesia)** — Indonesian text sourced via [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) (Quranic Universal Library / QUL-derived export, edition: Indonesian Al-Mukhtasar). Redistribution and attribution are provided to the extent applicable; rights remain with the respective content owners/publishers.

### License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**, in line with the upstream project. See [LICENSE](LICENSE) (and upstream licensing) for details.

## Building

```bash
./gradlew :app:assembleMadaniDebug -PdisableFirebase
```

> Note: Android SDK (platform 36 and compatible build-tools) must be configured. On aarch64 hosts, use matching aarch64 build-tools binaries.
