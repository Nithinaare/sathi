# Domain and Home Codebase Survey Handoff Report

**Investigator:** `teamwork_explorer_survey_2`  
**Date:** 2026-11-14T07:51:30Z  
**Target:** Telugu Agricultural Calendar & Panchangam System Survey  
**Working Directory:** `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi`  

---

## 1. Observation

### 1.1 Existing Telugu Calendar, Panchang & Lunar Domain Architecture
Direct inspection of files and `git status` reveals the following:

- **Data Models (`core/model/src/main/kotlin/com/farmsaathi/core/model/TeluguCalendarModels.kt`)**:
  - File exists and defines all foundational data structures:
    - `TeluguMonth` (enum 1..12: `CHAITRAM` to `PHALGUNAM` with Telugu/English names and seasons).
    - `Paksham` (enum: `SHUKLA` "శుక్ల పక్షం", `KRISHNA` "కృష్ణ పక్షం").
    - `TeluguTithi` (enum: `PADYAMI` to `POURNAMI`, `AMAVASYA`).
    - `TeluguKarte` data class + `TeluguKarteluCatalog.ALL` (complete verified catalog of all 27 Kartis from `అశ్విని` to `రేవతి` with solar date spans, Telugu proverbs, agricultural significance, and recommended crops).
    - `TeluguCalendarDay`: contains `epochDay`, `gregorianDate`, `teluguYear`, `teluguMonth`, `paksham`, `tithi`, `nakshatram`, `karteId`, `karteName`, `karteDayNumber`, `totalKarteDays`, `isAmavasya`, `isPournami`, `isFestival`, `festivalName`, `seasonalAdvisory`.
    - `CurrentKarteInfo`: contains active Karte details, `dayNumber`, `totalDays`, `daysRemaining`, `progressPercentage`, `proverb`, `agriculturalSignificance`, `recommendedCrops`.
    - `LunarAlertType` (enum: `DAY_STANDARD`, `AMAVASYA`, `POST_AMAVASYA_SPRAY`, `POURNAMI`, `KARTE_TRANSITION`, `FESTIVAL`).
    - `LunarDayAlert`: contains `alertType`, `title`, `subtitle`, `ttsSpeechText`, `actionText`, `actionRoute`, `isPriority`.

- **Repository Contract (`core/common/src/main/kotlin/com/farmsaathi/core/common/repository/TeluguCalendarRepository.kt`)**:
  - Exists and defines:
    - `fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?>`
    - `suspend fun getDay(epochDay: Long): TeluguCalendarDay?`
    - `fun observeMonth(year: Int, month: Int): Flow<List<TeluguCalendarDay>>`
    - `suspend fun getMonthDays(year: Int, month: Int): List<TeluguCalendarDay>`
    - `fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo>`
    - `suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo`
    - `fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert>`
    - `suspend fun getTodayAlert(epochDay: Long): LunarDayAlert`
    - `fun observeAllKartis(): Flow<List<TeluguKarte>>`

- **Repository Implementation (`core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`)**:
  - Exists and provides 100% offline Chandramana lunar calculations and solar Karte transitions:
    - Lines 409–437: `KARTE_BOUNDARIES` defines solar entry dates for all 27 Kartis.
    - Lines 449–482: `LUNAR_MONTH_SPANS` contains pre-computed verified lunar month spans for years 2025, 2026, and 2027 (Krodhi, Vishvavasu, Parabhava, and Plavanga).
    - Lines 316–346: Algorithmic fallback using mean synodic lunar month ($29.530588$ days) for dates outside the 2025–2027 span.
    - Lines 91–184: Dynamic alert engine (`calculateTodayAlert`):
      1. Amavasya Day Alert (`LunarAlertType.AMAVASYA`, priority).
      2. Post-Amavasya Spray Window: days $+3$ to $+5$ after Amavasya (`LunarAlertType.POST_AMAVASYA_SPRAY`, priority).
      3. Festival Alerts (`LunarAlertType.FESTIVAL`: ఏరువాక పౌర్ణమి, పోలాల అమావాస్య, కనుమ, etc.).
      4. Pournami Alert (`LunarAlertType.POURNAMI`).
      5. Karte Entry Alert: Day 1 of new Karte (`LunarAlertType.KARTE_TRANSITION`, priority).
      6. Clean Standard Day Alert (`LunarAlertType.DAY_STANDARD`).

- **Dagger/Hilt Dependency Injection (`core/database/src/main/kotlin/com/farmsaathi/core/database/di/DatabaseModule.kt`)**:
  - Lines 86–88:
    ```kotlin
    @Provides
    @Singleton
    fun provideTeluguCalendarRepository(): TeluguCalendarRepository = OfflineTeluguCalendarRepositoryImpl()
    ```
  - `TeluguCalendarRepository` is already bound as a singleton and ready for injection into any ViewModel.

- **Navigation Routes (`core/common/src/main/kotlin/com/farmsaathi/core/common/navigation/FarmRoute.kt`)**:
  - Lines 52–55:
    ```kotlin
    // 4b. Telugu Agricultural Calendar & Karte Guide
    @Serializable data class TeluguCalendar(val initialEpochDay: Long? = null) : FarmRoute

    @Serializable data class KarteGuide(val highlightKarteId: Int? = null) : FarmRoute
    ```
  - In `FarmRoute.ActivityForm`: `val initialDueDateEpochDay: Long? = null` is added to support 1-tap task creation prefilled from a calendar date.

- **Design System Strings (`core/designsystem/src/main/res/values/strings.xml` & `values-en/strings.xml`)**:
  - `cal_telugu_title`, `cal_month_view_tab`, `cal_karte_view_tab`, `cal_amavasya_badge`, `cal_pournami_badge`, `cal_day_details_title`, `cal_add_task_for_day`, `cal_no_tasks_for_day`, `cal_karte_guide_title`, `cal_days_remaining`, `cal_suited_crops`, `cal_proverb_label`, `cal_advisory_label`, `home_panchang_listen`, `home_panchang_view_calendar` are already added in both Telugu and English!

- **Test Suite Discrepancy (`core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`)**:
  - Running `.\gradlew.bat :core:common:testDebugUnitTest --console=plain` yielded:
    ```
    OfflineTeluguCalendarRepositoryTest > getTodayAlert_standardDayProducesCleanInformativeAlert FAILED
        java.lang.AssertionError at OfflineTeluguCalendarRepositoryTest.kt:88
    12 tests completed, 1 failed
    ```
  - Line 90 of `OfflineTeluguCalendarRepositoryTest.kt`: `val epochDay = LocalDate.of(2025, 7, 10).toEpochDay()`.
  - In `OfflineTeluguCalendarRepositoryImpl.kt` line 457: `LunarSpan("విశ్వావసు", TeluguMonth.ASHADHAM, 20265L, 20279L, 20293L)`.
  - Date `2025-07-10` has epochDay `20279L`, which is exactly Ashadha Pournami (`pournamiEpochDay = 20279L`). Therefore, `calculateTodayAlert(epochDay)` returns `LunarAlertType.POURNAMI`, not `LunarAlertType.DAY_STANDARD`.
  - Changing test date to `2025-07-08` (or `2025-07-12`) resolves the test failure.

---

### 1.2 Examination of `feature/home`

- **`HomeViewModel.kt` (`feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`)**:
  - Injects: `UserPreferencesRepository`, `FarmRepository`, `FinanceRepository`, `MachineryRepository`, `ContactAndSearchRepository`, `LocalPhotoStorage`, `VoiceNoteManager`, `AudioGuidanceEngine`, `TimeProvider`, `AppDispatchers`.
  - Exposes: `uiState: StateFlow<HomeUiState>`.
  - Current `HomeUiState` properties: `isLoading`, `prefs`, `todayFormatted`, `farms`, `activeFarm`, `seasonTotals`, `dueTasks`, `runningMachinery`, `recentLedger`, `contacts`, `playingContactId`, `isAudioPlaying`, `showBackupBanner`.
  - TTS Audio handling: Injects `AudioGuidanceEngine` (`audioEngine`). `audioEngine.isSpeaking` is collected into `isAudioPlaying`.
    - Predefined audio cues are triggered via `audioEngine.speakCue(cueKey, useTe)`.
    - Dynamic text is spoken via `audioEngine.speakDynamicText(text, useTelugu)`.
  - Missing in `HomeViewModel`:
    - Does not yet inject `TeluguCalendarRepository`.
    - `HomeUiState` does not yet contain `todayCalendarDay`, `todayKarteInfo`, `todayAlert`, or `isPanchangAudioPlaying`.
    - Lacks `togglePanchangAudio()` function to speak `alert.ttsSpeechText` using `audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)`.

- **`HomeDashboardScreen.kt` (`feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`)**:
  - Current items in `LazyColumn`:
    1. `ActiveMachineryBanner` (if `runningMachinery.isNotEmpty()`, lines 164–171).
    2. `BackupSafetyBanner` (if `showBackupBanner`, lines 175–181).
    3. `FarmSummaryCard` (`season_summary_card`, lines 183–196).
    4. `SnapPhotoHeroCard` (`snap_photo_hero`, lines 198–205).
    5. `TwoColActionRow` (`action_row_finance`: Expense & Sale, lines 207–244).
    6. `TwoColActionRow` (`action_row_labour_irrigation`: Labour & Irrigation, lines 246–283).
    7. `TwoColActionRow` (`action_row_activity_tractor`: Activities & Tractor, lines 285–322).
    8. `FarmSectionHeader` & `QuickContactCallCard` row (`contacts_row`, lines 324–379).
    9. `FarmSectionHeader` & `FarmRecordCard` for due tasks (`due_tasks_header`, lines 381–404).
    10. `FarmVisualGridTile` for extras (`extras_tile`, lines 406–422).

- **`HomePanchangKarteBanner.kt` (`feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt`)**:
  - Component already exists!
  - Signature:
    ```kotlin
    @Composable
    fun HomePanchangKarteBanner(
        calendarDay: TeluguCalendarDay?,
        karteInfo: CurrentKarteInfo?,
        alert: LunarDayAlert?,
        isPlayingAudio: Boolean,
        onPlayAudio: () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    )
    ```
  - Features:
    - Lunar phase disk badge: `FarmColors.MainText` for Amavasya, `FarmColors.HarvestAmber` for Pournami, `FarmColors.PrimaryForest` for other phases.
    - Month + Paksham + Tithi text header (`"${calendarDay.teluguMonth.teluguName} • ${calendarDay.paksham.teluguName} ${calendarDay.tithi.teluguName}"`).
    - 1-tap "వినండి" / "ఆపండి" audio button with volume icon and active playing state.
    - Karte badge chip (`"${karteInfo.teluguName} కార్తె"`) with day number counter (`"${karteInfo.dayNumber}/${karteInfo.totalDays}వ రోజు • ఇంకా ${karteInfo.daysRemaining} రోజులు"`).
    - Dynamic Alert box with priority highlighting (`FarmColors.AmberContainer` with terracotta text when priority, `FarmColors.SurfaceVariant` otherwise).
    - Footer navigation indicator: `"పంచాంగం & పనులు చూడండి >"`.

- **Banner Insertion Point**:
  - Inside `HomeDashboardScreen.kt`, within the `LazyColumn`, immediately after `BackupSafetyBanner` and immediately before `season_summary_card`:
    ```kotlin
    item(key = "home_panchang_karte_banner", contentType = "panchang_banner") {
        HomePanchangKarteBanner(
            calendarDay = state.todayCalendarDay,
            karteInfo = state.todayKarteInfo,
            alert = state.todayAlert,
            isPlayingAudio = state.isPanchangAudioPlaying,
            onPlayAudio = viewModel::togglePanchangAudio,
            onClick = onOpenTeluguCalendar,
        )
    }
    ```
  - Add parameter `onOpenTeluguCalendar: () -> Unit = {}` to `HomeDashboardScreen`.

---

### 1.3 `CropTask` Data Model and Persistence Architecture

- **Data Model (`core/model/src/main/kotlin/com/farmsaathi/core/model/DomainModels.kt`)**:
  - Lines 131–147:
    ```kotlin
    data class CropTask(
        val id: Long = 0L,
        val farmId: Long,
        val plotId: Long? = null,
        val cropCycleId: Long? = null,
        val title: String,
        val description: String? = null,
        val category: TaskCategory,
        val dueDateEpochDay: Long,
        val offsetFromSowingDays: Int? = null,
        val sourceTemplateStageKey: String? = null,
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val status: TaskStatus = TaskStatus.PENDING,
        val completedAtEpochMillis: Long? = null,
        val reminderEnabled: Boolean = true,
        val isArchived: Boolean = false,
    )
    ```
  - Key field: `dueDateEpochDay: Long`. This is an exact 1:1 match with `TeluguCalendarDay.epochDay`.

- **Persistence & Repository (`core/common/src/main/kotlin/com/farmsaathi/core/common/repository/FarmRepository.kt`)**:
  - Backed by Room DB (`FarmSaathiDatabase`, entity `CropTaskEntity`, DAO `CropDao`).
  - Read: `farmRepo.observeTasks(cropCycleId, plotId, farmId): Flow<List<CropTask>>`.
  - Read due/overdue: `farmRepo.observeDueAndOverdueTasks(todayEpochDay): Flow<List<CropTask>>`.
  - Save: `farmRepo.saveTask(task: CropTask): AppResult<Long>`.
  - Status update: `farmRepo.updateTaskStatus(taskId: Long, status: TaskStatus, newDueEpochDay: Long? = null): AppResult<Unit>`.

- **Integration with `TeluguCalendarViewModel`**:
  - `TeluguCalendarViewModel` can inject `FarmRepository` and observe `farmRepo.observeTasks()`.
  - In Month Grid View: tasks are grouped by `dueDateEpochDay`. Days with pending tasks display indicator dots and task count.
  - In Day Details Bottom Sheet: tasks for the selected date are filtered (`tasks.filter { it.dueDateEpochDay == selectedEpochDay }`) and presented with inline completion toggles calling `farmRepo.updateTaskStatus(task.id, TaskStatus.COMPLETED)`.
  - In 1-Tap Task Creation: clicking `+ పని జోడించండి` triggers `onAddTaskForDate: (Long) -> Unit` which navigates to `FarmRoute.ActivityForm(initialDueDateEpochDay = selectedEpochDay)`.

---

### 1.4 Text-To-Speech (TTS) Service & Audio Guidance

- **Implementation (`core/audio/src/main/kotlin/com/farmsaathi/core/audio/HybridAudioGuidanceEngine.kt`)**:
  - Implements `AudioGuidanceEngine` from `:core:common`.
  - Bound in Hilt as `@Singleton`.
  - Initializes Android system `TextToSpeech(context)` (API 29+ compatible).
  - Checks offline Telugu availability via:
    `engine.isLanguageAvailable(Locale("te", "IN")) >= TextToSpeech.LANG_AVAILABLE`.
  - Dynamic TTS function:
    `speakDynamicText(text: String, useTelugu: Boolean, speechRate: Float = 0.88f)`.
  - State tracking:
    `isSpeaking: StateFlow<Boolean>` and `activeCueKey: StateFlow<String?>`.
  - Stop function:
    `stop()` halts TTS speech or MediaPlayer prompt playback immediately.
  - Acoustic feedback:
    `playAcousticFeedback(AcousticCue.SAVED / ERROR / REMINDER)` via `SoundPool`.

---

## 2. Logic Chain

1. **Foundational Readiness**:
   - `core/model` has all models (`TeluguMonth`, `Paksham`, `TeluguTithi`, `TeluguKarte`, `TeluguCalendarDay`, `CurrentKarteInfo`, `LunarDayAlert`).
   - `core/common` has the repository contract and `OfflineTeluguCalendarRepositoryImpl`.
   - `core/database` has registered `TeluguCalendarRepository` in Hilt DI.
   - `core/designsystem` has all string resources in Telugu and English.
   - `FarmRoute.kt` has both `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` routes defined.

2. **Home Integration Path**:
   - `HomePanchangKarteBanner.kt` is already written and ready in `feature/home/components`.
   - `HomeViewModel` already injects `AudioGuidanceEngine` and `TimeProvider`.
   - Injecting `TeluguCalendarRepository` into `HomeViewModel` allows combining `teluguCalendarRepo.observeDay(todayEpochDay)`, `teluguCalendarRepo.observeCurrentKarte(todayEpochDay)`, and `teluguCalendarRepo.observeTodayAlert(todayEpochDay)` into `HomeUiState`.
   - Exposing `togglePanchangAudio()` allows the 1-tap "వినండి" audio button on `HomePanchangKarteBanner` to directly call `audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)`.
   - Placing `HomePanchangKarteBanner` right above `season_summary_card` in `HomeDashboardScreen` satisfies R1 without layout regressions.

3. **Calendar Screen & Task Scheduling Path**:
   - `CropTask` already uses `dueDateEpochDay: Long`.
   - `TeluguCalendarViewModel` in `feature/farm` can observe both `TeluguCalendarRepository` (month days, current karte, alert) and `FarmRepository` (`observeTasks()`).
   - `TeluguCalendarScreen` will present:
     - Header: Month selector (`<` Month `>`), audio readout button, view mode toggle (`నెలల పట్టిక` / `కార్తెల కాలచక్రం`).
     - Month Grid: 7-column layout (ఆది..శని), lunar phase badges (● Amavasya, ○ Pournami), crop task indicator dots.
     - Day Details Bottom Sheet: selected day Panchang details, proverb, agricultural advisory, scheduled farm tasks with completion checkboxes, and 1-tap `+ పని జోడించండి` button.
     - Karte Timeline View: progress bar of active Karte, days remaining counter, suited crops, and proverb.
   - `KarteGuideScreen`: catalog of all 27 Kartis with seasonal tabs (వానాకాలం, యాసంగి, వేసవి) and search.

4. **Navigation Wiring Path**:
   - Wire `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` in both `FarmAndCropsNavGraph.kt` and `OnboardingHomeMoreNavGraph.kt`.
   - Connect `onOpenTeluguCalendar` callback from `HomeDashboardScreen` to `nav.navigate(FarmRoute.TeluguCalendar())`.

---

## 3. Caveats

1. **Test Failure in `OfflineTeluguCalendarRepositoryTest`**:
   - Test `getTodayAlert_standardDayProducesCleanInformativeAlert` tested date `2025-07-10`, which happens to be Ashadha Pournami in `LUNAR_MONTH_SPANS`. The test must test a standard day (e.g. `2025-07-08` or `2025-07-12`) or expect `LunarAlertType.POURNAMI`.
2. **`HomeDashboardUiTest` Stub Update Required**:
   - `HomeDashboardUiTest` instantiates `HomeViewModel` directly with test stubs (`StubPrefsRepo`, `StubFarmRepo`, etc.). When `TeluguCalendarRepository` is added as a constructor dependency in `HomeViewModel`, `HomeDashboardUiTest` must provide `StubTeluguCalendarRepo()` (or `OfflineTeluguCalendarRepositoryImpl()`) to prevent unit test compilation errors.
3. **Android Device TTS Engine Variance**:
   - Some offline budget devices (like Realme 5s / Android 10) may not have Google Telugu TTS voice pack pre-installed. `HybridAudioGuidanceEngine` handles this gracefully: `checkTeluguAvailable(engine)` falls back to gentle acoustic cues (`AcousticCue.REMINDER`) without crashing.

---

## 4. Conclusion

1. **Panchang & Calendar Engine**:
   - The data models and offline repository are already completely implemented in `core/model` and `core/common`. No Room schema migration is needed.
   - All 27 Kartis with proverbs, solar dates, and agricultural recommendations are cataloged in `TeluguKarteluCatalog`.
   - Lunar spans for 2025–2027 with algorithmic fallback are verified in `OfflineTeluguCalendarRepositoryImpl`.

2. **Home Screen (R1)**:
   - `HomePanchangKarteBanner.kt` is already coded.
   - What remains: Inject `TeluguCalendarRepository` into `HomeViewModel`, add fields to `HomeUiState`, implement `togglePanchangAudio()`, place the banner in `HomeDashboardScreen.kt`, and update `HomeDashboardUiTest`.

3. **Farm Calendar & Task Scheduling (R2)**:
   - Create `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt`.
   - Create `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/TeluguCalendarScreen.kt`.
   - Create `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/KarteGuideScreen.kt`.
   - Merge `CropTask` items with lunar days by matching `dueDateEpochDay == epochDay`.

4. **Navigation Wiring (R3)**:
   - Register `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` in `FarmAndCropsNavGraph.kt` and `OnboardingHomeMoreNavGraph.kt`.
   - Wire `onOpenTeluguCalendar` in `HomeDashboardScreen`.

---

## 5. Verification Method

### 5.1 Verification Commands
Execute the following commands from the root directory:

1. **Verify `core:model` and `core:common` Unit Tests**:
   ```powershell
   .\gradlew.bat :core:model:testDebugUnitTest :core:common:testDebugUnitTest --console=plain
   ```
2. **Verify `feature:home` Tests**:
   ```powershell
   .\gradlew.bat :feature:home:testDebugUnitTest --console=plain
   ```
3. **Verify `feature:farm` Tests**:
   ```powershell
   .\gradlew.bat :feature:farm:testDebugUnitTest --console=plain
   ```
4. **Full Clean Build & Static Analysis**:
   ```powershell
   .\gradlew.bat detekt ktlintCheck testDebugUnitTest assembleDebug --console=plain
   ```

### 5.2 Files to Inspect
- `core/model/src/main/kotlin/com/farmsaathi/core/model/TeluguCalendarModels.kt`
- `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/TeluguCalendarRepository.kt`
- `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
- `core/database/src/main/kotlin/com/farmsaathi/core/database/di/DatabaseModule.kt`
- `feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt`
- `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
- `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`

### 5.3 Invalidation Conditions
- Any changes to Room database schema (`FarmSaathiDatabase`) that would necessitate schema migrations.
- Any regression where static placeholder/dummy tips are used instead of dynamic event-triggered alerts.
- Any audio playback implementation that blocks the UI thread or crashes when offline Telugu TTS is uninstalled.
