package com.farmsaathi.feature.farm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropStageTemplate
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.feature.farm.viewmodel.CropAndCalendarViewModel
import com.farmsaathi.feature.farm.viewmodel.FarmAndPlotViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class FarmAndCalendarViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDispatchers =
        object : AppDispatchers {
            override val io = testDispatcher
            override val default = testDispatcher
            override val main = testDispatcher
        }
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `parseAreaMicroAcres converts guntas cents and decimal acres accurately`() {
        val repo = FakeFarmRepo()
        val prefs = FakePrefsRepo()
        val audio = FakeAudioEngine()
        val vm = FarmAndPlotViewModel(repo, prefs, LocalPhotoStorage(context), audio, testDispatchers)

        assertEquals(2_500_000L, vm.parseAreaMicroAcres("2", "20", AreaUnit.ACRE_GUNTA))
        assertEquals(1_250_000L, vm.parseAreaMicroAcres("1", "25", AreaUnit.ACRE_CENT))
        assertEquals(3_750_000L, vm.parseAreaMicroAcres("3.75", "", AreaUnit.ACRE_DECIMAL))
    }

    @Test
    fun `saveFarm plays saved cue on success and error cue on invalid input`() =
        runTest {
            val repo = FakeFarmRepo()
            val prefs = FakePrefsRepo()
            val audio = FakeAudioEngine()
            val vm = FarmAndPlotViewModel(repo, prefs, LocalPhotoStorage(context), audio, testDispatchers)

            var savedId = 0L
            vm.saveFarm(
                existing = null,
                name = "Pedda Cheruvu",
                village = "Nalgonda",
                surveyNo = "44/A",
                areaMicroAcres = 2_000_000L,
                areaUnit = AreaUnit.ACRE_GUNTA,
                waterSource = null,
                photoPath = null,
                notes = "",
                onSaved = { savedId = it },
            )

            assertEquals(101L, savedId)
            assertEquals(AcousticCue.SAVED, audio.cues.last())

            repo.failNextSave = true
            vm.saveFarm(
                existing = null,
                name = "",
                village = "",
                surveyNo = "",
                areaMicroAcres = 0L,
                areaUnit = AreaUnit.ACRE_GUNTA,
                waterSource = null,
                photoPath = null,
                notes = "",
                onSaved = {},
            )
            assertEquals(AcousticCue.ERROR, audio.cues.last())
        }

    @Test
    fun `saveCropCycle triggers template task creation in telugu mode`() =
        runTest {
            val repo = FakeFarmRepo()
            val prefs = FakePrefsRepo()
            val audio = FakeAudioEngine()
            val vm = CropAndCalendarViewModel(repo, prefs, audio, FakeTimeProvider(), testDispatchers)
            val plot = Plot(id = 7L, farmId = 1L, name = "North Block", areaMicroAcres = 1_500_000L)

            var createdId = 0L
            vm.saveCropCycle(
                existing = null,
                plot = plot,
                templateId = 2L,
                cropName = "వరి",
                variety = "RNR 15048",
                season = SeasonType.KHARIF,
                sowingEpochDay = 20_650L,
                expectedHarvestEpochDay = 20_770L,
                generateTasksFromTemplate = true,
                recalculateTasksOnEdit = false,
                notes = "",
                onSaved = { createdId = it },
            )

            assertEquals(55L, createdId)
            assertTrue(repo.lastGenerateTasksFlag)
            assertEquals(AcousticCue.SAVED, audio.cues.last())
        }

    @Test
    fun `saveCropCycle with farmIdHint creates plot on target farm when none exist`() =
        runTest(testDispatcher) {
            val repo = FakeFarmRepo()
            repo.farmsFlow.value = listOf(Farm(id = 9L, name = "East Field", totalAreaMicroAcres = 2_000_000L))
            val prefs = FakePrefsRepo()
            val audio = FakeAudioEngine()
            val vm = CropAndCalendarViewModel(repo, prefs, audio, FakeTimeProvider(), testDispatchers)
            val collectorJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

            var savedId = 0L
            vm.saveCropCycle(
                existing = null,
                plot = null,
                templateId = null,
                cropName = "Cotton",
                variety = "",
                season = SeasonType.KHARIF,
                sowingEpochDay = 20_650L,
                expectedHarvestEpochDay = 20_800L,
                generateTasksFromTemplate = false,
                recalculateTasksOnEdit = false,
                notes = "",
                farmIdHint = 9L,
                onSaved = { savedId = it },
            )

            assertEquals(55L, savedId)
            assertEquals(9L, repo.lastSavedPlot?.farmId)
            assertEquals(9L, repo.lastSavedCycle?.farmId)
            assertEquals(33L, repo.lastSavedCycle?.plotId)
            collectorJob.cancel()
        }

    @Test
    fun `template upsert reset and apply delegate to repository with telugu preference`() =
        runTest(testDispatcher) {
            val repo = FakeFarmRepo()
            val prefs = FakePrefsRepo()
            val audio = FakeAudioEngine()
            val vm = CropAndCalendarViewModel(repo, prefs, audio, FakeTimeProvider(), testDispatchers)
            val collectorJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

            val template =
                CropTemplate(
                    id = 4L,
                    templateKey = "cotton_kharif",
                    cropNameTe = "పత్తి",
                    cropNameEn = "Cotton",
                    defaultSeason = SeasonType.KHARIF,
                    expectedDurationDays = 150,
                    defaultYieldUnit = QuantityUnit.QUINTAL,
                    stages =
                    listOf(
                        CropStageTemplate(
                            stageKey = "weeding",
                            titleTe = "కలుపు తీత",
                            titleEn = "Weeding",
                            offsetDaysFromSowing = 20,
                            category = TaskCategory.WEEDING,
                        ),
                    ),
                    disclaimerTe = "",
                    disclaimerEn = "",
                )
            var upsertedId = 0L
            var resetCalled = false
            var appliedCount = 0

            vm.upsertCropTemplate(template) { upsertedId = it }
            vm.resetDefaultTemplates { resetCalled = true }
            vm.applyTemplateToCrop(cropCycleId = 12L, templateId = 4L) { appliedCount = it }

            assertEquals(44L, upsertedId)
            assertEquals(template, repo.lastUpsertedTemplate)
            assertTrue(resetCalled)
            assertTrue(repo.resetTemplatesCalled)
            assertEquals(Triple(12L, 4L, true), repo.lastAppliedTemplate)
            assertEquals(3, appliedCount)
            collectorJob.cancel()
        }

    private class FakeFarmRepo : FarmRepository {
        var failNextSave = false
        var lastGenerateTasksFlag = false
        var lastSavedPlot: Plot? = null
        var lastSavedCycle: CropCycle? = null
        var lastUpsertedTemplate: CropTemplate? = null
        var resetTemplatesCalled = false
        var lastAppliedTemplate: Triple<Long, Long, Boolean>? = null
        val farmsFlow = MutableStateFlow<List<Farm>>(emptyList())

        override fun observeFarms(includeArchived: Boolean): Flow<List<Farm>> = farmsFlow

        override fun observeFarm(farmId: Long): Flow<Farm?> = flowOf(null)

        override suspend fun saveFarm(farm: Farm): AppResult<Long> =
            if (failNextSave) {
                AppResult.Error(DomainError.InvalidName)
            } else {
                AppResult.Success(101L)
            }

        override suspend fun ensureDefaultFarmAndPlot(): Pair<Long, Long> = 1L to 1L

        override suspend fun setPrimaryFarm(farmId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun archiveFarm(farmId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override fun observePlots(
            farmId: Long?,
            includeArchived: Boolean,
        ): Flow<List<Plot>> = flowOf(emptyList())

        override fun observePlot(plotId: Long): Flow<Plot?> = flowOf(null)

        override suspend fun savePlot(plot: Plot): AppResult<Long> {
            lastSavedPlot = plot
            return AppResult.Success(33L)
        }

        override suspend fun archivePlot(plotId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeCropTemplates(): Flow<List<CropTemplate>> = flowOf(emptyList())

        override suspend fun getCropTemplate(templateId: Long): CropTemplate? = null

        override suspend fun saveCropTemplate(template: CropTemplate): AppResult<Long> = AppResult.Success(1L)

        override suspend fun upsertCropTemplate(template: CropTemplate): AppResult<Long> {
            lastUpsertedTemplate = template
            return AppResult.Success(44L)
        }

        override suspend fun resetDefaultTemplates(): AppResult<Unit> {
            resetTemplatesCalled = true
            return AppResult.Success(Unit)
        }

        override suspend fun applyTemplateToCrop(
            cropCycleId: Long,
            templateId: Long,
            useTeluguTitles: Boolean,
        ): AppResult<Int> {
            lastAppliedTemplate = Triple(cropCycleId, templateId, useTeluguTitles)
            return AppResult.Success(3)
        }

        override suspend fun ensureStarterTemplatesSeeded() {}

        override fun observeCropCycles(
            plotId: Long?,
            farmId: Long?,
        ): Flow<List<CropCycle>> = flowOf(emptyList())

        override fun observeCropCycle(cropCycleId: Long): Flow<CropCycle?> = flowOf(null)

        override suspend fun createCropCycleWithTemplate(
            cycle: CropCycle,
            generateTasksFromTemplate: Boolean,
            useTeluguTitles: Boolean,
        ): AppResult<Long> {
            lastSavedCycle = cycle
            lastGenerateTasksFlag = generateTasksFromTemplate
            return AppResult.Success(55L)
        }

        override suspend fun updateCropCycle(
            cycle: CropCycle,
            recalculateUncompletedTemplateTasks: Boolean,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun updateCropCycleStatus(
            cropCycleId: Long,
            status: CropCycleStatus,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeTasks(
            cropCycleId: Long?,
            plotId: Long?,
            farmId: Long?,
        ): Flow<List<CropTask>> = flowOf(emptyList())

        override fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTask>> = flowOf(emptyList())

        override suspend fun getDueAndOverdueCount(todayEpochDay: Long): Int = 0

        override suspend fun getTask(taskId: Long): CropTask? = null

        override suspend fun saveTask(task: CropTask): AppResult<Long> = AppResult.Success(1L)

        override suspend fun updateTaskStatus(
            taskId: Long,
            status: TaskStatus,
            newDueEpochDay: Long?,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakePrefsRepo : UserPreferencesRepository {
        private val flow = MutableStateFlow(UserPreferences(language = AppLanguage.TELUGU))
        override val userPreferences: Flow<UserPreferences> = flow

        override suspend fun setOnboardingCompleted(completed: Boolean) {}

        override suspend fun updateProfile(
            farmerName: String,
            villageName: String,
        ) {}

        override suspend fun setLanguage(language: AppLanguage) {}

        override suspend fun setAudioGuidanceEnabled(enabled: Boolean) {}

        override suspend fun setPreferredAreaUnit(unit: AreaUnit) {}

        override suspend fun setActiveFarmId(farmId: Long?) {}

        override suspend fun setDailyReminder(
            enabled: Boolean,
            hour: Int,
            minute: Int,
        ) {}

        override suspend fun setLastBackupEpochMillis(epochMillis: Long) {}
    }

    private class FakeAudioEngine : AudioGuidanceEngine {
        val cues = mutableListOf<AcousticCue>()
        override val activeCueKey: StateFlow<String?> = MutableStateFlow(null)
        override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false)

        override fun speakCue(
            cueKey: String,
            useTelugu: Boolean,
        ) {}

        override fun speakDynamicText(
            text: String,
            useTelugu: Boolean,
        ) {}

        override fun playAcousticFeedback(cue: AcousticCue) {
            cues += cue
        }

        override fun stop() {}

        override fun isTeluguVoiceReady(): Boolean = true

        override fun hasCustomPrompt(cueKey: String): Boolean = false

        override fun customPromptFilePath(cueKey: String): String = ""

        override fun clearCustomPrompt(cueKey: String) {}
    }

    private class FakeTimeProvider : TimeProvider {
        override fun nowEpochMillis(): Long = 1_794_326_000_000L

        override fun todayEpochDay(): Long = 20_650L

        override fun elapsedRealtimeMillis(): Long = 1000L

        override fun bootCount(): Int = 1

        override fun epochMillisToEpochDay(epochMillis: Long): Long = 20_650L

        override fun epochDayToStartMillis(epochDay: Long): Long = epochDay * 86_400_000L

        override fun formatDate(
            epochMillis: Long,
            useTelugu: Boolean,
        ): String = "10 Nov 2026"

        override fun formatEpochDay(
            epochDay: Long,
            useTelugu: Boolean,
        ): String = "10 Nov 2026"

        override fun formatTime(epochMillis: Long): String = "10:00 AM"
    }
}
