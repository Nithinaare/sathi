# Antigravity task — Integrate and verify Farm Saathi Land Measurement

You are working in the existing Farm Saathi Android repository. The provided integration package contains an implementation of GPS/manual plot-area measurement that was written to match this project's current module boundaries, Room model, navigation routes and Compose design system.

## Main objective

Integrate the supplied changes into the active Antigravity workspace, build the complete Android application, fix any integration/compiler/test errors, and verify the migration and feature behaviour. Do not regenerate the feature from scratch and do not replace the existing Farm Saathi architecture.

## Important source files

Review all files under `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/measurement/`, plus:

- `core/model/.../LandMeasurement.kt`
- `core/common/.../repository/LandMeasurementRepository.kt`
- `core/database/.../entity/LandMeasurementEntities.kt`
- `core/database/.../dao/LandMeasurementDao.kt`
- `core/database/.../repository/RoomLandMeasurementRepository.kt`
- `core/database/FarmSaathiDatabase.kt` (migration 3 → 4)
- `core/database/di/DatabaseModule.kt`
- `core/common/.../navigation/FarmRoute.kt`
- `feature/farm/.../screens/FarmsAndPlotsScreens.kt`
- `app/.../navigation/FarmAndCropsNavGraph.kt`
- `app/src/main/AndroidManifest.xml`
- `gradle/libs.versions.toml`

Read `docs/land-measurement/README.md` before editing.

## Mandatory review before building

1. Compare the supplied implementation with the active branch and preserve any newer project changes.
2. Confirm dependency alias `libs.geographiclib.java` resolves to GeographicLib-Java 2.1.
3. Confirm the `PolygonArea` Java API call compiles against the actual dependency: `PolygonArea(Geodesic.WGS84, false)`, `AddPoint(lat, lon)`, `Compute(false, true)`, and public `area` / `perimeter` result fields.
4. Confirm `LocationManager.GPS_PROVIDER`, `allProviders`, the selected `requestLocationUpdates` overload and the `LocationListener` implementation are available at minSdk 29.
5. Confirm location permissions are requested at runtime and the feature does not require `ACCESS_BACKGROUND_LOCATION`, internet or a map API.
6. Confirm the current plot area is stored in `areaMicroAcres` and the feature uses the existing `AreaConverter` rather than creating another plot-area field.
7. Confirm migration SQL matches the Room entity schema exactly, including foreign keys and index names.
8. Confirm no current source files were overwritten with older versions from the ZIP.

## Build and testing tasks

Run:

```bash
./gradlew :core:model:test \
  :feature:farm:testDebugUnitTest \
  :core:database:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug
```

If a command fails:
- classify it as code, environment, dependency-resolution or device issue;
- fix code errors, do not hide or skip failing tests;
- rerun the exact failed task;
- retain Gradle XML/HTML test reports;
- record the final commit SHA and result.

Run Room/KSP generation. Commit the generated `core/database/schemas/com.farmsaathi.core.database.FarmSaathiDatabase/4.json`. Do not manually fabricate the schema JSON.

Add/execute any migration test needed to demonstrate a version-3 database upgrades to version 4 without losing existing farms, plots, crops, expenses, labour or diary data. Test on a disposable copy, never on the only copy of actual user data.

## Functional review

Verify the following journeys:

1. Plot detail → Measure plot area.
2. Method choice → GPS permission explanation → runtime permission.
3. GPS provider disabled → explanatory message → open location settings → retry.
4. GPS capture → points appear only when accepted by the quality filter.
5. Pause/resume, undo last accepted point, finish walk.
6. Invalid, self-intersecting, too-small, too-large and unclosed boundaries are rejected with understandable errors.
7. Review shows acres/guntas, square metres, square feet, point count and actual recorded GPS accuracy summary.
8. Save-only records measurement history but does not update the plot's saved area.
9. Save-and-update changes plot area atomically while retaining the measurement history.
10. Manual rectangle calculation accepts positive dimensions and rejects blank/zero/negative/invalid values.
11. Measurement history survives app restart after a completed measurement.
12. Core operation works offline.

## Geospatial verification

Use known reference coordinates and independently verified WGS84 reference results. Verify both clockwise and counter-clockwise boundaries. Do not use a raw latitude/longitude shoelace formula for area. The local planar projection in the implementation is only a self-intersection check; area must remain GeographicLib WGS84 geodesic area.

Check that a repeated first point is normalized correctly and that a boundary that ends far from its start is rejected. The closure tolerance is 5–10 m depending on observed mean point accuracy, bounded at 10 m. A bad final gap must not be silently closed to create a large false polygon.

## Target-device test

The target device is Realme 5s, Android 10. Use a real outdoor test, not only emulator mock locations, to evaluate actual GPS reception. Keep the phone unlocked and the measurement screen visible during this initial foreground-only implementation. Verify actual GPS update rates, accuracy, repeatability, heat/battery impact and usability in sunlight.

Do not claim physical device verification unless the device was actually used. Do not claim the feature supports process-death recovery for an unfinished GPS walk; only completed measurements are persisted in this version.

## Engineering constraints

- Keep existing Clean Architecture / MVVM / Hilt boundaries.
- Keep area calculation outside UI composables and run it off the main thread.
- Do not put direct Room access in Compose.
- Keep location capture user-initiated and stop updates when paused/finished/stopped.
- Do not silently overwrite the existing plot area.
- Preserve raw accepted GPS points and timestamps in measurement history.
- Keep all core behaviour offline.
- Do not add cloud, map tile, analytics or backend requirements.
- Do not mark Done without build/test evidence.

## Required final report

Return:
1. Files integrated or modified.
2. Build result per Gradle task.
3. Unit/database/UI test counts (from actual reports).
4. Room migration and generated schema result.
5. Emulator/device test evidence, separated clearly.
6. Known limitations and unverified items.
7. APK path and installation steps if the build succeeds.
