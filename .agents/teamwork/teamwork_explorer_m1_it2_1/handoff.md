# Remediation Investigation Report: Milestone 1 Issue 1 (HomeViewModel Audio Trigger)

**Investigator**: Explorer 1 (`teamwork_explorer_m1_it2_1`)  
**Milestone**: Milestone 1 Iteration 2 (Remediation)  
**Target Issue**: Issue 1 — Headless Audio Playback Failure in `HomeViewModel.togglePanchangAudio()`  
**Target File**: `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`  
**Challenger Test**: `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeAudioAndRecompositionChallengerTest.kt`  

---

## 1. Observation

### 1.1 Direct Source Code Observations in `HomeViewModel.kt`

In `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`:

1. **State Definition & Upstream Combining (Lines 125–204)**:
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
           // mapping 13 flows into HomeUiState...
           HomeUiState(
               ...
               todayCalendarDay = calendarDay,
               todayKarteInfo = karteInfo,
               todayAlert = todayAlert,
               isPanchangAudioPlaying = isPanchangAudioPlaying,
           )
       }.flowOn(dispatchers.default)
           .distinctUntilChanged()
           .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
   ```
   - `uiState` is declared with `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())`.
   - The initial value is `HomeUiState()` where `todayAlert` is `null` (line 72: `val todayAlert: LunarDayAlert? = null`).
   - `combine` aggregates 13 distinct streams on `dispatchers.default`.

2. **Imperative Action Method (Lines 218–228)**:
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
   ```
   - Line 224: `val alert = uiState.value.todayAlert ?: return`.
   - If `uiState.value.todayAlert` is `null`, the function returns early without calling `audioEngine.speakDynamicText(...)` or mutating `isPanchangAudioPlayingFlow`.

### 1.2 Direct Observations in `HomeAudioAndRecompositionChallengerTest.kt`

In `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeAudioAndRecompositionChallengerTest.kt`:

1. **Test Structure (Lines 108–134)**:
   ```kotlin
   @Test
   fun `rapid toggling of panchang audio toggles stop and speak without state desync`() = runTest {
       val audioEngine = ControllableAudioEngine()
       val calendarRepo = ControllableCalendarRepo(
           day = sampleDay(),
           karte = sampleKarte(),
           alert = sampleAlert("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి."),
       )
       val vm = createViewModel(
           audioEngine = audioEngine,
           calendarRepo = calendarRepo,
           dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler)),
       )
       backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

       testScheduler.advanceUntilIdle()

       assertFalse(audioEngine.isSpeaking.value)
       assertEquals(0, audioEngine.stopCalls)
       assertEquals(0, audioEngine.speakDynamicCalls)

       // 1st toggle: starts speaking
       vm.togglePanchangAudio()
       testScheduler.advanceUntilIdle()
       assertTrue("Audio engine should be speaking after 1st toggle", audioEngine.isSpeaking.value)
       assertEquals("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి.", audioEngine.lastSpokenText)
       assertEquals(1, audioEngine.speakDynamicCalls)
       ...
   ```
2. **Failure Points Observed in Forensic Auditor & Reviewer Reports**:
   - `rapid toggling of panchang audio toggles stop and speak without state desync`: Line 131 fails because `audioEngine.isSpeaking.value` is `false` (remained 0 calls to `speakDynamicCalls`).
   - `external tts completion resets panchang audio state to idle`: Line 184 fails because 1st toggle never began speaking.
   - `home header audio cancels active panchang audio without desync`: Line 219 fails because 1st toggle never began speaking.

---

## 2. Logic Chain

1. **Root Cause Analysis**:
   - `uiState` is hot-converted using `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())`.
   - In Kotlin Coroutines, `SharingStarted.WhileSubscribed(5_000)` cancels/delays upstream flow collection whenever the number of subscribers to `uiState` is 0.
   - When `HomeViewModel` is instantiated in headless unit tests or background contexts without an attached subscriber, `uiState.value` remains the initial value (`HomeUiState()`), in which `todayAlert == null`.
   - Even when a test launches `backgroundScope.launch { vm.uiState.collect {} }`:
     - The upstream `combine(...)` runs across 13 flows and is shifted via `.flowOn(dispatchers.default)`.
     - `stateIn` executes inside `viewModelScope`, which is bound to `SupervisorJob() + Dispatchers.Main.immediate`.
     - In Robolectric unit tests, `Dispatchers.Main` operates on Android's main looper. Unless the test sets `Dispatchers.setMain` or pumps Robolectric's Looper, coroutines inside `viewModelScope` on `Dispatchers.Main` do not execute simply by advancing `testScheduler`.
     - As a result, `uiState.value` is still `HomeUiState()` when `togglePanchangAudio()` is invoked immediately afterwards.
   - Line 224: `val alert = uiState.value.todayAlert ?: return` evaluates `uiState.value.todayAlert` as `null` and silently aborts.

2. **Race Condition & Fragile UX in Production**:
   - In production on Android, when `HomeDashboardScreen` is rendered, `collectAsStateWithLifecycle()` starts subscribing to `uiState`.
   - Combining 13 distinct asynchronous repository streams requires every single flow to emit at least one item before the combined lambda produces its first non-default `HomeUiState`.
   - If a user quickly taps "వినండి" before all 13 streams complete their initial emission, or if any upstream query stalls, `togglePanchangAudio()` silently drops the click event. The farmer hears nothing and receives no acoustic or visual error indication.

3. **Remediation Strategy Evaluation**:
   - **Option A (Tight Coupling)**: Keep reading `uiState.value.todayAlert`, and change `WhileSubscribed(5_000)` to `SharingStarted.Eagerly`.
     - *Rejection*: Eagerly sharing combines 13 streams indefinitely even when the app is backgrounded, wasting battery and CPU on farmer devices.
   - **Option B (Independent Alert Cache in `HomeViewModel`) [RECOMMENDED]**:
     - Maintain an internal `cachedTodayAlert: LunarDayAlert?` in `HomeViewModel`.
     - In `init`, collect `teluguCalendarRepo.observeTodayAlert(todayEpochDay)` directly using `viewModelScope.launch(dispatchers.default) { ... }`.
     - Because `teluguCalendarRepo.observeTodayAlert()` is an independent, lightweight single stream, it emits immediately without waiting for the other 12 streams of `HomeUiState`.
     - Using `dispatchers.default` guarantees that unit tests using `TestAppDispatchers` advance this collection deterministically upon `advanceUntilIdle()`.
     - In `togglePanchangAudio()`, resolve the alert with fallback:
       ```kotlin
       val alert = cachedTodayAlert ?: uiState.value.todayAlert ?: return
       ```
     - This guarantees that both UI-driven and headless/early invocations reliably obtain the current alert.

---

## 3. Caveats

- **Time Provider Midnight Rollover**: `todayEpochDay` is initialized once during ViewModel construction. If the application is left open across midnight without backgrounding or ViewModel recreation, `cachedTodayAlert` will reflect the epoch day of instantiation. This is consistent with the existing behavior across the entire `HomeViewModel` (e.g. `observeDueAndOverdueTasks(todayEpochDay)`).
- **Blank Alert Handling**: `HomeAudioAndRecompositionChallengerTest` verifies that blank TTS text (`alert.ttsSpeechText.isBlank()`) remains a silent no-op. The proposed cache must continue to respect `if (alert.ttsSpeechText.isBlank()) return`.

---

## 4. Conclusion

The 3 test failures in `HomeAudioAndRecompositionChallengerTest` are caused by coupling the imperative `togglePanchangAudio()` invocation to `uiState.value.todayAlert`, which remains `null` under `SharingStarted.WhileSubscribed(5_000)` in headless or non-subscribed states.

The fix requires:
1. Adding `private var cachedTodayAlert: LunarDayAlert? = null` in `HomeViewModel.kt`.
2. Launching an alert collector in `init` using `viewModelScope.launch(dispatchers.default)`.
3. Updating `togglePanchangAudio()` to read `cachedTodayAlert ?: uiState.value.todayAlert ?: return`.

---

## 5. Step-by-Step Remediation Instructions for Worker

The Worker agent should apply the following modifications to `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`:

### Step 1: Add Alert Cache Field & Collector in `HomeViewModel.kt`

Locate lines 102–115 in `HomeViewModel.kt`:
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

Replace with:
```kotlin
    private val playingContactIdFlow = MutableStateFlow<Long?>(null)
    private val totalRecordsFlow = MutableStateFlow(0)
    private val isPanchangAudioPlayingFlow = MutableStateFlow(false)
    private var cachedTodayAlert: LunarDayAlert? = null

    init {
        refreshBackupCheckCounts()
        viewModelScope.launch(dispatchers.default) {
            audioEngine.isSpeaking.collect { speaking ->
                if (!speaking) {
                    isPanchangAudioPlayingFlow.value = false
                }
            }
        }
        viewModelScope.launch(dispatchers.default) {
            teluguCalendarRepo.observeTodayAlert(todayEpochDay).collect { alert ->
                cachedTodayAlert = alert
            }
        }
    }
```

### Step 2: Update `togglePanchangAudio()` to Read Cached Alert

Locate lines 218–228 in `HomeViewModel.kt`:
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
```

Replace with:
```kotlin
    fun togglePanchangAudio() {
        if (isPanchangAudioPlayingFlow.value) {
            audioEngine.stop()
            isPanchangAudioPlayingFlow.value = false
            return
        }
        val alert = cachedTodayAlert ?: uiState.value.todayAlert ?: return
        if (alert.ttsSpeechText.isBlank()) return
        isPanchangAudioPlayingFlow.value = true
        audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)
    }
```

---

## 6. Verification Method

To independently verify this fix:

1. Navigate to project root:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   ```

2. Compile and run all unit tests in `:feature:home`:
   ```powershell
   .\gradlew.bat --no-daemon :feature:home:testDebugUnitTest :feature:home:compileDebugKotlin
   ```

3. Verification Criteria:
   - Before fix: 3 failures in `HomeAudioAndRecompositionChallengerTest`.
   - After fix: All 14 tests in `:feature:home` pass cleanly (`BUILD SUCCESSFUL`).
