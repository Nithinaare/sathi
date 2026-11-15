# Farm Saathi — GPS Land Area Measurement

## Purpose

This feature lets a farmer estimate the area of an existing plot in two ways:

1. **GPS boundary walk:** capture a series of precise GPS fixes while walking around the field.
2. **Manual rectangle:** enter length and width in feet for an approximately rectangular field.

The farmer reviews the result before saving. Saving a measurement does **not** change the plot's stored area unless the farmer explicitly chooses **Save and update plot area**.

This is an estimate from a consumer Android device, not a legal land survey or cadastral boundary record.

## Integrated location

The feature is integrated into the existing modular application. It does not create another application or a new backend.

- Plot detail entry point: `feature/farm/.../screens/FarmsAndPlotsScreens.kt`
- Navigation route: `core/common/.../navigation/FarmRoute.kt`
- Navigation wiring: `app/.../navigation/FarmAndCropsNavGraph.kt`
- Compose screens: `feature/farm/.../measurement/ui/LandMeasurementScreen.kt`
- ViewModel/state machine: `feature/farm/.../measurement/viewmodel/LandMeasurementViewModel.kt`
- GPS provider: `feature/farm/.../measurement/location/GpsLocationDataSource.kt` (`AndroidGpsLocationDataSource` implementation)
- GPS filter: `feature/farm/.../measurement/location/GpsPointAcceptancePolicy.kt`
- Geodesic calculator: `feature/farm/.../measurement/calculation/GeodesicAreaCalculator.kt`
- Domain model: `core/model/.../LandMeasurement.kt`
- Repository contract: `core/common/.../repository/LandMeasurementRepository.kt`
- Room persistence: `core/database/.../entity/LandMeasurementEntities.kt`, `dao/LandMeasurementDao.kt`, `repository/RoomLandMeasurementRepository.kt`
- Room migration: `FarmSaathiDatabase.MIGRATION_3_4`

## Farmer workflow

1. Open a plot's detail screen.
2. Tap **మడి విస్తీర్ణం కొలవండి / Measure plot area**.
3. Choose **Walk around the field** or **Enter length and width**.
4. For GPS, read the location explanation and grant precise location when asked.
5. Start GPS, walk the field boundary, pause/resume as needed, and undo the last accepted point if needed.
6. Finish the walk. The app checks the boundary and calculates area.
7. Review acres/guntas, square metres, square feet, point count and GPS accuracy details.
8. Choose either **Save measurement only** or **Save and update plot area**.
9. Previous measurements remain in the plot's measurement history.

## Calculation method

GPS boundaries are calculated on the WGS84 ellipsoid using GeographicLib-Java 2.1:

- `PolygonArea(Geodesic.WGS84, false)`
- `AddPoint(latitude, longitude)` for each accepted boundary point
- `Compute(false, true)` for the signed smaller polygon area
- absolute area is calculated in square metres, converted through the existing `AreaConverter` into the canonical `areaMicroAcres` value, and square metres are derived from that persisted value for later display

The local equirectangular projection in the calculator is used only for a planar self-intersection check over a small agricultural parcel. It is not used to calculate the area.

Manual dimensions are converted from square feet to square metres, then into the existing `areaMicroAcres` unit.

## GPS policy

Current configurable defaults in `GpsFilterConfig`:

| Rule | Default |
|---|---:|
| Maximum horizontal accuracy accepted | 20 metres |
| Minimum distance between accepted points | 2 metres |
| Maximum plausible walking speed | 8 metres/second |
| Maximum fix age | 20 seconds |
| Maximum future timestamp skew | 5 seconds |
| Maximum recorded boundary points | 5,000 |
| Minimum closure distance | 5 metres |
| Maximum closure distance when GPS accuracy is poor | 10 metres |

These are engineering guardrails, not a guarantee of area accuracy. Tune them after recording repeatable outdoor field traces on the Realme 5s. Do not weaken the warnings just to make a measurement appear successful.

The polygon must have at least three distinct points, produce a non-zero area, and not self-intersect. A boundary ending too far from its start is rejected instead of being silently closed across a large gap. The closing tolerance is at least 5 m, scales up to 1.5 times the mean observed point accuracy, and is capped at 10 m. These are product guardrails, not a statement that a 5–10 m closure is survey-grade.

## Map, imagery and offline map support

The GPS feature uses MapLibre Native Android 13.5.4 OpenGL renderer for a real geographical map. The map includes an OpenFreeMap/OpenStreetMap street layer and an Esri World Imagery satellite layer. The farmer can switch between them; the boundary overlay remains in the same geographic coordinates on both.

The review screen supports moving, inserting and removing boundary vertices on the actual map. User-edited points are marked as manually adjusted and have no fabricated GPS accuracy value. The geodesic area calculation runs again after each edit.

The farmer may download a small street-map region around the plot for offline use. It uses the same OpenFreeMap style URL, a limited zoom range, at most four concurrent requests and conservative app guardrails (2,700 tiles / 40 MiB). Satellite imagery is online-only in this version. See `MAP_AND_IMAGERY.md` for providers, attribution, limits and caveats.

## GPS offline and permission behaviour

- GPS coordinates are obtained from Android `LocationManager.GPS_PROVIDER` only.
- GPS capture, area calculation, manual measurement, boundary editing and persistence do not require internet. The geographical basemap requires internet unless the relevant street-map pack has already been downloaded.
- The app declares `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` and requests them only after the farmer starts a GPS measurement.
- The feature requires precise location; approximate-only access is not enough to measure a field boundary.
- It does not request `ACCESS_BACKGROUND_LOCATION` and does not run an always-on location service.
- The app keeps the screen on while recording and pauses GPS when the activity stops. The farmer must keep the measurement screen open during the walk.
- **An in-progress boundary is currently held in ViewModel memory.** Completed measurements are persisted to Room, but an app process kill or device restart during an unfinished walk can lose the unfinished boundary. Do not describe unfinished-session crash recovery as implemented.

This foreground-only choice keeps the permission footprint smaller. If background/screen-off capture is added later, it must use an explicitly user-started foreground location service and be designed/tested for Android 10+ permission and notification requirements.

## Persistence and data safety

The feature adds:

- `land_measurements`: completed measurement metadata and canonical area in `areaMicroAcres`. Square metres are derived for display to avoid storing two competing area values.
- `land_boundary_points`: ordered accepted GPS points with horizontal accuracy and timestamps. Average and maximum accuracy are computed from these original point records, not stored as duplicate aggregate columns.

Room database version is advanced from 3 to 4 through `MIGRATION_3_4`. The existing plot area remains `Plot.areaMicroAcres`; no competing plot-area field is introduced.

The measurement row and boundary points are inserted in one Room transaction. Both measurement and boundary-point foreign keys use `RESTRICT` in keeping with the project's history-preservation rule. When the farmer explicitly asks to apply the result, the plot's `areaMicroAcres` and `updatedAtEpochMillis` are updated in that same transaction. Previous measurement rows are retained.

After the first real Gradle/KSP build, verify that Room generates and commits:

`core/database/schemas/com.farmsaathi.core.database.FarmSaathiDatabase/4.json`

Do not hand-create that schema file.

## Screens

`LandMeasurementScreen` presents these states:

- Choose measurement method
- GPS capture
- Manual dimensions
- Review result
- Saved confirmation

The GPS capture and review screens use an actual MapLibre geographical map. If the map service is unavailable, an explicitly labelled local sketch fallback is shown.

The interface uses existing Farm Saathi design-system components and the currently selected app language (Telugu or English). It adds a pre-permission explanation in Telugu/English and a settings route when permission was denied.

## Tests added

### `feature:farm`

- WGS84 reference square area and perimeter
- Orientation-independent area
- Explicit repeated closing point
- Too few points
- Too many points
- Self-intersection
- Invalid latitude
- Unclosed boundary
- Manual 100 ft × 100 ft conversion
- Invalid manual dimensions
- GPS fix accuracy, age, future skew, ordering, spacing, speed and coordinate checks

### `core:database`

- Measurement and boundary-point persistence
- Plot area unchanged on save-only
- Plot area changed only on explicit apply
- History retained across saves
- Migration SQL creates both new tables, indices and foreign keys

## Build and verify in Android Studio / Antigravity

Run from the repository root:

```bash
./gradlew :core:model:test \
  :feature:farm:testDebugUnitTest \
  :core:database:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug
```

Then inspect the generated Room schema file and run the app on an emulator or physical phone.

For migration verification, install a build over a copy of an existing version-3 data store. Do not test migrations on the only copy of important farm data.

## Required physical-device acceptance on Realme 5s / Android 10

Automated and mocked tests cannot validate the handset's actual GPS receiver. On the target phone, test outdoors with a clear sky view:

1. Confirm location permission is requested only after the farmer chooses GPS measurement.
2. Confirm a first fix is received and the displayed accuracy changes with actual device readings.
3. Walk a simple rectangular field slowly and capture the four corners plus a return point near the start.
4. Repeat the same field at least three times; compare results and record observed variance.
5. Test trees/buildings, weak GPS, disabled GPS, denied permission, pause/resume and undo.
6. Confirm the app pauses when backgrounded and no location updates continue after the measurement screen is stopped.
7. Save without applying and verify the plot's existing area is unchanged.
8. Save and apply on a test plot; verify its area changes and the previous measurement remains in history.
9. Test with the farmer in direct sunlight and without verbal coaching.

Record device, Android version, location accuracy observations, measured area, reference method and results. Do not call the GPS result survey-grade.


## Current delivery verification status

The following checks were completed while preparing this integration package:

- The pure Kotlin geodesic-calculator and GPS-point-filter test methods were executed through a local Kotlin harness: 23 scenarios passed. **The harness used a test-only GeographicLib API stub**, so this is not a substitute for running the actual Gradle/JUnit test task against the real `GeographicLib-Java` artifact.
- An independent Python GeographicLib WGS84 calculation verified the square test fixture's expected area (`12,309.072079001264 m²`) and perimeter (`443.7875332131202 m`). This independently verifies the fixture values, not the Java dependency linkage.
- The exact `MIGRATION_3_4` SQL was extracted and exercised using SQLite: table/index creation, point insert, foreign-key check and `RESTRICT` behaviour passed.
- Android manifest XML, Gradle version-catalog parsing, and the satellite style JSON parsing passed. The manifest includes internet access for optional map tiles plus coarse/fine location permissions; no background-location permission was added.
- A local Kotlin smoke harness compiled and ran the actual `BoundaryPointEditor`, `GpsPointAcceptancePolicy`, and `OfflineMapDownloadBudget` source files against a minimal test `GpsLocationSample` model: 18 assertions passed. This does not exercise Android, MapLibre or Compose.
- The MapLibre API integration was checked against MapLibre's Android API documentation for `MapView`, custom OkHttp `Call.Factory`, offline regions and download status. This is source/API review, not compilation.
- The application Gradle tasks could **not** run in the preparation environment: the wrapper needed to download Gradle 8.9, but DNS resolution for `services.gradle.org` failed. Therefore no Android APK, Room/KSP-generated version-4 schema, Android lint result or real Gradle test report for the map additions is included.
- No physical Realme 5s GPS test has been performed. Outdoor accuracy and repeatability remain to be verified on the actual handset.

Run the Gradle commands above in Antigravity/Android Studio with dependency-network access before treating the feature as build-verified.
