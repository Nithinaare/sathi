package com.farmsaathi.feature.reports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DiaryRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.GlobalSearchHit
import com.farmsaathi.core.common.repository.InventoryRepository
import com.farmsaathi.core.common.repository.LabourRepository
import com.farmsaathi.core.common.repository.SearchHitType
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropProfitabilitySummary
import com.farmsaathi.core.model.DiaryEntry
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.WorkerBalanceSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchCategoryFilter(
    val labelTe: String,
    val labelEn: String,
) {
    ALL("అన్నీ", "All"),
    CONTACTS("పరిచయాలు", "Contacts"),
    CROPS("పంటలు", "Crops"),
    EXPENSES("ఖర్చులు", "Expenses"),
    WORKERS("కూలీలు", "Workers"),
    DIARY("డైరీ", "Diary"),
    INPUTS("మందులు", "Inputs"),
}

private data class SearchBrowseCatalog(
    val contactHits: List<GlobalSearchHit> = emptyList(),
    val cropHits: List<GlobalSearchHit> = emptyList(),
    val ledgerHits: List<GlobalSearchHit> = emptyList(),
    val workerHits: List<GlobalSearchHit> = emptyList(),
    val diaryHits: List<GlobalSearchHit> = emptyList(),
    val inputHits: List<GlobalSearchHit> = emptyList(),
    val contactPhonesById: Map<Long, String> = emptyMap(),
)

data class ReportsUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val seasonTotals: SeasonFinancialTotals =
        SeasonFinancialTotals(
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
        ),
    val categoryTotals: List<Pair<LedgerCategory, MoneyPaise>> = emptyList(),
    val cropProfitabilities: List<CropProfitabilitySummary> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: SearchCategoryFilter = SearchCategoryFilter.ALL,
    val searchHits: List<GlobalSearchHit> = emptyList(),
    val contactPhonesById: Map<Long, String> = emptyMap(),
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
)

@HiltViewModel
class ReportsAndSearchViewModel
@Inject
constructor(
    financeRepo: FinanceRepository,
    private val contactAndSearchRepo: ContactAndSearchRepository,
    farmRepo: FarmRepository,
    diaryRepo: DiaryRepository,
    labourRepo: LabourRepository,
    inventoryRepo: InventoryRepository,
    prefsRepo: UserPreferencesRepository,
    private val audioEngine: AudioGuidanceEngine,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow(SearchCategoryFilter.ALL)
    private val hitsFlow = MutableStateFlow<List<GlobalSearchHit>>(emptyList())

    private val ledgerEntriesFlow: Flow<List<LedgerEntry>> =
        financeRepo
            .observeLedgerEntries()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val catalogFlow: Flow<SearchBrowseCatalog> =
        combine(
            contactAndSearchRepo.observeContacts(),
            farmRepo.observeCropCycles(),
            ledgerEntriesFlow,
            labourRepo.observeWorkersWithBalance(),
            diaryRepo.observeDiaryEntries(),
            inventoryRepo.observeProducts(),
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            val contacts = arr[0] as List<Contact>

            @Suppress("UNCHECKED_CAST")
            val crops = arr[1] as List<CropCycle>

            @Suppress("UNCHECKED_CAST")
            val ledger = arr[2] as List<LedgerEntry>

            @Suppress("UNCHECKED_CAST")
            val workers = arr[3] as List<WorkerBalanceSummary>

            @Suppress("UNCHECKED_CAST")
            val diary = arr[4] as List<DiaryEntry>

            @Suppress("UNCHECKED_CAST")
            val products = arr[5] as List<InputProduct>

            SearchBrowseCatalog(
                contactHits =
                contacts.map { c ->
                    GlobalSearchHit(
                        id = c.id,
                        type = SearchHitType.CONTACT,
                        title = c.name,
                        subtitle = c.phone.orEmpty(),
                        timestampMillis = 0L,
                    )
                },
                cropHits =
                crops.map { crop ->
                    GlobalSearchHit(
                        id = crop.id,
                        type = SearchHitType.CROP,
                        title = crop.cropName,
                        subtitle = crop.variety.orEmpty(),
                        timestampMillis = crop.updatedAtEpochMillis,
                    )
                },
                ledgerHits =
                ledger
                    .filter { !it.isVoided }
                    .sortedByDescending { it.transactionDateEpochMillis }
                    .map { entry ->
                        GlobalSearchHit(
                            id = entry.id,
                            type = SearchHitType.LEDGER,
                            title = entry.title,
                            subtitle = entry.totalAmount.formatInr(),
                            timestampMillis = entry.transactionDateEpochMillis,
                        )
                    },
                workerHits =
                workers.map { wb ->
                    GlobalSearchHit(
                        id = wb.worker.id,
                        type = SearchHitType.WORKER,
                        title = wb.worker.name,
                        subtitle = wb.worker.phone.orEmpty(),
                        timestampMillis = 0L,
                    )
                },
                diaryHits =
                diary
                    .sortedByDescending { it.entryDateEpochMillis }
                    .map { entry ->
                        GlobalSearchHit(
                            id = entry.id,
                            type = SearchHitType.DIARY,
                            title = entry.title,
                            subtitle = entry.notes.orEmpty(),
                            timestampMillis = entry.entryDateEpochMillis,
                        )
                    },
                inputHits =
                products.map { p ->
                    GlobalSearchHit(
                        id = p.id,
                        type = SearchHitType.INVENTORY,
                        title = p.name,
                        subtitle = p.category.name,
                        timestampMillis = 0L,
                    )
                },
                contactPhonesById = contacts.mapNotNull { c ->
                    val phone = c.phone?.trim().orEmpty()
                    if (phone.isNotEmpty()) c.id to phone else null
                }.toMap(),
            )
        }.flowOn(dispatchers.default)

    val uiState: StateFlow<ReportsUiState> =
        combine(
            prefsRepo.userPreferences,
            financeRepo.observeSeasonTotals(),
            ledgerEntriesFlow,
            financeRepo.observeCropProfitabilitySummaries(),
            queryFlow,
            selectedCategoryFlow,
            hitsFlow,
            catalogFlow,
            audioEngine.isSpeaking,
            audioEngine.activeCueKey,
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            val ledger = arr[2] as List<LedgerEntry>
            val catMap =
                ledger
                    .filter { it.entryType == LedgerEntryType.EXPENSE && !it.isVoided }
                    .groupBy { it.category }
                    .map { (cat, list) -> cat to list.fold(MoneyPaise.ZERO) { acc, item -> acc + item.totalAmount } }
                    .sortedByDescending { it.second.paise }

            val query = arr[4] as String
            val category = arr[5] as SearchCategoryFilter

            @Suppress("UNCHECKED_CAST")
            val typedHits = arr[6] as List<GlobalSearchHit>
            val catalog = arr[7] as SearchBrowseCatalog

            val resolvedHits =
                if (query.isBlank()) {
                    browseHitsFor(catalog, category)
                } else {
                    filterByCategory(typedHits, category)
                }

            @Suppress("UNCHECKED_CAST")
            ReportsUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                seasonTotals = arr[1] as SeasonFinancialTotals,
                categoryTotals = catMap,
                cropProfitabilities = arr[3] as List<CropProfitabilitySummary>,
                searchQuery = query,
                selectedCategory = category,
                searchHits = resolvedHits,
                contactPhonesById = catalog.contactPhonesById,
                isAudioPlaying = arr[8] as Boolean,
                activeCueKey = arr[9] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { query ->
                    hitsFlow.value =
                        if (query.isBlank()) {
                            emptyList()
                        } else {
                            contactAndSearchRepo.searchOffline(query)
                        }
                }
        }
    }

    fun toggleGuideAudio() {
        audioEngine.speakCue(
            AudioCueCatalog.REPORTS_SUMMARY,
            uiState.value.prefs.language == AppLanguage.TELUGU,
        )
    }

    fun toggleSearchGuideAudio() {
        val state = uiState.value
        if (state.isAudioPlaying) {
            audioEngine.stop()
            return
        }
        val useTe = state.prefs.language == AppLanguage.TELUGU
        audioEngine.speakDynamicText(
            text =
            if (useTe) {
                "పైన ఉన్న గళ్ళలో పరిచయాలు, పంటలు, ఖర్చులు, కూలీలు, డైరీ " +
                    "లేదా మందులు నొక్కి మీ వివరాలను సులువుగా చూడవచ్చు."
            } else {
                "Tap any category button above to browse contacts, crops, " +
                    "expenses, workers, diary, or inputs, or type to search."
            },
            useTelugu = useTe,
        )
    }

    fun speakSearchHit(
        hit: GlobalSearchHit,
        typeLabel: String,
        subtitleLabel: String,
    ) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        val text =
            listOf(typeLabel, hit.title, subtitleLabel)
                .filter { it.isNotBlank() }
                .joinToString(", ")
        audioEngine.speakDynamicText(text = text, useTelugu = useTe)
    }

    fun selectCategory(category: SearchCategoryFilter) {
        selectedCategoryFlow.value = category
    }

    fun updateSearch(query: String) {
        queryFlow.value = query
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }

    private fun browseHitsFor(
        catalog: SearchBrowseCatalog,
        category: SearchCategoryFilter,
    ): List<GlobalSearchHit> =
        when (category) {
            SearchCategoryFilter.ALL ->
                (
                    catalog.contactHits.take(8) +
                        catalog.cropHits.take(8) +
                        catalog.ledgerHits.take(8) +
                        catalog.workerHits.take(6) +
                        catalog.diaryHits.take(6) +
                        catalog.inputHits.take(6)
                    )
            SearchCategoryFilter.CONTACTS -> catalog.contactHits
            SearchCategoryFilter.CROPS -> catalog.cropHits
            SearchCategoryFilter.EXPENSES -> catalog.ledgerHits
            SearchCategoryFilter.WORKERS -> catalog.workerHits
            SearchCategoryFilter.DIARY -> catalog.diaryHits
            SearchCategoryFilter.INPUTS -> catalog.inputHits
        }

    private fun filterByCategory(
        hits: List<GlobalSearchHit>,
        category: SearchCategoryFilter,
    ): List<GlobalSearchHit> =
        when (category) {
            SearchCategoryFilter.ALL -> hits
            SearchCategoryFilter.CONTACTS -> hits.filter { it.type == SearchHitType.CONTACT }
            SearchCategoryFilter.CROPS -> hits.filter { it.type == SearchHitType.CROP }
            SearchCategoryFilter.EXPENSES -> hits.filter { it.type == SearchHitType.LEDGER }
            SearchCategoryFilter.WORKERS -> hits.filter { it.type == SearchHitType.WORKER }
            SearchCategoryFilter.DIARY -> hits.filter { it.type == SearchHitType.DIARY }
            SearchCategoryFilter.INPUTS -> hits.filter { it.type == SearchHitType.INVENTORY }
        }

    fun buildShareSummaryText(): String {
        val state = uiState.value
        val useTe = state.prefs.language == AppLanguage.TELUGU
        val t = state.seasonTotals
        val sb = StringBuilder()
        if (useTe) {
            sb.appendLine("ఫామ్ సాథీ — ఆర్థిక నివేదిక (నమోదు చేసిన లెక్కల ప్రకారం)")
            sb.appendLine("మొత్తం ఖర్చు: ${t.totalExpense.formatInr()}")
            sb.appendLine("మొత్తం ఆదాయం: ${t.totalIncome.formatInr()}")
            sb.appendLine("నిల్వ / తేడా: ${t.netBalance.formatInr()}")
            sb.appendLine("ఇవ్వవలసిన బాకీ: ${t.pendingPayable.formatInr()}")
            sb.appendLine("రావలసిన బాకీ: ${t.pendingReceivable.formatInr()}")
        } else {
            sb.appendLine("Farm Saathi — Financial Summary (Recorded Data Only)")
            sb.appendLine("Total Expense: ${t.totalExpense.formatInr()}")
            sb.appendLine("Total Income: ${t.totalIncome.formatInr()}")
            sb.appendLine("Known Balance: ${t.netBalance.formatInr()}")
            sb.appendLine("Pending Payable: ${t.pendingPayable.formatInr()}")
            sb.appendLine("Pending Receivable: ${t.pendingReceivable.formatInr()}")
        }
        return sb.toString()
    }
}
