# Milestone 1 — Explorer 3 Handoff Report
**Target Focus:** UI & Test Integration in `feature/home` (`HomeDashboardScreen.kt` & `HomeDashboardUiTest.kt`)

---

## 1. Observation

### 1.1 `HomeDashboardScreen.kt` Inspection
- **File**: `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`
- **Function Signature (lines 101–119)**:
  ```kotlin
  @Composable
  fun HomeDashboardScreen(
      viewModel: HomeViewModel,
      onAddExpense: () -> Unit,
      onAddSale: () -> Unit,
      onOpenActivities: () -> Unit,
      onOpenAttendance: () -> Unit,
      onOpenMachinery: () -> Unit,
      onOpenAccounts: () -> Unit,
      onOpenTask: (Long) -> Unit,
      onOpenMachinerySession: (Long) -> Unit,
      onOpenContacts: () -> Unit = {},
      onOpenSearch: () -> Unit = {},
      onOpenSettings: () -> Unit = {},
      onOpenIrrigation: () -> Unit = {},
      onOpenExtras: () -> Unit = {},
      onOpenBackup: () -> Unit = onOpenSettings,
      onSnapPhotoReady: (String, Long?) -> Unit = { _, _ -> },
  )
  ```
  `onOpenTeluguCalendar` is currently not defined in the parameter list.
- **LazyColumn Layout (lines 154–196)**:
  ```kotlin
  LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
      contentPadding = PaddingValues(FarmSpacing.dp16),
      verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
  ) {
      if (state.runningMachinery.isNotEmpty()) {
          items(
              state.runningMachinery,
              key = { "timer_${it.id}" },
              contentType = { "running_machinery_banner" },
          ) { session ->
              ActiveMachineryBanner(...)
          }
      }

      if (state.showBackupBanner) {
          item(key = "backup_safety_banner", contentType = "backup_safety_banner") {
              BackupSafetyBanner(
                  useTe = useTe,
                  onOpenBackup = onOpenBackup,
              )
          }
      }

      item(key = "season_summary_card", contentType = "summary_card") {
          val totals = state.seasonTotals
          FarmSummaryCard(...)
      }
      ...
  ```
  The exact insertion point directly above `season_summary_card` is between line 182 and line 183.

### 1.2 `HomePanchangKarteBanner.kt` Contract
- **File**: `feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt`
- **Package**: `com.farmsaathi.feature.home.components`
- **Composable Signature (lines 40–48)**:
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
- **Guard Condition (line 49)**:
  `if (calendarDay == null || alert == null) return`
- **UI Elements Provided**:
  - Header row: Lunar phase circle disk indicator (`calendarDay.isAmavasya`, `calendarDay.isPournami`), Telugu month, paksham & tithi text (`calendarDay.teluguMonth.teluguName • calendarDay.paksham.teluguName calendarDay.tithi.teluguName`).
  - 1-tap "వినండి" / "ఆపండి" audio button wired to `onPlayAudio`.
  - Karte badge: `${karteInfo.teluguName} కార్తె` and progress text (`${karteInfo.dayNumber}/${karteInfo.totalDays}వ రోజు • ఇంకా ${karteInfo.daysRemaining} రోజులు`).
  - Dynamic alert card with `alert.title` and `alert.subtitle`.
  - Clickable navigation hint footer: `పంచాంగం & పనులు చూడండి >` wired to `onClick`.

### 1.3 `HomeDashboardUiTest.kt` Inspection
- **File**: `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`
- **Constructor Invocations**:
  `HomeViewModel` is instantiated in 4 tests using named parameters:
  - Line 76: `home dashboard displays telugu greeting quick tiles and handles expense click`
  - Line 124: `home dashboard renders english copy when locale switched`
  - Line 167: `home dashboard shows backup safety banner when records exceed 10 and no backup exists`
  - Line 220: `home dashboard shows contact role subtitle and speaks name plus role`
- **Existing Stubs (lines 259–530)**:
  Includes `StubPrefsRepo`, `StubFarmRepo`, `StubFinanceRepo`, `StubMachineryRepo`, `StubAudioEngine`, `StubTimeProvider`, `StubContactRepo`.
  Currently lacks `StubTeluguCalendarRepo`.
- **Baseline Test Run**:
  Executed `Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"; .\gradlew.bat :feature:home:testDebugUnitTest`.
  Result: `BUILD SUCCESSFUL in 2m 8s. 128 actionable tasks: 31 executed, 97 up-to-date.` All existing tests pass.

### 1.4 Navigation Graph Invocations
- **File**: `app/src/main/kotlin/com/farmsaathi/app/navigation/OnboardingHomeMoreNavGraph.kt` (line 82)
  Calls `HomeDashboardScreen` with named arguments without passing `onOpenTeluguCalendar`. Adding a default value `= {}` on `onOpenTeluguCalendar` prevents compile breakage.

---

## 2. Logic Chain

1. **Parameter Definition & Navigation Decoupling**:
   - Adding `onOpenTeluguCalendar: () -> Unit = {}` to `HomeDashboardScreen` parameters allows external callers (such as `OnboardingHomeMoreNavGraph.kt` in Milestone 3) to navigate to `FarmRoute.TeluguCalendar()`.
   - The default empty lambda ensures zero compilation regressions for existing call sites in `:app` before Milestone 3 is reached.

2. **Insertion Point in LazyColumn**:
   - Placement directly above `season_summary_card` ensures that the agricultural lunar advisory and panchang banner is positioned immediately below active machinery timers and backup alerts.
   - This provides the highest daily agricultural relevance to the farmer on app launch.

3. **Conditional Item Rendering in LazyColumn**:
   - Wrapping the `item(key = "panchang_karte_banner", contentType = "panchang_karte_banner")` with:
     ```kotlin
     if (state.todayCalendarDay != null && state.todayAlert != null) {
         item(key = "panchang_karte_banner", contentType = "panchang_karte_banner") {
             HomePanchangKarteBanner(
                 calendarDay = state.todayCalendarDay,
                 karteInfo = state.todayKarteInfo,
                 alert = state.todayAlert,
                 isPlayingAudio = state.isPanchangAudioPlaying,
                 onPlayAudio = viewModel::togglePanchangAudio,
                 onClick = onOpenTeluguCalendar,
             )
         }
     }
     ```
     is superior to an unconditional `item` call because:
     - While `HomePanchangKarteBanner` internally returns early when `calendarDay == null || alert == null`, an empty Compose item inside `LazyColumn(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12))` can still introduce an extra 12dp spacing artifact.
     - The explicit check cleanly avoids this layout anomaly.

4. **ViewModel Wiring**:
   - `state.todayCalendarDay`: provides the lunar day model for date header, tithi, and lunar phase indicator.
   - `state.todayKarteInfo`: provides the current solar Karte progress, days elapsed, and days remaining.
   - `state.todayAlert`: provides the dynamic lunar alert title, subtitle, priority styling, and TTS speech script.
   - `state.isPanchangAudioPlaying`: toggles the banner's audio tag button icon/label between "వినండి" and "ఆపండి".
   - `viewModel::togglePanchangAudio`: toggles TTS audio playback for the panchang alert.
   - `onOpenTeluguCalendar`: navigates to the interactive Telugu calendar screen when the banner or hint is clicked.

5. **Test Maintenance & Isolation**:
   - When `HomeViewModel` constructor adds `teluguCalendarRepo: TeluguCalendarRepository`, `HomeDashboardUiTest.kt` must supply a stub.
   - Even if `HomeViewModel` has a default parameter `= OfflineTeluguCalendarRepositoryImpl()`, Robolectric UI tests must avoid depending on production disk ephemeris or calculation side effects.
   - Providing `StubTeluguCalendarRepo` returning canned `TeluguCalendarDay`, `CurrentKarteInfo`, and `LunarDayAlert` ensures predictable UI tests that run 100% offline and in milliseconds.
   - Updating the 4 existing test cases with `teluguCalendarRepo = StubTeluguCalendarRepo()` guarantees no regression.
   - Adding two new tests in `HomeDashboardUiTest.kt` guarantees test coverage for:
     1. Displaying the Panchang banner details and handling banner click.
     2. Tapping the banner's "వినండి" audio button and verifying speech dynamic text trigger.

---

## 3. Caveats

1. **Ambiguous UI Nodes in Tests ("వినండి")**:
   - Both `HomeHeaderBar` (`HeaderUtilityPill` / `FarmAudioButton` with `R.string.common_listen`) and `HomePanchangKarteBanner` render a button displaying the text "వినండి" when audio is not playing.
   - Calling `composeRule.onNodeWithText("వినండి")` in `HomeDashboardUiTest` will throw an `AmbiguousNodeException` because multiple nodes match.
   - **Resolution for Worker**: In tests, target the banner's audio button unambiguously using:
     - `composeRule.onAllNodes(hasText("వినండి"))[1]` OR
     - Disambiguating by parent semantics or using `hasClickAction() and hasText("వినండి")` with `.onLast()`.
2. **Audio State in `StubAudioEngine`**:
   - `StubAudioEngine.isSpeaking` is a `MutableStateFlow(false)`. Calling `viewModel.togglePanchangAudio()` executes `audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)` and sets `StubAudioEngine.lastDynamicText`.
   - The test should verify `audioEngine.lastDynamicText == alert.ttsSpeechText` rather than checking `isSpeaking` unless the test explicitly flips `StubAudioEngine.isSpeaking`.
3. **Screen Real Estate on Small Displays**:
   - The banner is approximately ~140dp in height. Because `HomeDashboardScreen` uses a `LazyColumn`, any additional items (such as `season_summary_card` and `SnapPhotoHeroCard`) remain smoothly scrollable on all display densities without clipping.

---

## 4. Conclusion & Concrete Recommendations for Worker

### Recommendation 1: Update `HomeDashboardScreen.kt`
**File**: `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`

1. **Add Import**:
   ```kotlin
   import com.farmsaathi.feature.home.components.HomePanchangKarteBanner
   ```

2. **Add Parameter `onOpenTeluguCalendar`**:
   In `HomeDashboardScreen` parameters (around line 118):
   ```kotlin
       onOpenBackup: () -> Unit = onOpenSettings,
       onSnapPhotoReady: (String, Long?) -> Unit = { _, _ -> },
       onOpenTeluguCalendar: () -> Unit = {},
   ) {
   ```

3. **Insert Banner above `season_summary_card`**:
   In `LazyColumn` (around line 183):
   ```kotlin
               if (state.showBackupBanner) {
                   item(key = "backup_safety_banner", contentType = "backup_safety_banner") {
                       BackupSafetyBanner(
                           useTe = useTe,
                           onOpenBackup = onOpenBackup,
                       )
                   }
               }

               if (state.todayCalendarDay != null && state.todayAlert != null) {
                   item(key = "panchang_karte_banner", contentType = "panchang_karte_banner") {
                       HomePanchangKarteBanner(
                           calendarDay = state.todayCalendarDay,
                           karteInfo = state.todayKarteInfo,
                           alert = state.todayAlert,
                           isPlayingAudio = state.isPanchangAudioPlaying,
                           onPlayAudio = viewModel::togglePanchangAudio,
                           onClick = onOpenTeluguCalendar,
                       )
                   }
               }

               item(key = "season_summary_card", contentType = "summary_card") {
                   val totals = state.seasonTotals
   ```

---

### Recommendation 2: Update `HomeDashboardUiTest.kt`
**File**: `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`

1. **Add Imports**:
   ```kotlin
   import com.farmsaathi.core.common.repository.TeluguCalendarRepository
   import com.farmsaathi.core.model.CurrentKarteInfo
   import com.farmsaathi.core.model.LunarAlertType
   import com.farmsaathi.core.model.LunarDayAlert
   import com.farmsaathi.core.model.Paksham
   import com.farmsaathi.core.model.TeluguCalendarDay
   import com.farmsaathi.core.model.TeluguKarte
   import com.farmsaathi.core.model.TeluguKarteluCatalog
   import com.farmsaathi.core.model.TeluguMonth
   import com.farmsaathi.core.model.TeluguTithi
   import androidx.compose.ui.test.onAllNodesWithText
   ```

2. **Add `StubTeluguCalendarRepo` class**:
   Add to the bottom of `HomeDashboardUiTest`:
   ```kotlin
       private class StubTeluguCalendarRepo(
           private val day: TeluguCalendarDay? = createSampleDay(),
           private val karte: CurrentKarteInfo = createSampleKarte(),
           private val alert: LunarDayAlert = createSampleAlert(),
       ) : TeluguCalendarRepository {
           override fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?> = flowOf(day)

           override suspend fun getDay(epochDay: Long): TeluguCalendarDay? = day

           override fun observeMonth(
               year: Int,
               month: Int,
           ): Flow<List<TeluguCalendarDay>> = flowOf(day?.let { listOf(it) } ?: emptyList())

           override suspend fun getMonthDays(
               year: Int,
               month: Int,
           ): List<TeluguCalendarDay> = day?.let { listOf(it) } ?: emptyList()

           override fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo> = flowOf(karte)

           override suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo = karte

           override fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert> = flowOf(alert)

           override suspend fun getTodayAlert(epochDay: Long): LunarDayAlert = alert

           override fun observeAllKartis(): Flow<List<TeluguKarte>> = flowOf(TeluguKarteluCatalog.ALL)

           companion object {
               fun createSampleDay() =
                   TeluguCalendarDay(
                       epochDay = 20_650L,
                       gregorianDate = "2026-11-10",
                       teluguYear = "పరాభవ",
                       teluguMonth = TeluguMonth.KARTHIKAM,
                       paksham = Paksham.SHUKLA,
                       tithi = TeluguTithi.PADYAMI,
                       nakshatram = "అనూరాధ",
                       karteId = 16,
                       karteName = "విశాఖ",
                       karteDayNumber = 5,
                       totalKarteDays = 14,
                       isAmavasya = false,
                       isPournami = false,
                       isFestival = false,
                   )

               fun createSampleKarte() =
                   CurrentKarteInfo(
                       karteId = 16,
                       teluguName = "విశాఖ",
                       englishName = "Vishakha",
                       dayNumber = 5,
                       totalDays = 14,
                       daysRemaining = 9,
                       progressPercentage = 35.7f,
                       proverb = "విశాఖలో చల్లితే విరివిగా పంట",
                       agriculturalSignificance = "వరి కోతలు ప్రారంభం.",
                       recommendedCrops = listOf("వరి", "శనగ"),
                   )

               fun createSampleAlert() =
                   LunarDayAlert(
                       alertType = LunarAlertType.DAY_STANDARD,
                       title = "విశాఖ కార్తె 5వ రోజు",
                       subtitle = "వరి కోతలు ముమ్మరంగా సాగుతాయి. గింజ ఆరబెట్టండి.",
                       ttsSpeechText = "నేడు కార్తీక మాసం శుక్ల పక్షం పాడ్యమి. విశాఖ కార్తె 5వ రోజు.",
                       isPriority = false,
                   )
           }
       }
   ```

3. **Pass `teluguCalendarRepo = StubTeluguCalendarRepo()` to Existing 4 Tests**:
   Update line 86, line 134, line 177, and line 230 to include:
   ```kotlin
       teluguCalendarRepo = StubTeluguCalendarRepo(),
   ```

4. **Add Dedicated Unit Tests for Banner Rendering and Audio Playback**:
   ```kotlin
       @Test
       fun `home dashboard displays panchang karte banner with telugu details and handles click`() {
           val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
           val vm =
               HomeViewModel(
                   prefsRepo = StubPrefsRepo(),
                   farmRepo = StubFarmRepo(),
                   financeRepo = StubFinanceRepo(),
                   machineryRepo = StubMachineryRepo(),
                   contactRepo = StubContactRepo(),
                   photoStorage = LocalPhotoStorage(ctx),
                   voiceManager = VoiceNoteManager(ctx),
                   audioEngine = StubAudioEngine(),
                   timeProvider = StubTimeProvider(),
                   teluguCalendarRepo = StubTeluguCalendarRepo(),
               )
           var calendarTapped = false

           composeRule.setContent {
               FarmSaathiTheme {
                   ProvideFarmLocale(AppLanguage.TELUGU) {
                       HomeDashboardScreen(
                           viewModel = vm,
                           onAddExpense = {},
                           onAddSale = {},
                           onOpenActivities = {},
                           onOpenAttendance = {},
                           onOpenMachinery = {},
                           onOpenAccounts = {},
                           onOpenTask = {},
                           onOpenMachinerySession = {},
                           onOpenTeluguCalendar = { calendarTapped = true },
                       )
                   }
               }
           }

           composeRule.onNodeWithText("కార్తీకం • శుక్ల పక్షం పాడ్యమి").assertIsDisplayed()
           composeRule.onNodeWithText("విశాఖ కార్తె").assertIsDisplayed()
           composeRule.onNodeWithText("విశాఖ కార్తె 5వ రోజు").assertIsDisplayed()
           composeRule.onNodeWithText("పంచాంగం & పనులు చూడండి")
               .assertIsDisplayed()
               .performClick()
           assertTrue(calendarTapped)
       }

       @Test
       fun `home dashboard panchang banner audio button triggers tts speech`() {
           val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
           val audioEngine = StubAudioEngine()
           val vm =
               HomeViewModel(
                   prefsRepo = StubPrefsRepo(),
                   farmRepo = StubFarmRepo(),
                   financeRepo = StubFinanceRepo(),
                   machineryRepo = StubMachineryRepo(),
                   contactRepo = StubContactRepo(),
                   photoStorage = LocalPhotoStorage(ctx),
                   voiceManager = VoiceNoteManager(ctx),
                   audioEngine = audioEngine,
                   timeProvider = StubTimeProvider(),
                   teluguCalendarRepo = StubTeluguCalendarRepo(),
               )

           composeRule.setContent {
               FarmSaathiTheme {
                   ProvideFarmLocale(AppLanguage.TELUGU) {
                       HomeDashboardScreen(
                           viewModel = vm,
                           onAddExpense = {},
                           onAddSale = {},
                           onOpenActivities = {},
                           onOpenAttendance = {},
                           onOpenMachinery = {},
                           onOpenAccounts = {},
                           onOpenTask = {},
                           onOpenMachinerySession = {},
                       )
                   }
               }
           }

           // Disambiguate from header audio button (which also has label "వినండి")
           composeRule.onAllNodesWithText("వినండి").onLast()
               .assertIsDisplayed()
               .performClick()

           assertEquals(
               "నేడు కార్తీక మాసం శుక్ల పక్షం పాడ్యమి. విశాఖ కార్తె 5వ రోజు.",
               audioEngine.lastDynamicText,
           )
       }
   ```

---

## 5. Verification Method

1. **Run Feature Home Unit Tests**:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   .\gradlew.bat :feature:home:testDebugUnitTest
   ```
   **Expected Outcome**: All 6 tests in `HomeDashboardUiTest` (4 existing + 2 new banner tests) pass with 0 failures (`BUILD SUCCESSFUL`).

2. **Run Targeted Test Specifically**:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   .\gradlew.bat :feature:home:testDebugUnitTest --tests "com.farmsaathi.feature.home.HomeDashboardUiTest"
   ```

3. **Invalidation Conditions**:
   - `AmbiguousNodeException` when clicking "వినండి" if `onLast()` or disambiguation is not used.
   - Missing `onOpenTeluguCalendar` causing compilation error in `OnboardingHomeMoreNavGraph.kt` if the default value `= {}` is omitted.
   - Compilation error in `HomeDashboardUiTest` if `teluguCalendarRepo` is not passed to all 4 existing test instances.
