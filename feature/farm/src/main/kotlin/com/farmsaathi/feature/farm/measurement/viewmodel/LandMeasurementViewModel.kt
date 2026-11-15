package com.farmsaathi.feature.farm.measurement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.LandMeasurementRepository
import com.farmsaathi.core.model.LandBoundaryPoint
import com.farmsaathi.core.model.LandMeasurement
import com.farmsaathi.core.model.LandMeasurementMethod
import com.farmsaathi.core.model.Plot
import com.farmsaathi.feature.farm.measurement.calculation.AreaCalculationError
import com.farmsaathi.feature.farm.measurement.calculation.AreaCalculationResult
import com.farmsaathi.feature.farm.measurement.calculation.AreaResult
import com.farmsaathi.feature.farm.measurement.calculation.GeoPoint
import com.farmsaathi.feature.farm.measurement.calculation.GeodesicAreaCalculator
import com.farmsaathi.feature.farm.measurement.location.BoundaryPointEditError
import com.farmsaathi.feature.farm.measurement.location.BoundaryPointEditResult
import com.farmsaathi.feature.farm.measurement.location.BoundaryPointEditor
import com.farmsaathi.feature.farm.measurement.location.GpsFixDecision
import com.farmsaathi.feature.farm.measurement.location.GpsFixRejection
import com.farmsaathi.feature.farm.measurement.location.GpsLocationDataSource
import com.farmsaathi.feature.farm.measurement.location.GpsLocationException
import com.farmsaathi.feature.farm.measurement.location.GpsLocationSample
import com.farmsaathi.feature.farm.measurement.location.GpsPointAcceptancePolicy
import com.farmsaathi.feature.farm.measurement.offline.OfflineMapDownloadState
import com.farmsaathi.feature.farm.measurement.offline.OfflineMapPackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The small, explicit screen state machine used by the land-measurement flow. */
enum class LandMeasurementStep {
    CHOOSE_METHOD,
    GPS_CAPTURE,
    MANUAL_INPUT,
    REVIEW,
    SAVED,
}

enum class GpsCaptureStatus {
    IDLE,
    WAITING_FOR_FIX,
    RECORDING,
    PAUSED,
    ERROR,
}

data class LandMeasurementUiState(
    val plot: Plot? = null,
    val step: LandMeasurementStep = LandMeasurementStep.CHOOSE_METHOD,
    val method: LandMeasurementMethod? = null,
    val gpsStatus: GpsCaptureStatus = GpsCaptureStatus.IDLE,
    val acceptedPoints: List<GpsLocationSample> = emptyList(),
    val latestAccuracyMeters: Double? = null,
    val lastRejectedFix: GpsFixRejection? = null,
    val calculation: AreaResult? = null,
    val calculationError: AreaCalculationError? = null,
    val manualLengthFeet: String = "",
    val manualWidthFeet: String = "",
    val startedAtEpochMillis: Long = 0L,
    val isSaving: Boolean = false,
    val isCalculating: Boolean = false,
    val savedMeasurementId: Long? = null,
    val savedAndAppliedToPlot: Boolean = false,
    val history: List<LandMeasurement> = emptyList(),
    val offlineMapDownloadState: OfflineMapDownloadState = OfflineMapDownloadState.Idle,
    val messageTe: String? = null,
    val messageEn: String? = null,
)

@HiltViewModel
class LandMeasurementViewModel
@Inject
constructor(
    private val farmRepository: FarmRepository,
    private val measurementRepository: LandMeasurementRepository,
    private val locationDataSource: GpsLocationDataSource,
    private val areaCalculator: GeodesicAreaCalculator,
    private val pointAcceptancePolicy: GpsPointAcceptancePolicy,
    private val offlineMapPackManager: OfflineMapPackManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LandMeasurementUiState())
    val state: StateFlow<LandMeasurementUiState> = mutableState.asStateFlow()

    private var plotObserver: Job? = null
    private var historyObserver: Job? = null
    private var locationJob: Job? = null
    private var loadedPlotId: Long? = null

    init {
        viewModelScope.launch {
            offlineMapPackManager.state.collect { downloadState ->
                mutableState.update { current ->
                    val selectedPlotId = current.plot?.id ?: loadedPlotId
                    val belongsToCurrentPlot = when (downloadState) {
                        OfflineMapDownloadState.Idle -> true
                        is OfflineMapDownloadState.Downloading -> selectedPlotId == downloadState.plotId
                        is OfflineMapDownloadState.Saved -> selectedPlotId == downloadState.plotId
                        is OfflineMapDownloadState.LimitReached -> selectedPlotId == downloadState.plotId
                        is OfflineMapDownloadState.Failed -> downloadState.plotId == null || selectedPlotId == downloadState.plotId
                    }
                    current.copy(offlineMapDownloadState = if (belongsToCurrentPlot) downloadState else OfflineMapDownloadState.Idle)
                }
            }
        }
    }

    private fun mapDownloadStateForPlot(plotId: Long): OfflineMapDownloadState =
        when (val downloadState = offlineMapPackManager.state.value) {
            OfflineMapDownloadState.Idle -> OfflineMapDownloadState.Idle
            is OfflineMapDownloadState.Downloading -> downloadState.takeIf { it.plotId == plotId } ?: OfflineMapDownloadState.Idle
            is OfflineMapDownloadState.Saved -> downloadState.takeIf { it.plotId == plotId } ?: OfflineMapDownloadState.Idle
            is OfflineMapDownloadState.LimitReached -> downloadState.takeIf { it.plotId == plotId } ?: OfflineMapDownloadState.Idle
            is OfflineMapDownloadState.Failed -> downloadState.takeIf { it.plotId == null || it.plotId == plotId } ?: OfflineMapDownloadState.Idle
        }

    fun downloadOfflineStreetMap() {
        val current = mutableState.value
        val plot = current.plot ?: return setError("మడి వివరాలు లేవు.", "Plot details are unavailable.")
        if (current.acceptedPoints.size < 3) {
            return setError("ముందుగా కనీసం మూడు హద్దు మూలలు నమోదు చేయండి.", "Record at least three boundary points first.")
        }
        offlineMapPackManager.downloadForBoundary(plot.id, current.acceptedPoints)
    }

    fun cancelOfflineStreetMapDownload() = offlineMapPackManager.cancelDownload()

    fun loadPlot(plotId: Long) {
        if (loadedPlotId == plotId) return
        loadedPlotId = plotId
        mutableState.update { current ->
            current.copy(offlineMapDownloadState = mapDownloadStateForPlot(plotId))
        }
        offlineMapPackManager.refreshForPlot(plotId)
        plotObserver?.cancel()
        historyObserver?.cancel()
        plotObserver = viewModelScope.launch {
            farmRepository.observePlot(plotId).collect { plot ->
                mutableState.update { it.copy(plot = plot) }
            }
        }
        historyObserver = viewModelScope.launch {
            measurementRepository.observeMeasurements(plotId).collect { history ->
                mutableState.update { it.copy(history = history) }
            }
        }
    }

    fun chooseGpsMethod() {
        mutableState.update {
            it.copy(
                step = LandMeasurementStep.GPS_CAPTURE,
                method = LandMeasurementMethod.GPS_BOUNDARY,
                gpsStatus = GpsCaptureStatus.IDLE,
                acceptedPoints = emptyList(),
                calculation = null,
                calculationError = null,
                startedAtEpochMillis = 0L,
                messageTe = null,
                messageEn = null,
            )
        }
    }

    fun chooseManualMethod() {
        pauseGps()
        mutableState.update {
            it.copy(
                step = LandMeasurementStep.MANUAL_INPUT,
                method = LandMeasurementMethod.MANUAL_RECTANGLE,
                calculation = null,
                calculationError = null,
                manualLengthFeet = "",
                manualWidthFeet = "",
                messageTe = null,
                messageEn = null,
            )
        }
    }

    fun startGps() {
        if (locationJob?.isActive == true) return
        if (mutableState.value.plot == null) {
            setError("ముందుగా మడిని తెరవండి.", "The selected plot could not be loaded.")
            return
        }
        mutableState.update {
            it.copy(
                gpsStatus = GpsCaptureStatus.WAITING_FOR_FIX,
                startedAtEpochMillis = it.startedAtEpochMillis.takeIf { time -> time > 0L } ?: System.currentTimeMillis(),
                messageTe = "GPS స్థానం కోసం వేచి ఉండండి. ఫోన్‌ను ఆకాశం వైపు ఉంచండి.",
                messageEn = "Waiting for a GPS fix. Keep the phone with a clear view of the sky.",
                lastRejectedFix = null,
                latestAccuracyMeters = null,
            )
        }
        locationJob = viewModelScope.launch {
            locationDataSource.locationUpdates()
                .catch { error -> handleLocationError(error) }
                .collect { sample -> acceptLocationSample(sample) }
        }
    }

    fun onPermissionDenied() {
        mutableState.update {
            it.copy(
                gpsStatus = GpsCaptureStatus.ERROR,
                messageTe = "GPS కొలతకు ఖచ్చితమైన స్థానం అనుమతి అవసరం.",
                messageEn = "Precise location permission is required for GPS area measurement.",
            )
        }
    }

    fun pauseGps() {
        locationJob?.cancel()
        locationJob = null
        mutableState.update { current ->
            if (current.step == LandMeasurementStep.GPS_CAPTURE && current.gpsStatus in setOf(GpsCaptureStatus.RECORDING, GpsCaptureStatus.WAITING_FOR_FIX)) {
                current.copy(
                    gpsStatus = GpsCaptureStatus.PAUSED,
                    messageTe = "కొలత ఆగింది. మళ్లీ కొనసాగించవచ్చు.",
                    messageEn = "Measurement paused. You can resume it.",
                )
            } else {
                current
            }
        }
    }

    fun undoLastPoint() {
        mutableState.update { current ->
            current.copy(
                acceptedPoints = current.acceptedPoints.dropLast(1),
                lastRejectedFix = null,
                messageTe = "చివరి స్థానం తొలగించబడింది.",
                messageEn = "Last point removed.",
            )
        }
    }

    fun finishGps() {
        pauseGps()
        val points = mutableState.value.acceptedPoints.map { sample ->
            GeoPoint(sample.latitude, sample.longitude, sample.accuracyMeters, sample.capturedAtEpochMillis)
        }
        if (mutableState.value.isCalculating) return
        viewModelScope.launch {
            mutableState.update { it.copy(isCalculating = true, messageTe = null, messageEn = null) }
            val result = withContext(Dispatchers.Default) { areaCalculator.calculate(points) }
            mutableState.update { current ->
                when (result) {
                    is AreaCalculationResult.Success -> current.copy(
                        step = LandMeasurementStep.REVIEW,
                        method = LandMeasurementMethod.GPS_BOUNDARY,
                        calculation = result.value,
                        calculationError = null,
                        isCalculating = false,
                        messageTe = null,
                        messageEn = null,
                    )
                    is AreaCalculationResult.Invalid -> {
                        val (te, en) = calculationErrorText(result.reason)
                        current.copy(
                            step = LandMeasurementStep.REVIEW,
                            method = LandMeasurementMethod.GPS_BOUNDARY,
                            calculation = null,
                            calculationError = result.reason,
                            isCalculating = false,
                            messageTe = te,
                            messageEn = en,
                        )
                    }
                }
            }
        }
    }

    /** Move one selected vertex to a coordinate tapped on the map. */
    fun moveBoundaryPoint(index: Int, latitude: Double, longitude: Double) {
        if (mutableState.value.step != LandMeasurementStep.REVIEW) return
        val result = BoundaryPointEditor.move(
            points = mutableState.value.acceptedPoints,
            index = index,
            latitude = latitude,
            longitude = longitude,
            nowEpochMillis = System.currentTimeMillis(),
        )
        applyBoundaryEdit(result)
    }

    /** Insert a new manually placed vertex after the selected vertex. */
    fun insertBoundaryPointAfter(index: Int, latitude: Double, longitude: Double) {
        if (mutableState.value.step != LandMeasurementStep.REVIEW) return
        val result = BoundaryPointEditor.insertAfter(
            points = mutableState.value.acceptedPoints,
            index = index,
            latitude = latitude,
            longitude = longitude,
            nowEpochMillis = System.currentTimeMillis(),
        )
        applyBoundaryEdit(result)
    }

    fun removeBoundaryPoint(index: Int) {
        if (mutableState.value.step != LandMeasurementStep.REVIEW) return
        applyBoundaryEdit(BoundaryPointEditor.remove(mutableState.value.acceptedPoints, index))
    }

    private fun applyBoundaryEdit(result: BoundaryPointEditResult) {
        when (result) {
            is BoundaryPointEditResult.Rejected -> {
                val message = when (result.reason) {
                    BoundaryPointEditError.INVALID_INDEX -> "ఎంచుకున్న మూల దొరకలేదు." to "The selected boundary point no longer exists."
                    BoundaryPointEditError.INVALID_COORDINATE -> "మ్యాప్ స్థానం సరైనది కాదు." to "The selected map coordinate is invalid."
                    BoundaryPointEditError.TOO_FEW_POINTS_TO_REMOVE -> "కనీసం మూడు మూలలు అవసరం." to "A boundary needs at least three points."
                    BoundaryPointEditError.TOO_MANY_POINTS -> "ఇంకా మూలలు చేర్చలేరు." to "The boundary point limit has been reached."
                }
                setError(message.first, message.second)
            }
            is BoundaryPointEditResult.Updated -> {
                val updatedPoints = result.points
                mutableState.update {
                    it.copy(
                        acceptedPoints = updatedPoints,
                        calculation = null,
                        calculationError = null,
                        isCalculating = true,
                        messageTe = "మార్పును లెక్కిస్తున్నాం…",
                        messageEn = "Recalculating the edited boundary…",
                    )
                }
                viewModelScope.launch {
                    val calculation = withContext(Dispatchers.Default) {
                        areaCalculator.calculate(
                            updatedPoints.map { sample ->
                                GeoPoint(sample.latitude, sample.longitude, sample.accuracyMeters, sample.capturedAtEpochMillis)
                            },
                        )
                    }
                    mutableState.update { current ->
                        // Ignore a stale calculation if another edit has already been applied.
                        if (current.acceptedPoints != updatedPoints) return@update current
                        when (calculation) {
                            is AreaCalculationResult.Success -> current.copy(
                                calculation = calculation.value,
                                calculationError = null,
                                isCalculating = false,
                                messageTe = "కొత్త హద్దు లెక్క పూర్తయింది.",
                                messageEn = "The edited boundary has been recalculated.",
                            )
                            is AreaCalculationResult.Invalid -> {
                                val (te, en) = calculationErrorText(calculation.reason)
                                current.copy(
                                    calculation = null,
                                    calculationError = calculation.reason,
                                    isCalculating = false,
                                    messageTe = te,
                                    messageEn = en,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateManualLength(value: String) {
        if (value.matches(DECIMAL_INPUT_REGEX)) mutableState.update { it.copy(manualLengthFeet = value) }
    }

    fun updateManualWidth(value: String) {
        if (value.matches(DECIMAL_INPUT_REGEX)) mutableState.update { it.copy(manualWidthFeet = value) }
    }

    fun reviewManualMeasurement() {
        val current = mutableState.value
        val length = current.manualLengthFeet.toDoubleOrNull()
        val width = current.manualWidthFeet.toDoubleOrNull()
        if (length == null || width == null || length <= 0.0 || width <= 0.0) {
            setError("పొడవు, వెడల్పు సున్నా కంటే ఎక్కువగా రాయండి.", "Enter a length and width greater than zero.")
            return
        }
        if (current.isCalculating) return
        viewModelScope.launch {
            mutableState.update { it.copy(isCalculating = true, messageTe = null, messageEn = null) }
            val result = withContext(Dispatchers.Default) {
                areaCalculator.calculateRectangleFromFeet(length, width)
            }
            mutableState.update { latest ->
                when (result) {
                    is AreaCalculationResult.Success -> latest.copy(
                        step = LandMeasurementStep.REVIEW,
                        method = LandMeasurementMethod.MANUAL_RECTANGLE,
                        calculation = result.value,
                        calculationError = null,
                        isCalculating = false,
                        startedAtEpochMillis = System.currentTimeMillis(),
                        messageTe = null,
                        messageEn = null,
                    )
                    is AreaCalculationResult.Invalid -> {
                        val (te, en) = calculationErrorText(result.reason)
                        latest.copy(
                            calculationError = result.reason,
                            isCalculating = false,
                            messageTe = te,
                            messageEn = en,
                        )
                    }
                }
            }
        }
    }

    fun saveMeasurement(applyAreaToPlot: Boolean) {
        val current = mutableState.value
        val plot = current.plot ?: return setError("మడి వివరాలు లేవు.", "Plot details are unavailable.")
        val calculation = current.calculation ?: return setError("ముందుగా కొలతను లెక్కించండి.", "Calculate the area before saving.")
        val method = current.method ?: return setError("కొలత పద్ధతి ఎంచుకోలేదు.", "Measurement method is missing.")
        if (current.isSaving) return

        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, messageTe = null, messageEn = null) }
            val now = System.currentTimeMillis()
            val points =
                if (method == LandMeasurementMethod.GPS_BOUNDARY) {
                    current.acceptedPoints.mapIndexed { index, sample ->
                        LandBoundaryPoint(
                            sequenceNumber = index,
                            latitude = sample.latitude,
                            longitude = sample.longitude,
                            horizontalAccuracyMeters = sample.accuracyMeters,
                            capturedAtEpochMillis = sample.capturedAtEpochMillis,
                        )
                    }
                } else {
                    emptyList()
                }
            val record =
                LandMeasurement(
                    farmId = plot.farmId,
                    plotId = plot.id,
                    method = method,
                    areaMicroAcres = calculation.areaMicroAcres,
                    perimeterMeters = calculation.perimeterMeters,
                    boundaryPoints = points,
                    startedAtEpochMillis = current.startedAtEpochMillis.takeIf { it > 0L } ?: now,
                    completedAtEpochMillis = now,
                )
            try {
                val id = measurementRepository.saveMeasurement(record, applyAreaToPlot)
                mutableState.update {
                    it.copy(
                        step = LandMeasurementStep.SAVED,
                        isSaving = false,
                        savedMeasurementId = id,
                        savedAndAppliedToPlot = applyAreaToPlot,
                        messageTe = if (applyAreaToPlot) "కొలత దాచబడింది. మడి విస్తీర్ణం కూడా మార్చబడింది." else "కొలత దాచబడింది.",
                        messageEn = if (applyAreaToPlot) "Measurement saved and plot area updated." else "Measurement saved.",
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        messageTe = "కొలత దాచలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
                        messageEn = error.message ?: "Could not save the measurement.",
                    )
                }
            }
        }
    }

    fun backToMethodChoice() {
        pauseGps()
        mutableState.update {
            it.copy(
                step = LandMeasurementStep.CHOOSE_METHOD,
                method = null,
                gpsStatus = GpsCaptureStatus.IDLE,
                acceptedPoints = emptyList(),
                calculation = null,
                calculationError = null,
                messageTe = null,
                messageEn = null,
            )
        }
    }

    fun startAnotherMeasurement() = backToMethodChoice()

    private fun acceptLocationSample(sample: GpsLocationSample) {
        val current = mutableState.value
        if (current.acceptedPoints.size >= MAXIMUM_CAPTURED_POINTS) {
            locationJob?.cancel()
            locationJob = null
            mutableState.update {
                it.copy(
                    gpsStatus = GpsCaptureStatus.PAUSED,
                    messageTe = "చాలా స్థానాలు నమోదయ్యాయి. ఇప్పుడు కొలతను పూర్తిచేయండి.",
                    messageEn = "The point limit has been reached. Finish and review this measurement.",
                )
            }
            return
        }
        val previous = current.acceptedPoints.lastOrNull()
        when (val decision = pointAcceptancePolicy.evaluate(sample, previous, System.currentTimeMillis())) {
            GpsFixDecision.Accept -> {
                mutableState.update {
                    it.copy(
                        acceptedPoints = it.acceptedPoints + sample,
                        latestAccuracyMeters = sample.accuracyMeters,
                        gpsStatus = GpsCaptureStatus.RECORDING,
                        lastRejectedFix = null,
                        messageTe = null,
                        messageEn = null,
                    )
                }
            }
            is GpsFixDecision.Reject -> {
                mutableState.update {
                    it.copy(
                        latestAccuracyMeters = sample.accuracyMeters,
                        lastRejectedFix = decision.reason,
                        messageTe = rejectionTextTe(decision.reason),
                        messageEn = rejectionTextEn(decision.reason),
                    )
                }
            }
        }
    }

    private fun handleLocationError(error: Throwable) {
        val (te, en) = when (error) {
            is GpsLocationException.PermissionMissing -> "స్థానం అనుమతి ఇవ్వండి." to "Allow precise location permission to measure the boundary."
            is GpsLocationException.ProviderDisabled -> "ఫోన్ సెట్టింగ్స్‌లో GPS ఆన్ చేయండి." to "Turn on GPS in phone settings."
            is GpsLocationException.ProviderUnavailable -> "ఈ ఫోన్‌లో GPS అందుబాటులో లేదు." to "GPS provider is unavailable on this device."
            else -> "GPS స్థానం పొందలేకపోయాం. మళ్లీ ప్రయత్నించండి." to "Could not receive GPS updates. Please try again."
        }
        mutableState.update { it.copy(gpsStatus = GpsCaptureStatus.ERROR, messageTe = te, messageEn = en) }
        locationJob = null
    }

    private fun setError(te: String, en: String) {
        mutableState.update { it.copy(messageTe = te, messageEn = en) }
    }

    override fun onCleared() {
        locationJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val MAXIMUM_CAPTURED_POINTS = 5_000
        val DECIMAL_INPUT_REGEX = Regex("^\\d{0,8}(\\.\\d{0,2})?$")

        fun calculationErrorText(error: AreaCalculationError): Pair<String, String> =
            when (error) {
                AreaCalculationError.TOO_FEW_POINTS -> "కనీసం 3 వేర్వేరు మూలలు నమోదు చేయండి." to "Walk the boundary and capture at least three distinct points."
                AreaCalculationError.INVALID_COORDINATE -> "GPS స్థానం సరిగా లేదు. మళ్లీ ప్రయత్నించండి." to "One or more GPS coordinates are invalid."
                AreaCalculationError.SELF_INTERSECTING_BOUNDARY -> "నడిచిన హద్దు ఒకదానిని మరొకటి దాటుతోంది. పాయింట్లు సరిచేయండి." to "The captured boundary crosses itself. Review the points."
                AreaCalculationError.BOUNDARY_NOT_CLOSED -> "ప్రారంభించిన చోటుకు దగ్గరగా తిరిగి రండి. చివరి స్థానం చాలా దూరంలో ఉంది." to "Return closer to the starting point before finishing the boundary walk."
                AreaCalculationError.ZERO_OR_INVALID_AREA -> "విస్తీర్ణం లెక్కించలేకపోయాం." to "The boundary does not produce a valid area."
                AreaCalculationError.TOO_MANY_POINTS -> "చాలా ఎక్కువ స్థానాలు నమోదయ్యాయి. మళ్లీ కొలవండి." to "Too many boundary points were recorded. Please measure again."
            }

        fun rejectionTextTe(reason: GpsFixRejection): String =
            when (reason) {
                GpsFixRejection.INVALID_COORDINATE -> "GPS స్థానం సరైనది కాదు."
                GpsFixRejection.MISSING_OR_POOR_ACCURACY -> "GPS స్పష్టత తక్కువగా ఉంది. కొద్దిసేపు ఆగండి."
                GpsFixRejection.STALE_FIX -> "పాత GPS స్థానం వచ్చింది. కొత్త స్థానం కోసం వేచి ఉండండి."
                GpsFixRejection.OUT_OF_ORDER_FIX -> "GPS సమయం సరిగా లేదు."
                GpsFixRejection.TOO_CLOSE_TO_PREVIOUS_POINT -> "ఇక్కడ కొంచెం ముందుకు నడిచిన తర్వాత స్థానం నమోదు అవుతుంది."
                GpsFixRejection.UNLIKELY_MOVEMENT -> "స్థానం ఒక్కసారిగా మారింది. GPS స్థిరపడే వరకు వేచి ఉండండి."
            }

        fun rejectionTextEn(reason: GpsFixRejection): String =
            when (reason) {
                GpsFixRejection.INVALID_COORDINATE -> "The GPS coordinate is invalid."
                GpsFixRejection.MISSING_OR_POOR_ACCURACY -> "GPS accuracy is weak. Wait for a clearer fix."
                GpsFixRejection.STALE_FIX -> "A stale GPS fix was received. Waiting for a fresh fix."
                GpsFixRejection.OUT_OF_ORDER_FIX -> "The GPS timestamp is out of order."
                GpsFixRejection.TOO_CLOSE_TO_PREVIOUS_POINT -> "Walk a little farther before another point is recorded."
                GpsFixRejection.UNLIKELY_MOVEMENT -> "The location jumped unexpectedly. Wait for GPS to settle."
            }
    }
}
