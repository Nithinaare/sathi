# BRIEFING — 2026-11-14T09:18:00Z

## Mission
Review and adversarial stress-test Milestone 1 changes (Core Repository & Home Screen Dynamic Alert Banner).

## 🔒 My Identity
- Archetype: reviewer_critic
- Roles: reviewer, critic
- Working directory: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
- State folder: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_reviewer_m1_1
- Original parent: 079bcced-b104-4b1b-9219-13d48cdb214a
- Milestone: M1: Core Repository & Home Screen Dynamic Alert Banner
- Instance: 1 of 2 (Reviewer 1)

## 🔒 Key Constraints
- Review-only — do NOT modify implementation code
- Check for integrity violations (hardcoded test data, facades, shortcuts, fake verifications)
- Verify claims independently via code inspection, testing, and adversarial scenarios

## Current Parent
- Conversation ID: 079bcced-b104-4b1b-9219-13d48cdb214a
- Updated: 2026-11-14T09:18:00Z

## Review Scope
- **Files to review**:
  - `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`
  - `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
  - `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`
  - `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`
- **Interface contracts**:
  - `ORIGINAL_REQUEST.md`, `PROJECT.md`
  - Worker handoff report: `.agents/teamwork/teamwork_worker_m1_1/handoff.md`
- **Review criteria**: correctness, completeness against R1 requirements, null safety, audio state cleanup, reactive flow lifecycle, edge cases, test integrity.

## Key Decisions Made
- Verdict issued: REQUEST_CHANGES.
- Core integrity check passed (no facades, authentic ephemeris engine, real Compose layout).
- Critical failure identified: `:feature:home:testDebugUnitTest` fails on headless audio toggling when `uiState` is uncollected.
- Major adversarial bug surfaced: "పోలాల అమావాస్య" agro-festival alert is unreachable dead code due to Amavasya priority order in `OfflineTeluguCalendarRepositoryImpl`.

## Artifact Index
- `DISPATCH.md` — Inbound instructions from parent
- `BRIEFING.md` — Persistent working memory
- `progress.md` — Liveness heartbeat and task tracker
- `handoff.md` — Comprehensive review & challenge report

## Review Checklist
- **Items reviewed**:
  - `OfflineTeluguCalendarRepositoryTest.kt`: Standard day fixture fix and Pournami test verified
  - `HomeDashboardScreen.kt`: `HomePanchangKarteBanner` insertion, layout placement, backwards-compatible callback verified
  - `HomePanchangKarteBanner.kt`: Typography, lunar badge, click isolation, accessibility verified
  - `HomeDashboardUiTest.kt`: 6 unit tests passing deterministically
  - `HomeViewModel.kt`: Dependency on `WhileSubscribed` `uiState` causes silent drop of audio playback when uncollected (CRITICAL BUG)
  - `OfflineTeluguCalendarRepositoryImpl.kt`: `calculateTodayAlert` priority order eclipses "పోలాల అమావాస్య" (MAJOR ADVERSARIAL BUG)
- **Verdict**: REQUEST_CHANGES
- **Unverified claims**: Worker's claim that full test suite passes without regressions is invalidated by failing headless audio tests in `:feature:home:testDebugUnitTest`.

## Attack Surface
- **Hypotheses tested**:
  - Uncollected `uiState` during `togglePanchangAudio()` -> Confirmed failure mode (silent drop).
  - "పోలాల అమావాస్య" alert reachable -> Disproven (unreachable due to generic Amavasya branch precedence).
  - Date rollover across midnight -> Documented caveat.
- **Vulnerabilities found**:
  - `HomeViewModel.togglePanchangAudio()` reads `uiState.value.todayAlert`, which is null when `uiState` has no active subscribers.
  - `OfflineTeluguCalendarRepositoryImpl.calculateTodayAlert()` evaluates `day.isAmavasya` before `day.isFestival`, masking `పోలాల అమావాస్య`.
- **Untested angles**:
  - System font scaling (> 1.5x) on Karte badge wrapping.
