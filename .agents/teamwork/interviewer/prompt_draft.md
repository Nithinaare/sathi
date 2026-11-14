Implement the interactive Telugu Agricultural Calendar and dynamic lunar alert system for farmers in the Farm Saathi Android application.

Working directory: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
Integrity mode: development

Reference Material:
- Implementation Plan: file:///C:/Users/nithi/.gemini/antigravity/brain/10b008e7-4e30-42dd-aafb-3cafee65ec3d/implementation_plan.md
- Research Spec: file:///C:/Users/nithi/.gemini/antigravity/brain/10b008e7-4e30-42dd-aafb-3cafee65ec3d/TELUGU_AGRICULTURAL_CALENDAR_AND_PANCHANGAM_RESEARCH.md
- Architecture Spec: file:///C:/Users/nithi/.gemini/antigravity/brain/10b008e7-4e30-42dd-aafb-3cafee65ec3d/TELUGU_AGRICULTURAL_CALENDAR_ARCHITECTURE.md
- Interactive Spec: file:///C:/Users/nithi/.gemini/antigravity/brain/10b008e7-4e30-42dd-aafb-3cafee65ec3d/TELUGU_AGRICULTURAL_CALENDAR_INTERACTIVE_SPEC.md

## Requirements

### R1. Home Screen Dynamic Alert & Panchang Banner
Connect `HomeViewModel` to `TeluguCalendarRepository` to observe today's Telugu calendar day and alert, and inject TTS speech for the 1-tap "వినండి" audio button. Display the dynamic `HomePanchangKarteBanner` on `HomeDashboardScreen` with Amavasya and day alerts, keeping alerts dynamic and avoiding static or pre-fixed tips.

### R2. Farm Calendar, Timeline & Task Scheduling
Create `TeluguCalendarViewModel` in `feature/farm` merging panchang data with `CropTask` items. Create `TeluguCalendarScreen` featuring a toggle between Month View (`నెలల పట్టిక`) and Karte Timeline View (`కార్తెల కాలచక్రం`), a day details bottom sheet, and 1-tap task creation navigation (`+ పని జోడించండి`). Create `KarteGuideScreen`.

### R3. Navigation Wiring
Wire `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` in `FarmAndCropsNavGraph.kt` and `OnboardingHomeMoreNavGraph.kt`.

### R4. Verification & Git Synchronization
Run test verification and build static analysis tasks determined by the team. Commit and push the completed implementation to `https://github.com/Nithinaare/sathi.git` directly on the `main` branch.

## Acceptance Criteria

### Calendar & Alert Integration
- [ ] `HomeViewModel` observes `TeluguCalendarRepository` and exposes today's Telugu calendar day, dynamic lunar alert, and TTS audio playback triggering.
- [ ] `HomePanchangKarteBanner` is added to `HomeDashboardScreen`.
- [ ] `TeluguCalendarViewModel` merges lunar calendar data with `CropTask` items.
- [ ] `TeluguCalendarScreen` allows switching between Month View and Karte Timeline View, presents day details in a bottom sheet, and supports 1-tap navigation to add tasks.
- [ ] `KarteGuideScreen` displays the 27 Kartis catalog with their agricultural significance.
- [ ] `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` are navigable from both `FarmAndCropsNavGraph` and `OnboardingHomeMoreNavGraph`.

### Verification & Delivery
- [ ] Gradle build and automated test suites pass without regressions.
- [ ] The completed implementation is committed and pushed to `https://github.com/Nithinaare/sathi.git` on the `main` branch.