# Map-enabled Land Measurement — Integration overlay

This overlay adds the real MapLibre street/satellite map, editable boundary vertices, and bounded offline street-map packs to the earlier GPS Land Measurement integration.

## Apply safely

1. Back up your current Farm Saathi workspace and commit/stash any local changes.
2. Extract this ZIP.
3. Copy the contents of the `overlay/` directory into the root of the existing GPS-enabled Farm Saathi project, preserving relative paths and allowing the newer overlay files to replace the older GPS-only versions.
4. Open the project in Antigravity/Android Studio and inspect the diff before accepting it. Do not overwrite unrelated work.
5. Follow `overlay/ANTIGRAVITY_LAND_MEASUREMENT_INTEGRATION_PROMPT.md` and `overlay/docs/land-measurement/MAP_AND_IMAGERY.md`.
6. Run the required Gradle build and tests with dependency-network access.

## What the overlay adds

- MapLibre Native Android OpenGL map renderer.
- OpenFreeMap Liberty street/place basemap.
- Esri World Imagery satellite layer with provider attribution.
- Local GeoJSON polygon fill, boundary line and editable numbered vertices.
- Move, insert and remove boundary point operations followed by WGS84 area recalculation.
- Downloadable OpenFreeMap street-map region with conservative application limits (2,700 tiles / 40 MiB, max four parallel requests).
- Explicit online-only label for satellite imagery.
- A non-geographic fallback sketch when the basemap cannot load.

## Important limitations

- The map is an optional visual layer. GPS collection, area calculation, edits and Room persistence must continue without map tiles or internet.
- Satellite imagery is online-only. The Esri World Imagery service metadata says `exportTilesAllowed` is false, so this integration does not offer offline satellite downloads.
- OpenFreeMap allows user-triggered offline areas up to 3,000 tiles or 50 MB, with at most four parallel requests and a unique application User-Agent. Farm Saathi uses stricter limits.
- Map tile providers receive requested tile coordinates. The app does not send a farm/plot record or the polygon as an application API payload, but map tile requests reveal the viewed geographic area.
- This does not establish legal land ownership or cadastral boundaries.

## Verification status

A local Kotlin smoke harness ran 19 assertions against the actual pure Kotlin boundary editor, GPS point acceptance policy and offline-download budget sources. Static checks for the Gradle version catalogue, manifest XML and satellite-style JSON also passed.

The full Android Gradle build, MapLibre/Compose compilation, Room/KSP schema generation, Android lint, and physical-device GPS/map tests have not been completed in this environment. Antigravity must run the Gradle commands in the prompt and fix any compile/API issues before this is treated as build-verified.
