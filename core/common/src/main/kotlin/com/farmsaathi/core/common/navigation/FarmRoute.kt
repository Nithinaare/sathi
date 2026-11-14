package com.farmsaathi.core.common.navigation

import kotlinx.serialization.Serializable

/**
 * Single centralized type-safe navigation contract for Farm Saathi Stage 1.
 * No feature module depends on another feature module; :app wires all routes.
 */
@Serializable
sealed interface FarmRoute {
    // 1. Onboarding
    @Serializable data object OnboardingWelcome : FarmRoute

    @Serializable data object OnboardingAudioMicTest : FarmRoute

    @Serializable data object OnboardingProfile : FarmRoute

    // 2. Home (Bottom Tab 1)
    @Serializable data object HomeTab : FarmRoute

    // 3. Farms, Plots & Crops (Bottom Tab 2)
    @Serializable data object FarmsTab : FarmRoute

    @Serializable data class FarmDetail(val farmId: Long) : FarmRoute

    @Serializable data class FarmForm(val farmId: Long? = null) : FarmRoute

    @Serializable data class PlotDetail(val plotId: Long) : FarmRoute

    @Serializable data class LandMeasurement(val plotId: Long) : FarmRoute

    @Serializable data class PlotForm(val farmId: Long, val plotId: Long? = null) : FarmRoute

    @Serializable data class CropList(val plotId: Long? = null) : FarmRoute

    @Serializable data class CropDetail(val cropId: Long) : FarmRoute

    @Serializable data class CropForm(
        val plotId: Long? = null,
        val cropCycleId: Long? = null,
        val farmId: Long? = null,
    ) : FarmRoute

    @Serializable data object CropTemplateList : FarmRoute

    // 4. Calendar & Activities
    @Serializable data class ActivityList(val cropCycleId: Long? = null, val plotId: Long? = null) : FarmRoute

    @Serializable data class ActivityForm(
        val taskId: Long? = null,
        val cropCycleId: Long? = null,
        val plotId: Long? = null,
        val initialDueDateEpochDay: Long? = null,
    ) : FarmRoute

    // 4b. Telugu Agricultural Calendar & Karte Guide
    @Serializable data class TeluguCalendar(val initialEpochDay: Long? = null) : FarmRoute

    @Serializable data class KarteGuide(val highlightKarteId: Int? = null) : FarmRoute

    // 5. Photo & Voice Diary
    @Serializable data class DiaryList(val cropCycleId: Long? = null, val plotId: Long? = null) : FarmRoute

    @Serializable data class DiaryForm(
        val entryId: Long? = null,
        val cropCycleId: Long? = null,
        val plotId: Long? = null,
        val initialPhotoPath: String? = null,
        val initialDateMillis: Long? = null,
    ) : FarmRoute

    // 6. Accounts (Bottom Tab 3), Expenses & Income/Sales
    @Serializable data object AccountsTab : FarmRoute

    @Serializable data class ExpenseDetail(val ledgerEntryId: Long) : FarmRoute

    @Serializable data class ExpenseForm(
        val expenseId: Long? = null,
        val cropCycleId: Long? = null,
        val initialPhotoPath: String? = null,
        val initialDateMillis: Long? = null,
    ) : FarmRoute

    // Preserved for route compatibility and dedicated sale detail navigation
    @Serializable data class SaleDetail(val saleId: Long) : FarmRoute

    @Serializable data class SaleForm(val saleId: Long? = null, val cropCycleId: Long? = null) : FarmRoute

    // 7. Labour Management
    @Serializable data object WorkerList : FarmRoute

    @Serializable data class WorkerDetail(val workerId: Long) : FarmRoute

    @Serializable data class WorkerForm(
        val workerId: Long? = null,
        val initialPhotoPath: String? = null,
    ) : FarmRoute

    @Serializable data class DailyAttendance(val farmId: Long? = null) : FarmRoute

    @Serializable data class WorkerPaymentForm(val workerId: Long, val isAdvance: Boolean = false) : FarmRoute

    // 8. Machinery Rental
    @Serializable data class MachineryList(val cropCycleId: Long? = null) : FarmRoute

    @Serializable data class MachineryStartSession(val cropCycleId: Long? = null) : FarmRoute

    @Serializable data class MachineryDetail(val sessionId: Long) : FarmRoute

    @Serializable data class MachineryManualForm(val sessionId: Long? = null, val cropCycleId: Long? = null) : FarmRoute

    // 9. Input Inventory
    @Serializable data object InventoryList : FarmRoute

    @Serializable data class InventoryProductDetail(val productId: Long) : FarmRoute

    @Serializable data class InventoryProductForm(val productId: Long? = null) : FarmRoute

    @Serializable data class InventoryPurchaseForm(val productId: Long? = null) : FarmRoute

    @Serializable data class InventoryUsageForm(
        val productId: Long = 0L,
        val plotId: Long? = null,
        val cropCycleId: Long? = null,
        val initialPhotoPath: String? = null,
        val initialDateMillis: Long? = null,
    ) : FarmRoute

    // 10. Irrigation, Harvest & Storage, Soil Tests
    @Serializable data class IrrigationList(val plotId: Long? = null, val cropCycleId: Long? = null) : FarmRoute

    @Serializable data class IrrigationForm(
        val logId: Long? = null,
        val plotId: Long? = null,
        val cropCycleId: Long? = null,
        val initialPhotoPath: String? = null,
        val initialDateMillis: Long? = null,
    ) : FarmRoute

    @Serializable data class HarvestList(val cropCycleId: Long? = null) : FarmRoute

    @Serializable data class HarvestForm(
        val harvestId: Long? = null,
        val cropCycleId: Long? = null,
        val initialPhotoPath: String? = null,
        val initialDateMillis: Long? = null,
    ) : FarmRoute

    @Serializable data class SoilTestList(val plotId: Long? = null) : FarmRoute

    @Serializable data class SoilTestDetail(val testId: Long) : FarmRoute

    @Serializable data class SoilTestForm(
        val testId: Long? = null,
        val plotId: Long? = null,
        val initialPhotoPath: String? = null,
        val initialDateMillis: Long? = null,
    ) : FarmRoute

    // 11. Contacts & Reports & Offline Search
    @Serializable data class ContactList(val filterType: String? = null) : FarmRoute

    @Serializable data class ContactDetail(val contactId: Long) : FarmRoute

    @Serializable data class ContactForm(val contactId: Long? = null) : FarmRoute

    @Serializable data object ReportsOverview : FarmRoute

    @Serializable data object OfflineSearch : FarmRoute

    // 12. Extras Hub, Snap Photo Router & Settings / Backup
    @Serializable data class SnapToLog(val photoPath: String, val exifEpochMillis: Long? = null) : FarmRoute

    @Serializable data object MoreTab : FarmRoute

    @Serializable data object SettingsHome : FarmRoute

    @Serializable data object BackupRestore : FarmRoute

    @Serializable data object OfflineHelp : FarmRoute
}
