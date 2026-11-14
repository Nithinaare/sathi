package com.farmsaathi.core.model

/**
 * Editable starter crop stage checklists (Paddy, Cotton, Chilli, Maize, Groundnut, Red Gram).
 * These are planning checklists only — never authoritative KVK or agronomic prescriptions.
 */
object StarterCropTemplates {
    const val DISCLAIMER_TE =
        "ఇది మార్చుకోగల పంట పనుల జాబితా మాత్రమే. మీ వంగడం, నేల, వాతావరణం బట్టి రోజులు మార్చుకోండి."
    const val DISCLAIMER_EN =
        "Editable planning template — adjust timings to your variety, soil, and weather."

    fun all(): List<CropTemplate> =
        listOf(
            paddyTemplate(),
            cottonTemplate(),
            chilliTemplate(),
            maizeTemplate(),
            groundnutTemplate(),
            redGramTemplate(),
        )

    fun findByKey(templateKey: String): CropTemplate? = all().firstOrNull { it.templateKey == templateKey }

    private fun paddyTemplate() =
        template(
            key = "starter_paddy",
            nameTe = "వరి",
            nameEn = "Paddy",
            season = SeasonType.KHARIF,
            days = 125,
            unit = QuantityUnit.QUINTAL,
            stages =
            listOf(
                stage("paddy_nursery", "నారుమడి / పొలం తయారీ", "Nursery / Land Prep", 0, TaskCategory.LAND_PREP),
                stage("paddy_transplant", "నాట్లు వేయడం", "Transplanting", 21, TaskCategory.SOWING),
                stage("paddy_basal", "కలుపు & ఎరువు చూపు", "Weeding & Top-Dress Check", 40, TaskCategory.FERTILIZER),
                stage("paddy_panicle", "ఈనిక & నీటి నిల్వ", "Panicle & Water Check", 65, TaskCategory.IRRIGATION),
                stage("paddy_grain", "గింజ కట్టు & చీడల చూపు", "Grain Filling & Pest Check", 90, TaskCategory.SPRAYING),
                stage("paddy_harvest", "కోత తయారీ", "Harvest Preparation", 120, TaskCategory.HARVEST),
            ),
        )

    private fun cottonTemplate() =
        template(
            key = "starter_cotton",
            nameTe = "పత్తి",
            nameEn = "Cotton",
            season = SeasonType.KHARIF,
            days = 160,
            unit = QuantityUnit.QUINTAL,
            stages =
            listOf(
                stage("cotton_sowing", "విత్తనాలు & అడుగు ఎరువు", "Sowing & Basal", 0, TaskCategory.SOWING),
                stage("cotton_thinning", "మొక్కల పలుచచేత", "Thinning / Gap Fill", 15, TaskCategory.OTHER),
                stage("cotton_weeding", "గొర్రు & కలుపు తీత", "Inter-cultivation & Weeding", 35, TaskCategory.WEEDING),
                stage("cotton_square", "మొగ్గ దశ పరిశీలన", "Square Formation Check", 55, TaskCategory.SPRAYING),
                stage("cotton_boll", "కాయ దశ పరిశీలన", "Boll Development Check", 95, TaskCategory.FERTILIZER),
                stage("cotton_picking", "మొదటి తీత తయారీ", "First Picking Prep", 135, TaskCategory.HARVEST),
            ),
        )

    private fun chilliTemplate() =
        template(
            key = "starter_chilli",
            nameTe = "మిర్చి",
            nameEn = "Chilli",
            season = SeasonType.KHARIF,
            days = 150,
            unit = QuantityUnit.QUINTAL,
            stages =
            listOf(
                stage("chilli_transplant", "నారు నాటుట", "Nursery / Transplant", 0, TaskCategory.SOWING),
                stage("chilli_weeding", "వేరు స్థిరత్వం & కలుపు", "Root & Weeding Check", 20, TaskCategory.WEEDING),
                stage("chilli_nutrition", "పెరుగుదల ఎరువు చూపు", "Vegetative Nutrition Check", 45, TaskCategory.FERTILIZER),
                stage("chilli_flowering", "పూత & నల్లి చూపు", "Flowering & Thrips Check", 65, TaskCategory.SPRAYING),
                stage("chilli_picking", "పచ్చి / ఎండు మిర్చి కోతలు", "Green / Red Pickings", 100, TaskCategory.HARVEST),
            ),
        )

    private fun maizeTemplate() =
        template(
            key = "starter_maize",
            nameTe = "మొక్కజొన్న",
            nameEn = "Maize",
            season = SeasonType.KHARIF,
            days = 110,
            unit = QuantityUnit.QUINTAL,
            stages =
            listOf(
                stage("maize_sow", "విత్తడం", "Sowing", 0, TaskCategory.SOWING),
                stage("maize_weed", "కలుపు & కత్తెర పురుగు చూపు", "Weed & FAW Watch", 20, TaskCategory.WEEDING),
                stage("maize_topdress", "మోకాలి ఎత్తు ఎరువు", "Knee-High Top Dress", 35, TaskCategory.FERTILIZER),
                stage("maize_tassel", "పూత దశ నీటి పరిశీలన", "Tasseling Moisture Check", 60, TaskCategory.IRRIGATION),
                stage("maize_harvest", "కండె పక్వత & కోత", "Cob Maturity & Harvest", 105, TaskCategory.HARVEST),
            ),
        )

    private fun groundnutTemplate() =
        template(
            key = "starter_groundnut",
            nameTe = "వేరుశనగ",
            nameEn = "Groundnut",
            season = SeasonType.RABI,
            days = 115,
            unit = QuantityUnit.BAG,
            stages =
            listOf(
                stage("gnut_sow", "విత్తడం", "Sowing", 0, TaskCategory.SOWING),
                stage("gnut_weed", "గొర్రు & కలుపు తీత", "Inter-cultivation / Weeding", 22, TaskCategory.WEEDING),
                stage("gnut_peg", "ఊడలు & జిప్సం చూపు", "Pegging & Gypsum Check", 40, TaskCategory.FERTILIZER),
                stage("gnut_pod", "కాయ ఊరే దశ నీటి తడి", "Pod Moisture Check", 70, TaskCategory.IRRIGATION),
                stage("gnut_harvest", "కాయ పక్వత & తవ్వకం", "Pod Maturity & Harvest", 110, TaskCategory.HARVEST),
            ),
        )

    private fun redGramTemplate() =
        template(
            key = "starter_red_gram",
            nameTe = "కంది",
            nameEn = "Red Gram",
            season = SeasonType.KHARIF,
            days = 165,
            unit = QuantityUnit.QUINTAL,
            stages =
            listOf(
                stage("tur_sow", "విత్తడం", "Sowing", 0, TaskCategory.SOWING),
                stage("tur_thin", "పలుచచేత & కలుపు", "Thinning & Weeding", 25, TaskCategory.WEEDING),
                stage("tur_branch", "కొమ్మల దశ గొర్రు", "Branching Inter-cultivation", 50, TaskCategory.OTHER),
                stage("tur_pod_borer", "పూత / కాయ పురుగు చూపు", "Flower & Pod-Borer Watch", 95, TaskCategory.SPRAYING),
                stage("tur_harvest", "కాయ ఎండుట & కోత", "Pod Maturity & Harvest", 160, TaskCategory.HARVEST),
            ),
        )

    private fun template(
        key: String,
        nameTe: String,
        nameEn: String,
        season: SeasonType,
        days: Int,
        unit: QuantityUnit,
        stages: List<CropStageTemplate>,
    ) = CropTemplate(
        templateKey = key,
        cropNameTe = nameTe,
        cropNameEn = nameEn,
        defaultSeason = season,
        expectedDurationDays = days,
        defaultYieldUnit = unit,
        stages = stages,
        isStarterTemplate = true,
        isEditable = true,
        disclaimerTe = DISCLAIMER_TE,
        disclaimerEn = DISCLAIMER_EN,
    )

    private fun stage(
        key: String,
        titleTe: String,
        titleEn: String,
        offsetDays: Int,
        category: TaskCategory,
    ) = CropStageTemplate(
        stageKey = key,
        titleTe = titleTe,
        titleEn = titleEn,
        offsetDaysFromSowing = offsetDays,
        category = category,
    )
}
