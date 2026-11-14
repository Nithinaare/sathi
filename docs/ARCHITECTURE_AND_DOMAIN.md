# Farm Saathi — System Architecture & Domain Rules

> **Target Platform:** Android 10+ (`minSdk = 29`, `compileSdk = 35`, `targetSdk = 35`) — tuned for Realme 5s (Snapdragon 665, 4 GB RAM, offline-first).  
> **Root Package:** `com.farmsaathi.app`  
> **Stack:** Kotlin, Jetpack Compose (Material 3), Room (SQLite WAL), Hilt, WorkManager, Preferences DataStore, CameraX, Coil.

---

## 1. Multi-Module Gradle Architecture

### 1.1 Module Catalog

| Module | Plugin Type | Namespace | Responsibility |
| :--- | :--- | :--- | :--- |
| `:core:model` | `kotlin("jvm")` | `com.farmsaathi.core.model` | Pure Kotlin domain value types (`MoneyPaise`, `AreaMeasurement`, `QuantityMeasurement`), enums, domain calculators (INR grouping, rounding, wage, machinery billing), and starter crop template definitions. Zero Android/Compose/Room dependencies. |
| `:core:common` | `com.android.library` | `com.farmsaathi.core.common` | `AppDispatchers`, `TimeProvider` (epoch millis, local epoch day, `elapsedRealtime`, `bootCount`), `DomainException`, `StoragePaths`, `escapeSqlLike`, repository interfaces, `AudioCuePlayer` contract, and `BackupProvider` contract. |
| `:core:designsystem` | `com.android.library` | `com.farmsaathi.core.designsystem` | Material 3 Farm Saathi theme (sunlight-readable high-contrast palette), typography, reusable Compose inputs (`MoneyInputField`, `AreaInputField`, `StatusChip`, `ConfirmSheet`, `EmptyStateCard`). |
| `:core:datastore` | `com.android.library` | `com.farmsaathi.core.datastore` | Preferences DataStore (`UserPreferencesRepository`): `activeFarmId`, `preferredAreaUnit`, `languageCode` (`"te"` default / `"en"`), `audioGuidanceEnabled`, `dailyReminderEnabled`, `reminderHour`, `reminderMinute`, `lastBackupEpochMillis`. |
| `:core:audio` | `com.android.library` | `com.farmsaathi.core.audio` | `MediaRecorder` (AAC/M4A 16 kHz / 32 kbps mono) & `MediaPlayer` for diary voice notes, plus `HybridAudioCuePlayer` (bundled offline Telugu audio cues + custom family prompt `.m4a` overrides + `TextToSpeech(te-IN)` fallback). |
| `:core:media` | `com.android.library` | `com.farmsaathi.core.media` | CameraX capture + gallery import, EXIF-aware JPEG downsampling (max 1600 px edge, 80% quality), app-private `filesDir/media/` writer, Coil `ImageLoader` bounded for 4 GB RAM (15% memory cache). |
| `:core:database` | `com.android.library` | `com.farmsaathi.core.database` | Room (`FarmSaathiDatabase`, `version = 1`, WAL), 21 `@Entity` classes, DAOs, `TypeConverter`s, `@Transaction` repository implementations, starter template seeder, `DatabaseMaintenanceCoordinator`, and `LocalZipBackupProvider`. |
| `:feature:home` | `com.android.library` | `com.farmsaathi.feature.home` | Dashboard: today's due/overdue tasks, active machinery timer banner, net cashflow snapshot, pending dues alert, quick-add shortcuts. |
| `:feature:farm` | `com.android.library` | `com.farmsaathi.feature.farm` | Farms, plots, crop cycle creation/management, editable crop templates & tasks, soil tests, contacts directory. |
| `:feature:diary` | `com.android.library` | `com.farmsaathi.feature.diary` | Field diary timeline, photo + voice-note recording, plot/crop/category filtering, offline search. |
| `:feature:finance` | `com.android.library` | `com.farmsaathi.feature.finance` | Unified cashbook/ledger, expense logging with receipts, crop sales with partial/credit settlement, receivables & payables tracker. |
| `:feature:operations` | `com.android.library` | `com.farmsaathi.feature.operations` | Labour attendance & wage/advance/settlement manager, machinery live timer & manual/per-acre billing, input inventory & purchases, irrigation, harvest & storage. |
| `:feature:reports` | `com.android.library` | `com.farmsaathi.feature.reports` | Season & crop P&L, category breakdown, worker payout summary, inventory valuation, yield per acre/gunta, Backup & Restore UI + Share intent launcher. |
| `:app` | `com.android.application` | `com.farmsaathi.app` | `@HiltAndroidApp`, `MainActivity`, Compose `NavHost` + bottom navigation, `FileProvider`, `DailyTaskReminderWorker`, `BootRescheduleReceiver`, `HiltWorkerFactory`. |

### 1.2 Strict Allowed Dependency Graph

```
:core:model  <── (pure Kotlin JVM — depends on NOTHING)
     ▲
     ├── :core:common
     │        ▲
     │        ├── :core:datastore ──► :core:model
     │        ├── :core:audio
     │        ├── :core:media
     │        └── :core:database ──► :core:model, :core:common
     │
     └── :core:designsystem ──► :core:model

:feature:home       ──► :core:model, :core:common, :core:designsystem, :core:datastore
:feature:farm       ──► :core:model, :core:common, :core:designsystem, :core:datastore, :core:media
:feature:diary      ──► :core:model, :core:common, :core:designsystem, :core:audio, :core:media
:feature:finance    ──► :core:model, :core:common, :core:designsystem, :core:datastore, :core:media
:feature:operations ──► :core:model, :core:common, :core:designsystem, :core:datastore, :core:media
:feature:reports    ──► :core:model, :core:common, :core:designsystem, :core:datastore

:app ──► ALL :core:* and ALL :feature:*
```

**Hard Architecture Rules (enforced in Gradle `build.gradle.kts`):**
1. **No circular dependencies.**
2. **`:feature:*` NEVER depends on `:core:database`.** Feature ViewModels depend exclusively on repository interfaces in `:core:common`. Hilt binds `:core:database` implementations in `:app`'s component graph. Feature modules cannot import Room `@Entity` or `@Dao` types.
3. **`:feature:*` NEVER depends on another `:feature:*`.** Cross-feature navigation uses route contracts in `:core:common` wired inside `:app`'s `NavHost`.
4. **`:core:model` has zero Android, Room, or Compose dependencies.**

---

## 2. Domain Models & Financial / Measurement Rules

### 2.1 `MoneyPaise` (INR Integer Minor Units)

All monetary amounts across domain, database, and UI calculations use `Long` paise (`1 INR = 100 paise`). **`Double` and `Float` are strictly forbidden for money.**

```kotlin
@JvmInline
value class MoneyPaise(val paise: Long) : Comparable<MoneyPaise> {
    operator fun plus(other: MoneyPaise) = MoneyPaise(Math.addExact(paise, other.paise))
    operator fun minus(other: MoneyPaise) = MoneyPaise(Math.subtractExact(paise, other.paise))
    operator fun unaryMinus() = MoneyPaise(Math.negateExact(paise))
    override fun compareTo(other: MoneyPaise): Int = paise.compareTo(other.paise)

    fun roundToNearestRupee(): MoneyPaise =
        MoneyPaise(MathRounding.divideAndRoundHalfUp(paise, PAISE_PER_RUPEE) * PAISE_PER_RUPEE)

    fun percentage(percent: Int): MoneyPaise {
        require(percent in 0..10_000)
        return MoneyPaise(MathRounding.divideAndRoundHalfUp(paise * percent, 100L))
    }

    companion object {
        const val PAISE_PER_RUPEE = 100L
        val ZERO = MoneyPaise(0L)
        fun fromRupees(rupees: Long) = MoneyPaise(Math.multiplyExact(rupees, PAISE_PER_RUPEE))
    }
}
```

**Safe Integer Half-Up Rounding (No `Double`):**
```kotlin
object MathRounding {
    fun divideAndRoundHalfUp(numerator: Long, denominator: Long): Long {
        require(denominator > 0L) { "Denominator must be positive" }
        val quotient = numerator / denominator
        val remainder = kotlin.math.abs(numerator % denominator)
        return if (remainder * 2L >= denominator) {
            if (numerator >= 0L) quotient + 1L else quotient - 1L
        } else {
            quotient
        }
    }
}
```

**Input Parsing & Indian Grouping Formatting:**
- **Parsing:** Accepts optional commas and up to 2 decimal digits (`"1,250.5"` -> `125050L` paise, `"1250"` -> `125000L` paise). Rejects `> 2` decimal places; parses via integer substring arithmetic, never `String.toDouble()`.
- **Indian Grouping Format (`formatInr(showZeroPaise: Boolean = false)`):**
  - Groups the rightmost 3 rupee digits, then every 2 digits to the left separated by `,`.
  - Appends `.pp` only if `paise % 100 != 0` or `showZeroPaise == true`.
  - Examples: `0L -> "₹0"`, `12500000L -> "₹1,25,000"`, `12500050L -> "₹1,25,000.50"`, `-45000L -> "-₹450"`.
  - Implemented deterministically in `:core:model` without `java.text.NumberFormat` locale variance across OEM ROMs.

---

### 2.2 Area & Quantity Units

#### Area Canonical Representation (`areaMicroAcres: Long`)
`1 Acre = 1,000,000 microAcres`.

Why micro-acres? Because `LCM(40, 100)` divides `1,000,000` cleanly with zero rounding error:
- `ACRE_GUNTA` (**default**): `1 Acre = 40 Guntas` -> `1 Gunta = 25,000 microAcres` (exact integer, supports `0.5 gunta = 12,500`).
- `ACRE_CENT`: `1 Acre = 100 Cents` -> `1 Cent = 10,000 microAcres` (exact integer).
- `ACRE_DECIMAL`: `0.01 Acre = 10,000 microAcres`, `0.0001 Acre = 100 microAcres` (exact integer).

Each `FarmEntity` and `PlotEntity` persists `areaMicroAcres: Long` plus `preferredAreaUnit: AreaUnit` so the UI displays and edits in the farmer's chosen unit while farm totals, yield/acre, and machinery per-acre billing aggregate in exact integer `microAcres`.

#### Quantity Canonical Representation (`quantityMilliUnits: Long`)
`1 unit = 1,000 milliUnits` (3 decimal places exact; e.g., `2.500 kg -> 2500L`, `12.75 quintal -> 12750L`).

```kotlin
enum class QuantityUnit {
    KG,       // Mass base (1,000 milli-kg = 1 kg)
    QUINTAL,  // 100 KG
    BAG,      // Count of bags (optional bagWeightMilliKg metadata per product; never blindly assumed)
    TON,      // 1,000 KG
    LITRE,    // Volume base (1,000 ml)
    ML,       // 0.001 LITRE
    PIECE,    // Discrete count
    HOUR,     // Service hours
    ACRE      // Service area
}
```

- **Intra-family conversion only:** `KG <-> QUINTAL <-> TON` and `LITRE <-> ML` convert via exact integer scaling (`milliUnits`). Cross-family conversion (e.g., `BAG -> KG`) requires an explicit product `bagWeightMilliKg` or is blocked.
- **Line total formula:** `totalPaise = MathRounding.divideAndRoundHalfUp(unitPricePaise * quantityMilliUnits, 1000L)`.

---

## 3. Unified Financial Ledger Engine

### 3.1 Single Source of Truth & Idempotency

All cash/credit financial records flow into one **`LedgerEntryEntity`** table (`ledger_entries`).

- **Idempotency Constraint:** `UNIQUE(sourceType, sourceId)` index (`index_ledger_source_unique`).
  - `LedgerSourceType`:
    1. `MANUAL_EXPENSE` -> `sourceId = ExpenseDetailEntity.id` (non-null)
    2. `INPUT_PURCHASE` -> `sourceId = InputPurchaseEntity.id` (non-null)
    3. `CROP_SALE` -> `sourceId = CropSaleEntity.id` (non-null)
    4. `WORKER_PAYMENT` -> `sourceId = WorkerPaymentEntity.id` (non-null)
    5. `MACHINERY_SESSION` -> `sourceId = MachinerySessionEntity.id` (non-null, when `usageType != OWN_FARM_USE`)
    6. `MANUAL_INCOME` -> `sourceId = NULL` (no child table; SQLite treats multiple `NULL`s in a unique index as distinct, while UI uses a submit guard).
- **Atomicity:** Every source write and its corresponding `LedgerEntryEntity` insert/update execute inside a **single Room `@Transaction`**.
- **Anti-Double-Counting Rules:**
  1. **Labour:** `WorkerAttendanceEntity` creates a **wage accrual** only (no ledger entry). `LedgerEntryEntity(WORKER_PAYMENT)` is written ONLY when `WorkerPaymentEntity` (`ADVANCE` or `SETTLEMENT`) is paid.
  2. **Inventory:** `InputPurchaseEntity` writes the `EXPENSE` ledger entry + `StockMovementEntity(IN)`. Applying stock to a field (`StockMovementEntity(OUT)`) updates crop analytics cost snapshots only and **never** writes a cash ledger entry.
  3. **Machinery:** `OWN_FARM_USE` sessions record machine hours without a ledger entry (diesel/repair expenses are entered separately via `MANUAL_EXPENSE`). Only `HIRED_IN` (`EXPENSE`) and `HIRED_OUT` (`INCOME`) post to the ledger upon session completion.

### 3.2 Payment Status & Partial Settlement Invariants

On `LedgerEntryEntity`:
- `entryType`: `INCOME` | `EXPENSE`
- `totalAmountPaise`: `>= 0`
- `settledAmountPaise`: represents **`amountReceived`** for `INCOME` and **`amountPaid`** for `EXPENSE`
- **Invariant:** `0L <= settledAmountPaise <= totalAmountPaise`
- **Derived `PaymentStatus` (never manually overridden):**
  - `settledAmountPaise == totalAmountPaise` -> `PAID` (also `PAID` when `totalAmountPaise == 0L`)
  - `settledAmountPaise == 0L` -> `PENDING`
  - `0L < settledAmountPaise < totalAmountPaise` -> `PARTIAL`
- `balanceDuePaise = totalAmountPaise - settledAmountPaise`
- **Settlement Sync:** Calling `FinanceRepository.recordLedgerSettlement(ledgerId, additionalPaise, ...)` atomically updates `LedgerEntryEntity` AND syncs `CropSaleEntity.amountReceivedPaise` / `InputPurchaseEntity.paidAmountPaise` / `MachinerySessionEntity.paidAmountPaise` + `paymentStatus` in the same `@Transaction`.
- **No Hard Delete:** Financial rows are never deleted. Corrections or cancellations set `isVoided = true` + `voidReason` atomically on both the source entity and its `LedgerEntryEntity`. Voided rows are excluded from all cashbook, P&L, and balance totals.

---

## 4. Operational Domain Rules

### 4.1 Labour Rules

- **Attendance (`AttendanceStatus`):**
  - `FULL_DAY`: `wageAccruedPaise = baseDailyWagePaise`
  - `HALF_DAY`: `wageAccruedPaise = MathRounding.divideAndRoundHalfUp(baseDailyWagePaise * halfDayPercent, 100L)` (`halfDayPercent` defaults to `50`, editable `1..99` per worker and per day)
  - `ABSENT`: `wageAccruedPaise = 0L`
  - `PIECE_RATE`: `wageAccruedPaise = MathRounding.divideAndRoundHalfUp(pieceUnitRatePaise * pieceQuantityMilliUnits, 1000L)`
- **Unique Day Rule:** `UNIQUE(workerId, workDateEpochDay)` prevents duplicate attendance on the same calendar date; edits update the existing day's record in place.
- **Payments (`WorkerPaymentType`):** `ADVANCE` (pre-payment) and `SETTLEMENT` (wage payout). Both post to `LedgerEntryEntity(WORKER_PAYMENT)` immediately. Optional UI shortcut "Mark & Pay Today" executes `upsertAttendance` + `recordWorkerPayment` in one `@Transaction`.
- **Pending Balance Calculation (per worker, excluding voided rows):**
  ```kotlin
  val totalWageAccrued = sum(attendance.wageAccruedPaise)
  val totalAdvancesPaid = sum(payments.filter { it.paymentType == ADVANCE }.amountPaise)
  val totalSettlementsPaid = sum(payments.filter { it.paymentType == SETTLEMENT }.amountPaise)
  val pendingBalance = totalWageAccrued - (totalAdvancesPaid + totalSettlementsPaid)
  // > 0L => Farmer owes worker (Payable)
  // < 0L => Worker holds unadjusted advance credit of abs(pendingBalance)
  // == 0L => Settled
  ```

### 4.2 Machinery Rules

- **Start & Reboot Recovery:**
  - Starting a `LIVE_TIMER` session persists `startEpochMillis` (`System.currentTimeMillis()`), `startElapsedRealtimeMillis` (`SystemClock.elapsedRealtime()`), and `startBootCount` (`Settings.Global.BOOT_COUNT`).
  - Live elapsed calculation (zero background timer service required — resilient to ColorOS memory killing):
    - **Same boot** (`currentBootCount == startBootCount && startBootCount >= 0`): `elapsedMillis = SystemClock.elapsedRealtime() - startElapsedRealtimeMillis` (immune to manual/network clock drift).
    - **After device reboot** (`currentBootCount != startBootCount`, or `elapsedRealtime < startElapsedRealtimeMillis`): `elapsedMillis = maxOf(0L, System.currentTimeMillis() - startEpochMillis)`.
  - `BootRescheduleReceiver` (`BOOT_COMPLETED`) re-verifies reminder schedules; running timers automatically recover on next UI/repository read with zero drift.
- **Active Session Guard (Anti-Duplicate Start / Double-Stop):**
  - `activeLockKey` column has `UNIQUE` index (`index_machinery_active_lock_unique`).
  - When `status == RUNNING`, `activeLockKey = "${farmId}:${equipmentKey}"` (`equipmentKey = machineName.trim().lowercase()`).
  - On `COMPLETED` or `CANCELLED`, `activeLockKey` is set to `null` (SQLite allows multiple `NULL`s).
  - Starting a duplicate timer for the same machine throws `DomainException.ActiveSessionExists`.
  - Stopping inside `@Transaction` verifies `status == RUNNING`; otherwise returns/throws `DomainException.SessionAlreadyStopped`.
- **Billing Modes & Rounding:**
  - `elapsedMinutes = maxOf(0L, MathRounding.divideAndRoundHalfUp(elapsedMillis, 60_000L))` (user may confirm/adjust minutes on stop sheet).
  - **`HOURLY`:** Prorated by minutes and **rounded half-up to nearest ₹1 (`100L` paise)** in a single division to prevent double-rounding:
    ```kotlin
    val totalPaise = MathRounding.divideAndRoundHalfUp(hourlyRatePaise * elapsedMinutes, 6_000L) * 100L
    ```
  - **`PER_ACRE`:**
    ```kotlin
    val totalPaise = MathRounding.divideAndRoundHalfUp(perAcreRatePaise * areaCoveredMicroAcres, 1_000_000L)
    ```
  - **`FIXED_FLAT`:** Direct integer `MoneyPaise` entered on completion.

### 4.3 Inventory & Stock Rules

- **Stock Movements (`StockMovementEntity`):**
  - `movementType`: `IN`, `OUT`, `ADJUSTMENT`
  - `reason`: `PURCHASE`, `OPENING_STOCK`, `FIELD_APPLICATION`, `RETURN`, `WASTAGE_EXPIRED`, `STOCK_COUNT_CORRECTION`
  - `signedDeltaMilliUnits`: positive for stock additions, negative for deductions; `balanceAfterMilliUnits = currentStockMilliUnits + signedDeltaMilliUnits`.
- **Negative Stock Blocked:**
  - Any `OUT` or negative `ADJUSTMENT` checks inside `@Transaction`:
    `if (currentStockMilliUnits + signedDeltaMilliUnits < 0L) throw DomainException.InsufficientStock(...)`
  - Farmers must record opening stock or a purchase before logging consumption.
- **Atomic Input Purchase (`InventoryRepository.recordInputPurchase`):**
  1. Insert `InputPurchaseEntity` (`purchaseId`).
  2. Insert `StockMovementEntity(IN, PURCHASE, +qtyMilliUnits, purchaseId = purchaseId)` (protected by `UNIQUE(purchaseId)`).
  3. Increment `InputProductEntity.currentStockMilliUnits`.
  4. Insert `LedgerEntryEntity(EXPENSE, INPUT_PURCHASE, sourceId = purchaseId)` (protected by `UNIQUE(sourceType, sourceId)`).

### 4.4 Crop Cycles, Starter Templates & WorkManager Reminders

- **6 Pre-Seeded Starter Templates (+ Custom):**
  1. **Paddy** (`starter_paddy`, Kharif/Rabi, ~125 days): Nursery/Land Prep (D0), Transplanting (D21), Basal/Top-Dress Check (D40), Panicle/Water Check (D65), Grain Filling/Pest Check (D90), Harvest Prep (D120).
  2. **Cotton** (`starter_cotton`, Kharif, ~160 days): Sowing & Basal (D0), Thinning/Gap Fill (D15), Inter-cultivation & Weeding (D35), Square Formation Check (D55), Boll Development Check (D95), First Picking Prep (D135).
  3. **Chilli** (`starter_chilli`, Kharif/Rabi, ~150 days): Nursery/Transplant (D0), Root Establishment & Weeding (D20), Vegetative Nutrition Check (D45), Flowering/Thrips-Mite Check (D65), First Green/Red Pickings (D100).
  4. **Maize** (`starter_maize`, Kharif/Rabi, ~110 days): Sowing (D0), Weed Management & FAW Watch (D20), Knee-High Top Dress Check (D35), Tasseling/Silking Moisture Check (D60), Cob Maturity & Harvest (D105).
  5. **Groundnut** (`starter_groundnut`, Kharif/Rabi, ~115 days): Sowing (D0), Inter-cultivation/Weeding (D22), Pegging & Gypsum Timing Check (D40), Pod Filling Moisture Check (D70), Pod Maturity Test & Harvest (D110).
  6. **Red Gram / Tur** (`starter_red_gram`, Kharif, ~165 days): Sowing (D0), Thinning & Weeding (D25), Branching/Inter-cultivation (D50), Bud/Flower Pod-Borer Watch (D95), Pod Maturity & Harvest (D160).
  - **Honesty & Editability Rule:** Every starter template carries `isStarterTemplate = true`, `isEditable = true`, and explicit UI disclaimer: *"Editable planning template — adjust timings to your variety, soil, and weather. Not a KVK/agronomic prescription."* Farmers can edit templates or create custom crop templates anytime.
- **Sowing Shift Recalculation:** Updating `CropCycleEntity.sowingDateEpochDay` optionally recalculates uncompleted template-derived tasks (`dueDateEpochDay = newSowingEpochDay + offsetFromSowingDays`).
- **WorkManager Daily Reminders (`DailyTaskReminderWorker`):**
  - Enqueued as unique periodic work (`24h` interval aligned to user's preferred DataStore hour/minute, default `07:00`).
  - Rescheduled on DataStore time change and on `BOOT_COMPLETED` / `TIME_CHANGED` / `TIMEZONE_CHANGED`.
  - Queries live Room counts (`dueToday + overdue` where `!isCompleted && !isArchived`); posts one summary notification only when count `> 0`.

---

## 5. Backup & Restore Contract

### 5.1 Interface (`:core:common`)

```kotlin
interface BackupProvider {
    suspend fun exportBackup(): BackupResult
    suspend fun inspectBackup(sourceUri: Uri): BackupInspection
    suspend fun restoreBackup(sourceUri: Uri): RestoreResult
}
```

Models in `:core:common`:
- `BackupResult(uri: Uri, fileName: String, sizeBytes: Long, manifestSha256: String, createdAtEpochMillis: Long)`
- `BackupInspection(isValid: Boolean, formatVersion: Int, dbSchemaVersion: Int, createdAtEpochMillis: Long, mediaCount: Int, error: BackupError?)`
- `RestoreResult` sealed interface: `Success(restoredMediaCount: Int)` | `FailedAndRolledBack(reason: String)` | `AbortedBeforeMutation(reason: String)`.

### 5.2 `LocalZipBackupProvider` (`:core:database`)

> **API 29 Compatibility Note:** Android 10 (Realme 5s) ships SQLite `3.22.0`, which does **not** support `VACUUM INTO` (added in SQLite `3.27.0`). Snapshot consistency MUST use `DatabaseMaintenanceCoordinator` + `PRAGMA wal_checkpoint(TRUNCATE)`.

**Export Flow (`Downloads/FarmSaathiBackups/farm_saathi_YYYYMMDD_HHmmss.zip` via `MediaStore.Downloads`):**
1. Acquire `DatabaseMaintenanceCoordinator` exclusive lock (blocks concurrent repository writes).
2. Execute `PRAGMA wal_checkpoint(TRUNCATE)` on `SupportSQLiteDatabase` and verify checkpoint `busy == 0`.
3. Copy `farm_saathi.db` and `filesDir/media/` into `cacheDir/backup_staging/<uuid>/`.
4. Compute SHA-256 for `db/farm_saathi.db` and every relative media path; write `manifest.json` (`formatVersion = 1`, `dbSchemaVersion`, `createdAtEpochMillis`, `dbSha256`, `mediaFiles[]`).
5. Package into ZIP, write stream into `MediaStore.Downloads` (`RELATIVE_PATH = Download/FarmSaathiBackups`), update DataStore `lastBackupEpochMillis`, delete staging dir, and return `BackupResult` for `Intent.ACTION_SEND` Share Sheet.

**Rollback-Safe Restore Flow:**
1. Unzip `sourceUri` into `cacheDir/restore_staging/<uuid>/`.
2. Parse `manifest.json`; verify `formatVersion == 1` and `dbSchemaVersion <= CURRENT_DB_VERSION`. Verify SHA-256 of `db/farm_saathi.db` and every listed media file. Abort with `AbortedBeforeMutation` on any mismatch.
3. Open staged DB read-only via `SQLiteDatabase.openDatabase` and verify `PRAGMA integrity_check == "ok"`.
4. **Create Pre-Restore Rollback Snapshot** in `filesDir/rollback_snapshot/`: checkpoint live DB (`PRAGMA wal_checkpoint(TRUNCATE)`), copy current `farm_saathi.db` and move/copy `filesDir/media/` -> `filesDir/rollback_snapshot/media/`.
5. Close `FarmSaathiDatabase`, remove live `farm_saathi.db` + `-wal` + `-shm`.
6. Move staged `farm_saathi.db` and media into live locations.
7. Re-open `FarmSaathiDatabase` (Room runs deterministic `Migration` automatically if `dbSchemaVersion < CURRENT_DB_VERSION`) and execute smoke query (`SELECT COUNT(*) FROM farms`).
8. **If any step 5–7 fails:** restore `farm_saathi.db` and `media/` from `filesDir/rollback_snapshot/`, re-open DB, and return `FailedAndRolledBack`.
9. On success: clean up `rollback_snapshot/` and staging dir, reschedule `DailyTaskReminderWorker`, and emit database-reloaded signal.
