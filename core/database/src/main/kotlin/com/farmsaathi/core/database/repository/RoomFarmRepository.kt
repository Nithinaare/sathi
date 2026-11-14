package com.farmsaathi.core.database.repository

import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.database.DatabaseMaintenanceCoordinator
import com.farmsaathi.core.database.dao.CropDao
import com.farmsaathi.core.database.dao.FarmDao
import com.farmsaathi.core.database.dao.PlotDao
import com.farmsaathi.core.database.entity.CropTaskEntity
import com.farmsaathi.core.database.entity.CropTemplateEntity
import com.farmsaathi.core.database.entity.FarmEntity
import com.farmsaathi.core.database.entity.PlotEntity
import com.farmsaathi.core.database.mapper.toDomain
import com.farmsaathi.core.database.mapper.toEntity
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.StarterCropTemplates
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskPriority
import com.farmsaathi.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomFarmRepository
@Inject
constructor(
    private val farmDao: FarmDao,
    private val plotDao: PlotDao,
    private val cropDao: CropDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
) : FarmRepository {
    override fun observeFarms(includeArchived: Boolean): Flow<List<Farm>> =
        farmDao.observeFarms(includeArchived).map { list -> list.map { it.toDomain() } }

    override fun observeFarm(farmId: Long): Flow<Farm?> = farmDao.observeFarm(farmId).map { it?.toDomain() }

    override suspend fun saveFarm(farm: Farm): AppResult<Long> {
        if (farm.name.isBlank()) return AppResult.Error(DomainError.InvalidName)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity = farm.toEntity(now)
            val id =
                if (farm.id == 0L) {
                    farmDao.insertFarm(entity)
                } else {
                    farmDao.updateFarm(entity)
                    farm.id
                }
            if (farm.isPrimary) farmDao.setPrimaryFarm(id)
            AppResult.Success(id)
        }
    }

    override suspend fun ensureDefaultFarmAndPlot(): Pair<Long, Long> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val farmId =
                farmDao.getFirstActiveFarm()?.id ?: farmDao.insertFarm(
                    FarmEntity(
                        name = "మా పొలం",
                        totalAreaMicroAcres = AreaConverter.MICRO_ACRES_PER_ACRE,
                        isPrimary = true,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )
            val plotId =
                plotDao.getFirstActivePlot(farmId)?.id ?: plotDao.insertPlot(
                    PlotEntity(
                        farmId = farmId,
                        name = "ప్రధాన మడి",
                        areaMicroAcres = AreaConverter.MICRO_ACRES_PER_ACRE,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )
            farmId to plotId
        }

    override suspend fun setPrimaryFarm(farmId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            if (farmDao.getFarm(farmId) == null) {
                AppResult.Error(DomainError.FarmNotFound)
            } else {
                farmDao.setPrimaryFarm(farmId)
                AppResult.Success(Unit)
            }
        }

    override suspend fun archiveFarm(farmId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            farmDao.archiveFarm(farmId, timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override fun observePlots(
        farmId: Long?,
        includeArchived: Boolean,
    ): Flow<List<Plot>> = plotDao.observePlots(farmId, includeArchived).map { list -> list.map { it.toDomain() } }

    override fun observePlot(plotId: Long): Flow<Plot?> = plotDao.observePlot(plotId).map { it?.toDomain() }

    override suspend fun savePlot(plot: Plot): AppResult<Long> {
        if (plot.name.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (plot.areaMicroAcres <= 0L) return AppResult.Error(DomainError.InvalidArea)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity = plot.toEntity(now)
            val id =
                if (plot.id == 0L) {
                    plotDao.insertPlot(entity)
                } else {
                    plotDao.updatePlot(entity)
                    plot.id
                }
            AppResult.Success(id)
        }
    }

    override suspend fun archivePlot(plotId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            plotDao.archivePlot(plotId, timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override fun observeCropTemplates(): Flow<List<CropTemplate>> =
        cropDao.observeTemplates()
            .onStart { ensureStarterTemplatesSeeded() }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getCropTemplate(templateId: Long): CropTemplate? = cropDao.getTemplate(templateId)?.toDomain()

    override suspend fun saveCropTemplate(template: CropTemplate): AppResult<Long> = upsertCropTemplate(template)

    override suspend fun upsertCropTemplate(template: CropTemplate): AppResult<Long> {
        if (template.cropNameTe.isBlank() && template.cropNameEn.isBlank()) {
            return AppResult.Error(DomainError.InvalidName)
        }
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val resolvedKey =
                template.templateKey.trim().ifEmpty {
                    "custom_${now}_${UUID.randomUUID().toString().take(8)}"
                }
            val entity = template.copy(templateKey = resolvedKey).toEntity(now)
            val id = upsertTemplateInternal(cropDao, entity, now)
            AppResult.Success(id)
        }
    }

    override suspend fun resetDefaultTemplates(): AppResult<Unit> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            StarterCropTemplates.all().forEach { def ->
                upsertTemplateInternal(cropDao, def.toEntity(now), now)
            }
            AppResult.Success(Unit)
        }

    override suspend fun applyTemplateToCrop(
        cropCycleId: Long,
        templateId: Long,
        useTeluguTitles: Boolean,
    ): AppResult<Int> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            when (val count = cropDao.applyTemplateToCropAtomic(cropCycleId, templateId, useTeluguTitles, now)) {
                -1 -> AppResult.Error(DomainError.ValidationFailed("cropCycle", "missing"))
                -2 -> AppResult.Error(DomainError.ValidationFailed("cropTemplate", "missing"))
                else -> AppResult.Success(count)
            }
        }

    override suspend fun ensureStarterTemplatesSeeded() {
        if (cropDao.countTemplates() > 0) return
        coordinator.withExclusiveLock {
            if (cropDao.countTemplates() == 0) {
                val now = timeProvider.nowEpochMillis()
                cropDao.insertTemplates(StarterCropTemplates.all().map { it.toEntity(now) })
            }
        }
    }

    override fun observeCropCycles(
        plotId: Long?,
        farmId: Long?,
    ): Flow<List<CropCycle>> = cropDao.observeCropCycles(plotId, farmId).map { list -> list.map { it.toDomain() } }

    override fun observeCropCycle(cropCycleId: Long): Flow<CropCycle?> = cropDao.observeCropCycle(cropCycleId).map { it?.toDomain() }

    override suspend fun createCropCycleWithTemplate(
        cycle: CropCycle,
        generateTasksFromTemplate: Boolean,
        useTeluguTitles: Boolean,
    ): AppResult<Long> {
        if (cycle.cropName.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (cycle.plantedAreaMicroAcres <= 0L) return AppResult.Error(DomainError.InvalidArea)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val template = cycle.templateId?.let { cropDao.getTemplate(it) }
            val entity = cycle.toEntity(now)
            val id =
                cropDao.insertCycleWithTasks(entity) { cycleId ->
                    if (!generateTasksFromTemplate || template == null) {
                        emptyList()
                    } else {
                        template.stages.map { stage ->
                            CropTaskEntity(
                                farmId = cycle.farmId,
                                plotId = cycle.plotId,
                                cropCycleId = cycleId,
                                title = if (useTeluguTitles) stage.titleTe else stage.titleEn,
                                description = if (useTeluguTitles) stage.notesTe else stage.notesEn,
                                category = stage.category,
                                dueDateEpochDay = cycle.sowingDateEpochDay + stage.offsetDaysFromSowing,
                                offsetFromSowingDays = stage.offsetDaysFromSowing,
                                sourceTemplateStageKey = stage.stageKey,
                                createdAtEpochMillis = now,
                                updatedAtEpochMillis = now,
                            )
                        }
                    }
                }
            ensureExpectedHarvestReminder(cropDao, cycle.copy(id = id), useTeluguTitles, now)
            AppResult.Success(id)
        }
    }

    override suspend fun updateCropCycle(
        cycle: CropCycle,
        recalculateUncompletedTemplateTasks: Boolean,
    ): AppResult<Unit> {
        if (cycle.cropName.isBlank()) return AppResult.Error(DomainError.InvalidName)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            cropDao.updateCycleAndOptionallyShiftTasks(
                cycle = cycle.toEntity(now),
                shiftTasks = recalculateUncompletedTemplateTasks,
                nowMillis = now,
            )
            ensureExpectedHarvestReminder(cropDao, cycle, useTeluguTitles = true, nowMillis = now)
            AppResult.Success(Unit)
        }
    }

    override suspend fun updateCropCycleStatus(
        cropCycleId: Long,
        status: CropCycleStatus,
    ): AppResult<Unit> =
        coordinator.withExclusiveLock {
            cropDao.updateStatus(cropCycleId, status, timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override fun observeTasks(
        cropCycleId: Long?,
        plotId: Long?,
        farmId: Long?,
    ): Flow<List<CropTask>> = cropDao.observeTasks(cropCycleId, plotId, farmId).map { list -> list.map { it.toDomain() } }

    override fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTask>> =
        cropDao.observeDueAndOverdueTasks(todayEpochDay).map { list -> list.map { it.toDomain() } }

    override suspend fun getDueAndOverdueCount(todayEpochDay: Long): Int = cropDao.countDueAndOverdueTasks(todayEpochDay)

    override suspend fun getTask(taskId: Long): CropTask? = cropDao.getTask(taskId)?.toDomain()

    override suspend fun saveTask(task: CropTask): AppResult<Long> {
        if (task.title.isBlank()) return AppResult.Error(DomainError.InvalidName)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity = task.toEntity(now)
            val id =
                if (task.id == 0L) {
                    cropDao.insertTask(entity)
                } else {
                    cropDao.updateTask(entity)
                    task.id
                }
            AppResult.Success(id)
        }
    }

    override suspend fun updateTaskStatus(
        taskId: Long,
        status: TaskStatus,
        newDueEpochDay: Long?,
    ): AppResult<Unit> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val completedAt = if (status == TaskStatus.COMPLETED) now else null
            cropDao.updateTaskStatus(
                taskId = taskId,
                status = status,
                completedAtMillis = completedAt,
                newDueEpochDay = newDueEpochDay,
                nowMillis = now,
            )
            AppResult.Success(Unit)
        }
}

private suspend fun upsertTemplateInternal(
    cropDao: CropDao,
    entity: CropTemplateEntity,
    nowMillis: Long,
): Long {
    val existing =
        if (entity.id != 0L) {
            cropDao.getTemplate(entity.id)
        } else {
            cropDao.getTemplateByKey(entity.templateKey)
        }
    return if (existing == null) {
        cropDao.insertTemplate(
            entity.copy(
                createdAtEpochMillis = nowMillis,
                updatedAtEpochMillis = nowMillis,
            ),
        )
    } else {
        cropDao.updateTemplate(
            entity.copy(
                id = existing.id,
                createdAtEpochMillis = existing.createdAtEpochMillis,
                updatedAtEpochMillis = nowMillis,
            ),
        )
        existing.id
    }
}

private suspend fun ensureExpectedHarvestReminder(
    cropDao: CropDao,
    cycle: CropCycle,
    useTeluguTitles: Boolean,
    nowMillis: Long,
) {
    val harvestDay = cycle.expectedHarvestEpochDay ?: return
    val stageKey = "auto_harvest_${cycle.id}"
    val existingAuto = cropDao.getTaskByStageKeyGlobal(stageKey)
    if (existingAuto != null) {
        if (existingAuto.status == TaskStatus.PENDING || existingAuto.status == TaskStatus.RESCHEDULED) {
            cropDao.updateTask(
                existingAuto.copy(
                    dueDateEpochDay = harvestDay,
                    updatedAtEpochMillis = nowMillis,
                ),
            )
        }
        return
    }
    val existingTasks = cropDao.getTasksForCropCycle(cycle.id)
    if (existingTasks.any { it.category == TaskCategory.HARVEST }) return
    cropDao.insertTask(
        CropTaskEntity(
            farmId = cycle.farmId,
            plotId = cycle.plotId,
            cropCycleId = cycle.id,
            title = if (useTeluguTitles) "కోత తయారీ (${cycle.cropName})" else "Harvest Ready (${cycle.cropName})",
            category = TaskCategory.HARVEST,
            dueDateEpochDay = harvestDay,
            sourceTemplateStageKey = stageKey,
            priority = TaskPriority.HIGH,
            reminderEnabled = true,
            createdAtEpochMillis = nowMillis,
            updatedAtEpochMillis = nowMillis,
        ),
    )
}
