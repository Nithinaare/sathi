package com.farmsaathi.feature.operations.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.InventoryRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.InputPurchase
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.StockMovementReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class InventoryUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val products: List<InputProduct> = emptyList(),
    val purchases: List<InputPurchase> = emptyList(),
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val crops: List<CropCycle> = emptyList(),
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val insufficientStockPrompt: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class InventoryViewModel
@Inject
constructor(
    private val inventoryRepo: InventoryRepository,
    private val farmRepo: FarmRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val photoStorage: LocalPhotoStorage,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val stockBlocked = MutableStateFlow(false)

    val uiState: StateFlow<InventoryUiState> =
        combine(
            prefsRepo.userPreferences,
            inventoryRepo.observeProducts(),
            inventoryRepo.observePurchases(),
            farmRepo.observeFarms(),
            farmRepo.observePlots(),
            farmRepo.observeCropCycles(),
            saving,
            combine(audioEngine.isSpeaking, audioEngine.activeCueKey) { s, k -> s to k },
            stockBlocked,
            error,
        ) { arr ->
            val audioState = arr[7] as Pair<*, *>
            @Suppress("UNCHECKED_CAST")
            InventoryUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                products = arr[1] as List<InputProduct>,
                purchases = arr[2] as List<InputPurchase>,
                farms = arr[3] as List<Farm>,
                plots = arr[4] as List<Plot>,
                crops = arr[5] as List<CropCycle>,
                isSaving = arr[6] as Boolean,
                isAudioPlaying = audioState.first as Boolean,
                activeCueKey = audioState.second as String?,
                insufficientStockPrompt = arr[8] as Boolean,
                errorMessage = arr[9] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun observeMovements(productId: Long) = inventoryRepo.observeMovements(productId)

    fun formatDate(millis: Long): String = timeProvider.formatDate(millis, uiState.value.prefs.language == AppLanguage.TELUGU)

    fun epochMillisToDay(millis: Long): Long = timeProvider.epochMillisToEpochDay(millis)

    fun toggleGuideAudio() {
        toggleAudio(AudioCueCatalog.INVENTORY_HELP)
    }

    fun toggleAudio(cueKey: String) {
        audioEngine.speakCue(
            cueKey,
            uiState.value.prefs.language == AppLanguage.TELUGU,
        )
    }

    fun playErrorCue() {
        audioEngine.playAcousticFeedback(AcousticCue.ERROR)
    }

    fun resolvePhotoFile(path: String): File = photoStorage.resolveFile(path)

    fun createTempCaptureFile(): File = photoStorage.createTempCaptureFile()

    fun saveCapturedPhoto(
        file: File,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.compressAndPersist(file)
                .onSuccess { rel ->
                    file.delete()
                    onSaved(rel)
                }
                .onFailure { error.value = "Photo save failed" }
        }
    }

    fun importGalleryPhoto(
        uri: Uri,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.saveCompressedFromUri(uri)
                .onSuccess(onSaved)
                .onFailure { error.value = "Photo import failed" }
        }
    }

    fun clearStockPrompt() {
        stockBlocked.value = false
        error.value = null
    }

    fun saveProduct(
        existing: InputProduct?,
        name: String,
        category: InputCategory,
        brand: String,
        unit: QuantityUnit,
        lowThresholdMilli: Long,
        notes: String,
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val model =
                InputProduct(
                    id = existing?.id ?: 0L,
                    farmId = uiState.value.prefs.activeFarmId ?: uiState.value.farms.firstOrNull()?.id,
                    name = name,
                    category = category,
                    brand = brand.ifBlank { null },
                    unit = unit,
                    currentStockMilliUnits = existing?.currentStockMilliUnits ?: 0L,
                    lowStockThresholdMilliUnits = lowThresholdMilli,
                    defaultUnitPrice = existing?.defaultUnitPrice,
                    notes = notes.ifBlank { null },
                )
            when (val res = inventoryRepo.saveProduct(model)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onSaved(res.data)
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }

    fun recordPurchase(
        product: InputProduct,
        farmId: Long,
        cropCycleId: Long?,
        vendor: String,
        qtyMilli: Long,
        unitPrice: MoneyPaise,
        totalAmount: MoneyPaise,
        paidAmount: MoneyPaise,
        mode: PaymentMode,
        dateMillis: Long,
        notes: String,
        receiptPhotoPath: String? = null,
        onSaved: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val safePaid = paidAmount.coerceIn(MoneyPaise.ZERO, totalAmount)
            val purchase =
                InputPurchase(
                    productId = product.id,
                    farmId = farmId,
                    cropCycleId = cropCycleId,
                    vendorNameSnapshot = vendor.ifBlank { null },
                    quantityMilliUnits = qtyMilli,
                    unit = product.unit,
                    unitPrice = unitPrice,
                    totalAmount = totalAmount,
                    paidAmount = safePaid,
                    paymentStatus = FinancialCalculators.derivePaymentStatus(totalAmount, safePaid),
                    paymentMode = mode,
                    purchaseDateEpochMillis = dateMillis,
                    receiptPhotoPath = receiptPhotoPath,
                    notes = notes.ifBlank { null },
                )
            when (val res = inventoryRepo.recordPurchase(purchase)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onSaved()
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }

    fun recordUsageOrAdjustment(
        productId: Long,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        signedDeltaMilli: Long,
        reason: StockMovementReason,
        notes: String,
        photoPath: String? = null,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            stockBlocked.value = false
            when (
                val res =
                    inventoryRepo.recordStockAdjustmentOrUsage(
                        productId = productId,
                        farmId = farmId,
                        plotId = plotId,
                        cropCycleId = cropCycleId,
                        signedDeltaMilliUnits = signedDeltaMilli,
                        reason = reason,
                        notes = notes.ifBlank { null },
                        photoPath = photoPath,
                    )
            ) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onSaved()
                }
                is AppResult.Error -> {
                    if (res.error is DomainError.InsufficientInventory) {
                        stockBlocked.value = true
                    }
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }
}
