package com.farmsaathi.core.common.repository

import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface FarmRepository {
    fun observeFarms(includeArchived: Boolean = false): Flow<List<Farm>>

    fun observeFarm(farmId: Long): Flow<Farm?>

    suspend fun saveFarm(farm: Farm): AppResult<Long>

    suspend fun ensureDefaultFarmAndPlot(): Pair<Long, Long>

    suspend fun setPrimaryFarm(farmId: Long): AppResult<Unit>

    suspend fun archiveFarm(farmId: Long): AppResult<Unit>

    fun observePlots(
        farmId: Long? = null,
        includeArchived: Boolean = false,
    ): Flow<List<Plot>>

    fun observePlot(plotId: Long): Flow<Plot?>

    suspend fun savePlot(plot: Plot): AppResult<Long>

    suspend fun archivePlot(plotId: Long): AppResult<Unit>

    fun observeCropTemplates(): Flow<List<CropTemplate>>

    suspend fun getCropTemplate(templateId: Long): CropTemplate?

    suspend fun saveCropTemplate(template: CropTemplate): AppResult<Long>

    suspend fun upsertCropTemplate(template: CropTemplate): AppResult<Long> = saveCropTemplate(template)

    suspend fun resetDefaultTemplates(): AppResult<Unit> = AppResult.Success(Unit)

    suspend fun applyTemplateToCrop(
        cropCycleId: Long,
        templateId: Long,
        useTeluguTitles: Boolean = true,
    ): AppResult<Int> = AppResult.Success(0)

    suspend fun ensureStarterTemplatesSeeded()

    fun observeCropCycles(
        plotId: Long? = null,
        farmId: Long? = null,
    ): Flow<List<CropCycle>>

    fun observeCropCycle(cropCycleId: Long): Flow<CropCycle?>

    suspend fun createCropCycleWithTemplate(
        cycle: CropCycle,
        generateTasksFromTemplate: Boolean,
        useTeluguTitles: Boolean,
    ): AppResult<Long>

    suspend fun updateCropCycle(
        cycle: CropCycle,
        recalculateUncompletedTemplateTasks: Boolean,
    ): AppResult<Unit>

    suspend fun updateCropCycleStatus(
        cropCycleId: Long,
        status: CropCycleStatus,
    ): AppResult<Unit>

    fun observeTasks(
        cropCycleId: Long? = null,
        plotId: Long? = null,
        farmId: Long? = null,
    ): Flow<List<CropTask>>

    fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTask>>

    suspend fun getDueAndOverdueCount(todayEpochDay: Long): Int

    suspend fun getTask(taskId: Long): CropTask?

    suspend fun saveTask(task: CropTask): AppResult<Long>

    suspend fun updateTaskStatus(
        taskId: Long,
        status: TaskStatus,
        newDueEpochDay: Long? = null,
    ): AppResult<Unit>
}
