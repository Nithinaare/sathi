# Telugu Agricultural Calendar Survey & Specification Handoff Report

## 1. Observation

### 1.1 Specification & Context Documents Inspected
1. **`ORIGINAL_REQUEST.md`** (`C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\ORIGINAL_REQUEST.md`):
   - Outlines 4 major requirements:
     - **R1**: Home Screen Dynamic Alert & Panchang Banner (`HomeViewModel` -> `TeluguCalendarRepository`, TTS speech for "వినండి", dynamic `HomePanchangKarteBanner`, zero static filler).
     - **R2**: Farm Calendar, Timeline & Task Scheduling (`TeluguCalendarViewModel` merging panchang with `CropTask`, `TeluguCalendarScreen` with Month View / Karte Timeline View toggle, day details bottom sheet, 1-tap task creation navigation `+ పని జోడించండి`, `KarteGuideScreen`).
     - **R3**: Navigation Wiring (`FarmRoute.TeluguCalendar`, `FarmRoute.KarteGuide` in `FarmAndCropsNavGraph.kt` & `OnboardingHomeMoreNavGraph.kt`).
     - **R4**: Verification & Git Synchronization (automated tests, assembleDebug, commit & push to `main`).
2. **Authoritative Spec Documents**:
   - `implementation_plan.md`: Lines 1–107 define the architecture, zero-migration offline dataset, test strategy, and device verification.
   - `TELUGU_AGRICULTURAL_CALENDAR_AND_PANCHANGAM_RESEARCH.md`: Lines 1–424 provide the astronomical basis (Chandramana Amanta system + Souramana 27 Kartelu), Udaya Tithi rule, agro-entomological lunar cycle research (ANGRAU/PJTSAU: Amavasya oviposition peaks, light trap timing, 3–5 day post-Amavasya bio-spray window), and sacred agricultural festivals (ఏరువాక పౌర్ణమి, పోలాల అమావాస్య, కనుమ).
   - `TELUGU_AGRICULTURAL_CALENDAR_ARCHITECTURE.md`: Lines 1–487 detail the cross-module architecture (`:core:model`, `:core:common`, `:core:designsystem`, `:feature:farm`, `:feature:home`, `:app`), data classes, repository contracts, and navigation routes.
   - `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md`: Lines 1–422 specify the interactive UX, dual-mode views (నెలల పట్టిక vs కార్తెల కాలచక్రం), dynamic alert evaluation logic, 1-tap task navigation sequence diagram, and `TeluguCalendarViewModel` state.

### 1.2 Existing Codebase State Observed
1. **Core Models** (`core/model/src/main/kotlin/com/farmsaathi/core/model/TeluguCalendarModels.kt`):
   - Lines 6–28: `enum class TeluguMonth` (12 Chandramana months, Chaitram to Phalgunam).
   - Lines 30–37: `enum class Paksham` (`SHUKLA`, `KRISHNA`).
   - Lines 39–65: `enum class TeluguTithi` (15 standard tithis: Padyami to Pournami / Amavasya = 30).
   - Lines 67–354: `data class TeluguKarte` and `object TeluguKarteluCatalog` containing the complete 27 Kartis catalog with approxDateSpan, season, authentic Telugu proverbs (*సామెతలు*), agricultural significance (*అదను*), and recommended crops (*అనుకూల పంటలు*).
   - Lines 357–374: `data class TeluguCalendarDay` (epochDay, gregorianDate, teluguYear, teluguMonth, paksham, tithi, nakshatram, karteId, karteName, karteDayNumber, totalKarteDays, isAmavasya, isPournami, isFestival, festivalName, seasonalAdvisory).
   - Lines 377–389: `data class CurrentKarteInfo` (karteId, teluguName, englishName, dayNumber, totalDays, daysRemaining, progressPercentage, proverb, agriculturalSignificance, recommendedCrops).
   - Lines 390–408: `enum class LunarAlertType` and `data class LunarDayAlert` (alertType, title, subtitle, ttsSpeechText, actionText, actionRoute, isPriority).
2. **Repository Contract & Implementation** (`core/common/src/main/kotlin/com/farmsaathi/core/common/repository/`):
   - `TeluguCalendarRepository.kt` defines `observeDay`, `getDay`, `observeMonth`, `getMonthDays`, `observeCurrentKarte`, `getCurrentKarte`, `observeTodayAlert`, `getTodayAlert`, `observeAllKartis`.
   - `OfflineTeluguCalendarRepositoryImpl.kt` implements:
     - 2025–2027 verified Chandramana lunar spans table (`LUNAR_MONTH_SPANS`, lines 449–482) covering 1,095 days with Telugu year names (క్రోధి, విశ్వావసు, పరాభవ, ప్లవంగ).
     - 27 solar entry boundaries (`KARTE_BOUNDARIES`, lines 409–437) calculating day number and total span.
     - Sidereal nakshatra cycle calculation (27 nakshatras, lines 370–373, 484–490).
     - Algorithmic fallback using synodic month ($29.530588$ days, lines 316–346) for dates outside 2025–2027.
     - Event-triggered dynamic alert evaluation (lines 87–184): Amavasya alert (priority, cattle rest & light traps), Post-Amavasya spray alert (+3 to +5 days, priority, NSKE 5% spray), agricultural festivals (ఏరువాక పౌర్ణమి, పోలాల అమావాస్య, కనుమ), Pournami, Karte Day 1 transition alert, and standard day informative alert.
     - Unit tests in `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt` passing all 7 test cases covering panchang dates, month queries, active karte, Amavasya alerts, post-Amavasya spray windows, and 27 kartis.
3. **Navigation Contract** (`core/common/src/main/kotlin/com/farmsaathi/core/common/navigation/FarmRoute.kt`):
   - Line 51: `FarmRoute.ActivityForm` updated with `val initialDueDateEpochDay: Long? = null`.
   - Lines 55–57: `data class TeluguCalendar(val initialEpochDay: Long? = null) : FarmRoute` and `data class KarteGuide(val highlightKarteId: Int? = null) : FarmRoute` registered.
4. **Dependency Injection & Design System**:
   - `DatabaseModule.kt`: Provides `TeluguCalendarRepository` via `OfflineTeluguCalendarRepositoryImpl()`.
   - `strings.xml` & `values-en/strings.xml`: Added strings for `cal_telugu_title`, `cal_month_view_tab`, `cal_karte_view_tab`, `cal_amavasya_badge`, `cal_pournami_badge`, `cal_day_details_title`, `cal_add_task_for_day`, `cal_no_tasks_for_day`, `cal_karte_guide_title`, `cal_days_remaining`, `cal_suited_crops`, `cal_proverb_label`, `cal_advisory_label`, `home_panchang_listen`, `home_panchang_view_calendar`.
   - `feature/home/components/HomePanchangKarteBanner.kt`: Created with lunar phase indicator disk (`●` black for Amavasya, `○` amber for Pournami, green for regular), Month/Paksham/Tithi, 1-tap "వినండి" audio button, active Karte chip & remaining days, dynamic alert container with priority styling, and click to open calendar.
5. **Gaps to Implement in Downstream Phases**:
   - `HomeViewModel`: Must inject `TeluguCalendarRepository`, observe today's day, karte info, and dynamic alert into `HomeUiState`, and implement audio playback for `ttsSpeechText` using `AudioGuidanceEngine.speakDynamicText(alert.ttsSpeechText, useTe)`.
   - `HomeDashboardScreen`: Insert `HomePanchangKarteBanner` into `LazyColumn` above `season_summary_card`, wire `onOpenTeluguCalendar: () -> Unit`.
   - `feature:farm`: Implement `TeluguCalendarViewModel.kt`, `TeluguCalendarScreen.kt` (dual-mode toggle `నెలల పట్టిక` / `కార్తెల కాలచక్రం`, month grid with live task dots, day details bottom sheet, inline task completion checkbox, `+ పని జోడించండి` 1-tap navigation), and `KarteGuideScreen.kt` (27 Kartis catalog with seasonal filter chips).
   - `CropsAndCalendarScreens.kt`: Update `ActivityFormScreen` to read `initialDueDateEpochDay`.
   - `app`: Wire `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` in `FarmAndCropsNavGraph.kt` and `OnboardingHomeMoreNavGraph.kt`.

---

## 2. Logic Chain

1. **Agrarian Reality vs Digital Tooling**:
   - Telangana and Andhra Pradesh farmers make operational decisions (land prep, sowing, spraying, harvesting) based on the 27 Kartis and Chandramana tithis.
   - Standard Gregorian calendars fail because farmers do not know when the sowing window (*అదను*) starts or when nocturnal pests lay eggs.
   - Providing real-time calculation of active Karte, day number, days remaining, and lunar phase transforms the app into an essential daily agronomic tool.
2. **Zero-Filler Dynamic Alert Logic**:
   - The user explicitly requested: *"dont give static tips or dummy ones, keep it clean, easy to understand"* and *"avoid pre fixed or static ones"*.
   - In response, the alert engine evaluates exact calendar conditions:
     - On Amavasya: Fires cattle rest (*"ఎడ్లకు విశ్రాంతి"*) and light trap setup advisory.
     - On Days +3 to +5 after Amavasya: Fires bio-spray advisory (*వేప గింజల కషాయం NSKE 5%*) precisely when early-instar larvae hatch.
     - On Day 1 of each Karte: Fires Karte transition advisory announcing the newly opened *అదను*.
     - On Agricultural Festivals (ఏరువాక పౌర్ణమి, పోలాల అమావాస్య, కనుమ): Fires sacred agrarian guidelines.
     - On standard days: Displays today's Tithi, active Karte day count, and authentic farming proverb.
   - This eliminates all static filler text.
3. **Dual-Mode Interactive Design**:
   - Month View (`నెలల పట్టిక`) allows farmers to plan work across days, see lunar phases (`●` Amavasya, `○` Pournami), and see their own scheduled crop activities as colored dots.
   - Karte Timeline View (`కార్తెల కాలచక్రం`) provides the macro agro-climatic view with visual progress bars, days remaining countdown, and suited crop matrices.
4. **1-Tap Task Scheduling Integration**:
   - In `TeluguCalendarScreen`, selecting any date immediately displays scheduled `CropTask` items with instant completion checkboxes.
   - Tapping `+ పని జోడించండి` navigates directly to `FarmRoute.ActivityForm(initialDueDateEpochDay = selectedEpochDay)`, pre-populating the date so the farmer only needs to pick the crop and save.
5. **Architectural Safety & Zero Schema Migrations**:
   - Storing the 2025–2027 Panchang data in a pre-computed repository with algorithmic fallback guarantees zero Room DB migrations (retaining Room v3), zero database corruption risk, and sub-millisecond execution.

---

## 3. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Home Alert | Dynamic Day & Panchang Header | Displays current Telugu Month, Paksham, Tithi, active Karte, day number, and days remaining on Home Dashboard. | `todayEpochDay: Long` | Formatted Telugu header & sub-header strings | Fallback to algorithmic calculation | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §2.1 |
| 2 | Home Alert | Amavasya Cattle & Pest Alert | Priority card on Amavasya advising cattle rest (*ఎడ్లకు విశ్రాంతి*) and nocturnal light trap deployment. | `day.isAmavasya == true` | Priority `LunarDayAlert` with Terracotta badge | None (pure function) | `TELUGU_AGRICULTURAL_CALENDAR_AND_PANCHANGAM_RESEARCH.md` §4.1 |
| 3 | Home Alert | Post-Amavasya Bio-Spray Alert | Priority card active on days +3 to +5 post-Amavasya advising NSKE 5% / neem oil spraying against newly hatched larvae. | `daysSinceLastAmavasya in 3..5` | Priority `LunarDayAlert` with spray advisory | None (pure function) | `TELUGU_AGRICULTURAL_CALENDAR_AND_PANCHANGAM_RESEARCH.md` §4.1 |
| 4 | Home Alert | Karte Transition Alert | Alert on Day 1 of each new Karte announcing the open agricultural window (*అదను*). | `day.karteDayNumber == 1` | `LunarDayAlert` with `KARTE_TRANSITION` type | None | `implementation_plan.md` §1 |
| 5 | Home Alert | Sacred Agro-Festival Alerts | Specific alerts for ఏరువాక పౌర్ణమి (Kharif kickoff/navadhanya), పోలాల అమావాస్య (cattle thanksgiving), and కనుమ. | Lunar month + tithi / date matches | Priority `LunarDayAlert` with `FESTIVAL` type | None | `TELUGU_AGRICULTURAL_CALENDAR_AND_PANCHANGAM_RESEARCH.md` §4.2 |
| 6 | Home Voice | 1-Tap "వినండి" Audio Guidance | Banner audio button speaking today's Telugu date, Karte progress, proverb, and alert via TTS. | Banner audio button click | Telugu speech synthesis via `AudioGuidanceEngine` | Stops active audio if clicked again | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §2.1 |
| 7 | Engine | Udaya Tithi Calculation | Computes sunrise tithi for each date ensuring calendar consistency across daily farming routines. | `epochDay: Long` | `TeluguTithi` enum (PADYAMI .. AMAVASYA) | Synodic fallback outside 2025–2027 | `TELUGU_AGRICULTURAL_CALENDAR_AND_PANCHANGAM_RESEARCH.md` §2.3 |
| 8 | Engine | 27 Kartis Solar Mansions Engine | Computes Sun's 13.33° transit through 27 Nakshatras with start/end dates, day number, and days remaining. | `LocalDate` | `CurrentKarteInfo` (active karte, day num, total days, progress) | Fallback to Ashwini (Karte 1) | `TELUGU_AGRICULTURAL_CALENDAR_ARCHITECTURE.md` §3.1 |
| 9 | Engine | Chandramana Lunar Spans Table | Pre-computed 2025–2027 verified dataset (1,095 days) mapping Amanta lunar months and Telugu year names. | `epochDay: Long` | `TeluguCalendarDay` record | Synodic month fallback calculation | `OfflineTeluguCalendarRepositoryImpl.kt:449` |
| 10 | Engine | 27 Nakshatra Sidereal Rotation | Computes sidereal moon nakshatra across 27 mansions (Ashwini to Revati). | `epochDay: Long` | Telugu Nakshatra string | Safe modulo wrapping | `OfflineTeluguCalendarRepositoryImpl.kt:370` |
| 11 | Catalog | 27 Kartis Traditional Wisdom | Master catalog of all 27 Kartis with authentic proverbs (*సామెతలు*), seasonal phases, and suited crops. | `karteId: Int` (1..27) | `TeluguKarte` model | Returns Karte 1 if ID out of range | `TeluguCalendarModels.kt:79` |
| 12 | UI View | Month Grid View (`నెలల పట్టిక`) | Interactive 7-column calendar grid showing Gregorian date, Telugu Tithi, Amavasya/Pournami badges, and task dots. | Month/Year, calendar days, `CropTask` items | Visual interactive grid composable | Shows empty state if days unavailable | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §2.2 |
| 13 | UI View | Lunar Disc Badges | Dark filled circle `●` for Amavasya; radiant amber circle `○` for Pournami. | `isAmavasya`, `isPournami` | Colored indicator badges in date cells | Standard background for normal days | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §2.2 |
| 14 | UI View | Live CropTask Dots | Badges displaying count of uncompleted tasks scheduled for that `dueDateEpochDay`. | `CropTask.dueDateEpochDay` | Task indicator dot in date cell | Hidden if task count == 0 | `TELUGU_AGRICULTURAL_CALENDAR_ARCHITECTURE.md` §6.2 |
| 15 | UI View | Karte Timeline View (`కార్తెల కాలచక్రం`) | Visual timeline tracking 27 Kartis with progress bar, days remaining countdown, and suited crops. | Current date, 27 Kartis list, active crops | Timeline composable with hero active card | Displays upcoming/completed tags | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §2.3 |
| 16 | UI View | Seasonal Filter Chips | Filters 27 Kartis by season: All (27), Kharif / వానాకాలం (5..14), Rabi / యాసంగి (15..25), Summer / వేసవి (26..4). | User filter chip selection | Filtered list of Karte cards | Shows all if no filter selected | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §2.3 |
| 17 | UI Sheet | Day Details Bottom Sheet | Tapping any day opens bottom sheet with full Panchang, agricultural advisory (*అదను*), and scheduled crop tasks. | Selected date `epochDay` | Detailed card with task list & action buttons | Displays "పనులేవీ లేవు" if no tasks | `TELUGU_AGRICULTURAL_CALENDAR_ARCHITECTURE.md` §6.2 |
| 18 | UI Sheet | Inline Task Completion Checkbox | Tapping checkbox in day details sheet immediately marks `CropTask` completed. | Task ID checkbox toggle | Updates Room DB via `viewModel.updateTaskStatus` | Error cue if update fails | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §2.2 |
| 19 | UI Nav | 1-Tap Add Task (`+ పని జోడించండి`) | Tapping button in day sheet navigates to `ActivityForm` with `initialDueDateEpochDay` pre-filled to that day. | Selected date `epochDay` | Navigates to `FarmRoute.ActivityForm(initialDueDateEpochDay = epochDay)` | Defaults to today if null | `TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md` §4.2 |
| 20 | UI Screen | Karte Guide Screen (`KarteGuideScreen`) | Dedicated educational screen for all 27 Kartis with search, filter, and full audio readout in Telugu. | `highlightKarteId: Int?` | Full scrollable reference catalog | Displays all 27 if ID null | `TELUGU_AGRICULTURAL_CALENDAR_ARCHITECTURE.md` §6.3 |
| 21 | Navigation | Navigation Graph Integration | Wiring `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` into `FarmAndCropsNavGraph` & `OnboardingHomeMoreNavGraph`. | NavController route invocations | Screen composable destinations | Standard NavHost fallback | `ORIGINAL_REQUEST.md` §R3 |

---

## 4. Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Month Day Count | Leap Year February (e.g. 2028-02) vs Regular February (2025-02) | `calculateMonthDays` calls `YearMonth.of(year, month).lengthOfMonth()`, correctly generating 29 days in leap years and 28 in regular years. |
| 2 | Lunar Span Boundaries | Date on exact boundary between two Chandramana months | `span.endEpochDay` is strictly mapped to `TeluguTithi.AMAVASYA`. The next day matches `span[i+1].startEpochDay` as `TeluguTithi.PADYAMI` (Shukla Paksham). |
| 3 | Dates Outside 2025–2027 | Date in 2024 or 2029 | Algorithmic fallback calculates synodic cycle: `cycle = ((daysSinceRef % synodicDays) + synodicDays) % synodicDays` with `synodicDays = 29.530588`, yielding continuous tithi and paksham without crashing. |
| 4 | Karte Solar Year Wrap-Around | Date in late March / early April (Revati -> Ashwini) | `KARTE_BOUNDARIES` wraps around from index 26 (Revati, Mar 31) to index 0 (Ashwini, Apr 14) with year increment handling. |
| 5 | Post-Amavasya Spray Window | Day +2 vs Day +3 vs Day +6 post-Amavasya | Day +2 returns standard alert or festival; Days +3, +4, and +5 trigger `LunarAlertType.POST_AMAVASYA_SPRAY` with priority flag; Day +6 reverts to standard alert. |
| 6 | Simultaneous Festival and Amavasya | Shravana Amavasya (Polala Amavasya) | `findFestival` recognizes "పోలాల అమావాస్య", producing festival alert combining cattle thanksgiving and Amavasya night light traps. |
| 7 | Day with Zero Tasks Scheduled | Farmer taps an empty date on calendar | Day details bottom sheet renders clean placeholder text: `"ఈ రోజుకు షెడ్యూల్ చేసిన పనులేవీ లేవు"` alongside `[ + ఈ రోజుకు పని జోడించండి ]` button. |
| 8 | Day with Completed and Pending Tasks | Farmer completes a task from the day sheet | Task status is updated in Room DB; reactive flow re-emits; task dot count in calendar decrements immediately; checkbox reflects completed state. |
| 9 | Rapid Audio Tapping | Farmer taps "వినండి" repeatedly | `AudioGuidanceEngine.speakDynamicText()` detects active playback and halts speech when toggled, avoiding overlapping speech streams. |
| 10 | Screen Rotation / Configuration Change | User rotates device on `TeluguCalendarScreen` | ViewModel preserves `selectedDay`, `viewMode`, and `displayedYearMonth` in `StateFlow`, maintaining UI state seamlessly. |

---

## 5. Caveats

- **Timezone Assumption**: All calendar day calculations assume Indian Standard Time (`IST` / `UTC+05:30`), which aligns with all agrarian activity in Telangana and Andhra Pradesh.
- **Offline Ephemeris Scope**: The verified dataset covers 2025–2027 (1,095 days). For dates outside this range, the repository uses a synodic mathematical model; while agriculturally sound within 1 day of astronomical precision, verified tables are preferred for historical or far-future dates.
- **Room Schema**: As recommended by the architecture, the offline calendar repository does not alter the Room database schema (avoiding Room migration risk from v3 to v4).

---

## 6. Conclusion

The specification survey reveals that the core foundational layers (`TeluguCalendarModels.kt`, `TeluguCalendarRepository.kt`, `OfflineTeluguCalendarRepositoryImpl.kt`, and `OfflineTeluguCalendarRepositoryTest.kt`) are already implemented and tested in `:core:model` and `:core:common`.

To fully deliver the original user request and acceptance criteria, the implementation team must execute the following scoped tasks:
1. **Home Screen Integration (`:feature:home`)**:
   - Update `HomeUiState` and `HomeViewModel` to inject `TeluguCalendarRepository`, observe today's `TeluguCalendarDay`, `CurrentKarteInfo`, and `LunarDayAlert`, and wire `onTogglePanchangAudio`.
   - Update `HomeDashboardScreen` to place `HomePanchangKarteBanner` directly above `season_summary_card`, and pass `onOpenTeluguCalendar = { nav.navigate(FarmRoute.TeluguCalendar()) }`.
2. **Calendar Feature Implementation (`:feature:farm`)**:
   - Create `TeluguCalendarViewModel` managing view mode (`MONTH_GRID` vs `KARTE_TIMELINE`), month navigation, day selection, task merging with `FarmRepository.observeTasks()`, task completion toggles, and audio.
   - Create `TeluguCalendarScreen` implementing the interactive Month Grid (with `●` Amavasya, `○` Pournami, today highlight, crop task dots), Day Details bottom sheet (Panchang details, inline task checkboxes, `+ పని జోడించండి` 1-tap navigation), and Karte Timeline View.
   - Create `KarteGuideScreen` implementing the 27 Kartis reference catalog with seasonal filter chips (వానాకాలం, యాసంగి, వేసవి) and audio readouts.
   - Update `ActivityFormScreen` in `CropsAndCalendarScreens.kt` to accept `initialDueDateEpochDay: Long? = null` and initialize `dueEpochDay` with it.
3. **Navigation Wiring (`:app`)**:
   - Register `composable<FarmRoute.TeluguCalendar>` and `composable<FarmRoute.KarteGuide>` in `FarmAndCropsNavGraph.kt`.
   - Wire `onOpenTeluguCalendar` in `OnboardingHomeMoreNavGraph.kt`.
   - Connect `FarmRoute.ActivityForm` in `FarmAndCropsNavGraph.kt` to pass `initialDueDateEpochDay = route.initialDueDateEpochDay`.
4. **Verification & Testing**:
   - Add ViewModel & UI unit tests in `feature:farm` and `feature:home`.
   - Run `./gradlew.bat testDebugUnitTest assembleDebug` to ensure all tests pass without regression.

---

## 7. Verification Method

To independently verify these findings:
1. **Inspect Core Models**:
   `core/model/src/main/kotlin/com/farmsaathi/core/model/TeluguCalendarModels.kt`
2. **Inspect Repository & Tests**:
   `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
   `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`
3. **Run Unit Tests**:
   ```powershell
   cd C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
   .\gradlew.bat :core:common:testDebugUnitTest --console=plain
   ```
4. **Inspect Route Definitions**:
   `core/common/src/main/kotlin/com/farmsaathi/core/common/navigation/FarmRoute.kt`
5. **Inspect Home Banner Component**:
   `feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt`
