package com.farmsaathi.core.model

/**
 * Typed domain errors for predictable failure handling across repositories, use cases, and ViewModels.
 */
sealed class DomainError(val messageKey: String, val detail: String? = null) {
    data object FarmNotFound : DomainError("error_farm_not_found")

    data object PlotNotFound : DomainError("error_plot_not_found")

    data object PlotNotSelected : DomainError("error_plot_not_selected")

    data object CropCycleNotFound : DomainError("error_crop_cycle_not_found")

    data object InvalidName : DomainError("error_invalid_name")

    data object InvalidAmount : DomainError("error_invalid_amount")

    data object InvalidQuantity : DomainError("error_invalid_quantity")

    data object InvalidArea : DomainError("error_invalid_area")

    data object DuplicateAttendance : DomainError("error_duplicate_attendance")

    data object ActiveMachinerySessionExists : DomainError("error_active_machinery_session")

    data object MachinerySessionNotFound : DomainError("error_machinery_session_not_found")

    data object InvalidMachineryDuration : DomainError("error_invalid_machinery_duration")

    data class InsufficientInventory(val availableMilliUnits: Long) :
        DomainError("error_insufficient_inventory", "available=$availableMilliUnits")

    data object PaymentExceedsTotal : DomainError("error_payment_exceeds_total")

    data object MediaStorageFailure : DomainError("error_media_storage")

    data class BackupFailure(val reason: String) : DomainError("error_backup_failure", reason)

    data class ValidationFailed(val field: String, val reason: String) :
        DomainError("error_validation_failed", "$field:$reason")
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>

    data class Error(val error: DomainError) : AppResult<Nothing>
}
