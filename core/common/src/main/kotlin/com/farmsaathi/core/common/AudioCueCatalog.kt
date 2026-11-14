package com.farmsaathi.core.common

data class AudioCueScript(
    val key: String,
    val titleTe: String,
    val titleEn: String,
    val scriptTe: String,
    val scriptEn: String,
)

/**
 * Authoritative spoken audio prompt scripts for Stage 1 screens and save confirmations.
 */
object AudioCueCatalog {
    const val ONBOARDING_WELCOME = "cue_onboarding_welcome"
    const val ONBOARDING_AUDIO_TEST = "cue_onboarding_audio_test"
    const val HOME_SUMMARY = "cue_home_summary"
    const val FARMS_OVERVIEW = "cue_farms_overview"
    const val PLOTS_HELP = "cue_plots_help"
    const val CROPS_OVERVIEW = "cue_crops_overview"
    const val ACTIVITIES_HELP = "cue_activities_help"
    const val DIARY_HELP = "cue_diary_help"
    const val ACCOUNTS_SUMMARY = "cue_accounts_summary"
    const val SALES_SUMMARY = "cue_sales_summary"
    const val LABOUR_HELP = "cue_labour_help"
    const val MACHINERY_HELP = "cue_machinery_help"
    const val INVENTORY_HELP = "cue_inventory_help"
    const val IRRIGATION_HELP = "cue_irrigation_help"
    const val HARVEST_HELP = "cue_harvest_help"
    const val SOIL_HELP = "cue_soil_help"
    const val CONTACTS_HELP = "cue_contacts_help"
    const val REPORTS_SUMMARY = "cue_reports_summary"
    const val SETTINGS_BACKUP = "cue_settings_backup"
    const val SAVED_GENERIC = "cue_saved_generic"
    const val EXPENSE_FORM_HELP = "cue_expense_form_help"
    const val SALE_FORM_HELP = "cue_sale_form_help"
    const val WATERING_FORM_HELP = "cue_watering_form_help"
    const val CROP_FORM_HELP = "cue_crop_form_help"
    const val HARVEST_FORM_HELP = "cue_harvest_form_help"
    const val SOIL_FORM_HELP = "cue_soil_form_help"
    const val DIARY_FORM_HELP = "cue_diary_form_help"
    const val SPRAY_FORM_HELP = "cue_spray_form_help"
    const val TRACTOR_FORM_HELP = "cue_tractor_form_help"
    const val WORKER_FORM_HELP = "cue_worker_form_help"
    const val CONTACT_FORM_HELP = "cue_contact_form_help"
    const val LEDGER_DETAIL_HELP = "cue_ledger_detail_help"

    val all: List<AudioCueScript> =
        listOf(
            AudioCueScript(
                key = ONBOARDING_WELCOME,
                titleTe = "స్వాగతం",
                titleEn = "Welcome",
                scriptTe =
                "నమస్కారం రైతన్నా! ఫామ్ సాథీకి స్వాగతం. మీ పొలం ఖర్చులు, పంటలు, ఆదాయం అన్నీ " +
                    "ఇంటర్నెట్ లేకుండా మీ ఫోన్‌లోనే భద్రంగా రాసుకోవచ్చు. ముందు మీ భాష ఎంచుకోండి.",
                scriptEn =
                "Welcome to Farm Saathi. You can safely track your farm expenses, crops, and sales " +
                    "offline on your phone. First choose your language.",
            ),
            AudioCueScript(
                key = ONBOARDING_AUDIO_TEST,
                titleTe = "మాట & మైక్ పరీక్ష",
                titleEn = "Audio & Mic Test",
                scriptTe = "ఇప్పుడు నా మాట మీకు స్పష్టంగా వినిపిస్తుంటే, మైక్ పరీక్ష చేసి ముందుకు వెళ్ళండి.",
                scriptEn = "If you can hear this voice clearly, test your microphone and continue.",
            ),
            AudioCueScript(
                key = HOME_SUMMARY,
                titleTe = "హోమ్ వివరాలు",
                titleEn = "Home Summary",
                scriptTe =
                "నమస్కారం. ఇక్కడ పైనుండే కార్డులో ఈ సీజన్ మొత్తం ఖర్చు, ఆదాయం చూడొచ్చు. " +
                    "కింద ఉండే పెద్ద బటన్లతో కొత్త ఖర్చు, అమ్మకం, పని లేదా ఫోటో త్వరగా రాసుకోవచ్చు.",
                scriptEn =
                "Hello. At the top you can see this season's total expense and income. " +
                    "Tap the big tiles below to quickly log an expense, sale, farm task, or photo.",
            ),
            AudioCueScript(
                key = FARMS_OVERVIEW,
                titleTe = "నా పొలాలు",
                titleEn = "My Farms",
                scriptTe =
                "ఇక్కడ మీ దగ్గర ఉన్న పొలాలన్నీ ఉంటాయి. " +
                    "కొత్త పొలం కలపడానికి కింద ఉన్న ఆకుపచ్చ బటన్ నొక్కండి.",
                scriptEn =
                "Here are all your farms. Tap the green button below to add a farm, " +
                    "or tap any farm to open its plots and crops.",
            ),
            AudioCueScript(
                key = PLOTS_HELP,
                titleTe = "మడులు",
                titleEn = "Plots",
                scriptTe =
                "ఒక్కో పొలంలో ఉండే మడులు లేదా భాగాలను ఇక్కడ రాసుకోవచ్చు. " +
                    "ఎకరాలు, గుంటల్లో విస్తీర్ణం దాచుకోవచ్చు.",
                scriptEn =
                "Record separate plots inside your farm along with area in acres and guntas, " +
                    "soil type, and water source.",
            ),
            AudioCueScript(
                key = CROPS_OVERVIEW,
                titleTe = "పంటలు",
                titleEn = "Crops",
                scriptTe =
                "మీరు వేసిన పంటలు, నాటిన తేదీలు ఇక్కడ ఉంటాయి. " +
                    "పంట మీద నొక్కితే ఆ పంట ఖర్చులు, పనులు, అమ్మకాలు కనిపిస్తాయి.",
                scriptEn =
                "View active and past crops here. Tap a crop to see all expenses, tasks, " +
                    "and sales linked to it.",
            ),
            AudioCueScript(
                key = ACTIVITIES_HELP,
                titleTe = "పొలం పనులు",
                titleEn = "Farm Tasks",
                scriptTe =
                "దున్నుడు, కలుపు, మందు కొట్టడం లాంటి పనులను ఇక్కడ రాసుకోవచ్చు. " +
                    "పని పూర్తయ్యాక టిక్ నొక్కితే చాలు.",
                scriptEn =
                "Track ploughing, weeding, spraying, and upcoming tasks here. " +
                    "Tap mark done once completed.",
            ),
            AudioCueScript(
                key = DIARY_HELP,
                titleTe = "ఫోటో & మాట డైరీ",
                titleEn = "Photo & Voice Diary",
                scriptTe =
                "రాయడానికి సమయం లేకపోతే మైక్ బటన్ నొక్కి మీ మాటలోనే పంట స్థితి రికార్డ్ చేయండి, " +
                    "లేదా పొలం ఫోటో తీసి దాచుకోండి.",
                scriptEn = "Tap the mic to record a voice note or take a field photo to save your crop diary offline.",
            ),
            AudioCueScript(
                key = ACCOUNTS_SUMMARY,
                titleTe = "ఖాతాలు & ఖర్చులు",
                titleEn = "Accounts & Expenses",
                scriptTe =
                "ఇక్కడ మీ పొలం డబ్బుల లెక్కలన్నీ ఉంటాయి. పైన మొత్తం ఖర్చు, ఆదాయం చూసి కింద " +
                    "ఖర్చులు, అమ్మకాలు, కూలీలు, ట్రాక్టర్ అద్దె చూడొచ్చు.",
                scriptEn =
                "Here are your farm accounts. View total expense and income at the top, " +
                    "or open expenses, sales, labour, and machinery.",
            ),
            AudioCueScript(
                key = SALES_SUMMARY,
                titleTe = "అమ్మకాలు & ఆదాయం",
                titleEn = "Crop Sales",
                scriptTe = "పంట అమ్మిన వివరాలు ఇక్కడ రాసుకోవచ్చు. డబ్బులు ఇంకా రావలసి ఉంటే బాకీ అని పెట్టుకోవచ్చు.",
                scriptEn = "Record crop sales and track received cash versus pending buyer balances here.",
            ),
            AudioCueScript(
                key = LABOUR_HELP,
                titleTe = "కూలీల లెక్క",
                titleEn = "Labour Wages",
                scriptTe =
                "రోజూ కూలీలకు పూర్తి సూర్యుడు పూర్తి రోజు, సగం సూర్యుడు సగం రోజు, లేదా గైరు గుర్తు పెట్టండి. " +
                    "నిన్నటి కూలీలనే ఒక్క నొక్కుతో మళ్ళీ వేయవచ్చు; అడ్వాన్స్, బాకీ ఆటోమేటిక్‌గా లెక్క వస్తాయి.",
                scriptEn =
                "Mark full-sun for full day, half-sun for half day, or cross for absent, " +
                    "or repeat yesterday's roll call in one tap. Advances and pending pay update automatically.",
            ),
            AudioCueScript(
                key = MACHINERY_HELP,
                titleTe = "యంత్రాల అద్దె",
                titleEn = "Machinery Rental",
                scriptTe =
                "ట్రాక్టర్ లేదా హార్వెస్టర్ పని మొదలైనప్పుడు స్టార్ట్ నొక్కండి, పూర్తయ్యాక స్టాప్ నొక్కండి. " +
                    "ఫోన్ ఆఫ్ అయినా సమయం లెక్క సరిగ్గా ఉంటుంది.",
                scriptEn =
                "Tap Start when tractor work begins and Stop when done. " +
                    "Elapsed time and cost survive app close or phone restart.",
            ),
            AudioCueScript(
                key = INVENTORY_HELP,
                titleTe = "విత్తనాలు & మందుల నిల్వ",
                titleEn = "Input Inventory",
                scriptTe =
                "ఇంట్లో ఉన్న విత్తనాలు, ఎరువులు, మందుల నిల్వ ఇక్కడ చూడొచ్చు. " +
                    "కొన్నప్పుడు నిల్వ పెరుగుతుంది, ఖర్చు కూడా పద్దులోకి వస్తుంది.",
                scriptEn =
                "Track seeds, fertilizers, and pesticides in stock. " +
                    "Purchases update both stock and expense automatically.",
            ),
            AudioCueScript(
                key = IRRIGATION_HELP,
                titleTe = "నీటి తడులు",
                titleEn = "Irrigation",
                scriptTe =
                "నీటి వనరు అంటే బోరు లేదా కాలువ వేరుగా, తడి పెట్టే పద్ధతి అంటే పారబోత లేదా డ్రిప్ వేరుగా ఎంచుకోండి. " +
                    "మళ్ళీ తడి తేదీ ఇస్తే రిమైండర్ కూడా వస్తుంది.",
                scriptEn =
                "Choose water source like borewell or canal separately from method like flood or drip. " +
                    "Set a next watering date to create a reminder automatically.",
            ),
            AudioCueScript(
                key = HARVEST_HELP,
                titleTe = "కోత & నిల్వ",
                titleEn = "Harvest & Storage",
                scriptTe = "కోత తర్వాత ఎన్ని బస్తాలు లేదా క్వింటాళ్ళు వచ్చాయో, ఎక్కడ నిల్వ చేశారో ఇక్కడ రాసుకోవచ్చు.",
                scriptEn = "Record harvested yield and track how much produce remains in home or godown storage.",
            ),
            AudioCueScript(
                key = SOIL_HELP,
                titleTe = "మట్టి పరీక్షలు",
                titleEn = "Soil Tests",
                scriptTe = "మీ మడి మట్టి పరీక్ష కార్డు ఫోటో తీసి ఇందులో దాచుకోండి. pH, నత్రజని విలువలు కూడా రాసుకోవచ్చు.",
                scriptEn = "Save photos of your soil health card and record lab values manually here.",
            ),
            AudioCueScript(
                key = CONTACTS_HELP,
                titleTe = "పరిచయాలు",
                titleEn = "Contacts",
                scriptTe =
                "మందుల షాప్ వాళ్ళు, వ్యాపారులు, ట్రాక్టర్ యజమానుల నంబర్లు ఇక్కడ దాచుకుని " +
                    "ఒక్క నొక్కుతో ఫోన్ చేయొచ్చు.",
                scriptEn = "Save dealers, buyers, tractor owners, and officers here for one-tap calling.",
            ),
            AudioCueScript(
                key = REPORTS_SUMMARY,
                titleTe = "లాభ-నష్టాల నివేదిక",
                titleEn = "Reports",
                scriptTe = "ఇక్కడ మీ పంటల వారీ ఖర్చు, ఆదాయం, లాభం లేదా నష్టం పూర్తిగా చూడొచ్చు.",
                scriptEn = "Review crop-wise recorded expenses, income, and known balance based on your entries.",
            ),
            AudioCueScript(
                key = SETTINGS_BACKUP,
                titleTe = "సెట్టింగ్స్ & బ్యాకప్",
                titleEn = "Settings & Backup",
                scriptTe = "ఇక్కడ భాషను మార్చుకోవచ్చు. మీ ఫోన్ మారినా డేటా పోకుండా బ్యాకప్ ఫైల్ తీసి దాచుకోవచ్చు.",
                scriptEn = "Switch language, manage spoken help, and export or restore full offline zip backups.",
            ),
            AudioCueScript(
                key = SAVED_GENERIC,
                titleTe = "సేవ్ నిర్ధారణ",
                titleEn = "Saved Confirmation",
                scriptTe = "వివరాలు మీ ఫోన్‌లో భద్రంగా సేవ్ అయ్యాయి.",
                scriptEn = "Details saved safely on your phone.",
            ),
            AudioCueScript(
                key = EXPENSE_FORM_HELP,
                titleTe = "ఖర్చు నమోదు",
                titleEn = "Log Expense",
                scriptTe = "ఖర్చు వర్గం, రూపాయలు, చెల్లింపు రకం ఎంచుకోండి. కావాలంటే బిల్ ఫోటో కూడా జత చేసి సేవ్ నొక్కండి.",
                scriptEn = "Choose expense category, amount, and payment mode. Attach a bill photo if needed and tap Save.",
            ),
            AudioCueScript(
                key = SALE_FORM_HELP,
                titleTe = "అమ్మకం నమోదు",
                titleEn = "Log Crop Sale",
                scriptTe = "కొనుగోలుదారు, బరువు, ధర నమోదు చేయండి. వచ్చిన నగదు లేదా రావలసిన బాకీ ఇక్కడే రాసుకోవచ్చు.",
                scriptEn = "Enter buyer, quantity, and rate. Record cash received now and any pending buyer balance.",
            ),
            AudioCueScript(
                key = WATERING_FORM_HELP,
                titleTe = "నీటి తడి నమోదు",
                titleEn = "Log Watering",
                scriptTe = "మడి, నీటి వనరు, తడి పద్ధతి ఎంచుకోండి. తదుపరి నీటి తడి తేదీ పెడితే క్యాలెండర్‌లో గుర్తు చేస్తుంది.",
                scriptEn = "Select plot, water source, and irrigation method. Add a next watering date for an automatic reminder.",
            ),
            AudioCueScript(
                key = CROP_FORM_HELP,
                titleTe = "పంట నమోదు",
                titleEn = "Add Crop",
                scriptTe = "పొలం మడి, పంట పేరు, నాటిన తేదీ ఎంచుకోండి. టెంప్లేట్ ఎంచుకుంటే పనుల పట్టిక తయారవుతుంది.",
                scriptEn = "Pick plot, crop name, and sowing date. Selecting a template generates stage tasks automatically.",
            ),
            AudioCueScript(
                key = HARVEST_FORM_HELP,
                titleTe = "కోత నమోదు",
                titleEn = "Log Harvest",
                scriptTe = "పంట దిగుబడి బస్తాలు లేదా క్వింటాళ్ళలో రాయండి. కాటా రసీదు ఫోటో తీసి జత చేయవచ్చు.",
                scriptEn = "Record harvested yield in bags or quintals and optionally attach a weigh-slip photo.",
            ),
            AudioCueScript(
                key = SOIL_FORM_HELP,
                titleTe = "మట్టి పరీక్ష నమోదు",
                titleEn = "Soil Test Entry",
                scriptTe = "మట్టి పరీక్ష కార్డు ఫోటో తీసి, ఉన్నట్లయితే pH మరియు పోషకాల విలువలు ఇక్కడ రాసుకోండి.",
                scriptEn = "Snap your soil health card photo and enter pH or nutrient values if available.",
            ),
            AudioCueScript(
                key = DIARY_FORM_HELP,
                titleTe = "డైరీ నమోదు",
                titleEn = "Diary Entry",
                scriptTe = "పంట స్థితి ఫోటో తీసి, మాట లేదా చిన్న గమనికతో డైరీలో భద్రంగా దాచుకోండి.",
                scriptEn = "Capture a field photo and save a voice note or short observation to your crop diary.",
            ),
            AudioCueScript(
                key = SPRAY_FORM_HELP,
                titleTe = "మందు / ఎరువు వాడకం",
                titleEn = "Spray & Input Usage",
                scriptTe = "వాడిన మందు లేదా ఎరువు, పరిమాణం, మడి ఎంచుకోండి. మందు డబ్బా ఫోటో కూడా జత చేయవచ్చు.",
                scriptEn = "Select the input product, quantity used, and plot. You can also attach a bottle or bag photo.",
            ),
            AudioCueScript(
                key = TRACTOR_FORM_HELP,
                titleTe = "యంత్రం పని నమోదు",
                titleEn = "Tractor & Machinery",
                scriptTe = "యంత్రం పేరు, పని రకం, గంటకు లేదా ఎకరాకు రేటు ఇచ్చి స్టార్ట్ లేదా మాన్యువల్ లెక్క సేవ్ చేయండి.",
                scriptEn = "Enter machine, operation type, and hourly or per-acre rate to start a timer or log past work.",
            ),
            AudioCueScript(
                key = WORKER_FORM_HELP,
                titleTe = "కూలీ వివరాలు",
                titleEn = "Worker Profile",
                scriptTe = "కూలీ పేరు, రోజువారీ కూలి మరియు సగం రోజు కూలి ఇక్కడ రాయండి. ఫోటో లేదా మాట పేరు కూడా జత చేయవచ్చు.",
                scriptEn = "Enter worker name, full-day wage, and optional half-day rate. Add a photo or voice name if helpful.",
            ),
            AudioCueScript(
                key = CONTACT_FORM_HELP,
                titleTe = "పరిచయం నమోదు",
                titleEn = "Save Contact",
                scriptTe = "వ్యాపారి, షాప్ లేదా ట్రాక్టర్ యజమాని పేరు, ఫోన్ నంబర్ రాసుకోండి.",
                scriptEn = "Save the name, role, and phone number of a buyer, dealer, or tractor owner.",
            ),
            AudioCueScript(
                key = LEDGER_DETAIL_HELP,
                titleTe = "పద్దు వివరాలు",
                titleEn = "Ledger Entry Detail",
                scriptTe = "ఈ పద్దు మొత్తం, చెల్లించిన మొత్తం మరియు బాకీ ఇక్కడ కనిపిస్తాయి. బాకీ చెల్లింపులు ఇక్కడే నమోదు చేయొచ్చు.",
                scriptEn = "View total, settled amount, and remaining balance here. Record follow-up payments or void mistakes.",
            ),
        )

    private val byKey = all.associateBy { it.key }

    fun find(key: String): AudioCueScript? = byKey[key]
}
