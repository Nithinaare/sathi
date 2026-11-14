# Project: Farm Saathi — Telugu Agricultural Calendar & Dynamic Lunar Alert System

## Architecture
- **Multi-module Android Gradle Architecture**:
  - `:core:model`: Pure Kotlin data models (`TeluguCalendarModels.kt`: `TeluguMonth`, `Paksham`, `TeluguTithi`, `TeluguKarte`, `TeluguCalendarDay`, `CurrentKarteInfo`, `LunarDayAlert`, `DomainModels.kt`: `CropTask`).
  - `:core:common`: Domain repositories & navigation (`TeluguCalendarRepository.kt`, `OfflineTeluguCalendarRepositoryImpl.kt`, `FarmRoute.kt`: `TeluguCalendar`, `KarteGuide`, `ActivityForm`).
  - `:core:database`: Room DB & Dagger-Hilt DI (`DatabaseModule.kt` providing `TeluguCalendarRepository`).
  - `:core:audio`: TTS speech synthesis & audio cues (`HybridAudioGuidanceEngine.kt` implementing `AudioGuidanceEngine`).
  - `:core:designsystem`: Farm typography, colors, theme tokens, and localized strings (`values/strings.xml`, `values-en/strings.xml`).
  - `:feature:home`: Home dashboard, banners (`HomePanchangKarteBanner.kt`), `HomeViewModel`, and `HomeDashboardScreen`.
  - `:feature:farm`: Farm management, `TeluguCalendarViewModel`, `TeluguCalendarScreen` (dual-mode: Month Grid & Karte Timeline + Day Details bottom sheet), `KarteGuideScreen`, `CropsAndCalendarScreens.kt`.
  - `:app`: Root application wiring, navigation graphs (`FarmSaathiNavHost.kt`, `FarmAndCropsNavGraph.kt`, `OnboardingHomeMoreNavGraph.kt`).

- **Data Flow**:
  1. Offline Panchang & Solar Ephemeris Engine (100% offline Chandramana 2025–2027 spans + mean synodic algorithm fallback + 27 solar Kartis boundaries) -> `TeluguCalendarRepository`.
  2. `TeluguCalendarRepository` + `FarmRepository.observeTasks()` -> `HomeViewModel` (home banner + audio trigger) & `TeluguCalendarViewModel` (interactive calendar + task integration).
  3. UI Events -> `AudioGuidanceEngine.speakDynamicText()` (1-tap voice audio playback in Telugu) & `FarmRepository.updateTaskStatus()` (instant task completion) & `nav.navigate(FarmRoute.ActivityForm(initialDueDateEpochDay))` (1-tap task creation).

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Dynamic Day & Panchang Header | Displays current Telugu Month, Paksham, Tithi, active Karte, day number, and days remaining on Home Dashboard. | M1 | survey |
| 2 | Amavasya Cattle & Pest Alert | Priority card on Amavasya advising cattle rest (*ఎడ్లకు విశ్రాంతి*) and nocturnal light trap deployment. | M1 | survey |
| 3 | Post-Amavasya Bio-Spray Alert | Priority card on days +3 to +5 post-Amavasya advising NSKE 5% / neem oil spraying against newly hatched larvae. | M1 | survey |
| 4 | Karte Transition Alert | Alert on Day 1 of each new Karte announcing the open agricultural window (*అదను*). | M1 | survey |
| 5 | Sacred Agro-Festival Alerts | Specific alerts for ఏరువాక పౌర్ణమి (Kharif kickoff/navadhanya), పోలాల అమావాస్య (cattle thanksgiving), and కనుమ. | M1 | survey |
| 6 | 1-Tap "వినండి" Audio Guidance | Banner audio button speaking today's Telugu date, Karte progress, proverb, and alert via TTS. | M1 | survey |
| 7 | Udaya Tithi Calculation | Computes sunrise tithi for each date ensuring calendar consistency across daily farming routines. | M1 | survey |
| 8 | 27 Kartis Solar Mansions Engine | Computes Sun's 13.33° transit through 27 Nakshatras with start/end dates, day number, and days remaining. | M1 | survey |
| 9 | Chandramana Lunar Spans Table | Pre-computed 2025–2027 verified dataset (1,095 days) mapping Amanta lunar months and Telugu year names. | M1 | survey |
| 10 | 27 Nakshatra Sidereal Rotation | Computes sidereal moon nakshatra across 27 mansions (Ashwini to Revati). | M1 | survey |
| 11 | 27 Kartis Traditional Wisdom | Master catalog of all 27 Kartis with authentic proverbs (*సామెతలు*), seasonal phases, and suited crops. | M2 | survey |
| 12 | Month Grid View (`నెలల పట్టిక`) | Interactive 7-column calendar grid showing Gregorian date, Telugu Tithi, Amavasya/Pournami badges, and task dots. | M2 | survey |
| 13 | Lunar Disc Badges | Dark filled circle `●` for Amavasya; radiant amber circle `○` for Pournami. | M2 | survey |
| 14 | Live CropTask Dots | Badges displaying count of uncompleted tasks scheduled for that `dueDateEpochDay`. | M2 | survey |
| 15 | Karte Timeline View (`కార్తెల కాలచక్రం`) | Visual timeline tracking 27 Kartis with progress bar, days remaining countdown, and suited crops. | M2 | survey |
| 16 | Seasonal Filter Chips | Filters 27 Kartis by season: All (27), Kharif / వానాకాలం (5..14), Rabi / యాసంగి (15..25), Summer / వేసవి (26..4). | M2 | survey |
| 17 | Day Details Bottom Sheet | Tapping any day opens bottom sheet with full Panchang, agricultural advisory (*అదను*), and scheduled crop tasks. | M2 | survey |
| 18 | Inline Task Completion Checkbox | Tapping checkbox in day details sheet immediately marks `CropTask` completed. | M2 | survey |
| 19 | 1-Tap Add Task (`+ పని జోడించండి`) | Tapping button in day sheet navigates to `ActivityForm` with `initialDueDateEpochDay` pre-filled to that day. | M2 | survey |
| 20 | Karte Guide Screen (`KarteGuideScreen`) | Dedicated educational screen for all 27 Kartis with search, filter, and full audio readout in Telugu. | M2 | survey |
| 21 | Navigation Graph Integration | Wiring `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` into `FarmAndCropsNavGraph` & `OnboardingHomeMoreNavGraph`. | M3 | survey |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Repository & Home Screen Dynamic Alert Banner | Fix test in `OfflineTeluguCalendarRepositoryTest`; wire `HomeViewModel` to `TeluguCalendarRepository` with audio playback; insert `HomePanchangKarteBanner` into `HomeDashboardScreen`; update `HomeDashboardUiTest`. | none | DONE |
| M2 | Farm Calendar ViewModel, Interactive Views & Task Integration | Create `TeluguCalendarViewModel` merging panchang data with `CropTask` items; create `TeluguCalendarScreen` (Month Grid & Karte Timeline, Day Details bottom sheet, 1-tap task creation); create `KarteGuideScreen`; update `ActivityFormScreen` for `initialDueDateEpochDay`. | M1 | DONE |
| M3 | Navigation Wiring in App & Feature Graphs | Wire `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` destinations in `FarmAndCropsNavGraph.kt`; wire `onOpenTeluguCalendar` in `OnboardingHomeMoreNavGraph.kt`. | M2 | DONE |
| M4 | Final Milestone: Full E2E & Unit Test Pass, Static Analysis, Git Push | Run all unit tests, static analysis (`detekt`, `ktlintCheck`), verify clean build (`assembleDebug`), commit and push to `https://github.com/Nithinaare/sathi.git` on `main`. | M3 | IN_PROGRESS |

---

## Interface Contracts

### `TeluguCalendarRepository` ↔ `HomeViewModel` / `TeluguCalendarViewModel`
- `observeDay(epochDay: Long): Flow<TeluguCalendarDay?>`
- `observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo>`
- `observeTodayAlert(epochDay: Long): Flow<LunarDayAlert>`
- `observeMonth(year: Int, month: Int): Flow<List<TeluguCalendarDay>>`
- `observeAllKartis(): Flow<List<TeluguKarte>>`
- Error handling: Non-null safe fallback to synodic mathematical calculation and Ashwini default if outside verified ephemeris bounds.

### `FarmRepository` ↔ `TeluguCalendarViewModel`
- `observeTasks(cropCycleId: Long?, plotId: Long?, farmId: Long?): Flow<List<CropTask>>`
- `updateTaskStatus(taskId: Long, status: TaskStatus, newDueEpochDay: Long?): AppResult<Unit>`
- Error handling: Emits `AppResult.Error` gracefully handled with UI snackbar / acoustic feedback.

### `AudioGuidanceEngine` ↔ UI Components
- `speakDynamicText(text: String, useTelugu: Boolean, speechRate: Float = 0.88f)`
- `isSpeaking: StateFlow<Boolean>`
- `stop()`

### Navigation Contracts
- `FarmRoute.TeluguCalendar(val initialEpochDay: Long? = null)`
- `FarmRoute.KarteGuide(val highlightKarteId: Int? = null)`
- `FarmRoute.ActivityForm(val farmId: Long? = null, val cropCycleId: Long? = null, val taskId: Long? = null, val initialDueDateEpochDay: Long? = null)`

---

## Code Layout
- `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt` (Owned by M1)
- `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt` (Owned by M1)
- `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt` (Owned by M1)
- `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt` (Owned by M1)
- `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt` (Owned by M2)
- `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/TeluguCalendarScreen.kt` (Owned by M2)
- `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/KarteGuideScreen.kt` (Owned by M2)
- `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/CropsAndCalendarScreens.kt` (Owned by M2)
- `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt` (Owned by M2)
- `app/src/main/kotlin/com/farmsaathi/app/navigation/FarmAndCropsNavGraph.kt` (Owned by M3)
- `app/src/main/kotlin/com/farmsaathi/app/navigation/OnboardingHomeMoreNavGraph.kt` (Owned by M3)
