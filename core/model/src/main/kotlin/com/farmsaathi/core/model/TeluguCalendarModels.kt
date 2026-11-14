package com.farmsaathi.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TeluguMonth(
    val id: Int,
    val teluguName: String,
    val englishName: String,
    val season: String,
) {
    CHAITRAM(1, "చైత్రం", "Chaitram", "వేసవి"),
    VAISHAKHAM(2, "వైశాఖం", "Vaishakham", "వేసవి"),
    JYESHTHAM(3, "జ్యేష్ఠం", "Jyeshtham", "వానాకాలం ప్రారంభం"),
    ASHADHAM(4, "ఆషాఢం", "Ashadham", "వానాకాలం"),
    SHRAVANAM(5, "శ్రావణం", "Shravanam", "వానాకాలం"),
    BHADRAPADAM(6, "భాద్రపదం", "Bhadrapadam", "వానాకాలం"),
    ASHWAYUJAM(7, "ఆశ్వయుజం", "Ashwayujam", "శరదృతువు"),
    KARTHIKAM(8, "కార్తీకం", "Karthikam", "యాసంగి ప్రారంభం"),
    MARGASHIRAM(9, "మార్గశిరం", "Margashiram", "యాసంగి (శీతాకాలం)"),
    PUSHYAM(10, "పుష్యం", "Pushyam", "యాసంగి"),
    MAGHAM(11, "మాఘం", "Magham", "శిశిరం"),
    PHALGUNAM(12, "ఫాల్గుణం", "Phalgunam", "వసంతం"),
    ;

    companion object {
        fun fromId(id: Int): TeluguMonth = entries.firstOrNull { it.id == id } ?: CHAITRAM
    }
}

@Serializable
enum class Paksham(
    val teluguName: String,
    val englishName: String,
) {
    SHUKLA("శుక్ల పక్షం", "Shukla"),
    KRISHNA("కృష్ణ పక్షం", "Krishna"),
}

@Serializable
enum class TeluguTithi(
    val id: Int,
    val teluguName: String,
    val englishName: String,
) {
    PADYAMI(1, "పాడ్యమి", "Padyami"),
    VIDIYA(2, "విదియ", "Vidiya"),
    TADIYA(3, "తదియ", "Tadiya"),
    CHAVITHI(4, "చవితి", "Chavithi"),
    PANCHAMI(5, "పంచమి", "Panchami"),
    SHASHTHI(6, "షష్ఠి", "Shashthi"),
    SAPTHAMI(7, "సప్తమి", "Sapthami"),
    ASHTAMI(8, "అష్టమి", "Ashtami"),
    NAVAMI(9, "నవమి", "Navami"),
    DASHAMI(10, "దశమి", "Dashami"),
    EKADASHI(11, "ఏకాదశి", "Ekadashi"),
    DWADASHI(12, "ద్వాదశి", "Dwadashi"),
    TRAYODASHI(13, "త్రయోదశి", "Trayodashi"),
    CHATURDASHI(14, "చతుర్దశి", "Chaturdashi"),
    POURNAMI(15, "పౌర్ణమి", "Pournami"),
    AMAVASYA(30, "అమావాస్య", "Amavasya"),
    ;

    companion object {
        fun fromId(id: Int): TeluguTithi = entries.firstOrNull { it.id == id } ?: PADYAMI
    }
}

@Serializable
data class TeluguKarte(
    val id: Int,
    val teluguName: String,
    val englishName: String,
    val approxDateSpan: String,
    val season: String,
    val proverb: String,
    val agriculturalSignificance: String,
    val recommendedCrops: List<String>,
)

object TeluguKarteluCatalog {
    val ALL: List<TeluguKarte> = listOf(
        TeluguKarte(
            id = 1,
            teluguName = "అశ్విని",
            englishName = "Ashwini",
            approxDateSpan = "ఏప్రిల్ 14 – ఏప్రిల్ 26",
            season = "వేసవి",
            proverb = "అశ్విని కురిస్తే అన్నీ కురుస్తాయి",
            agriculturalSignificance = "వేసవి లోతైన దుక్కులకు అనువైన సమయం. నేలలోని క్రిమికీటకాలు నశిస్తాయి.",
            recommendedCrops = listOf("వేసవి దుక్కులు", "పచ్చిరొట్ట ఎరువులు"),
        ),
        TeluguKarte(
            id = 2,
            teluguName = "భరణి",
            englishName = "Bharani",
            approxDateSpan = "ఏప్రిల్ 27 – మే 10",
            season = "వేసవి",
            proverb = "భరణిలో చల్లితే ధరణి పండుతుంది",
            agriculturalSignificance = "తోటల శుభ్రత, ఎండ తీవ్రతను తట్టుకునేందుకు నీటి యాజమాన్యం.",
            recommendedCrops = listOf("పచ్చిరొట్ట", "జీలుగ", "జనుము"),
        ),
        TeluguKarte(
            id = 3,
            teluguName = "కృత్తిక",
            englishName = "Krittika",
            approxDateSpan = "మే 11 – మే 24",
            season = "వేసవి",
            proverb = "కృత్తిక కార్తెలో ఎండలు మెండు",
            agriculturalSignificance = "పొలాల్లో గట్లు చదును చేయడం, పశువుల ఎరువు తోలడం.",
            recommendedCrops = listOf("పశువుల ఎరువు సేకరణ", "నత్రజని నేల సన్నద్ధత"),
        ),
        TeluguKarte(
            id = 4,
            teluguName = "రోహిణి",
            englishName = "Rohini",
            approxDateSpan = "మే 25 – జూన్ 7",
            season = "వేసవి ముగింపు",
            proverb = "రోహిణి ఎండకు రోళ్ళు పగులుతాయి",
            agriculturalSignificance = "వానాకాలం విత్తనాలు, ఎరువుల సేకరణ. రోహిణి ఎండలు ఎంత తీవ్రమైతే వర్షాలు అంత బాగుంటాయి.",
            recommendedCrops = listOf("విత్తనాల ఎంపిక", "వరి నారుమడి సిద్ధం"),
        ),
        TeluguKarte(
            id = 5,
            teluguName = "మృగశిర",
            englishName = "Mrigashira",
            approxDateSpan = "జూన్ 8 – జూన్ 21",
            season = "వానాకాలం",
            proverb = "మృగశిర కురిస్తే ముంగిట్లో బంగారం",
            agriculturalSignificance = "వానాకాలం సాగుకు తొలి అడుగు. పత్తి, మొక్కజొన్న, సోయా చిక్కుడు విత్తుకోవడానికి ఉత్తమ అదను.",
            recommendedCrops = listOf("పత్తి", "మొక్కజొన్న", "వేరుశనగ", "కంది"),
        ),
        TeluguKarte(
            id = 6,
            teluguName = "ఆరుద్ర",
            englishName = "Arudra",
            approxDateSpan = "జూన్ 22 – జూలై 5",
            season = "వానాకాలం",
            proverb = "ఆరుద్ర కురిస్తే అనర్థాలు పోతాయి",
            agriculturalSignificance = "వరి నారు పోయడానికి, మెట్ట పంటల పూర్తి విత్తనానికి అత్యంత శుభప్రదమైన సమయం.",
            recommendedCrops = listOf("వరి నారుమడి", "పత్తి", "సోయా", "మిర్చి నారు"),
        ),
        TeluguKarte(
            id = 7,
            teluguName = "పునర్వసు",
            englishName = "Punarvasu",
            approxDateSpan = "జూలై 6 – జూలై 19",
            season = "వానాకాలం",
            proverb = "పునర్వసులో పుట్టెడు విత్తులు చల్లు",
            agriculturalSignificance = "వరి నాట్లకు నారు సిద్ధం చేయడం. పత్తి, మిరప చేలలో కలుపు నివారణ.",
            recommendedCrops = listOf("వరి నాట్లు", "కలుపు తీత", "ఎరువుల మొదటి మోతాదు"),
        ),
        TeluguKarte(
            id = 8,
            teluguName = "పుష్యమి",
            englishName = "Pushyami",
            approxDateSpan = "జూలై 20 – ఆగస్టు 2",
            season = "వానాకాలం",
            proverb = "పుష్యమి కురిస్తే పుట్టెడు ధాన్యం",
            agriculturalSignificance = "వరి నాట్లు ముమ్మరంగా సాగే కాలం. చేలలో నీటి నిల్వ, మొదటి దఫా యూరియా.",
            recommendedCrops = listOf("వరి ప్రధాన నాట్లు", "అంతరకృషి"),
        ),
        TeluguKarte(
            id = 9,
            teluguName = "ఆశ్లేష",
            englishName = "Ashlesha",
            approxDateSpan = "ఆగస్టు 3 – ఆగస్టు 16",
            season = "వానాకాలం",
            proverb = "ఆశ్లేష వర్షం ఆశీర్వాదం",
            agriculturalSignificance = "వరి నాట్లు పూర్తి చేయాలి. పత్తిలో అంతరకృషి, గులికలు లేదా పోషకాల పిచికారీ.",
            recommendedCrops = listOf("వరి", "పత్తి పోషణ", "మిర్చి నాట్లు"),
        ),
        TeluguKarte(
            id = 10,
            teluguName = "మఖ",
            englishName = "Makha",
            approxDateSpan = "ఆగస్టు 17 – ఆగస్టు 30",
            season = "వానాకాలం",
            proverb = "మఖ ఉరిమినా మన్ను కురిసినా ఒకటే",
            agriculturalSignificance = "మఖ ఉరుములతో కూడిన వర్షాలు. వరిలో పిలకలు తొడిగే దశ, ఎరువుల సమతుల్యత.",
            recommendedCrops = listOf("వరి పిలకల దశ", "పత్తిలో రసం పీల్చే పురుగుల నివారణ"),
        ),
        TeluguKarte(
            id = 11,
            teluguName = "పుబ్బ",
            englishName = "Pubba",
            approxDateSpan = "ఆగస్టు 31 – సెప్టెంబర్ 12",
            season = "వానాకాలం",
            proverb = "పుబ్బలో చల్లినా బుట్టెడు పండుతుంది",
            agriculturalSignificance = "మిరప, పత్తి తోటల్లో దోమల నివారణ, పోషకాల ద్రవాలు పిచికారీ.",
            recommendedCrops = listOf("మిర్చి సంరక్షణ", "పత్తి కాయ దశ", "వరి దుబ్బు కట్టడం"),
        ),
        TeluguKarte(
            id = 12,
            teluguName = "ఉత్తర",
            englishName = "Uttara",
            approxDateSpan = "సెప్టెంబర్ 13 – సెప్టెంబర్ 26",
            season = "వానాకాలం ముగింపు",
            proverb = "ఉత్తర కురిస్తే ఊరంతా పండుగ",
            agriculturalSignificance = "వరిలో చిరుపొట్ట దశ. నీటి ఎద్దడి లేకుండా చూసుకోవడం ముఖ్యం.",
            recommendedCrops = listOf("వరి చిరుపొట్ట", "రెండో దఫా పొటాష్", "పప్పుదినుసులు"),
        ),
        TeluguKarte(
            id = 13,
            teluguName = "హస్త",
            englishName = "Hasta",
            approxDateSpan = "సెప్టెంబర్ 27 – అక్టోబర్ 10",
            season = "శరదృతువు",
            proverb = "హస్తలో కురిస్తే అన్నీ సమృద్ధి",
            agriculturalSignificance = "చెరువులు, కుంటలు నిండే వర్షాలు. రబీ (యాసంగి) సన్నద్ధత.",
            recommendedCrops = listOf("వరి ఈనిక దశ", "రబీ దుక్కులు", "శనగ తయారీ"),
        ),
        TeluguKarte(
            id = 14,
            teluguName = "చిత్త",
            englishName = "Chitta",
            approxDateSpan = "అక్టోబర్ 11 – అక్టోబర్ 23",
            season = "శరదృతువు",
            proverb = "చిత్త కురిస్తే చింతలు తీరుతాయి",
            agriculturalSignificance = "వరి గింజ పాలుపోసుకునే దశ. తెగుళ్ల నివారణకు జాగ్రత్తలు.",
            recommendedCrops = listOf("వరి రక్షణ", "మిరప కాయ ఎదుగుదల"),
        ),
        TeluguKarte(
            id = 15,
            teluguName = "స్వాతి",
            englishName = "Swati",
            approxDateSpan = "అక్టోబర్ 24 – నవంబర్ 5",
            season = "శరదృతువు",
            proverb = "స్వాతి వాన ముత్యాల వాన",
            agriculturalSignificance = "ఆలస్యపు వరి కోతలు, రబీ శనగ, జొన్న విత్తడానికి ఉత్తమ సమయం.",
            recommendedCrops = listOf("శనగ విత్తడం", "జొన్న", "రబీ మొక్కజొన్న"),
        ),
        TeluguKarte(
            id = 16,
            teluguName = "విశాఖ",
            englishName = "Vishakha",
            approxDateSpan = "నవంబర్ 6 – నవంబర్ 19",
            season = "యాసంగి ప్రారంభం",
            proverb = "విశాఖలో చల్లితే విరివిగా పంట",
            agriculturalSignificance = "వరి కోతలు ప్రారంభం. గింజ ఆరబెట్టడం, ధాన్యం భద్రపరచడం.",
            recommendedCrops = listOf("వరి కోతలు", "శనగ", "రబీ నూనెగింజలు"),
        ),
        TeluguKarte(
            id = 17,
            teluguName = "అనూరాధ",
            englishName = "Anuradha",
            approxDateSpan = "నవంబర్ 20 – డిసెంబర్ 2",
            season = "యాసంగి",
            proverb = "అనూరాధ చల్లదనానికి పంట పచ్చదనం",
            agriculturalSignificance = "చలి ప్రారంభం. యాసంగి వరి నారుమడులు పోయడానికి సమయం.",
            recommendedCrops = listOf("యాసంగి వరి నారు", "వేరుశనగ", "పొద్దుతిరుగుడు"),
        ),
        TeluguKarte(
            id = 18,
            teluguName = "జ్యేష్ఠ",
            englishName = "Jyeshtha",
            approxDateSpan = "డిసెంబర్ 3 – డిసెంబర్ 15",
            season = "యాసంగి",
            proverb = "జ్యేష్ఠలో చలి తీవ్రం - పంటకు రక్షణం",
            agriculturalSignificance = "రబీ పంటలకు మొదటి నీటి తడి, కంది కోతలు.",
            recommendedCrops = listOf("కంది కోత", "రబీ నీటి యాజమాన్యం"),
        ),
        TeluguKarte(
            id = 19,
            teluguName = "మూల",
            englishName = "Moola",
            approxDateSpan = "డిసెంబర్ 16 – డిసెంబర్ 28",
            season = "యాసంగి",
            proverb = "మూల కార్తె చలి ముదిరితే మేలు",
            agriculturalSignificance = "పత్తి చివరి ఏతలు, యాసంగి వరి నాట్లు మొదలు.",
            recommendedCrops = listOf("యాసంగి వరి నాట్లు", "మిర్చి కోతలు"),
        ),
        TeluguKarte(
            id = 20,
            teluguName = "పూర్వాషాఢ",
            englishName = "Purvashadha",
            approxDateSpan = "డిసెంబర్ 29 – జనవరి 10",
            season = "యాసంగి",
            proverb = "పూర్వాషాఢలో పొగమంచు ఎక్కువ",
            agriculturalSignificance = "మిరపలో బూడిద తెగులు నివారణ జాగ్రత్తలు.",
            recommendedCrops = listOf("మిర్చి రక్షణ", "రబీ ఎరువుల యాజమాన్యం"),
        ),
        TeluguKarte(
            id = 21,
            teluguName = "ఉత్తరాషాఢ",
            englishName = "Uttarashadha",
            approxDateSpan = "జనవరి 11 – జనవరి 23",
            season = "సంక్రాంతి సమయం",
            proverb = "ఉత్తరాషాఢలో ఇల్లు నిండే ధాన్యం",
            agriculturalSignificance = "సంక్రాంతి పంటల పండుగ, పశువుల సంరక్షణ, కొత్త ధాన్యం ఇళ్లకు చేరే వేళ.",
            recommendedCrops = listOf("సంక్రాంతి వేడుకలు", "పశువుల అలంకరణ"),
        ),
        TeluguKarte(
            id = 22,
            teluguName = "శ్రవణం",
            englishName = "Shravana",
            approxDateSpan = "జనవరి 24 – ఫిబ్రవరి 5",
            season = "యాసంగి",
            proverb = "శ్రవణంలో శ్రమపడితే శ్రేయస్సు",
            agriculturalSignificance = "యాసంగి వరి చేలలో కలుపు, మొదటి దఫా ఎరువులు.",
            recommendedCrops = listOf("యాసంగి వరి సంరక్షణ", "వేరుశనగ"),
        ),
        TeluguKarte(
            id = 23,
            teluguName = "ధనిష్ఠ",
            englishName = "Dhanishtha",
            approxDateSpan = "ఫిబ్రవరి 6 – ఫిబ్రవరి 18",
            season = "యాసంగి",
            proverb = "ధనిష్ఠ ఎండలు ధాన్యం కాపుకు బలం",
            agriculturalSignificance = "యాసంగి మొక్కజొన్న, జొన్న కండె దశ.",
            recommendedCrops = listOf("మొక్కజొన్న నీటి తడి", "శనగ కోత"),
        ),
        TeluguKarte(
            id = 24,
            teluguName = "శతభిషం",
            englishName = "Shatabhisha",
            approxDateSpan = "ఫిబ్రవరి 19 – మార్చి 3",
            season = "యాసంగి ముగింపు",
            proverb = "శతభిషంలో చలి తగ్గి వేడి మొదలు",
            agriculturalSignificance = "మిరప ఆఖరి కోతలు, రబీ ఆవాలు, శనగ నూర్పిళ్లు.",
            recommendedCrops = listOf("మిర్చి ఆరబెట్టుట", "శనగ నూర్పిడి"),
        ),
        TeluguKarte(
            id = 25,
            teluguName = "పూర్వాభాద్ర",
            englishName = "Purvabhadra",
            approxDateSpan = "మార్చి 4 – మార్చి 17",
            season = "వసంతం",
            proverb = "పూర్వాభాద్రలో పొలం పనులు చురుకు",
            agriculturalSignificance = "యాసంగి వరి గింజ దశ. చివరి నీటి తడులు.",
            recommendedCrops = listOf("యాసంగి వరి నీటి నిర్వహణ"),
        ),
        TeluguKarte(
            id = 26,
            teluguName = "ఉత్తరాభాద్ర",
            englishName = "Uttarabhadra",
            approxDateSpan = "మార్చి 18 – మార్చి 30",
            season = "వసంతం",
            proverb = "ఉత్తరాభాద్ర ఎండలు ఉధృతం",
            agriculturalSignificance = "యాసంగి పంటల కోతలు ముమ్మరం.",
            recommendedCrops = listOf("రబీ కోతలు", "గింజ ఆరబెట్టడం"),
        ),
        TeluguKarte(
            id = 27,
            teluguName = "రేవతి",
            englishName = "Revati",
            approxDateSpan = "మార్చి 31 – ఏప్రిల్ 13",
            season = "సంవత్సరాంతం",
            proverb = "రేవతితో కాలచక్రం పూర్తి - కొత్త ఉగాదికి స్వాగతం",
            agriculturalSignificance = "యాసంగి లెక్కల ముగింపు, ఉగాది పండుగ సన్నద్ధత, వచ్చే వానాకాలం ప్రణాళిక.",
            recommendedCrops = listOf("వ్యవసాయ లెక్కలు", "ఉగాది సన్నద్ధత"),
        ),
    )

    fun getById(id: Int): TeluguKarte = ALL.firstOrNull { it.id == id } ?: ALL.first()
}

@Serializable
data class TeluguCalendarDay(
    val epochDay: Long,
    val gregorianDate: String, // "YYYY-MM-DD"
    val teluguYear: String, // "క్రోధి", "విశ్వావసు", etc.
    val teluguMonth: TeluguMonth,
    val paksham: Paksham,
    val tithi: TeluguTithi,
    val nakshatram: String,
    val karteId: Int,
    val karteName: String,
    val karteDayNumber: Int,
    val totalKarteDays: Int,
    val isAmavasya: Boolean,
    val isPournami: Boolean,
    val isFestival: Boolean,
    val festivalName: String? = null,
    val seasonalAdvisory: String? = null,
)

@Serializable
data class CurrentKarteInfo(
    val karteId: Int,
    val teluguName: String,
    val englishName: String,
    val dayNumber: Int,
    val totalDays: Int,
    val daysRemaining: Int,
    val progressPercentage: Float,
    val proverb: String,
    val agriculturalSignificance: String,
    val recommendedCrops: List<String>,
)

enum class LunarAlertType {
    DAY_STANDARD,
    AMAVASYA,
    POST_AMAVASYA_SPRAY,
    POURNAMI,
    KARTE_TRANSITION,
    FESTIVAL,
}

@Serializable
data class LunarDayAlert(
    val alertType: LunarAlertType,
    val title: String,
    val subtitle: String,
    val ttsSpeechText: String,
    val actionText: String? = null,
    val actionRoute: String? = null,
    val isPriority: Boolean = false,
)
