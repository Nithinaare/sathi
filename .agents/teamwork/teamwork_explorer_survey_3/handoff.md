# Investigation Handoff Report: Navigation, Farm Feature & Build Architecture

## 1. Observation

### 1.1 Navigation Contract & Route Definitions
- File: `core/common/src/main/kotlin/com/farmsaathi/core/common/navigation/FarmRoute.kt`
- Exact code at lines 54-57:
  ```kotlin
  // 4b. Telugu Agricultural Calendar & Karte Guide
  @Serializable data class TeluguCalendar(val initialEpochDay: Long? = null) : FarmRoute

  @Serializable data class KarteGuide(val highlightKarteId: Int? = null) : FarmRoute
  ```
- Base interface at line 10:
  ```kotlin
  @Serializable
  sealed interface FarmRoute
  ```
- Serialization framework: `kotlinx.serialization.Serializable`.
- Both routes use optional parameters with `null` defaults, permitting zero-parameter navigation: `FarmRoute.TeluguCalendar()` and `FarmRoute.KarteGuide()`.

### 1.2 Navigation Graph Wiring
- File: `app/src/main/kotlin/com/farmsaathi/app/navigation/FarmAndCropsNavGraph.kt`
  - Defines `fun NavGraphBuilder.farmAndCropsNavGraph(nav: NavController)` delegating to `farmAndPlotDestinations(nav)` and `cropAndActivityDestinations(nav)`.
  - Registered destinations:
    - `farmAndPlotDestinations`: `FarmRoute.FarmsTab` (lines 29-47), `FarmRoute.FarmDetail` (lines 49-60), `FarmRoute.FarmForm` (lines 62-70), `FarmRoute.PlotDetail` (lines 72-85), `FarmRoute.PlotForm` (lines 87-96).
    - `cropAndActivityDestinations`: `FarmRoute.CropList` (lines 100-110), `FarmRoute.CropDetail` (lines 112-125), `FarmRoute.CropForm` (lines 127-137), `FarmRoute.CropTemplateList` (lines 139-145), `FarmRoute.ActivityList` (lines 147-158), `FarmRoute.ActivityForm` (lines 160-170).
  - Verbatim check: Neither `FarmRoute.TeluguCalendar` nor `FarmRoute.KarteGuide` is registered in `FarmAndCropsNavGraph.kt`.
- File: `app/src/main/kotlin/com/farmsaathi/app/navigation/OnboardingHomeMoreNavGraph.kt`
  - Defines `fun NavGraphBuilder.onboardingHomeMoreNavGraph(nav: NavController)` with `onboardingDestinations`, `homeAndMoreDestinations`, `utilityAndReportDestinations`, and `snapToLogDestination`.
  - In `homeAndMoreDestinations` (lines 79-113):
    - `HomeDashboardScreen` receives callbacks: `onAddExpense`, `onAddSale`, `onOpenActivities`, `onOpenAttendance`, `onOpenMachinery`, `onOpenAccounts`, `onOpenTask`, `onOpenMachinerySession`, `onOpenContacts`, `onOpenSearch`, `onOpenSettings`, `onOpenBackup`, `onOpenIrrigation`, `onOpenExtras`, `onSnapPhotoReady`.
    - No callback is currently wired to navigate to `FarmRoute.TeluguCalendar` or `FarmRoute.KarteGuide`.
- File: `app/src/main/kotlin/com/farmsaathi/app/navigation/FarmSaathiNavHost.kt`
  - Houses the single root `NavHost` containing all 4 graph builder extensions:
    ```kotlin
    onboardingHomeMoreNavGraph(navController)
    farmAndCropsNavGraph(navController)
    financeAndDiaryNavGraph(navController)
    operationsNavGraph(navController)
    ```

### 1.3 `feature/farm` Module Structure & Conventions
- Module build config: `feature/farm/build.gradle.kts`
  - `namespace = "com.farmsaathi.feature.farm"`, `compileSdk = 35`, `minSdk = 29`, JVM target `17`.
  - Dependencies: `:core:model`, `:core:common`, `:core:designsystem`, `:core:datastore`, `:core:media`.
  - Test framework: JUnit 4, Robolectric 4.14.1, `kotlinx-coroutines-test`, `testOptions { unitTests.isIncludeAndroidResources = true }`.
- Source structure (`feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/`):
  - ViewModels in `viewmodel/`:
    - `CropAndCalendarViewModel.kt`: manages `CropCalendarUiState` (farms, plots, templates, crop cycles, tasks, todayEpochDay, audio playback state). Injects `FarmRepository`, `UserPreferencesRepository`, `AudioGuidanceEngine`, `TimeProvider`, `AppDispatchers`.
    - `FarmAndPlotViewModel.kt`: manages `FarmUiState` (farms, plots, crop cycles, photo capture, saving state).
  - Screens in `screens/`:
    - `CropsAndCalendarScreens.kt`: contains composables `CropListScreen`, `CropDetailScreen`, `CropFormScreen`, `CropTemplatesScreen`, `ActivityListScreen`, `ActivityFormScreen`.
    - `FarmsAndPlotsScreens.kt`: contains composables `FarmsListScreen`, `FarmDetailScreen`, `FarmFormScreen`, `PlotDetailScreen`, `PlotFormScreen`.
  - Tests in `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/`:
    - `FarmAndCalendarViewModelTest.kt`: Robolectric tests for `FarmAndPlotViewModel` and `CropAndCalendarViewModel`.
- Verbatim check for Requirement R2:
  - `TeluguCalendarViewModel` does NOT exist yet in `feature/farm/viewmodel/`.
  - `TeluguCalendarScreen` does NOT exist yet in `feature/farm/screens/`.
  - `KarteGuideScreen` does NOT exist yet in `feature/farm/screens/`.
- UI & Architectural Conventions:
  - Architecture: Hilt `@HiltViewModel` + immutable UI State data class + unidirectional data flow via `StateFlow` + `SharingStarted.WhileSubscribed(5_000)`.
  - Jetpack Compose styling: uses `FarmColors`, `FarmSpacing`, `FarmTypography`, `FarmDimensions`, `FarmShapeTokens` from `com.farmsaathi.core.designsystem.theme.*`.
  - UI Scaffolding: `Scaffold(containerColor = FarmColors.Background)`, `FarmPageHeader(title = ..., onBack = ..., audioCueKey = ..., speakText = ..., isAudioPlaying = ..., onToggleAudio = ...)`.
  - Audio & TTS: `AudioGuidanceEngine` for TTS playback (`speakCue(key, useTe)`) and acoustic feedback (`playAcousticFeedback(AcousticCue.SAVED / ERROR)`).
  - Localization: `LocalAppLanguage.current == AppLanguage.TELUGU` check, bilingual string resources and inline Telugu literals.

### 1.4 Gradle Build Configuration
- Root files:
  - `build.gradle.kts`: Configures Detekt (`io.gitlab.arturbosch.detekt` v1.23.8) with `config/detekt/detekt.yml`, and Ktlint (`org.jlleitschuh.gradle.ktlint` v12.1.2) across all subprojects.
  - `settings.gradle.kts`: Defines root project name `"FarmSaathi"` and includes 13 modules (`:app`, `:core:model`, `:core:common`, `:core:designsystem`, `:core:database`, `:core:datastore`, `:core:audio`, `:core:media`, `:feature:home`, `:feature:farm`, `:feature:diary`, `:feature:finance`, `:feature:operations`, `:feature:reports`).
  - `gradle.properties`: `org.gradle.jvmargs=-Xmx2048m -XX:+UseG1GC`, `android.suppressUnsupportedCompileSdk=35`.
  - `gradle/wrapper/gradle-wrapper.properties`: Gradle version `8.9` (`gradle-8.9-bin.zip`).
  - `local.properties`: `sdk.dir=C\:\\Users\\nithi\\AppData\\Local\\Android\\Sdk`.
- CLI commands & tasks:
  - Wrapper: `.\gradlew.bat` (Windows PowerShell/CMD) or `./gradlew` (Bash).
  - Gradle runtime: Gradle 8.9 running on JDK 21 (`C:\Program Files\Java\jdk-21`).
  - Compilation tasks:
    - `:feature:farm:compileDebugKotlin` / `:core:common:compileDebugKotlin` / `:app:compileDebugKotlin`
    - `:app:assembleDebug` / `assembleDebug`
  - Unit test tasks:
    - `:feature:farm:testDebugUnitTest` / `:core:common:testDebugUnitTest` / `:feature:home:testDebugUnitTest`
    - `testDebugUnitTest` (all modules)
  - Lint and formatting tasks:
    - Ktlint: `.\gradlew.bat ktlintCheck` (check), `.\gradlew.bat ktlintFormat` (auto-format)
    - Detekt: `.\gradlew.bat detekt`
    - Android Lint: `.\gradlew.bat lintDebug`

### 1.5 Git Configuration
- Working directory: `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi`
- Git branch: `main`
- Status: `Your branch is up to date with 'origin/main'.`
- Git remotes (`git remote -v`):
  ```
  origin  https://github.com/Nithinaare/sathi.git (fetch)
  origin  https://github.com/Nithinaare/sathi.git (push)
  ```
- Confirmed: matches required target repository URL `https://github.com/Nithinaare/sathi.git`.

---

## 2. Logic Chain

1. **Premise (Navigation)**:
   - Observation 1.1 establishes that `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` are already defined as `@Serializable data class` in `FarmRoute.kt`.
   - Observation 1.2 establishes that `FarmSaathiNavHost.kt` includes both `onboardingHomeMoreNavGraph` and `farmAndCropsNavGraph` in a single `NavHost`.
   - In Navigation Compose 2.8+, destinations cannot be registered twice in the same `NavHost` without throwing collision errors.
   - Therefore, the destination composables (`composable<FarmRoute.TeluguCalendar>` and `composable<FarmRoute.KarteGuide>`) must be declared once in `farmAndCropsNavGraph(nav)` (as calendar and karte guide are farm features implemented in `feature/farm`).
   - Callers from `OnboardingHomeMoreNavGraph.kt` (such as `HomeDashboardScreen` clicking on `HomePanchangKarteBanner` or `MoreHubScreen`) and callers from `FarmAndCropsNavGraph.kt` (such as `FarmsListScreen`) can navigate to them by executing `nav.navigate(FarmRoute.TeluguCalendar(...))` and `nav.navigate(FarmRoute.KarteGuide(...))`.

2. **Premise (Feature Implementation Scope)**:
   - Observation 1.3 reveals that `feature/farm` contains `CropAndCalendarViewModel`, `CropsAndCalendarScreens.kt`, and `FarmsAndPlotsScreens.kt`, but lacks `TeluguCalendarViewModel`, `TeluguCalendarScreen`, and `KarteGuideScreen`.
   - To fulfill R2, `TeluguCalendarViewModel` must be created in `feature/farm/viewmodel/`, injecting `TeluguCalendarRepository` (from `:core:common`), `FarmRepository`, `UserPreferencesRepository`, `AudioGuidanceEngine`, `TimeProvider`, and `AppDispatchers`.
   - `TeluguCalendarScreen` and `KarteGuideScreen` should be placed under `feature/farm/screens/` conforming to the existing design system patterns observed in `CropsAndCalendarScreens.kt`.

3. **Premise (Build & Test Toolchain)**:
   - Observation 1.4 confirms that the project uses AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9 wrapper (`gradlew.bat`), and JDK 21.
   - Detekt and Ktlint are configured in the root build file and enforce code quality.
   - The verified test task is `testDebugUnitTest` and compile task is `assembleDebug` / `compileDebugKotlin`.

---

## 3. Caveats

- **Existing Uncommitted Working Tree Files**: There are already working tree modifications present in `core/common/`, `core/database/`, `core/designsystem/`, and `gradle.properties`, as well as untracked files (`OfflineTeluguCalendarRepositoryImpl.kt`, `TeluguCalendarModels.kt`, `HomePanchangKarteBanner.kt`). The navigation and feature implementers must build upon these rather than overwriting or duplicating them.
- **Navigation Graph Destination Ownership**: Because `FarmSaathiNavHost` combines all graphs, destination `composable<FarmRoute.TeluguCalendar>` should be housed in `FarmAndCropsNavGraph.kt`, while `HomeDashboardScreen` in `OnboardingHomeMoreNavGraph.kt` is passed a navigation lambda (`onOpenCalendar = { epochDay -> nav.navigate(FarmRoute.TeluguCalendar(epochDay)) }`).

---

## 4. Conclusion

1. **Navigation Wiring Plan**:
   - In `FarmAndCropsNavGraph.kt`: Register `composable<FarmRoute.TeluguCalendar>` and `composable<FarmRoute.KarteGuide>`.
   - In `OnboardingHomeMoreNavGraph.kt`: Wire navigation callbacks from `HomeDashboardScreen` (and optionally `MoreHubScreen`) to call `nav.navigate(FarmRoute.TeluguCalendar())` and `nav.navigate(FarmRoute.KarteGuide())`.
   - In `FarmAndCropsNavGraph.kt`: Provide quick navigation to `TeluguCalendar` and `KarteGuide` from `FarmsListScreen`.
2. **Feature Farm Implementation Plan**:
   - Create `TeluguCalendarViewModel.kt` in `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/` exposing a merged state of Panchang calendar day/alert data with `CropTask` schedules.
   - Create `TeluguCalendarScreen.kt` in `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/` with Month View, Karte Timeline View, day details bottom sheet, and 1-tap task navigation.
   - Create `KarteGuideScreen.kt` in `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/` displaying the 27 Kartis catalog.
   - Write unit tests in `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt` using Robolectric and coroutine test dispatchers.
3. **Build & Quality Commands**:
   - Compile: `.\gradlew.bat :feature:farm:compileDebugKotlin :app:compileDebugKotlin`
   - Test: `.\gradlew.bat :feature:farm:testDebugUnitTest :core:common:testDebugUnitTest :feature:home:testDebugUnitTest`
   - Quality checks: `.\gradlew.bat ktlintCheck detekt`
   - Assemble: `.\gradlew.bat :app:assembleDebug`
   - Git push target: `https://github.com/Nithinaare/sathi.git` on branch `main`.

---

## 5. Verification Method

To independently verify the observations and conclusions in this report:

1. **Verify Navigation Routes & Graph Files**:
   ```powershell
   Get-Content "core\common\src\main\kotlin\com\farmsaathi\core\common\navigation\FarmRoute.kt" | Select-String "TeluguCalendar|KarteGuide"
   Get-Content "app\src\main\kotlin\com\farmsaathi\app\navigation\FarmAndCropsNavGraph.kt" | Select-String "TeluguCalendar"
   Get-Content "app\src\main\kotlin\com\farmsaathi\app\navigation\OnboardingHomeMoreNavGraph.kt" | Select-String "HomeDashboardScreen"
   ```
2. **Verify Feature Farm Structure**:
   ```powershell
   Get-ChildItem -Recurse "feature\farm\src\main\kotlin" | Select-Object FullName
   ```
3. **Verify Git Remote**:
   ```powershell
   git remote -v
   git branch
   ```
4. **Verify Gradle Commands**:
   ```powershell
   .\gradlew.bat --version
   .\gradlew.bat :feature:farm:tasks --all | Select-String "testDebugUnitTest|compileDebugKotlin"
   ```
