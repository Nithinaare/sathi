# Milestone 1 — Explorer 2 Handoff Report
**Target Component:** `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`

---

## 1. Observation

### 1.1 Current Architecture and Injection in `HomeViewModel.kt`
From `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt` (lines 67–82):
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val farmRepo: FarmRepository,
    private val financeRepo: FinanceRepository,
    private val machineryRepo: MachineryRepository,
    private val contactRepo: ContactAndSearchRepository,
    private val photoStorage: LocalPhotoStorage,
    private val voiceManager: VoiceNoteManager,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
```
- The ViewModel is annotated with `@HiltViewModel` and uses `@Inject constructor`.
- `dispatchers` is already configured with a default parameter `= DefaultAppDispatchers`.
- In `core/database/src/main/kotlin/com/farmsaathi/core/database/di/DatabaseModule.kt` (lines 86–89):
```kotlin
    @Provides
    @Singleton
    fun provideTeluguCalendarRepository(): TeluguCalendarRepository = OfflineTeluguCalendarRepositoryImpl()
```
`TeluguCalendarRepository` is already bound and provided as a `@Singleton` in `DatabaseProvideModule` (installed in `SingletonComponent`).
- In `feature/home/build.gradle.kts` (lines 23–24):
```kotlin
    implementation(project(":core:model"))
    implementation(project(":core:common"))
```
Both `:core:model` (which contains `TeluguCalendarDay`, `CurrentKarteInfo`, `LunarDayAlert`) and `:core:common` (which contains `TeluguCalendarRepository` and `OfflineTeluguCalendarRepositoryImpl`) are already direct dependencies of `:feature:home`.

### 1.2 Current `HomeUiState` Data Structure
From `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt` (lines 45–65):
```kotlin
data class HomeUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val todayFormatted: String = "",
    val farms: List<Farm> = emptyList(),
    val activeFarm: Farm? = null,
    val seasonTotals: SeasonFinancialTotals =
        SeasonFinancialTotals(
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
        ),
    val dueTasks: List<CropTask> = emptyList(),
    val runningMachinery: List<MachinerySession> = emptyList(),
    val recentLedger: List<LedgerEntry> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val playingContactId: Long? = null,
    val isAudioPlaying: Boolean = false,
    val showBackupBanner: Boolean = false,
)
```

### 1.3 Current Flow Combination Pattern
From `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt` (lines 107–171):
```kotlin
    val uiState: StateFlow<HomeUiState> =
        combine(
            prefsRepo.userPreferences,                                       // arr[0]
            farmRepo.observeFarms(),                                         // arr[1]
            activeFarmIdFlow,                                                // arr[2]
            farmRepo.observeDueAndOverdueTasks(timeProvider.todayEpochDay()),// arr[3]
            machineryRepo.observeRunningSessions(),                          // arr[4]
            contactRepo.observeContacts(),                                   // arr[5]
            playingContactIdFlow,                                            // arr[6]
            audioEngine.isSpeaking,                                          // arr[7]
            totalRecordsFlow,                                                // arr[8]
        ) { arr ->
            val prefs = arr[0] as UserPreferences
            ...
            val playingContactId = arr[6] as Long?
            val speaking = arr[7] as Boolean
            val totalRecords = arr[8] as Int
            ...
            HomeUiState(
                ...
                isAudioPlaying = speaking,
                showBackupBanner = backupBannerVisible,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
```
- `combine` utilizes the array-based overload for >5 flows (`{ arr -> ... }`).
- `arr[7]` directly reads `audioEngine.isSpeaking` to drive `isAudioPlaying`.

### 1.4 Current Audio Guidance API & Interaction
From `core/common/src/main/kotlin/com/farmsaathi/core/common/BackupAndAudioContracts.kt` (lines 56–79):
```kotlin
interface AudioGuidanceEngine {
    val activeCueKey: StateFlow<String?>
    val isSpeaking: StateFlow<Boolean>
    ...
    fun speakCue(cueKey: String, useTelugu: Boolean)
    fun speakDynamicText(text: String, useTelugu: Boolean)
    fun speakDynamicText(text: String, useTelugu: Boolean, speechRate: Float)
    fun stop()
    ...
}
```
In `HybridAudioGuidanceEngine.kt`:
- `speakDynamicText` stops any running audio, sets `_activeCueKey.value = "dynamic"`, and speaks the given text via Android offline TTS.
- When TTS starts, `_isSpeaking.value` becomes `true`.
- When TTS finishes (in `UtteranceProgressListener.onDone` or `onError`), `_isSpeaking.value` becomes `false` and `_activeCueKey.value` becomes `null`.
- `stop()` halts TTS immediately and sets `_isSpeaking.value = false`.

### 1.5 Current Banner Contract in `HomePanchangKarteBanner.kt`
From `feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt` (lines 40–49):
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
) {
    if (calendarDay == null || alert == null) return
    ...
```
- Guard clause: If `calendarDay == null || alert == null`, nothing is rendered.
- `isPlayingAudio` controls whether the tag button shows `Icons.AutoMirrored.Rounded.VolumeUp` with label "వినండి" or `Icons.Rounded.Stop` with label "ఆపండి".

### 1.6 Existing Constructor Usages in Tests
Search across the repository reveals `HomeViewModel` is manually instantiated in exactly 4 unit test methods within `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`:
- Line 76: `HomeViewModel(...)`
- Line 124: `HomeViewModel(...)`
- Line 167: `HomeViewModel(...)`
- Line 220: `HomeViewModel(...)`

---

## 2. Logic Chain

1. **Dependency Injection**:
   - Because `TeluguCalendarRepository` is already provided as a singleton in `DatabaseProvideModule`, Hilt can inject it into `HomeViewModel` without any DI module adjustments.
   - Giving `teluguCalendarRepo: TeluguCalendarRepository = OfflineTeluguCalendarRepositoryImpl()` a default parameter allows existing tests in `HomeDashboardUiTest.kt` to compile and function without breaking changes, while Hilt at runtime automatically injects the singleton provider.

2. **State Model Additions (`HomeUiState`)**:
   - `todayCalendarDay: TeluguCalendarDay? = null` allows `HomeDashboardScreen` to bind `state.todayCalendarDay` to `HomePanchangKarteBanner`.
   - `todayKarteInfo: CurrentKarteInfo? = null` allows displaying current Karte progress (`${karteInfo.teluguName} కార్తె`, day number and days remaining).
   - `todayAlert: LunarDayAlert? = null` allows displaying dynamic lunar and agricultural advisories (e.g. Amavasya cattle rest, post-Amavasya spray, Karte onset).
   - `isPanchangAudioPlaying: Boolean = false` allows the banner's 1-tap audio tag button to toggle between "వినండి" and "ఆపండి".
   - Default values `= null` and `= false` ensure 100% backward compatibility with existing tests and `HomeUiState()` invocations.

3. **Flow Integration & Combine Stream**:
   - `timeProvider.todayEpochDay()` returns the current epoch day (e.g. 20650L).
   - `teluguCalendarRepo.observeDay(todayEpochDay)`, `teluguCalendarRepo.observeCurrentKarte(todayEpochDay)`, and `teluguCalendarRepo.observeTodayAlert(todayEpochDay)` return cold flows that immediately calculate and emit the respective data.
   - Adding these 3 flows alongside a local `isPanchangAudioPlayingFlow` into `combine(...)` extends the array from 9 to 13 flows.
   - All 13 flows emit their initial values immediately upon collection, guaranteeing no startup deadlock in `uiState`.
   - To prevent the general Home header "సహాయం వినండి" audio button from lighting up when the Panchang alert audio is speaking, `isAudioPlaying` in `HomeUiState` should be set to:
     `isAudioPlaying = speaking && !isPanchangAudioPlaying`.

4. **Audio Playback Management (`togglePanchangAudio`)**:
   - A private `isPanchangAudioPlayingFlow = MutableStateFlow(false)` tracks the playing state.
   - When `togglePanchangAudio()` is called:
     - If `isPanchangAudioPlayingFlow.value` is `true`: The user requested to stop playback. Call `audioEngine.stop()` and set `isPanchangAudioPlayingFlow.value = false`.
     - If `isPanchangAudioPlayingFlow.value` is `false`: Read `val alert = uiState.value.todayAlert ?: return`. If `alert.ttsSpeechText.isBlank()`, return. Otherwise, set `isPanchangAudioPlayingFlow.value = true` and call `audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)`.
   - In `init`, launching a collector on `audioEngine.isSpeaking`:
     ```kotlin
     viewModelScope.launch {
         audioEngine.isSpeaking.collect { speaking ->
             if (!speaking) {
                 isPanchangAudioPlayingFlow.value = false
             }
         }
     }
     ```
     ensures that when TTS speech finishes speaking the advisory naturally (or encounters an error or is stopped elsewhere), the UI state automatically resets from "ఆపండి" back to "వినండి".
   - In `toggleHomeAudio()`, `playContactBadge()`, and `onCleared()`, resetting `isPanchangAudioPlayingFlow.value = false` ensures cross-feature audio cancellation.

---

## 3. Caveats

- **Time Provider across midnight**: `todayEpochDay` is evaluated when the ViewModel initializes. If the user leaves the app open across midnight without backgrounding or recreating the ViewModel, it retains the epoch day from initial launch. This is the exact existing behavior of `farmRepo.observeDueAndOverdueTasks(timeProvider.todayEpochDay())` in `HomeViewModel.kt:112`.
- **Test Stubbing**: In `HomeDashboardUiTest.kt`, four calls instantiate `HomeViewModel`. Providing the default parameter `= OfflineTeluguCalendarRepositoryImpl()` makes this non-breaking. However, Explorer 3 / Worker can also explicitly pass a `StubTeluguCalendarRepo()` if desired for test isolation.
- **AudioEngine Mock in Tests**: In `HomeDashboardUiTest.kt`, `StubAudioEngine.isSpeaking` defaults to `MutableStateFlow(false)`. Calling `togglePanchangAudio()` will record the spoken text into `audioEngine.lastDynamicText`, which allows straightforward test assertions.

---

## 4. Conclusion & Concrete Worker Recommendations

### Step 1: Update Imports in `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
Add the following imports:
```kotlin
import com.farmsaathi.core.common.repository.OfflineTeluguCalendarRepositoryImpl
import com.farmsaathi.core.common.repository.TeluguCalendarRepository
import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.LunarDayAlert
import com.farmsaathi.core.model.TeluguCalendarDay
```

### Step 2: Update `HomeUiState`
Modify lines 45–65 to add the 4 properties:
```kotlin
data class HomeUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val todayFormatted: String = "",
    val farms: List<Farm> = emptyList(),
    val activeFarm: Farm? = null,
    val seasonTotals: SeasonFinancialTotals =
        SeasonFinancialTotals(
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
        ),
    val dueTasks: List<CropTask> = emptyList(),
    val runningMachinery: List<MachinerySession> = emptyList(),
    val recentLedger: List<LedgerEntry> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val playingContactId: Long? = null,
    val isAudioPlaying: Boolean = false,
    val showBackupBanner: Boolean = false,
    val todayCalendarDay: TeluguCalendarDay? = null,
    val todayKarteInfo: CurrentKarteInfo? = null,
    val todayAlert: LunarDayAlert? = null,
    val isPanchangAudioPlaying: Boolean = false,
)
```

### Step 3: Inject `TeluguCalendarRepository` in `HomeViewModel`
Add `teluguCalendarRepo` to the `@Inject constructor`:
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val farmRepo: FarmRepository,
    private val financeRepo: FinanceRepository,
    private val machineryRepo: MachineryRepository,
    private val contactRepo: ContactAndSearchRepository,
    private val teluguCalendarRepo: TeluguCalendarRepository = OfflineTeluguCalendarRepositoryImpl(),
    private val photoStorage: LocalPhotoStorage,
    private val voiceManager: VoiceNoteManager,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
```

### Step 4: Add Audio State Flow & Collector in `init`
Add `isPanchangAudioPlayingFlow` and listen to `audioEngine.isSpeaking`:
```kotlin
    private val playingContactIdFlow = MutableStateFlow<Long?>(null)
    private val totalRecordsFlow = MutableStateFlow(0)
    private val isPanchangAudioPlayingFlow = MutableStateFlow(false)

    init {
        refreshBackupCheckCounts()
        viewModelScope.launch {
            audioEngine.isSpeaking.collect { speaking ->
                if (!speaking) {
                    isPanchangAudioPlayingFlow.value = false
                }
            }
        }
    }
```

### Step 5: Update `combine` in `val uiState`
Add the 4 flows to `combine` and map them into `HomeUiState`:
```kotlin
    private val todayEpochDay = timeProvider.todayEpochDay()

    val uiState: StateFlow<HomeUiState> =
        combine(
            prefsRepo.userPreferences,
            farmRepo.observeFarms(),
            activeFarmIdFlow,
            farmRepo.observeDueAndOverdueTasks(todayEpochDay),
            machineryRepo.observeRunningSessions(),
            contactRepo.observeContacts(),
            playingContactIdFlow,
            audioEngine.isSpeaking,
            totalRecordsFlow,
            teluguCalendarRepo.observeDay(todayEpochDay),
            teluguCalendarRepo.observeCurrentKarte(todayEpochDay),
            teluguCalendarRepo.observeTodayAlert(todayEpochDay),
            isPanchangAudioPlayingFlow,
        ) { arr ->
            val prefs = arr[0] as UserPreferences

            @Suppress("UNCHECKED_CAST")
            val farms = arr[1] as List<Farm>

            @Suppress("UNCHECKED_CAST")
            val financePair = arr[2] as Pair<SeasonFinancialTotals, List<LedgerEntry>>

            @Suppress("UNCHECKED_CAST")
            val tasks = arr[3] as List<CropTask>

            @Suppress("UNCHECKED_CAST")
            val running = arr[4] as List<MachinerySession>

            @Suppress("UNCHECKED_CAST")
            val contacts = arr[5] as List<Contact>
            val playingContactId = arr[6] as Long?
            val speaking = arr[7] as Boolean
            val totalRecords = arr[8] as Int
            val calendarDay = arr[9] as TeluguCalendarDay?
            val karteInfo = arr[10] as CurrentKarteInfo?
            val todayAlert = arr[11] as LunarDayAlert?
            val isPanchangAudioPlaying = arr[12] as Boolean

            val activeFarm =
                farms.firstOrNull { it.id == prefs.activeFarmId }
                    ?: farms.firstOrNull { it.isPrimary }
                    ?: farms.firstOrNull()

            val lastBackup = prefs.lastBackupEpochMillis
            val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
            val backupBannerVisible =
                totalRecords > 10 &&
                    (lastBackup == null || (timeProvider.nowEpochMillis() - lastBackup) > thirtyDaysMs)

            HomeUiState(
                isLoading = false,
                prefs = prefs,
                todayFormatted =
                timeProvider.formatDate(
                    timeProvider.nowEpochMillis(),
                    prefs.language == AppLanguage.TELUGU,
                ),
                farms = farms,
                activeFarm = activeFarm,
                seasonTotals = financePair.first,
                dueTasks = tasks.take(5),
                runningMachinery = running,
                recentLedger = financePair.second,
                contacts = contacts.take(8),
                playingContactId = playingContactId,
                isAudioPlaying = speaking && !isPanchangAudioPlaying,
                showBackupBanner = backupBannerVisible,
                todayCalendarDay = calendarDay,
                todayKarteInfo = karteInfo,
                todayAlert = todayAlert,
                isPanchangAudioPlaying = isPanchangAudioPlaying,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
```

### Step 6: Add `togglePanchangAudio()` and Update Audio Cancellation Methods
Add the new method and ensure existing audio triggers clear `isPanchangAudioPlayingFlow`:
```kotlin
    fun togglePanchangAudio() {
        if (isPanchangAudioPlayingFlow.value) {
            audioEngine.stop()
            isPanchangAudioPlayingFlow.value = false
            return
        }
        val alert = uiState.value.todayAlert ?: return
        if (alert.ttsSpeechText.isBlank()) return
        isPanchangAudioPlayingFlow.value = true
        audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)
    }

    fun toggleHomeAudio() {
        isPanchangAudioPlayingFlow.value = false
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        audioEngine.speakCue(AudioCueCatalog.HOME_SUMMARY, useTe)
    }

    override fun onCleared() {
        voiceManager.stopPlayback()
        if (isPanchangAudioPlayingFlow.value) {
            audioEngine.stop()
        }
        super.onCleared()
    }
```

---

## 5. Verification Method

To independently verify the implementation:
1. **Compilation Check**:
   ```powershell
   ./gradlew.bat :feature:home:compileDebugKotlin
   ```
   *Expected result:* Successful compilation without type or Hilt injection errors.

2. **Unit Test Suite**:
   ```powershell
   ./gradlew.bat :feature:home:testDebugUnitTest
   ```
   *Expected result:* All unit tests in `feature/home` pass with 100% success rate.

3. **Behavioral Assertion Check**:
   In `HomeDashboardUiTest.kt`, verify that:
   - `vm.uiState.value.todayCalendarDay` is populated with the day calculation for `timeProvider.todayEpochDay()`.
   - `vm.uiState.value.todayAlert` is non-null.
   - Calling `vm.togglePanchangAudio()` triggers `audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)` with `audioEngine.lastDynamicText == alert.ttsSpeechText`.
   - Calling `vm.togglePanchangAudio()` while playing invokes `audioEngine.stop()`.
