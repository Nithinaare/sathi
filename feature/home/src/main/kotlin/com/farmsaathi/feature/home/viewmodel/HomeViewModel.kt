package com.farmsaathi.feature.home.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.MachineryRepository
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val todayFormatted: String = "",
    val farms: List<Farm> = emptyList(),
    val activeFarm: Farm? = null,
    val seasonTotals: SeasonFinancialTotals =
        SeasonFinancialTotals(
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
        ),
    val dueTasks: List<CropTask> = emptyList(),
    val runningMachinery: List<MachinerySession> = emptyList(),
    val recentLedger: List<LedgerEntry> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val playingContactId: Long? = null,
    val isAudioPlaying: Boolean = false,
    val showBackupBanner: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val farmRepo: FarmRepository,
    private val financeRepo: FinanceRepository,
    private val machineryRepo: MachineryRepository,
    private val contactRepo: ContactAndSearchRepository,
    private val photoStorage: LocalPhotoStorage,
    private val voiceManager: VoiceNoteManager,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val activeFarmIdFlow =
        prefsRepo.userPreferences.flatMapLatest { prefs ->
            val fid = prefs.activeFarmId
            combine(
                financeRepo.observeSeasonTotals(fid),
                financeRepo.observeLedgerEntries(farmId = fid),
            ) { totals, ledger -> totals to ledger.take(4) }
        }

    private val playingContactIdFlow = MutableStateFlow<Long?>(null)
    private val totalRecordsFlow = MutableStateFlow(0)

    init {
        refreshBackupCheckCounts()
    }

    fun refreshBackupCheckCounts() {
        viewModelScope.launch {
            val c = contactRepo.getDataOverviewCounts()
            totalRecordsFlow.value =
                c.farms + c.plots + c.cropCycles + c.diaryEntries + c.ledgerEntries + c.workers + c.contacts
        }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(
            prefsRepo.userPreferences,
            farmRepo.observeFarms(),
            activeFarmIdFlow,
            farmRepo.observeDueAndOverdueTasks(timeProvider.todayEpochDay()),
            machineryRepo.observeRunningSessions(),
            contactRepo.observeContacts(),
            playingContactIdFlow,
            audioEngine.isSpeaking,
            totalRecordsFlow,
        ) { arr ->
            val prefs = arr[0] as UserPreferences

            @Suppress("UNCHECKED_CAST")
            val farms = arr[1] as List<Farm>

            @Suppress("UNCHECKED_CAST")
            val financePair = arr[2] as Pair<SeasonFinancialTotals, List<LedgerEntry>>

            @Suppress("UNCHECKED_CAST")
            val tasks = arr[3] as List<CropTask>

            @Suppress("UNCHECKED_CAST")
            val running = arr[4] as List<MachinerySession>

            @Suppress("UNCHECKED_CAST")
            val contacts = arr[5] as List<Contact>
            val playingContactId = arr[6] as Long?
            val speaking = arr[7] as Boolean
            val totalRecords = arr[8] as Int

            val activeFarm =
                farms.firstOrNull { it.id == prefs.activeFarmId }
                    ?: farms.firstOrNull { it.isPrimary }
                    ?: farms.firstOrNull()

            val lastBackup = prefs.lastBackupEpochMillis
            val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
            val backupBannerVisible =
                totalRecords > 10 &&
                    (lastBackup == null || (timeProvider.nowEpochMillis() - lastBackup) > thirtyDaysMs)

            HomeUiState(
                isLoading = false,
                prefs = prefs,
                todayFormatted =
                timeProvider.formatDate(
                    timeProvider.nowEpochMillis(),
                    prefs.language == AppLanguage.TELUGU,
                ),
                farms = farms,
                activeFarm = activeFarm,
                seasonTotals = financePair.first,
                dueTasks = tasks.take(5),
                runningMachinery = running,
                recentLedger = financePair.second,
                contacts = contacts.take(8),
                playingContactId = playingContactId,
                isAudioPlaying = speaking,
                showBackupBanner = backupBannerVisible,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectActiveFarm(farmId: Long?) {
        viewModelScope.launch {
            prefsRepo.setActiveFarmId(farmId)
        }
    }

    fun markTaskDone(taskId: Long) {
        viewModelScope.launch {
            farmRepo.updateTaskStatus(taskId, TaskStatus.COMPLETED)
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
        }
    }

    fun toggleHomeAudio() {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        audioEngine.speakCue(AudioCueCatalog.HOME_SUMMARY, useTe)
    }

    fun createTempCaptureFile(): File = photoStorage.createTempCaptureFile()

    fun resolvePhotoFile(path: String): File = photoStorage.resolveFile(path)

    fun capturePhotoWithExif(
        file: File,
        onReady: (String, Long?) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.compressAndPersistWithExif(file)
                .onSuccess { meta ->
                    file.delete()
                    onReady(meta.relativePath, meta.capturedAtEpochMillis)
                }
        }
    }

    fun importGalleryWithExif(
        uri: Uri,
        onReady: (String, Long?) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.saveCompressedWithExifFromUri(uri)
                .onSuccess { meta ->
                    onReady(meta.relativePath, meta.capturedAtEpochMillis)
                }
        }
    }

    fun formatExifDateTime(epochMillis: Long?): String {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        val ts = epochMillis ?: return if (useTe) "ఇప్పుడే" else "Just now"
        return "${timeProvider.formatDate(ts, useTe)} • ${timeProvider.formatTime(ts)}"
    }

    fun playContactBadge(contact: Contact) {
        val lang = uiState.value.prefs.language
        val useTe = lang == AppLanguage.TELUGU
        val roleLabel = FarmLabels.contactType(contact.contactType, lang)
        val spokenFallback = "${contact.name}, $roleLabel"
        if (playingContactIdFlow.value == contact.id) {
            voiceManager.stopPlayback()
            playingContactIdFlow.value = null
            return
        }
        val voicePath = contact.voiceNamePath
        if (!voicePath.isNullOrBlank()) {
            val started =
                voiceManager.startPlayback(
                    file = voiceManager.resolveMediaFile(voicePath),
                    onComplete = { playingContactIdFlow.value = null },
                    onError = {
                        playingContactIdFlow.value = null
                        audioEngine.speakDynamicText(spokenFallback, useTe)
                    },
                )
            if (started) {
                playingContactIdFlow.value = contact.id
                return
            }
        }
        audioEngine.speakDynamicText(spokenFallback, useTe)
    }

    fun currentElapsedSeconds(session: MachinerySession): Long {
        val ms =
            FinancialCalculators.computeMachineryElapsedMillis(
                startEpochMillis = session.startEpochMillis,
                startElapsedRealtimeMillis = session.startElapsedRealtimeMillis,
                startBootCount = session.startBootCount,
                nowEpochMillis = timeProvider.nowEpochMillis(),
                nowElapsedRealtimeMillis = timeProvider.elapsedRealtimeMillis(),
                nowBootCount = timeProvider.bootCount(),
            )
        return ms / 1000L
    }

    override fun onCleared() {
        voiceManager.stopPlayback()
        super.onCleared()
    }
}
