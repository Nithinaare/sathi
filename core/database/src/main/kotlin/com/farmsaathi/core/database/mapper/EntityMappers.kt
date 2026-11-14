package com.farmsaathi.core.database.mapper

import com.farmsaathi.core.database.entity.ContactEntity
import com.farmsaathi.core.database.entity.CropCycleEntity
import com.farmsaathi.core.database.entity.CropSaleEntity
import com.farmsaathi.core.database.entity.CropTaskEntity
import com.farmsaathi.core.database.entity.CropTemplateEntity
import com.farmsaathi.core.database.entity.DiaryEntryEntity
import com.farmsaathi.core.database.entity.ExpenseDetailEntity
import com.farmsaathi.core.database.entity.FarmEntity
import com.farmsaathi.core.database.entity.HarvestRecordEntity
import com.farmsaathi.core.database.entity.InputProductEntity
import com.farmsaathi.core.database.entity.InputPurchaseEntity
import com.farmsaathi.core.database.entity.IrrigationLogEntity
import com.farmsaathi.core.database.entity.LedgerEntryEntity
import com.farmsaathi.core.database.entity.MachinerySessionEntity
import com.farmsaathi.core.database.entity.PlotEntity
import com.farmsaathi.core.database.entity.SoilTestRecordEntity
import com.farmsaathi.core.database.entity.StockMovementEntity
import com.farmsaathi.core.database.entity.StorageRecordEntity
import com.farmsaathi.core.database.entity.WorkerAttendanceEntity
import com.farmsaathi.core.database.entity.WorkerEntity
import com.farmsaathi.core.database.entity.WorkerPaymentEntity
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropSale
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.DiaryEntry
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.HarvestRecord
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.InputPurchase
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.SoilTestRecord
import com.farmsaathi.core.model.StockMovement
import com.farmsaathi.core.model.StorageRecord
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerPayment

fun FarmEntity.toDomain() =
    Farm(
        id = id,
        name = name,
        villageOrLocation = villageOrLocation,
        surveyNumber = surveyNumber,
        totalAreaMicroAcres = totalAreaMicroAcres,
        preferredAreaUnit = preferredAreaUnit,
        soilType = soilType,
        waterSource = waterSource,
        latitude = latitude,
        longitude = longitude,
        photoPath = photoPath,
        voiceNamePath = voiceNamePath,
        notes = notes,
        isPrimary = isPrimary,
        isArchived = isArchived,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun Farm.toEntity(nowMillis: Long) =
    FarmEntity(
        id = id,
        name = name.trim(),
        villageOrLocation = villageOrLocation?.trim()?.takeIf { it.isNotEmpty() },
        surveyNumber = surveyNumber?.trim()?.takeIf { it.isNotEmpty() },
        totalAreaMicroAcres = totalAreaMicroAcres,
        preferredAreaUnit = preferredAreaUnit,
        soilType = soilType?.trim()?.takeIf { it.isNotEmpty() },
        waterSource = waterSource,
        latitude = latitude,
        longitude = longitude,
        photoPath = photoPath,
        voiceNamePath = voiceNamePath,
        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
        isPrimary = isPrimary,
        isArchived = isArchived,
        createdAtEpochMillis = if (id == 0L) nowMillis else createdAtEpochMillis,
        updatedAtEpochMillis = nowMillis,
    )

fun PlotEntity.toDomain() =
    Plot(
        id = id,
        farmId = farmId,
        name = name,
        areaMicroAcres = areaMicroAcres,
        preferredAreaUnit = preferredAreaUnit,
        soilType = soilType,
        irrigationSource = irrigationSource,
        latitude = latitude,
        longitude = longitude,
        photoPaths = photoPaths,
        notes = notes,
        isArchived = isArchived,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun Plot.toEntity(nowMillis: Long) =
    PlotEntity(
        id = id,
        farmId = farmId,
        name = name.trim(),
        areaMicroAcres = areaMicroAcres,
        preferredAreaUnit = preferredAreaUnit,
        soilType = soilType?.trim()?.takeIf { it.isNotEmpty() },
        irrigationSource = irrigationSource,
        latitude = latitude,
        longitude = longitude,
        photoPaths = photoPaths,
        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
        isArchived = isArchived,
        createdAtEpochMillis = if (id == 0L) nowMillis else createdAtEpochMillis,
        updatedAtEpochMillis = nowMillis,
    )

fun CropTemplateEntity.toDomain() =
    CropTemplate(
        id = id,
        templateKey = templateKey,
        cropNameTe = cropNameTe,
        cropNameEn = cropNameEn,
        defaultSeason = defaultSeason,
        expectedDurationDays = expectedDurationDays,
        defaultYieldUnit = defaultYieldUnit,
        stages = stages,
        isStarterTemplate = isStarterTemplate,
        isEditable = isEditable,
        disclaimerTe = disclaimerTe,
        disclaimerEn = disclaimerEn,
        isArchived = isArchived,
    )

fun CropTemplate.toEntity(nowMillis: Long) =
    CropTemplateEntity(
        id = id,
        templateKey = templateKey,
        cropNameTe = cropNameTe.trim(),
        cropNameEn = cropNameEn.trim(),
        defaultSeason = defaultSeason,
        expectedDurationDays = expectedDurationDays,
        defaultYieldUnit = defaultYieldUnit,
        stages = stages,
        isStarterTemplate = isStarterTemplate,
        isEditable = isEditable,
        disclaimerTe = disclaimerTe,
        disclaimerEn = disclaimerEn,
        isArchived = isArchived,
        createdAtEpochMillis = nowMillis,
        updatedAtEpochMillis = nowMillis,
    )

fun CropCycleEntity.toDomain() =
    CropCycle(
        id = id,
        farmId = farmId,
        plotId = plotId,
        templateId = templateId,
        cropName = cropName,
        variety = variety,
        season = season,
        cropYear = cropYear,
        plantedAreaMicroAcres = plantedAreaMicroAcres,
        sowingDateEpochDay = sowingDateEpochDay,
        expectedHarvestEpochDay = expectedHarvestEpochDay,
        actualHarvestEpochDay = actualHarvestEpochDay,
        status = status,
        isIntercrop = isIntercrop,
        voiceNamePath = voiceNamePath,
        notes = notes,
        isArchived = isArchived,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun CropCycle.toEntity(nowMillis: Long) =
    CropCycleEntity(
        id = id,
        farmId = farmId,
        plotId = plotId,
        templateId = templateId,
        cropName = cropName.trim(),
        variety = variety?.trim()?.takeIf { it.isNotEmpty() },
        season = season,
        cropYear = cropYear,
        plantedAreaMicroAcres = plantedAreaMicroAcres,
        sowingDateEpochDay = sowingDateEpochDay,
        expectedHarvestEpochDay = expectedHarvestEpochDay,
        actualHarvestEpochDay = actualHarvestEpochDay,
        status = status,
        isIntercrop = isIntercrop,
        voiceNamePath = voiceNamePath,
        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
        isArchived = isArchived,
        createdAtEpochMillis = if (id == 0L) nowMillis else createdAtEpochMillis,
        updatedAtEpochMillis = nowMillis,
    )

fun CropTaskEntity.toDomain() =
    CropTask(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        title = title,
        description = description,
        category = category,
        dueDateEpochDay = dueDateEpochDay,
        offsetFromSowingDays = offsetFromSowingDays,
        sourceTemplateStageKey = sourceTemplateStageKey,
        priority = priority,
        status = status,
        completedAtEpochMillis = completedAtEpochMillis,
        reminderEnabled = reminderEnabled,
        isArchived = isArchived,
    )

fun CropTask.toEntity(nowMillis: Long) =
    CropTaskEntity(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        title = title.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        category = category,
        dueDateEpochDay = dueDateEpochDay,
        offsetFromSowingDays = offsetFromSowingDays,
        sourceTemplateStageKey = sourceTemplateStageKey,
        priority = priority,
        status = status,
        completedAtEpochMillis = completedAtEpochMillis,
        reminderEnabled = reminderEnabled,
        isArchived = isArchived,
        createdAtEpochMillis = nowMillis,
        updatedAtEpochMillis = nowMillis,
    )

fun DiaryEntryEntity.toDomain() =
    DiaryEntry(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        entryDateEpochMillis = entryDateEpochMillis,
        category = category,
        title = title,
        notes = notes,
        photoPaths = photoPaths,
        voiceNotePath = voiceNotePath,
        voiceNoteDurationMillis = voiceNoteDurationMillis,
        isArchived = isArchived,
    )

fun DiaryEntry.toEntity(nowMillis: Long) =
    DiaryEntryEntity(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        entryDateEpochMillis = entryDateEpochMillis,
        category = category,
        title = title.trim(),
        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
        photoPaths = photoPaths,
        voiceNotePath = voiceNotePath,
        voiceNoteDurationMillis = voiceNoteDurationMillis,
        isArchived = isArchived,
        createdAtEpochMillis = nowMillis,
        updatedAtEpochMillis = nowMillis,
    )

fun LedgerEntryEntity.toDomain() =
    LedgerEntry(
        id = id,
        entryType = entryType,
        sourceType = sourceType,
        sourceId = sourceId,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        contactId = contactId,
        category = category,
        title = title,
        notes = notes,
        totalAmount = MoneyPaise(totalAmountPaise),
        settledAmount = MoneyPaise(settledAmountPaise),
        paymentStatus = paymentStatus,
        paymentMode = paymentMode,
        transactionDateEpochMillis = transactionDateEpochMillis,
        dueDateEpochMillis = dueDateEpochMillis,
        receiptPhotoPath = receiptPhotoPath,
        voiceNotePath = voiceNotePath,
        isVoided = isVoided,
        voidReason = voidReason,
    )

fun ExpenseDetailEntity.toDomain() =
    ExpenseDetail(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        contactId = contactId,
        category = category,
        description = description,
        vendorNameSnapshot = vendorNameSnapshot,
        billNumber = billNumber,
        quantityMilliUnits = quantityMilliUnits,
        quantityUnit = quantityUnit,
        unitPrice = unitPricePaise?.let(::MoneyPaise),
        totalAmount = MoneyPaise(totalAmountPaise),
        paidAmount = MoneyPaise(paidAmountPaise),
        paymentMode = paymentMode,
        expenseDateEpochMillis = expenseDateEpochMillis,
        receiptPhotoPath = receiptPhotoPath,
        voiceNotePath = voiceNotePath,
        voiceNoteDurationMillis = voiceNoteDurationMillis,
        notes = notes,
        isVoided = isVoided,
    )

fun CropSaleEntity.toDomain() =
    CropSale(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        harvestRecordId = harvestRecordId,
        storageRecordId = storageRecordId,
        buyerContactId = buyerContactId,
        buyerNameSnapshot = buyerNameSnapshot,
        mandiOrLocation = mandiOrLocation,
        quantityMilliUnits = quantityMilliUnits,
        quantityUnit = quantityUnit,
        ratePerUnit = MoneyPaise(ratePerUnitPaise),
        grossAmount = MoneyPaise(grossAmountPaise),
        deductions = MoneyPaise(deductionsPaise),
        netTotalAmount = MoneyPaise(netTotalAmountPaise),
        amountReceived = MoneyPaise(amountReceivedPaise),
        paymentStatus = paymentStatus,
        paymentMode = paymentMode,
        saleDateEpochMillis = saleDateEpochMillis,
        dueDateEpochMillis = dueDateEpochMillis,
        receiptPhotoPath = receiptPhotoPath,
        notes = notes,
        isVoided = isVoided,
    )

fun WorkerEntity.toDomain() =
    Worker(
        id = id,
        farmId = farmId,
        name = name,
        phone = phone,
        skillRole = skillRole,
        photoPath = photoPath,
        voiceNamePath = voiceNamePath,
        daysWorkedTenths = daysWorkedTenths,
        defaultDailyWage = MoneyPaise(defaultDailyWagePaise),
        defaultHalfDayPercent = defaultHalfDayPercent,
        notes = notes,
        isArchived = isArchived,
        halfDayRatePaise = halfDayRatePaise,
    )

fun WorkerAttendanceEntity.toDomain() =
    WorkerAttendance(
        id = id,
        workerId = workerId,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        workDateEpochDay = workDateEpochDay,
        status = status,
        baseDailyWage = MoneyPaise(baseDailyWagePaise),
        halfDayPercent = halfDayPercent,
        pieceRateUnit = pieceRateUnit,
        pieceQuantityMilliUnits = pieceQuantityMilliUnits,
        pieceUnitRate = pieceUnitRatePaise?.let(::MoneyPaise),
        wageAccrued = MoneyPaise(wageAccruedPaise),
        notes = notes,
        isVoided = isVoided,
    )

fun WorkerPaymentEntity.toDomain() =
    WorkerPayment(
        id = id,
        workerId = workerId,
        farmId = farmId,
        cropCycleId = cropCycleId,
        paymentType = paymentType,
        amount = MoneyPaise(amountPaise),
        paymentDateEpochMillis = paymentDateEpochMillis,
        paymentMode = paymentMode,
        notes = notes,
        isVoided = isVoided,
    )

fun MachinerySessionEntity.toDomain() =
    MachinerySession(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        providerContactId = providerContactId,
        machineName = machineName,
        operatorName = operatorName,
        operationType = operationType,
        equipmentKey = equipmentKey,
        usageType = usageType,
        billingMode = billingMode,
        entryMode = entryMode,
        status = status,
        startEpochMillis = startEpochMillis,
        startElapsedRealtimeMillis = startElapsedRealtimeMillis,
        startBootCount = startBootCount,
        endEpochMillis = endEpochMillis,
        endElapsedRealtimeMillis = endElapsedRealtimeMillis,
        elapsedMinutes = elapsedMinutes,
        hourlyRate = hourlyRatePaise?.let(::MoneyPaise),
        perAcreRate = perAcreRatePaise?.let(::MoneyPaise),
        areaCoveredMicroAcres = areaCoveredMicroAcres,
        computedAmount = MoneyPaise(computedAmountPaise),
        paidAmount = MoneyPaise(paidAmountPaise),
        paymentStatus = paymentStatus,
        notes = notes,
        isVoided = isVoided,
    )

fun InputProductEntity.toDomain() =
    InputProduct(
        id = id,
        farmId = farmId,
        name = name,
        category = category,
        brand = brand,
        unit = unit,
        bagWeightMilliKg = bagWeightMilliKg,
        currentStockMilliUnits = currentStockMilliUnits,
        lowStockThresholdMilliUnits = lowStockThresholdMilliUnits,
        defaultUnitPrice = defaultUnitPricePaise?.let(::MoneyPaise),
        notes = notes,
        isArchived = isArchived,
    )

fun InputPurchaseEntity.toDomain() =
    InputPurchase(
        id = id,
        productId = productId,
        farmId = farmId,
        cropCycleId = cropCycleId,
        vendorContactId = vendorContactId,
        vendorNameSnapshot = vendorNameSnapshot,
        billNumber = billNumber,
        quantityMilliUnits = quantityMilliUnits,
        unit = unit,
        unitPrice = MoneyPaise(unitPricePaise),
        totalAmount = MoneyPaise(totalAmountPaise),
        paidAmount = MoneyPaise(paidAmountPaise),
        paymentStatus = paymentStatus,
        paymentMode = paymentMode,
        purchaseDateEpochMillis = purchaseDateEpochMillis,
        expiryDateEpochDay = expiryDateEpochDay,
        dueDateEpochMillis = dueDateEpochMillis,
        receiptPhotoPath = receiptPhotoPath,
        notes = notes,
        isVoided = isVoided,
    )

fun StockMovementEntity.toDomain() =
    StockMovement(
        id = id,
        productId = productId,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        purchaseId = purchaseId,
        movementType = movementType,
        reason = reason,
        signedDeltaMilliUnits = signedDeltaMilliUnits,
        balanceAfterMilliUnits = balanceAfterMilliUnits,
        unitCostSnapshot = unitCostPaiseSnapshot?.let(::MoneyPaise),
        movementDateEpochMillis = movementDateEpochMillis,
        notes = notes,
        photoPath = photoPath,
    )

fun IrrigationLogEntity.toDomain() =
    IrrigationLog(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        irrigationDateEpochMillis = irrigationDateEpochMillis,
        durationMinutes = durationMinutes,
        source = source,
        method = method,
        nextWateringEpochDay = nextWateringEpochDay,
        motorHoursOrFuelNote = motorHoursOrFuelNote,
        photoPath = photoPath,
        notes = notes,
        isArchived = isArchived,
    )

fun HarvestRecordEntity.toDomain() =
    HarvestRecord(
        id = id,
        farmId = farmId,
        plotId = plotId,
        cropCycleId = cropCycleId,
        harvestDateEpochDay = harvestDateEpochDay,
        quantityMilliUnits = quantityMilliUnits,
        quantityUnit = quantityUnit,
        bagCount = bagCount,
        moisturePercentTenths = moisturePercentTenths,
        qualityGrade = qualityGrade,
        notes = notes,
        isArchived = isArchived,
        photoPath = photoPath,
    )

fun StorageRecordEntity.toDomain() =
    StorageRecord(
        id = id,
        farmId = farmId,
        cropCycleId = cropCycleId,
        harvestRecordId = harvestRecordId,
        storageLocation = storageLocation,
        quantityMilliUnits = quantityMilliUnits,
        remainingQuantityMilliUnits = remainingQuantityMilliUnits,
        quantityUnit = quantityUnit,
        storedDateEpochDay = storedDateEpochDay,
        bagCount = bagCount,
        status = status,
        notes = notes,
        isArchived = isArchived,
    )

fun SoilTestRecordEntity.toDomain() =
    SoilTestRecord(
        id = id,
        farmId = farmId,
        plotId = plotId,
        testDateEpochDay = testDateEpochDay,
        labName = labName,
        sampleNumber = sampleNumber,
        phTenths = phTenths,
        ecMilliSiemensPerCmHundredths = ecMilliSiemensPerCmHundredths,
        organicCarbonPercentHundredths = organicCarbonPercentHundredths,
        nitrogenKgPerHaTenths = nitrogenKgPerHaTenths,
        phosphorusKgPerHaTenths = phosphorusKgPerHaTenths,
        potassiumKgPerHaTenths = potassiumKgPerHaTenths,
        overallRating = overallRating,
        reportPhotoPath = reportPhotoPath,
        recommendationsNote = recommendationsNote,
        isArchived = isArchived,
    )

fun ContactEntity.toDomain() =
    Contact(
        id = id,
        name = name,
        phone = phone,
        contactType = contactType,
        villageOrMandi = villageOrMandi,
        upiId = upiId,
        voiceNamePath = voiceNamePath,
        notes = notes,
        isArchived = isArchived,
    )
