# Farm Saathi — Room Database Schema Specification

> **Database Class:** `com.farmsaathi.core.database.FarmSaathiDatabase`  
> **Database File:** `farm_saathi.db` | **Version:** `1` | **Journal Mode:** `WRITE_AHEAD_LOGGING` (`WAL`)  
> **Schema Export:** `exportSchema = true` (KSP `room.schemaLocation = "$projectDir/schemas"`)  
> **Destructive Migration:** **PROHIBITED** (`fallbackToDestructiveMigration` must never be configured).

---

## 1. Global Schema Conventions

1. **Money:** All monetary columns end in `Paise` and are stored as SQLite `INTEGER` (`Long`). `Double`/`Float` prohibited.
2. **Area:** All area columns end in `MicroAcres` (`Long`, `1 Acre = 1_000_000L`, `1 Gunta = 25_000L`, `1 Cent = 10_000L`).
3. **Quantities:** All quantity columns end in `MilliUnits` (`Long`, `1 Unit = 1_000L`).
4. **Dates & Times:**
   - Instant timestamps end in `EpochMillis` (`Long`, UTC millis).
   - Calendar dates without time-of-day (attendance day, sowing date, task due date, harvest date) end in `EpochDay` (`Long`, `LocalDate.toEpochDay()`).
5. **Foreign Keys & History Preservation:**
   - **Every** `@ForeignKey` uses `onDelete = ForeignKey.RESTRICT` (or `NO_ACTION` only if deferred; default `RESTRICT`). **`CASCADE` is strictly prohibited** across all financial, operational, and parent entities.
   - **Every** FK child column has an explicit `@Index` to avoid Room warnings and table scans on Snapdragon 665.
   - Parent entities (`farms`, `plots`, `crop_cycles`, `crop_templates`, `workers`, `input_products`, `contacts`) use `isArchived: Boolean = false` instead of `DELETE`.
   - Financial/ledger entities use `isVoided: Boolean = false` (+ `voidReason: String?`) instead of `DELETE`.
6. **Enums:** Converted via `.name` (`String`) in `FarmSaathiTypeConverters` — never by ordinal.

---

## 2. Room Entities (All 21 Tables)

### 2.1 `FarmEntity` (`tableName = "farms"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `name` | `String` | `TEXT` | `NOT NULL` |
| `villageOrLocation` | `String?` | `TEXT` | Nullable |
| `surveyNumber` | `String?` | `TEXT` | Nullable |
| `totalAreaMicroAcres` | `Long` | `INTEGER` | `>= 0` |
| `preferredAreaUnit` | `AreaUnit` | `TEXT` | Default `ACRE_GUNTA` |
| `soilType` | `String?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isPrimary` | `Boolean` | `INTEGER` | Default `0` |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_farms_archived(isArchived)`, `index_farms_name(name)`.

---

### 2.2 `PlotEntity` (`tableName = "plots"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `name` | `String` | `TEXT` | `NOT NULL` |
| `areaMicroAcres` | `Long` | `INTEGER` | `> 0` |
| `preferredAreaUnit` | `AreaUnit` | `TEXT` | Default `ACRE_GUNTA` |
| `soilType` | `String?` | `TEXT` | Nullable |
| `irrigationSource` | `IrrigationSource?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_plots_farm(farmId)`, `index_plots_farm_archived(farmId, isArchived)`.

---

### 2.3 `CropTemplateEntity` (`tableName = "crop_templates"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `templateKey` | `String` | `TEXT` | `NOT NULL`, e.g. `"starter_paddy"`, `"custom_<uuid>"` |
| `cropName` | `String` | `TEXT` | `NOT NULL` |
| `cropNameTe` | `String?` | `TEXT` | Nullable (Telugu display label for starter templates) |
| `defaultSeason` | `SeasonType` | `TEXT` | `KHARIF`, `RABI`, `ZAID`, `PERENNIAL` |
| `expectedDurationDays` | `Int` | `INTEGER` | `> 0` |
| `defaultYieldUnit` | `QuantityUnit` | `TEXT` | e.g. `QUINTAL` |
| `stages` | `List<CropStageTemplate>` | `TEXT` | JSON array via `TypeConverter` |
| `isStarterTemplate` | `Boolean` | `INTEGER` | `1` for 6 pre-seeded crops |
| `isEditable` | `Boolean` | `INTEGER` | Default `1` |
| `disclaimerText` | `String` | `TEXT` | Editable-template notice |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `UNIQUE index_crop_templates_key(templateKey)`, `index_crop_templates_archived(isArchived)`.

---

### 2.4 `CropCycleEntity` (`tableName = "crop_cycles"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `templateId` | `Long?` | `INTEGER` | FK -> `crop_templates(id)` `RESTRICT`, nullable |
| `cropName` | `String` | `TEXT` | `NOT NULL` |
| `variety` | `String?` | `TEXT` | Nullable |
| `season` | `SeasonType` | `TEXT` | `NOT NULL` |
| `cropYear` | `Int` | `INTEGER` | e.g. `2026` |
| `plantedAreaMicroAcres` | `Long` | `INTEGER` | `> 0` |
| `sowingDateEpochDay` | `Long` | `INTEGER` | `NOT NULL` |
| `expectedHarvestEpochDay` | `Long?` | `INTEGER` | Nullable |
| `actualHarvestEpochDay` | `Long?` | `INTEGER` | Nullable |
| `status` | `CropCycleStatus` | `TEXT` | `PLANNED`, `ACTIVE`, `HARVESTED`, `CLOSED`, `FAILED` |
| `isIntercrop` | `Boolean` | `INTEGER` | Default `0` |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_crop_cycles_farm(farmId)`, `index_crop_cycles_plot(plotId)`, `index_crop_cycles_template(templateId)`, `index_crop_cycles_status_archived(status, isArchived)`, `index_crop_cycles_sowing(sowingDateEpochDay)`.

---

### 2.5 `CropTaskEntity` (`tableName = "crop_tasks"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `title` | `String` | `TEXT` | `NOT NULL` |
| `description` | `String?` | `TEXT` | Nullable |
| `category` | `TaskCategory` | `TEXT` | `NOT NULL` |
| `dueDateEpochDay` | `Long` | `INTEGER` | `NOT NULL` |
| `offsetFromSowingDays` | `Int?` | `INTEGER` | Set if derived from template stage |
| `sourceTemplateStageKey` | `String?` | `TEXT` | Nullable |
| `priority` | `TaskPriority` | `TEXT` | `LOW`, `MEDIUM`, `HIGH` |
| `isCompleted` | `Boolean` | `INTEGER` | Default `0` |
| `completedAtEpochMillis` | `Long?` | `INTEGER` | Nullable |
| `reminderEnabled` | `Boolean` | `INTEGER` | Default `1` |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_crop_tasks_farm(farmId)`, `index_crop_tasks_plot(plotId)`, `index_crop_tasks_cycle(cropCycleId)`, `index_crop_tasks_due_lookup(isCompleted, isArchived, dueDateEpochDay)`.

---

### 2.6 `DiaryEntryEntity` (`tableName = "diary_entries"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `entryDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `category` | `DiaryCategory` | `TEXT` | `NOT NULL` |
| `title` | `String` | `TEXT` | `NOT NULL` |
| `notes` | `String?` | `TEXT` | Nullable |
| `photoPaths` | `List<String>` | `TEXT` | JSON array of relative paths under `filesDir/media/` |
| `voiceNotePath` | `String?` | `TEXT` | Relative `.m4a` path |
| `voiceNoteDurationMillis` | `Long?` | `INTEGER` | Nullable |
| `weatherTag` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_diary_farm(farmId)`, `index_diary_plot(plotId)`, `index_diary_cycle(cropCycleId)`, `index_diary_archived_date(isArchived, entryDateEpochMillis)`.

---

### 2.7 `LedgerEntryEntity` (`tableName = "ledger_entries"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `entryType` | `LedgerEntryType` | `TEXT` | `INCOME` \| `EXPENSE` |
| `sourceType` | `LedgerSourceType` | `TEXT` | `MANUAL_EXPENSE`, `MANUAL_INCOME`, `INPUT_PURCHASE`, `CROP_SALE`, `WORKER_PAYMENT`, `MACHINERY_SESSION` |
| `sourceId` | `Long?` | `INTEGER` | Non-null for all sources except `MANUAL_INCOME` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `contactId` | `Long?` | `INTEGER` | FK -> `contacts(id)` `RESTRICT` |
| `category` | `LedgerCategory` | `TEXT` | `NOT NULL` |
| `title` | `String` | `TEXT` | `NOT NULL` |
| `notes` | `String?` | `TEXT` | Nullable |
| `totalAmountPaise` | `Long` | `INTEGER` | `>= 0` |
| `settledAmountPaise` | `Long` | `INTEGER` | `0 <= settledAmountPaise <= totalAmountPaise` |
| `paymentStatus` | `PaymentStatus` | `TEXT` | `PAID`, `PARTIAL`, `PENDING` |
| `paymentMode` | `PaymentMode` | `TEXT` | `CASH`, `UPI`, `BANK_TRANSFER`, `CREDIT`, `CHEQUE`, `OTHER` |
| `transactionDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `dueDateEpochMillis` | `Long?` | `INTEGER` | Nullable |
| `receiptPhotoPath` | `String?` | `TEXT` | Nullable |
| `isVoided` | `Boolean` | `INTEGER` | Default `0` |
| `voidReason` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:**
- `UNIQUE index_ledger_source_unique(sourceType, sourceId)` (**prevents duplicate ledger rows per source!**)
- `index_ledger_farm(farmId)`, `index_ledger_plot(plotId)`, `index_ledger_cycle(cropCycleId)`, `index_ledger_contact(contactId)`
- `index_ledger_timeline(isVoided, isArchived, entryType, transactionDateEpochMillis)`
- `index_ledger_pending(paymentStatus, isVoided, isArchived)`

---

### 2.8 `ExpenseDetailEntity` (`tableName = "expense_details"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `contactId` | `Long?` | `INTEGER` | FK -> `contacts(id)` `RESTRICT` |
| `category` | `LedgerCategory` | `TEXT` | Expense categories |
| `description` | `String` | `TEXT` | `NOT NULL` |
| `vendorNameSnapshot` | `String?` | `TEXT` | Nullable |
| `billNumber` | `String?` | `TEXT` | Nullable |
| `quantityMilliUnits` | `Long?` | `INTEGER` | Nullable |
| `quantityUnit` | `QuantityUnit?` | `TEXT` | Nullable |
| `unitPricePaise` | `Long?` | `INTEGER` | Nullable |
| `totalAmountPaise` | `Long` | `INTEGER` | `>= 0` |
| `paidAmountPaise` | `Long` | `INTEGER` | `0 <= paidAmountPaise <= totalAmountPaise` |
| `paymentMode` | `PaymentMode` | `TEXT` | `NOT NULL` |
| `expenseDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `receiptPhotoPath` | `String?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isVoided` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_expense_farm(farmId)`, `index_expense_plot(plotId)`, `index_expense_cycle(cropCycleId)`, `index_expense_contact(contactId)`, `index_expense_date(expenseDateEpochMillis)`.

---

### 2.9 `CropSaleEntity` (`tableName = "crop_sales"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `harvestRecordId` | `Long?` | `INTEGER` | FK -> `harvest_records(id)` `RESTRICT` |
| `storageRecordId` | `Long?` | `INTEGER` | FK -> `storage_records(id)` `RESTRICT` |
| `buyerContactId` | `Long?` | `INTEGER` | FK -> `contacts(id)` `RESTRICT` |
| `buyerNameSnapshot` | `String` | `TEXT` | `NOT NULL` |
| `mandiOrLocation` | `String?` | `TEXT` | Nullable |
| `quantityMilliUnits` | `Long` | `INTEGER` | `> 0` |
| `quantityUnit` | `QuantityUnit` | `TEXT` | `NOT NULL` |
| `ratePerUnitPaise` | `Long` | `INTEGER` | `>= 0` |
| `grossAmountPaise` | `Long` | `INTEGER` | `>= 0` |
| `deductionsPaise` | `Long` | `INTEGER` | Default `0` (mandi fee/hamali/transport) |
| `netTotalAmountPaise` | `Long` | `INTEGER` | `grossAmountPaise - deductionsPaise`, `>= 0` |
| `amountReceivedPaise` | `Long` | `INTEGER` | `0 <= amountReceivedPaise <= netTotalAmountPaise` |
| `paymentStatus` | `PaymentStatus` | `TEXT` | `PAID`, `PARTIAL`, `PENDING` |
| `paymentMode` | `PaymentMode` | `TEXT` | `NOT NULL` |
| `saleDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `dueDateEpochMillis` | `Long?` | `INTEGER` | Nullable |
| `receiptPhotoPath` | `String?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isVoided` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_sale_farm(farmId)`, `index_sale_plot(plotId)`, `index_sale_cycle(cropCycleId)`, `index_sale_harvest(harvestRecordId)`, `index_sale_storage(storageRecordId)`, `index_sale_buyer(buyerContactId)`, `index_sale_date(saleDateEpochMillis)`.

---

### 2.10 `WorkerEntity` (`tableName = "workers"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long?` | `INTEGER` | FK -> `farms(id)` `RESTRICT`, nullable (shared across farms) |
| `name` | `String` | `TEXT` | `NOT NULL` |
| `phone` | `String?` | `TEXT` | Nullable |
| `skillRole` | `String?` | `TEXT` | Nullable |
| `defaultDailyWagePaise` | `Long` | `INTEGER` | `>= 0` |
| `defaultHalfDayPercent` | `Int` | `INTEGER` | Default `50` (`1..99`) |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_workers_farm(farmId)`, `index_workers_archived_name(isArchived, name)`.

---

### 2.11 `WorkerAttendanceEntity` (`tableName = "worker_attendance"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `workerId` | `Long` | `INTEGER` | FK -> `workers(id)` `RESTRICT` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `workDateEpochDay` | `Long` | `INTEGER` | `NOT NULL` |
| `status` | `AttendanceStatus` | `TEXT` | `FULL_DAY`, `HALF_DAY`, `ABSENT`, `PIECE_RATE` |
| `baseDailyWagePaise` | `Long` | `INTEGER` | Snapshot at entry, `>= 0` |
| `halfDayPercent` | `Int` | `INTEGER` | Default `50` (`1..99`) |
| `pieceRateUnit` | `QuantityUnit?` | `TEXT` | Nullable |
| `pieceQuantityMilliUnits` | `Long?` | `INTEGER` | Nullable |
| `pieceUnitRatePaise` | `Long?` | `INTEGER` | Nullable |
| `wageAccruedPaise` | `Long` | `INTEGER` | `>= 0` (persisted calculated accrual) |
| `notes` | `String?` | `TEXT` | Nullable |
| `isVoided` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `UNIQUE index_attendance_worker_date_unique(workerId, workDateEpochDay)`, `index_attendance_farm(farmId)`, `index_attendance_plot(plotId)`, `index_attendance_cycle(cropCycleId)`, `index_attendance_date(workDateEpochDay)`.

---

### 2.12 `WorkerPaymentEntity` (`tableName = "worker_payments"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `workerId` | `Long` | `INTEGER` | FK -> `workers(id)` `RESTRICT` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `paymentType` | `WorkerPaymentType` | `TEXT` | `ADVANCE` \| `SETTLEMENT` |
| `amountPaise` | `Long` | `INTEGER` | `> 0` |
| `paymentDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `paymentMode` | `PaymentMode` | `TEXT` | `NOT NULL` |
| `notes` | `String?` | `TEXT` | Nullable |
| `isVoided` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_worker_payments_worker(workerId)`, `index_worker_payments_farm(farmId)`, `index_worker_payments_cycle(cropCycleId)`, `index_worker_payments_date(paymentDateEpochMillis)`.

---

### 2.13 `MachinerySessionEntity` (`tableName = "machinery_sessions"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `providerContactId` | `Long?` | `INTEGER` | FK -> `contacts(id)` `RESTRICT` |
| `machineName` | `String` | `TEXT` | `NOT NULL` |
| `operationType` | `String` | `TEXT` | `NOT NULL` |
| `equipmentKey` | `String` | `TEXT` | Normalized `machineName.trim().lowercase()` |
| `activeLockKey` | `String?` | `TEXT` | `"$farmId:$equipmentKey"` while `RUNNING`, else `NULL` |
| `usageType` | `MachineryUsageType` | `TEXT` | `HIRED_IN`, `OWN_FARM_USE`, `HIRED_OUT` |
| `billingMode` | `MachineryBillingMode` | `TEXT` | `HOURLY`, `PER_ACRE`, `FIXED_FLAT` |
| `entryMode` | `MachineryEntryMode` | `TEXT` | `LIVE_TIMER`, `MANUAL_LOG` |
| `status` | `MachinerySessionStatus` | `TEXT` | `RUNNING`, `COMPLETED`, `CANCELLED` |
| `startEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `startElapsedRealtimeMillis` | `Long?` | `INTEGER` | Null for `MANUAL_LOG` |
| `startBootCount` | `Int?` | `INTEGER` | `Settings.Global.BOOT_COUNT` |
| `endEpochMillis` | `Long?` | `INTEGER` | Nullable while `RUNNING` |
| `endElapsedRealtimeMillis` | `Long?` | `INTEGER` | Nullable |
| `elapsedMinutes` | `Long?` | `INTEGER` | Nullable while `RUNNING` |
| `hourlyRatePaise` | `Long?` | `INTEGER` | Nullable |
| `perAcreRatePaise` | `Long?` | `INTEGER` | Nullable |
| `areaCoveredMicroAcres` | `Long?` | `INTEGER` | Nullable |
| `computedAmountPaise` | `Long` | `INTEGER` | Default `0`, rounded on stop |
| `paidAmountPaise` | `Long` | `INTEGER` | Default `0` |
| `paymentStatus` | `PaymentStatus` | `TEXT` | Default `PENDING` |
| `notes` | `String?` | `TEXT` | Nullable |
| `isVoided` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `UNIQUE index_machinery_active_lock_unique(activeLockKey)`, `index_machinery_farm(farmId)`, `index_machinery_plot(plotId)`, `index_machinery_cycle(cropCycleId)`, `index_machinery_contact(providerContactId)`, `index_machinery_status_start(status, startEpochMillis)`.

---

### 2.14 `InputProductEntity` (`tableName = "input_products"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long?` | `INTEGER` | FK -> `farms(id)` `RESTRICT`, nullable = shared store |
| `name` | `String` | `TEXT` | `NOT NULL` |
| `category` | `InputCategory` | `TEXT` | `SEED`, `FERTILIZER`, `PESTICIDE`, `HERBICIDE`, `FUNGICIDE`, `MICRONUTRIENT`, `ORGANIC`, `OTHER` |
| `brand` | `String?` | `TEXT` | Nullable |
| `unit` | `QuantityUnit` | `TEXT` | Canonical stock unit |
| `bagWeightMilliKg` | `Long?` | `INTEGER` | Optional weight when `unit == BAG` |
| `currentStockMilliUnits` | `Long` | `INTEGER` | `>= 0` (mutated only via `@Transaction`) |
| `lowStockThresholdMilliUnits` | `Long` | `INTEGER` | Default `0` |
| `defaultUnitPricePaise` | `Long?` | `INTEGER` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_input_products_farm(farmId)`, `index_input_products_lookup(isArchived, category, name)`.

---

### 2.15 `InputPurchaseEntity` (`tableName = "input_purchases"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `productId` | `Long` | `INTEGER` | FK -> `input_products(id)` `RESTRICT` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `vendorContactId` | `Long?` | `INTEGER` | FK -> `contacts(id)` `RESTRICT` |
| `vendorNameSnapshot` | `String?` | `TEXT` | Nullable |
| `billNumber` | `String?` | `TEXT` | Nullable |
| `quantityMilliUnits` | `Long` | `INTEGER` | `> 0` (stored in canonical `product.unit`) |
| `unit` | `QuantityUnit` | `TEXT` | Matches `product.unit` |
| `unitPricePaise` | `Long` | `INTEGER` | `>= 0` |
| `totalAmountPaise` | `Long` | `INTEGER` | `>= 0` |
| `paidAmountPaise` | `Long` | `INTEGER` | `0 <= paidAmountPaise <= totalAmountPaise` |
| `paymentStatus` | `PaymentStatus` | `TEXT` | `PAID`, `PARTIAL`, `PENDING` |
| `paymentMode` | `PaymentMode` | `TEXT` | `NOT NULL` |
| `purchaseDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `dueDateEpochMillis` | `Long?` | `INTEGER` | Nullable |
| `receiptPhotoPath` | `String?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isVoided` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_input_purchases_product(productId)`, `index_input_purchases_farm(farmId)`, `index_input_purchases_cycle(cropCycleId)`, `index_input_purchases_vendor(vendorContactId)`, `index_input_purchases_date(purchaseDateEpochMillis)`.

---

### 2.16 `StockMovementEntity` (`tableName = "stock_movements"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `productId` | `Long` | `INTEGER` | FK -> `input_products(id)` `RESTRICT` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long?` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `purchaseId` | `Long?` | `INTEGER` | FK -> `input_purchases(id)` `RESTRICT` |
| `movementType` | `StockMovementType` | `TEXT` | `IN`, `OUT`, `ADJUSTMENT` |
| `reason` | `StockMovementReason` | `TEXT` | `PURCHASE`, `OPENING_STOCK`, `FIELD_APPLICATION`, `RETURN`, `WASTAGE_EXPIRED`, `STOCK_COUNT_CORRECTION` |
| `signedDeltaMilliUnits` | `Long` | `INTEGER` | `!= 0` (`+` IN, `-` OUT) |
| `balanceAfterMilliUnits` | `Long` | `INTEGER` | `>= 0` |
| `unitCostPaiseSnapshot` | `Long?` | `INTEGER` | Analytics snapshot only; never creates cash ledger row |
| `movementDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `notes` | `String?` | `TEXT` | Nullable |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `UNIQUE index_stock_movement_purchase_unique(purchaseId)` (**prevents duplicate stock-in per purchase**), `index_stock_movement_product_date(productId, movementDateEpochMillis)`, `index_stock_movement_farm(farmId)`, `index_stock_movement_plot(plotId)`, `index_stock_movement_cycle(cropCycleId)`.

---

### 2.17 `IrrigationLogEntity` (`tableName = "irrigation_logs"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long?` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `irrigationDateEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `durationMinutes` | `Int` | `INTEGER` | `> 0` |
| `source` | `IrrigationSource` | `TEXT` | `BOREWELL`, `CANAL`, `OPEN_WELL`, `RAINFED`, `DRIP`, `TANK`, `OTHER` |
| `method` | `IrrigationMethod` | `TEXT` | `FLOOD`, `DRIP`, `SPRINKLER`, `FURROW`, `OTHER` |
| `motorHoursOrFuelNote` | `String?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_irrigation_farm(farmId)`, `index_irrigation_plot(plotId)`, `index_irrigation_cycle(cropCycleId)`, `index_irrigation_date(irrigationDateEpochMillis)`.

---

### 2.18 `HarvestRecordEntity` (`tableName = "harvest_records"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `cropCycleId` | `Long` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `harvestDateEpochDay` | `Long` | `INTEGER` | `NOT NULL` |
| `quantityMilliUnits` | `Long` | `INTEGER` | `> 0` |
| `quantityUnit` | `QuantityUnit` | `TEXT` | `NOT NULL` |
| `bagCount` | `Int?` | `INTEGER` | Nullable |
| `moisturePercentTenths` | `Int?` | `INTEGER` | e.g., `145` = `14.5%` |
| `qualityGrade` | `String?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_harvest_farm(farmId)`, `index_harvest_plot(plotId)`, `index_harvest_cycle(cropCycleId)`, `index_harvest_date(harvestDateEpochDay)`.

---

### 2.19 `StorageRecordEntity` (`tableName = "storage_records"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `cropCycleId` | `Long` | `INTEGER` | FK -> `crop_cycles(id)` `RESTRICT` |
| `harvestRecordId` | `Long?` | `INTEGER` | FK -> `harvest_records(id)` `RESTRICT` |
| `storageLocation` | `String` | `TEXT` | `NOT NULL` |
| `quantityMilliUnits` | `Long` | `INTEGER` | `> 0` initial quantity |
| `remainingQuantityMilliUnits` | `Long` | `INTEGER` | `0 <= remaining <= quantityMilliUnits` |
| `quantityUnit` | `QuantityUnit` | `TEXT` | `NOT NULL` |
| `storedDateEpochDay` | `Long` | `INTEGER` | `NOT NULL` |
| `bagCount` | `Int?` | `INTEGER` | Nullable |
| `status` | `StorageStatus` | `TEXT` | `IN_STORAGE`, `PARTIALLY_DISPATCHED`, `CLEARED` |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_storage_farm(farmId)`, `index_storage_cycle(cropCycleId)`, `index_storage_harvest(harvestRecordId)`, `index_storage_status(status, isArchived)`.

---

### 2.20 `SoilTestRecordEntity` (`tableName = "soil_test_records"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `farmId` | `Long` | `INTEGER` | FK -> `farms(id)` `RESTRICT` |
| `plotId` | `Long` | `INTEGER` | FK -> `plots(id)` `RESTRICT` |
| `testDateEpochDay` | `Long` | `INTEGER` | `NOT NULL` |
| `labName` | `String?` | `TEXT` | Nullable |
| `sampleNumber` | `String?` | `TEXT` | Nullable |
| `phTenths` | `Int?` | `INTEGER` | e.g., `72` = `pH 7.2` |
| `ecMilliSiemensPerCmHundredths` | `Int?` | `INTEGER` | e.g., `45` = `0.45 dS/m` |
| `organicCarbonPercentHundredths` | `Int?` | `INTEGER` | e.g., `58` = `0.58%` |
| `nitrogenKgPerHaTenths` | `Int?` | `INTEGER` | Nullable |
| `phosphorusKgPerHaTenths` | `Int?` | `INTEGER` | Nullable |
| `potassiumKgPerHaTenths` | `Int?` | `INTEGER` | Nullable |
| `overallRating` | `SoilHealthRating?` | `TEXT` | `LOW`, `MEDIUM`, `OPTIMAL`, `HIGH`, `SALINE_ALKALINE` |
| `reportPhotoPath` | `String?` | `TEXT` | Nullable |
| `recommendationsNote` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_soil_test_farm(farmId)`, `index_soil_test_plot(plotId)`, `index_soil_test_date(testDateEpochDay)`.

---

### 2.21 `ContactEntity` (`tableName = "contacts"`)
| Column | Kotlin Type | SQLite Type | Constraints / Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `INTEGER` | `@PrimaryKey(autoGenerate = true)` |
| `name` | `String` | `TEXT` | `NOT NULL` |
| `phone` | `String?` | `TEXT` | Nullable |
| `contactType` | `ContactType` | `TEXT` | `BUYER_TRADER`, `INPUT_DEALER`, `MACHINERY_PROVIDER`, `LABOUR_CONTRACTOR`, `AGRI_OFFICER_LAB`, `OTHER` |
| `villageOrMandi` | `String?` | `TEXT` | Nullable |
| `upiId` | `String?` | `TEXT` | Nullable |
| `notes` | `String?` | `TEXT` | Nullable |
| `isArchived` | `Boolean` | `INTEGER` | Default `0` |
| `createdAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |
| `updatedAtEpochMillis` | `Long` | `INTEGER` | `NOT NULL` |

**Indices:** `index_contacts_lookup(isArchived, contactType, name)`, `index_contacts_phone(phone)`.

---

## 3. Room TypeConverters & Migration Strategy

### 3.1 `FarmSaathiTypeConverters`
- **Enums** (`AreaUnit`, `QuantityUnit`, `SeasonType`, `CropCycleStatus`, `TaskCategory`, `TaskPriority`, `DiaryCategory`, `LedgerEntryType`, `LedgerSourceType`, `LedgerCategory`, `PaymentStatus`, `PaymentMode`, `AttendanceStatus`, `WorkerPaymentType`, `MachineryUsageType`, `MachineryBillingMode`, `MachineryEntryMode`, `MachinerySessionStatus`, `InputCategory`, `StockMovementType`, `StockMovementReason`, `IrrigationSource`, `IrrigationMethod`, `StorageStatus`, `SoilHealthRating`, `ContactType`): stored as `.name` strings (`@TypeConverter fun fromX(v: X): String = v.name`, `@TypeConverter fun toX(raw: String): X = enumValueOf(raw)`).
- **`List<String>` (`photoPaths`):** Serialized with `org.json.JSONArray` (zero external serialization library needed in `:core:database`).
- **`List<CropStageTemplate>` (`stages`):** Serialized with `org.json.JSONArray` / `JSONObject` (`stageKey`, `title`, `titleTe`, `offsetDaysFromSowing`, `category`, `notes`).

### 3.2 Schema Export & Migration Readiness
- `core/database/build.gradle.kts`:
  ```kotlin
  ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
      arg("room.incremental", "true")
      arg("room.generateKotlin", "true")
  }
  ```
- `@Database(entities = [...21 entities...], version = 1, exportSchema = true)`
- `Room.databaseBuilder(context, FarmSaathiDatabase::class.java, "farm_saathi.db").setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)`
- **Future upgrades:** Every version bump adds a deterministic `Migration(n, n + 1)` verified by `MigrationTestHelper` in `:core:database/src/androidTest/` against committed JSON schema files (`schemas/com.farmsaathi.core.database.FarmSaathiDatabase/1.json`, etc.).

---

## 4. Atomic `@Transaction` DAO / Repository Operations

| Operation | DAO / Coordinator | Atomic Steps Inside One Room `@Transaction` |
| :--- | :--- | :--- |
| **Manual Expense** | `LedgerTransactionDao` | Insert `ExpenseDetailEntity` -> Insert `LedgerEntryEntity(MANUAL_EXPENSE, expenseId)` |
| **Crop Sale** | `LedgerTransactionDao` | Insert `CropSaleEntity` -> If `storageRecordId != null`, validate & decrement `StorageRecordEntity.remainingQuantityMilliUnits` & update `StorageStatus` -> Insert `LedgerEntryEntity(CROP_SALE, saleId)` |
| **Ledger Settlement** | `LedgerTransactionDao` | Read `LedgerEntryEntity`, verify `settled + add <= total`, update `settledAmountPaise` & `paymentStatus` -> Sync source (`CropSaleEntity` / `InputPurchaseEntity` / `MachinerySessionEntity`) |
| **Void Financial Entry** | `LedgerTransactionDao` | Set `isVoided = 1` on `LedgerEntryEntity` + linked source entity (`ExpenseDetail` / `CropSale` / `WorkerPayment` / `MachinerySession` / `InputPurchase`; stock reversal requires explicit adjustment) |
| **Worker Attendance** | `LabourDao` | Upsert `WorkerAttendanceEntity` by `(workerId, workDateEpochDay)` with calculated `wageAccruedPaise` (no cash ledger entry) |
| **Worker Payment** | `LabourDao` | Insert `WorkerPaymentEntity` -> Insert `LedgerEntryEntity(WORKER_PAYMENT, paymentId)` |
| **Start Machinery** | `MachineryDao` | Insert `MachinerySessionEntity` with `status = RUNNING`, `activeLockKey = "$farmId:$equipmentKey"` (unique index rejects duplicate) |
| **Stop Machinery** | `MachineryDao` | Read session, verify `status == RUNNING`, compute `elapsedMinutes` & `computedAmountPaise` (hourly rounded to ₹1 / per-acre), set `status = COMPLETED, activeLockKey = NULL` -> If `usageType != OWN_FARM_USE`, insert `LedgerEntryEntity(MACHINERY_SESSION, sessionId)` |
| **Input Purchase** | `InventoryDao` | Insert `InputPurchaseEntity` -> Insert `StockMovementEntity(IN, PURCHASE, +qty, purchaseId)` -> Increment `InputProductEntity.currentStockMilliUnits` -> Insert `LedgerEntryEntity(INPUT_PURCHASE, purchaseId)` |
| **Stock Out / Adjust** | `InventoryDao` | Read `InputProductEntity`, verify `currentStockMilliUnits + signedDelta >= 0` (else throw `InsufficientStock`), insert `StockMovementEntity`, update `currentStockMilliUnits` |
| **Start Crop Cycle** | `CropDao` | Insert `CropCycleEntity` -> Bulk-insert `CropTaskEntity` stages computed from `CropTemplateEntity.stages` |
| **Shift Sowing Date** | `CropDao` | Update `CropCycleEntity.sowingDateEpochDay` -> Update `dueDateEpochDay = newSowing + offsetFromSowingDays` on uncompleted template-derived `CropTaskEntity` rows |

---

## 5. Offline Search Queries (SQL `LIKE` Without FTS Bloat)

### Why SQL `LIKE` Over FTS4
- A single farmer's multi-year database typically stays within `500–5,000` diary/ledger entries; indexed pre-filters + `LIKE` execute in `< 10 ms` on Snapdragon 665.
- Avoids FTS4 table duplication, insert/update triggers, tokenizer limitations on substring matches (e.g., `"urea"` matching `"50kg-urea"`), and FTS virtual-table rebuild risks during backup/restore.

### Query Safety & Example Pattern
User search text is sanitized in `:core:common` before binding:
```kotlin
fun escapeSqlLike(raw: String): String =
    raw.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
```

**Indexed Offline Search Examples:**
```sql
-- Diary Search (uses index_diary_archived_date + optional farm/plot/cycle pre-filters)
SELECT * FROM diary_entries
WHERE isArchived = 0
  AND farmId = :farmId
  AND (:plotId IS NULL OR plotId = :plotId)
  AND (:query = '' OR title LIKE '%' || :escapedQuery || '%' ESCAPE '\' COLLATE NOCASE
                   OR notes LIKE '%' || :escapedQuery || '%' ESCAPE '\' COLLATE NOCASE)
ORDER BY entryDateEpochMillis DESC
LIMIT :limit OFFSET :offset;

-- Unified Ledger Search (uses index_ledger_timeline)
SELECT * FROM ledger_entries
WHERE isVoided = 0 AND isArchived = 0
  AND farmId = :farmId
  AND (:entryType IS NULL OR entryType = :entryType)
  AND (:query = '' OR title LIKE '%' || :escapedQuery || '%' ESCAPE '\' COLLATE NOCASE
                   OR notes LIKE '%' || :escapedQuery || '%' ESCAPE '\' COLLATE NOCASE)
ORDER BY transactionDateEpochMillis DESC
LIMIT :limit OFFSET :offset;
```
