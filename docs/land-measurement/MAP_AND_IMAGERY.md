# Farm Saathi — Map, Satellite Imagery and Offline Map Packs

## Map implementation

The map is rendered with MapLibre Native Android using the OpenGL artifact (`org.maplibre.gl:android-sdk-opengl:13.5.4`). The OpenGL artifact is selected for broad Android device compatibility, including the project's Android 10 target. The Compose screen hosts the native `MapView` through `AndroidView` and forwards Android lifecycle events.

The boundary geometry is created locally as GeoJSON and overlaid as a fill, outline and numbered vertex points. The field boundary coordinates are not submitted to an application backend. The map tile requests themselves necessarily disclose requested tile coordinates to the selected tile provider.

## Basemaps

### Street / village map

- Style: `https://tiles.openfreemap.org/styles/liberty`
- Data: OpenStreetMap-derived street/place data rendered by OpenFreeMap/OpenMapTiles.
- Attribution shown in-app: `© OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors`.
- No application API key is currently configured.
- Internet access is required to retrieve tiles that are not already held in the MapLibre offline database/cache.
- OpenFreeMap provides the service as-is and does not promise an SLA. Do not make map availability a requirement for GPS capture or area calculation.

### Satellite imagery

- Raster service: `https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}`
- Attribution shown in-app: `Source: Esri, Vantor, Earthstar Geographics, and the GIS User Community`.
- Satellite imagery is an online basemap in this implementation. It is not included in downloadable offline packs. Esri World Imagery service metadata currently reports `exportTilesAllowed: false`, so this integration deliberately does not export or pre-download satellite tiles.
- Imagery availability, date, resolution and accuracy vary by location. It is a visual reference, not a legal parcel or land-ownership boundary.
- The current project is personal/non-commercial. Before public distribution, monetization or use outside the applicable Esri terms, re-check the current imagery terms and any third-party data restrictions.

## Offline map support

The feature includes an explicit **Download street map** action on the GPS boundary review screen. The download covers a padded bounding box around the measured field, uses the same OpenFreeMap style URL as the online street map, and is stored by MapLibre's offline-region database.

The offline pack is deliberately conservative:

- Zoom range: 12–16.
- App guardrail: stop before 2,700 completed map tiles or 40 MiB of completed resources.
- HTTP concurrency: maximum four requests.
- Requests identify the application with `User-Agent: FarmSaathi/1.0 (Android; offline field maps)`.
- A pack that reaches the app guardrail is stopped and removed; the user is asked to select a smaller map area.
- Completed packs are rediscovered from MapLibre's offline-region metadata when the plot is opened again.

OpenFreeMap's current published policy permits an app offline download up to 3,000 tiles or 50 MB, with at most four parallel requests and a unique User-Agent identifying the app. Farm Saathi uses lower guardrails to leave headroom for resource-counting differences. Re-check the provider's terms before increasing limits or changing the tile source.

**Offline guarantees:**

- GPS capture, local polygon calculation, manual measurement, boundary editing and Room persistence work without internet.
- A previously downloaded street-map region can be used offline by MapLibre when the same style URL is loaded.
- Ambient caching may help, but it is not a substitute for a completed offline region.
- Satellite imagery requires internet in this version.
- If map tiles cannot load, the app falls back to a schematic local boundary sketch. The sketch can accept approximate vertex edits, but it is not a geographic basemap and should not be used for location-specific precision corrections. The measurement data remains available.

## Boundary editing

On the review screen, the farmer can:

1. Select a numbered boundary vertex.
2. Choose **Move point**, then tap its new position on the map.
3. Choose **Add point**, then tap where the new vertex belongs after the selected vertex.
4. Remove a selected point, while keeping at least three points.

Any moved or manually inserted point has a null GPS-accuracy value. The app labels it as manually adjusted and does not present it as a measured GPS fix. The geodesic calculation and polygon validation are rerun after each edit.

## Privacy and reliability

- Tile providers receive the map tile coordinates requested by the phone; map tile viewing is not fully private from the tile provider.
- The app sends no field polygon or farm/plot identifier to OpenFreeMap or Esri as an application-level feature query. The map tile requests do disclose requested tile coordinates to the tile provider.
- Never use a map image to establish ownership or legal cadastral boundaries.
- Do not use the standard OSM Foundation community tile endpoint for bulk/offline downloads. The configured source is OpenFreeMap, whose own published download policy applies.
- Keep map loading optional. If the service is unavailable, farmers can still complete GPS measurement and area calculation.
