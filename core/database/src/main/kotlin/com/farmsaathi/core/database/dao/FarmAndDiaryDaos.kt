package com.farmsaathi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.farmsaathi.core.database.entity.CropCycleEntity
import com.farmsaathi.core.database.entity.CropTaskEntity
import com.farmsaathi.core.database.entity.CropTemplateEntity
import com.farmsaathi.core.database.entity.DiaryEntryEntity
import com.farmsaathi.core.database.entity.FarmEntity
import com.farmsaathi.core.database.entity.PlotEntity
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropStageTemplate
import com.farmsaathi.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms WHERE (:includeArchived = 1 OR isArchived = 0) ORDER BY isPrimary DESC, name ASC")
    fun observeFarms(includeArchived: Boolean): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE id = :farmId")
    fun observeFarm(farmId: Long): Flow<FarmEntity?>

    @Query("SELECT * FROM farms WHERE id = :farmId")
    suspend fun getFarm(farmId: Long): FarmEntity?

    @Insert
    suspend fun insertFarm(farm: FarmEntity): Long

    @Update
    suspend fun updateFarm(farm: FarmEntity)

    @Query("UPDATE farms SET isPrimary = CASE WHEN id = :farmId THEN 1 ELSE 0 END")
    suspend fun setPrimaryFarm(farmId: Long)

    @Query("UPDATE farms SET isArchived = 1, updatedAtEpochMillis = :nowMillis WHERE id = :farmId")
    suspend fun archiveFarm(
        farmId: Long,
        nowMillis: Long,
    )

    @Query(
        """
        SELECT * FROM farms
        WHERE isArchived = 0
          AND (name LIKE '%' || :escaped || '%' ESCAPE '\'
               OR villageOrLocation LIKE '%' || :escaped || '%' ESCAPE '\')
        LIMIT :limit
        """,
    )
    suspend fun searchFarms(
        escaped: String,
        limit: Int,
    ): List<FarmEntity>

    @Query("SELECT COUNT(*) FROM farms WHERE isArchived = 0")
    suspend fun countActiveFarms(): Int

    @Query("SELECT * FROM farms WHERE isArchived = 0 ORDER BY isPrimary DESC, id ASC LIMIT 1")
    suspend fun getFirstActiveFarm(): FarmEntity?
}

@Dao
interface PlotDao {
    @Query(
        """
        SELECT * FROM plots
        WHERE (:farmId IS NULL OR farmId = :farmId)
          AND (:includeArchived = 1 OR isArchived = 0)
        ORDER BY name ASC
        """,
    )
    fun observePlots(
        farmId: Long?,
        includeArchived: Boolean,
    ): Flow<List<PlotEntity>>

    @Query("SELECT * FROM plots WHERE id = :plotId")
    fun observePlot(plotId: Long): Flow<PlotEntity?>

    @Query("SELECT * FROM plots WHERE id = :plotId")
    suspend fun getPlot(plotId: Long): PlotEntity?

    @Insert
    suspend fun insertPlot(plot: PlotEntity): Long

    @Update
    suspend fun updatePlot(plot: PlotEntity)

    @Query("UPDATE plots SET isArchived = 1, updatedAtEpochMillis = :nowMillis WHERE id = :plotId")
    suspend fun archivePlot(
        plotId: Long,
        nowMillis: Long,
    )

    @Query("SELECT * FROM plots WHERE isArchived = 0 AND name LIKE '%' || :escaped || '%' ESCAPE '\\' LIMIT :limit")
    suspend fun searchPlots(
        escaped: String,
        limit: Int,
    ): List<PlotEntity>

    @Query("SELECT COUNT(*) FROM plots WHERE isArchived = 0")
    suspend fun countActivePlots(): Int

    @Query("SELECT * FROM plots WHERE farmId = :farmId AND isArchived = 0 ORDER BY id ASC LIMIT 1")
    suspend fun getFirstActivePlot(farmId: Long): PlotEntity?
}

@Dao
interface CropDao {
    @Query("SELECT * FROM crop_templates WHERE isArchived = 0 ORDER BY isStarterTemplate DESC, cropNameEn ASC")
    fun observeTemplates(): Flow<List<CropTemplateEntity>>

    @Query("SELECT * FROM crop_templates WHERE id = :templateId")
    suspend fun getTemplate(templateId: Long): CropTemplateEntity?

    @Query("SELECT * FROM crop_templates WHERE templateKey = :templateKey LIMIT 1")
    suspend fun getTemplateByKey(templateKey: String): CropTemplateEntity?

    @Query("SELECT COUNT(*) FROM crop_templates")
    suspend fun countTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplates(templates: List<CropTemplateEntity>)

    @Insert
    suspend fun insertTemplate(template: CropTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: CropTemplateEntity)

    @Query(
        """
        SELECT * FROM crop_cycles
        WHERE isArchived = 0
          AND (:plotId IS NULL OR plotId = :plotId)
          AND (:farmId IS NULL OR farmId = :farmId)
        ORDER BY sowingDateEpochDay DESC, id DESC
        """,
    )
    fun observeCropCycles(
        plotId: Long?,
        farmId: Long?,
    ): Flow<List<CropCycleEntity>>

    @Query("SELECT * FROM crop_cycles WHERE id = :cropCycleId")
    fun observeCropCycle(cropCycleId: Long): Flow<CropCycleEntity?>

    @Query("SELECT * FROM crop_cycles WHERE id = :cropCycleId")
    suspend fun getCropCycle(cropCycleId: Long): CropCycleEntity?

    @Insert
    suspend fun insertCropCycle(cycle: CropCycleEntity): Long

    @Update
    suspend fun updateCropCycle(cycle: CropCycleEntity)

    @Query("UPDATE crop_cycles SET status = :status, updatedAtEpochMillis = :nowMillis WHERE id = :cropCycleId")
    suspend fun updateStatus(
        cropCycleId: Long,
        status: CropCycleStatus,
        nowMillis: Long,
    )

    @Query(
        """
        SELECT * FROM crop_cycles
        WHERE isArchived = 0
          AND (cropName LIKE '%' || :escaped || '%' ESCAPE '\'
               OR variety LIKE '%' || :escaped || '%' ESCAPE '\')
        LIMIT :limit
        """,
    )
    suspend fun searchCropCycles(
        escaped: String,
        limit: Int,
    ): List<CropCycleEntity>

    @Query("SELECT COUNT(*) FROM crop_cycles WHERE isArchived = 0")
    suspend fun countActiveCropCycles(): Int

    @Query(
        """
        SELECT * FROM crop_tasks
        WHERE isArchived = 0
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
          AND (:plotId IS NULL OR plotId = :plotId)
          AND (:farmId IS NULL OR farmId = :farmId)
        ORDER BY CASE status WHEN 'PENDING' THEN 0 WHEN 'MISSED' THEN 1 WHEN 'RESCHEDULED' THEN 2 ELSE 3 END,
                 dueDateEpochDay ASC
        """,
    )
    fun observeTasks(
        cropCycleId: Long?,
        plotId: Long?,
        farmId: Long?,
    ): Flow<List<CropTaskEntity>>

    @Query(
        """
        SELECT * FROM crop_tasks
        WHERE isArchived = 0
          AND status IN ('PENDING', 'RESCHEDULED', 'MISSED')
          AND dueDateEpochDay <= :todayEpochDay
        ORDER BY dueDateEpochDay ASC
        """,
    )
    fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTaskEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM crop_tasks
        WHERE isArchived = 0
          AND reminderEnabled = 1
          AND status IN ('PENDING', 'RESCHEDULED', 'MISSED')
          AND dueDateEpochDay <= :todayEpochDay
        """,
    )
    suspend fun countDueAndOverdueTasks(todayEpochDay: Long): Int

    @Query("SELECT * FROM crop_tasks WHERE id = :taskId")
    suspend fun getTask(taskId: Long): CropTaskEntity?

    @Query("SELECT * FROM crop_tasks WHERE cropCycleId = :cropCycleId AND isArchived = 0")
    suspend fun getTasksForCropCycle(cropCycleId: Long): List<CropTaskEntity>

    @Query("SELECT * FROM crop_tasks WHERE sourceTemplateStageKey = :stageKey AND isArchived = 0 LIMIT 1")
    suspend fun getTaskByStageKeyGlobal(stageKey: String): CropTaskEntity?

    @Insert
    suspend fun insertTask(task: CropTaskEntity): Long

    @Insert
    suspend fun insertTasks(tasks: List<CropTaskEntity>)

    @Update
    suspend fun updateTask(task: CropTaskEntity)

    @Query(
        """
        UPDATE crop_tasks
        SET dueDateEpochDay = :newSowingEpochDay + offsetFromSowingDays,
            updatedAtEpochMillis = :nowMillis
        WHERE cropCycleId = :cropCycleId
          AND isArchived = 0
          AND offsetFromSowingDays IS NOT NULL
          AND status IN ('PENDING', 'RESCHEDULED')
        """,
    )
    suspend fun shiftUncompletedTemplateTasks(
        cropCycleId: Long,
        newSowingEpochDay: Long,
        nowMillis: Long,
    )

    @Query(
        """
        UPDATE crop_tasks
        SET status = :status,
            completedAtEpochMillis = :completedAtMillis,
            dueDateEpochDay = COALESCE(:newDueEpochDay, dueDateEpochDay),
            updatedAtEpochMillis = :nowMillis
        WHERE id = :taskId
        """,
    )
    suspend fun updateTaskStatus(
        taskId: Long,
        status: TaskStatus,
        completedAtMillis: Long?,
        newDueEpochDay: Long?,
        nowMillis: Long,
    )

    @Transaction
    suspend fun insertCycleWithTasks(
        cycle: CropCycleEntity,
        tasksBuilder: (cycleId: Long) -> List<CropTaskEntity>,
    ): Long {
        val id = insertCropCycle(cycle)
        val tasks = tasksBuilder(id)
        if (tasks.isNotEmpty()) insertTasks(tasks)
        return id
    }

    @Transaction
    suspend fun updateCycleAndOptionallyShiftTasks(
        cycle: CropCycleEntity,
        shiftTasks: Boolean,
        nowMillis: Long,
    ) {
        updateCropCycle(cycle)
        if (shiftTasks) {
            shiftUncompletedTemplateTasks(cycle.id, cycle.sowingDateEpochDay, nowMillis)
        }
    }

    @Transaction
    suspend fun applyTemplateToCropAtomic(
        cropCycleId: Long,
        templateId: Long,
        useTeluguTitles: Boolean,
        nowMillis: Long,
    ): Int {
        val cycle = getCropCycle(cropCycleId) ?: return -1
        val template = getTemplate(templateId) ?: return -2
        updateCropCycle(cycle.copy(templateId = templateId, updatedAtEpochMillis = nowMillis))

        val existingTasks = getTasksForCropCycle(cropCycleId)
        val newStageKeys = template.stages.map { it.stageKey }.toSet()
        existingTasks
            .filter { it.shouldArchiveForTemplate(newStageKeys) }
            .forEach { updateTask(it.copy(isArchived = true, updatedAtEpochMillis = nowMillis)) }

        val existingByKey =
            existingTasks.mapNotNull { task ->
                task.sourceTemplateStageKey?.takeIf { !task.isArchived }?.let { it to task }
            }.toMap()
        val toInsert = mutableListOf<CropTaskEntity>()
        var appliedCount = 0
        for (stage in template.stages) {
            val candidate = stage.toCropTaskEntity(cycle, useTeluguTitles, nowMillis)
            val existing = existingByKey[stage.stageKey]
            if (existing == null) {
                toInsert += candidate
                appliedCount++
            } else if (existing.isReschedulable()) {
                updateTask(existing.withAppliedStage(candidate, nowMillis))
                appliedCount++
            }
        }
        if (toInsert.isNotEmpty()) insertTasks(toInsert)
        return appliedCount
    }
}

private fun CropTaskEntity.isReschedulable(): Boolean =
    status == TaskStatus.PENDING || status == TaskStatus.RESCHEDULED

private fun CropTaskEntity.shouldArchiveForTemplate(newStageKeys: Set<String>): Boolean {
    val key = sourceTemplateStageKey ?: return false
    return !key.startsWith("auto_") && isReschedulable() && key !in newStageKeys
}

private fun CropStageTemplate.toCropTaskEntity(
    cycle: CropCycleEntity,
    useTeluguTitles: Boolean,
    nowMillis: Long,
): CropTaskEntity =
    CropTaskEntity(
        farmId = cycle.farmId,
        plotId = cycle.plotId,
        cropCycleId = cycle.id,
        title = if (useTeluguTitles) titleTe else titleEn,
        description = (if (useTeluguTitles) notesTe else notesEn).takeIf { it.isNotBlank() },
        category = category,
        dueDateEpochDay = cycle.sowingDateEpochDay + offsetDaysFromSowing,
        offsetFromSowingDays = offsetDaysFromSowing,
        sourceTemplateStageKey = stageKey,
        createdAtEpochMillis = nowMillis,
        updatedAtEpochMillis = nowMillis,
    )

private fun CropTaskEntity.withAppliedStage(
    candidate: CropTaskEntity,
    nowMillis: Long,
): CropTaskEntity =
    copy(
        title = candidate.title,
        description = candidate.description,
        category = candidate.category,
        dueDateEpochDay = candidate.dueDateEpochDay,
        offsetFromSowingDays = candidate.offsetFromSowingDays,
        updatedAtEpochMillis = nowMillis,
    )

@Dao
interface DiaryDao {
    @Query(
        """
        SELECT * FROM diary_entries
        WHERE isArchived = 0
          AND (:farmId IS NULL OR farmId = :farmId)
          AND (:plotId IS NULL OR plotId = :plotId)
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
          AND (:escaped = '' OR title LIKE '%' || :escaped || '%' ESCAPE '\'
                             OR notes LIKE '%' || :escaped || '%' ESCAPE '\')
        ORDER BY entryDateEpochMillis DESC, id DESC
        """,
    )
    fun observeEntries(
        farmId: Long?,
        plotId: Long?,
        cropCycleId: Long?,
        escaped: String,
    ): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE id = :entryId")
    suspend fun getEntry(entryId: Long): DiaryEntryEntity?

    @Insert
    suspend fun insertEntry(entry: DiaryEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntryEntity)

    @Query("UPDATE diary_entries SET isArchived = 1, updatedAtEpochMillis = :nowMillis WHERE id = :entryId")
    suspend fun archiveEntry(
        entryId: Long,
        nowMillis: Long,
    )

    @Query(
        """
        SELECT * FROM diary_entries
        WHERE isArchived = 0
          AND (title LIKE '%' || :escaped || '%' ESCAPE '\'
               OR notes LIKE '%' || :escaped || '%' ESCAPE '\')
        LIMIT :limit
        """,
    )
    suspend fun searchDiary(
        escaped: String,
        limit: Int,
    ): List<DiaryEntryEntity>

    @Query("SELECT COUNT(*) FROM diary_entries WHERE isArchived = 0")
    suspend fun countActiveDiaryEntries(): Int
}
