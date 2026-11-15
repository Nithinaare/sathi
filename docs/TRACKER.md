# Farm Saathi — Lean Build & Verification Tracker

**Baseline:** [Farm_Saathi_Project_Context_and_Requirements.pdf](file:///C:/Users/nithi/Downloads/Farm_Saathi_Project_Context_and_Requirements.pdf)
**Target Device:** Realme 5s (4 GB RAM, Snapdragon 665, Android 10 / API 29)
**Package / App ID:** `com.farmsaathi.app` (`minSdk = 29`, `compileSdk = 35`, `targetSdk = 35`)

## Locked Product & Engineering Decisions
1. **Docs & Delivery:** Lean engineering docs + working Stage 1 Android APK as primary goal (`Farm_Saathi_Task_Tracker.xlsx` not present on disk).
2. **Language:** Simple conversational Telugu default + 1-tap Telugu/English toggle in Settings.
3. **Audio Guidance:** Hybrid offline Telugu audio cues (bundled acoustic cues + Android `TextToSpeech(te-IN)` + custom family `.m4a` prompt override support).
4. **Units & Money:** Acres + Guntas default (`40 guntas = 1 acre`, Telangana standard), Cents & decimal Acres supported; kg, quintal, bag, ton, litre, ml, piece, hour, acre; INR in `Long` paise (`MoneyPaise`) with Indian `₹1,25,000` grouping.
5. **Crops:** Editable starter stage templates for Paddy, Cotton, Chilli, Maize, Groundnut, Red Gram + custom crops.
6. **Accounting Rules:** Half-day = 50% daily rate (editable); hourly machinery prorated by minutes rounded half-up to nearest ₹1 (plus per-acre & flat-rate options); negative inventory blocked with quick Add Stock prompt.
7. **Backup & Restore:** Full self-contained `.zip` (checkpointed SQLite DB via `PRAGMA wal_checkpoint(TRUNCATE)` + `filesDir/media/` + SHA-256 `manifest.json`) in `Downloads/FarmSaathiBackups` + 1-tap Share + rollback-safe restore behind `BackupProvider`.
8. **Stage 1 Boundaries:** No weather API, speech-to-text/ASR, disease-detection AI, receipt OCR, or Google Drive cloud sync. The map-enabled land-measurement feature adds `INTERNET` permission only for optional MapLibre street/satellite tiles and user-confirmed offline street-map downloads. Farm records, GPS capture, area calculation, boundary editing, and local persistence remain offline-first.

## Feature & Phase Status
| ID | Slice / Milestone | Status | Source Modules | Verification Evidence |
|---|---|---|---|---|
| P0-P3 | Workspace, Design System & DB Architecture Specs | Completed | `docs/` | `DESIGN_SYSTEM.md`, `SCREEN_MAP_AND_TELUGU_COPY.md`, `ARCHITECTURE_AND_DOMAIN.md`, `DATABASE_SCHEMA.md` |
| P4 | Gradle, Foundation (21 Room entities, 10 DAOs, DataStore, Audio, Media, DesignSystem) | Completed | `:core:*`, `:app` | Compiles cleanly; `FarmSaathiDatabaseTest` (5 tests), `LocalZipBackupTest` (2 tests), `DesignSystemComponentsTest` (2 tests), `FarmLabelsTest` (2 tests), `MoneyAndUnitsTest` (4 tests), `FinancialCalculatorsTest` (5 tests) |
| S1 | Onboarding & Settings + Backup/Restore UI | Completed | `:feature:home` | Compiled + ZIP backup/restore integration verified under Robolectric SDK 29 (`LocalZipBackupTest`) |
| S2 | Home Dashboard & 4-Tab Bottom Navigation | Completed | `:feature:home`, `:app` | `HomeDashboardUiTest` (2 Robolectric Compose UI tests covering Telugu greeting, financial card, scroll+click, English locale switch) |
| S3 | Farm & Plot Management | Completed | `:feature:farm` | `FarmAndCalendarViewModelTest` (area conversions + audio cue feedback + validation) |
| S4 | Crop Cycles, Editable Templates & WorkManager Reminders | Completed | `:feature:farm`, `:app` | `FarmAndCalendarViewModelTest` + `DailyTaskReminderWorkerTest` (3 Robolectric WorkManager notification tests) |
| S5 | Photo & Voice Diary (`CameraX`/picker + `.m4a` Recording/Playback) | Completed | `:feature:diary`, `:core:media`, `:core:audio` | Compiles cleanly; `:app:lintDebug`, `detekt`, `ktlintCheck` clean (live mic/camera hardware execution pending device attach) |
| S6 | Unified Ledger, Expenses & Crop Sales/Income | Completed | `:feature:finance`, `:core:database` | `FinanceViewModelTest` (2 tests) + `FarmSaathiDatabaseTest` atomic ledger & voiding tests |
| S7 | Labour Management (Workers, Attendance, Wages, Advances, Settlements) | Completed | `:feature:operations`, `:core:database` | `OperationsViewModelTest` (half-day 50% wage calc) + `FarmSaathiDatabaseTest` worker balance test |
| S8 | Machinery Rental Timer (Start/Stop/Reboot Recovery) & Contacts | Completed | `:feature:operations`, `:core:database` | `OperationsViewModelTest` (timer cues) + `FinancialCalculatorsTest` (elapsed reboot recovery & single-active-lock DAO constraints) |
| S9 | Input Inventory, Irrigation, Harvest, Storage & Soil Tests | Completed | `:feature:operations`, `:core:database` | `OperationsViewModelTest` + `FarmSaathiDatabaseTest` (negative inventory stock guard & atomic purchase ledger) |
| S10 | Reports, Offline Search, Static Analysis & Signed Release APK | Completed | `:feature:reports`, `:app` | `ktlintCheck`, `detekt`, `:app:lintDebug`, `:app:lintVitalRelease`, `:app:assembleDebug`, `:app:assembleRelease` passed |
| S11 | Real Indian Photo Assets, UX Overhaul, Sticky Icons & New Work Fix | Completed | `:app`, `:core:designsystem`, `:feature:{home,farm,diary,finance,operations,reports}`, `docs/IMAGE_ATTRIBUTIONS.md` | 19 bundled `192x192` WebP assets (`225,330` bytes) + full-bleed rice seedling adaptive/legacy launcher icon; Hold-to-Speak (`LocalFarmSpeakHandler`) across tiles, buttons, tabs & cards; full-bleed image + scrim overlay prototype (`FullBleedTileContent`) on `ఖర్చు రాయండి`; Home 2×2 segregated Daily Must-Haves (with Contacts) & Field Work; `ktlintCheck`, `detekt`, lint, `34/34` tests & R8 APK verified |

## Latest Verification Run Summary (Post-Final Code Edit)
- **Unit, Room, Worker & Compose UI Tests (`task-2005` -> `testDebugUnitTest :core:model:test`):**
  - **Total:** **34 tests** across **11 test suites**, **0 failed**, **0 skipped**, **0 errors**.
  - Suites verified across `:core:model`, `:core:designsystem` (`DesignSystemComponentsTest` 3), `:core:database`, `:feature:{home,farm,finance,operations,diary}`, and `:app`: `DailyTaskReminderWorkerTest` (3), `FarmSaathiDatabaseTest` (5), `LocalZipBackupTest` (2), `FarmLabelsTest` (2), `FinancialCalculatorsTest` (5), `MoneyAndUnitsTest` (4), `FarmAndCalendarViewModelTest` (3), `FinanceViewModelTest` (2), `HomeDashboardUiTest` (2), `OperationsViewModelTest` (3).
- **Formatting, Static Analysis & Lint (`task-2005` -> `ktlintFormat`, `ktlintCheck`, `detekt`, `:app:lintDebug`, `:app:lintVitalRelease`):**
  - Passed across all 14 Gradle modules (`:app`, `:core:{model,common,designsystem,database,datastore,audio,media}`, `:feature:{home,farm,diary,finance,operations,reports}`).
- **Build Outputs (`task-2005` / `task-2009`):**
  - Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (74,093,289 bytes / 70.66 MB).
  - Signed R8-shrunk Release APK: `FarmSaathi-v1.0.0.apk` & `app/build/outputs/apk/release/app-release.apk` (6,184,656 bytes / 5.90 MB).
  - Release signature verified via `apksigner verify --verbose --print-certs` (APK Signature Scheme v2 `true`, `CN=Farm Saathi, OU=Personal, O=Farm Saathi, L=Hyderabad, ST=Telangana, C=IN`, SHA-256 `d7c4caa5c630a5180ae086562041e13cc03ccd7b5c0948ccb7121ea791c458b`).

## Device & Hardware Verification Status
- **Latest Rebuilt APK Verified Live on Phone (`SM-M176B`, Android 16 / API 36, serial `RZGL5289M3X`):**
  - Installed updated signed release APK (`adb -s RZGL5289M3X install -r FarmSaathi-v1.0.0.apk` -> `Success`, cold start `892 ms` / `941 ms`, zero `AndroidRuntime:E` crashes in `logcat`).
  - Visually inspected live phone screenshots (`fs_v4_overlay_top.png` & `fs_v4_overlay_row.png`) showing the full-bleed overlaid **ఖర్చు రాయండి** (`Add Expense`) tile side-by-side against **అమ్మకం రాయండి** (`Add Sale`) with aligned height, crisp dark bottom gradient scrim, and legible white text.
  - Prior live checks on `RZGL5289M3X` verified Hold-to-Speak (`900 ms` hold bound `com.google.android.tts` cleanly), 1-tap `ఫోన్ పరిచయాలు` navigation, live `"Weeding Check"` task save without plots/crops, Settings Telugu/English toggle, and local `.zip` backup export (`/sdcard/Download/FarmSaathiBackups/farm_saathi_20261111_065649.zip`, 9,955 bytes).
- **Pending Target Hardware Acceptance:**
  - Local SDK contains only an `android-25` emulator image (`< minSdk 29`).
  - Final physical acceptance testing on the target **Realme 5s (Android 10, API 29, 4 GB RAM)** — as well as interactive hardware mic `.m4a` recording, CameraX capture, ZIP restore-from-file flow, and TTS Telugu playback volume check — remains pending access to the Realme 5s device.


## S12 — v1.1.0 Rural UX, Wage Calculator, EXIF Snap-to-Log & Voice Badges (`2026-11-12`)

| Item | Status | Verification / Notes |
| --- | --- | --- |
| **App Version & Offline Boundary** | DONE (historical v1.1.0 base) | `versionCode = 2`, `versionName = "1.1.0"`; this verification predates the later MapLibre integration, which adds `INTERNET` only for optional maps |
| **Bottom Navigation & Extras Hub** | DONE | Simplified to 3 primary tabs (`Home`, `Crops`, `Money`); moved `Diary`, `Inventory`, `Farms`, and `Settings` into `MoreHubScreen` (`Extras`) |
| **Snap Photo to Log & EXIF Extraction** | DONE | Added `LocalPhotoStorage` EXIF capture/gallery metadata extraction + `SnapToLogScreen` router to `Expense`, `Diary`, and `Irrigation` forms |
| **Labour Hub, Wage Calculator & Days Counter** | DONE | Added quick wage calculator + group expense logging, photo-first worker creation, `daysWorkedTenths` stepper with undo, and `FarmVoiceNameBadge` audio names |
| **2-Tap Image Forms & Spoken Rupee Chips** | DONE | Updated Expense, Sale, Irrigation, Crop Cycle, Diary, and Contacts screens with `FarmImageGridPicker`, `FarmRupeeChipInput`, and `FarmMoreDetailsAccordion` |
| **Room Schema `v1 -> v2` (`MIGRATION_1_2`)** | DONE | Migrated `workers`, `crop_cycles`, `farms`, `contacts`, `irrigation_logs` cleanly; verified with Robolectric migration test |
| **Static Analysis & Lint (`ktlintCheck`, `detekt`, Lint)** | DONE | All 14 Gradle modules pass `ktlintCheck` and `detekt`; `:app:lintDebug` and `:app:lintVitalRelease` pass cleanly |
| **Unit, Room, EXIF & UI Tests** | DONE | **42/42 tests passed** (`0` failed, `0` errors, `0` skipped) across `app` (3), `core:database` (10), `core:designsystem` (6), `core:media` (2), `core:model` (10), `feature:farm` (3), `feature:finance` (2), `feature:home` (2), `feature:operations` (4) |
| **Signed R8 Release APK** | DONE | `FarmSaathi-v1.1.0.apk` (`6,380,015` bytes); verified v2 signature via `apksigner` (`CN=Farm Saathi, OU=Personal, O=Farm Saathi, L=Hyderabad, ST=Telangana, C=IN`) |
| **Live Device Install & Screenshot Verification (`SM_M176B`)** | DONE | Installed `FarmSaathi-v1.1.0.apk` (`versionCode = 2`) via ADB on `RZGL5289M3X` (`SM_M176B`); visually inspected live screenshots of Home, Labour Hub + Wage Calculator, Worker quick photo form, Watering EXIF form, Crops, Money, Sale, Expense, Contacts, and Extras, and fixed 4 real-device alignment/truncation issues |
| **Remaining Physical Hardware Checks (Realme 5s / Interactive Mic & Shutter)** | PENDING | Interactive CameraX shutter photo EXIF test, `.m4a` voice name recording/playback, Telugu TTS volume check, and final acceptance on the target Realme 5s (API 29) remain open |


## S13 — Map-Enabled Land Measurement Integration (`2026-11-14`)

| Item | Status | Verification / Notes |
| --- | --- | --- |
| **Map SDK** | IMPLEMENTED — BUILD VERIFICATION PENDING | MapLibre Native Android OpenGL `13.5.4` integrated in `:feature:farm`; Android 10 minSdk compatibility is a project requirement; full Gradle dependency resolution/build still must be run in Antigravity. |
| **Street / Place Map** | IMPLEMENTED — NETWORK CHECK PENDING | OpenFreeMap Liberty style, with OpenFreeMap/OpenMapTiles/OpenStreetMap attribution. Tiles are optional and map service has no SLA. |
| **Satellite Basemap** | IMPLEMENTED — NETWORK CHECK PENDING | Esri World Imagery raster tiles, with the current service copyright attribution. Satellite imagery is online-only in this version. |
| **Boundary Overlay & Editing** | IMPLEMENTED — UNIT TESTS ADDED | Local GeoJSON polygon/line/vertex overlay; move, insert-after and remove point; edited points are marked as manually adjusted and area is recalculated. |
| **Offline Street Map Pack** | IMPLEMENTED — PROVIDER/DEVICE TEST PENDING | MapLibre offline region around selected plot, zoom 12–16, app guardrails 2,700 tiles / 40 MiB, no more than four HTTP requests, identifiable User-Agent. Satellite imagery is not downloaded. |
| **Offline Core Behaviour** | DESIGN VERIFIED — DEVICE TEST PENDING | GPS capture, WGS84 area calculation, manual rectangle calculation, boundary editing and Room persistence do not require map tiles. Local boundary sketch is shown if map tiles cannot load. |
| **Full Android Build / Tests** | PENDING | Run `:feature:farm:testDebugUnitTest`, `:app:assembleDebug`, `ktlintCheck`, `detekt`, and `:app:lintDebug` in Antigravity. Previous build environment could not resolve `services.gradle.org`; no successful map-feature Gradle build is claimed. |
| **Realme 5s Acceptance** | PENDING | Verify street map, satellite layer, offline street pack, point editing and GPS capture on Android 10 outdoors. |
