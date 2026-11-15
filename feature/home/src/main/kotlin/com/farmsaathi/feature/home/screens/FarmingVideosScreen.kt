package com.farmsaathi.feature.home.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.localization.LocalAppLanguage
import com.farmsaathi.core.designsystem.localization.LocalFarmSpeakHandler
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage

enum class VideoCategory(val teluguName: String, val englishName: String) {
    ALL("అన్నీ", "All"),
    ORGANIC("సేంద్రీయం & జీవామృతం", "Natural Farming"),
    PEST_CONTROL("పురుగు & తెగులు నివారణ", "Pest Control"),
    WATER_IRRIGATION("డ్రిప్ & నీటి యాజమాన్యం", "Water & Drip"),
    CROP_PRACTICES("వరి & మిరప సాగు", "Paddy & Chilli"),
    MACHINERY("యంత్రాలు & పనిముట్లు", "Farm Machinery"),
}

data class FarmingVideoItem(
    val id: String,
    val youtubeVideoId: String,
    val category: VideoCategory,
    val titleTe: String,
    val titleEn: String,
    val descriptionTe: String,
    val descriptionEn: String,
    val durationMinutes: Int,
    val sourceAgency: String,
    val keyTakeawaysTe: List<String>,
)

val CURATED_FARMING_VIDEOS = listOf(
    FarmingVideoItem(
        id = "video_jeevamrutham",
        youtubeVideoId = "3yYVbK_v_e0",
        category = VideoCategory.ORGANIC,
        titleTe = "జీవామృతం తయారీ మరియు పొలంలో వాడే పద్ధతి",
        titleEn = "Preparation and Application of Jeevamrutham",
        descriptionTe = "దేశీ ఆవు పేడ, మూత్రం, బెల్లం మరియు పిండితో అత్యంత శక్తివంతమైన జీవామృతం సులభంగా తయారుచేసే సరైన పద్ధతి.",
        descriptionEn = "Scientific and traditional method to prepare Jeevamrutham bio-fertilizer using indigenous cow dung and urine.",
        durationMinutes = 6,
        sourceAgency = "సేంద్రీయ వ్యవసాయ విభాగం",
        keyTakeawaysTe = listOf(
            "10 కిలోల దేశీ ఆవు పేడ + 10 లీటర్ల ఆవు మూత్రం ఒక డ్రమ్ములో కలపండి.",
            "2 కిలోల బెల్లం, 2 కిలోల శనగ లేదా మినుము పిండి, పిడికెడు గట్టు మట్టి చేర్చండి.",
            "200 లీటర్ల నీరు పోసి రోజుకు రెండు సార్లు సవ్యదిశలో కర్రతో తిప్పండి.",
            "48 గంటల్లో తయారవుతుంది; 7 రోజుల్లోగా నీటితో పాటు పొలానికి అందించాలి.",
        ),
    ),
    FarmingVideoItem(
        id = "video_nske_neem",
        youtubeVideoId = "6m9H_aE_R8Y",
        category = VideoCategory.PEST_CONTROL,
        titleTe = "వేప గింజల కషాయం (NSKE 5%) తయారీ విధానం",
        titleEn = "How to Prepare 5% Neem Seed Kernel Extract (NSKE)",
        descriptionTe = "గుడ్ల దశలోనే పురుగులను నిర్మూలించడానికి మరియు రసం పీల్చే పురుగుల నివారణకు వేప కషాయం ఉత్తమమైనది.",
        descriptionEn = "Natural pest repellent made from dried neem seeds to protect against sucking pests and caterpillars.",
        durationMinutes = 5,
        sourceAgency = "వ్యవసాయ విస్తరణ విభాగం",
        keyTakeawaysTe = listOf(
            "5 కిలోల బాగా ఎండిన వేప గింజలను మెత్తగా దంచండి.",
            "పల్చటి గుడ్డలో మూటకట్టి 10 లీటర్ల నీటిలో రాత్రంతా నానబెట్టండి.",
            "ఉదయాన్నే మూటను బాగా పిండి కషాయం తీయండి.",
            "100 లీటర్ల నీటిలో 100 గ్రాముల సబ్బు పొడి చేర్చి సాయంత్రం వేళ పిచికారీ చేయండి.",
        ),
    ),
    FarmingVideoItem(
        id = "video_paddy_stemborer",
        youtubeVideoId = "7KxH8aWw8yM",
        category = VideoCategory.CROP_PRACTICES,
        titleTe = "వరిలో కాండం తొలిచే పురుగు మరియు సుడిదోమ నివారణ",
        titleEn = "Stem Borer & BPH Management in Paddy",
        descriptionTe = "వరి దుబ్బు దశలో మరియు వెన్ను దశలో వచ్చే కాండం తొలిచే పురుగు లక్షణాలు మరియు సమగ్ర సస్యరక్షణ చర్యలు.",
        descriptionEn = "Integrated management of stem borer and Brown Planthopper in rice crops.",
        durationMinutes = 7,
        sourceAgency = "వరి పరిశోధనా స్థానం",
        keyTakeawaysTe = listOf(
            "ఎకరాకు 4 నుండి 6 లింగాకర్షక బుట్టలు (Pheromone Traps) అమర్చండి.",
            "పిలక దశలో ఎండిన మోవులు (Dead Hearts) కనిపిస్తే కార్టాప్ హైడ్రోక్లోరైడ్ వాడండి.",
            "సుడిదోమ గమనిస్తే పొలంలో నీరు తీసి ఆరబెట్టండి.",
            "నత్రజని ఎరువులను మోతాదుకు మించి ఒకేసారి వేయవద్దు; 3 దఫాలుగా వేయండి.",
        ),
    ),
    FarmingVideoItem(
        id = "video_drip_maintenance",
        youtubeVideoId = "9FqY_r_s8kQ",
        category = VideoCategory.WATER_IRRIGATION,
        titleTe = "డ్రిప్ ఇరిగేషన్ ఫిల్టర్ల శుభ్రత మరియు యాసిడ్ ట్రీట్మెంట్",
        titleEn = "Drip Filter Cleaning & Hydrochloric Acid Treatment",
        descriptionTe = "డ్రిప్ డ్రిప్పర్లు పూడిపోకుండా హైడ్రోక్లోరిక్ యాసిడ్ తో ఫ్లషింగ్ చేసే సురక్షిత విధానం.",
        descriptionEn = "Maintaining drip lines, screen and disc filters, and unclogging drippers safely.",
        durationMinutes = 8,
        sourceAgency = "సూక్ష్మ సేద్యం ప్రాజెక్ట్",
        keyTakeawaysTe = listOf(
            "స్క్రీన్ మరియు డిస్క్ ఫిల్టర్లను ప్రతి వారం శుభ్రమైన నీటితో కడగాలి.",
            "ఉప్పు పేరుకుపోయినపుడు హైడ్రోక్లోరిక్ ఆమ్లం (HCl) తో ఫ్లష్ చేయండి.",
            "పైపుల చివరి ఎండ్ క్యాప్స్ తీసి అధిక పీడనంతో నీటిని వదిలి మట్టిని బయటకు పంపండి.",
        ),
    ),
    FarmingVideoItem(
        id = "video_chilli_thrips",
        youtubeVideoId = "2PzX_bN_8kQ",
        category = VideoCategory.PEST_CONTROL,
        titleTe = "మిరపలో నల్ల తామర పురుగు మరియు ఆకుముడత నివారణ",
        titleEn = "Management of Black Thrips and Leaf Curl in Chilli",
        descriptionTe = "మిరప పూత రాలకుండా, తామర పురుగుల ఉధృతిని తగ్గించే నీలి మరియు పసుపు రంగు జిగురు అట్టల ప్రాముఖ్యత.",
        descriptionEn = "Effective control of invasive black thrips in chilli using colored sticky cards and biological sprays.",
        durationMinutes = 6,
        sourceAgency = "ఉద్యానవన పరిశోధనా కేంద్రం",
        keyTakeawaysTe = listOf(
            "ఎకరాకు 30 నీలి రంగు మరియు 20 పసుపు రంగు జిగురు అట్టలు అమర్చండి.",
            "తొలి దశలో వేప నూనె (10,000 PPM) 1 లీటరు నీటికి 2 మి.లీ చొప్పున పిచికారీ చేయండి.",
            "విచక్షణారహితంగా సింథటిక్ పైరెథ్రాయిడ్స్ వాడితే పురుగు ఉధృతి పెరుగుతుంది.",
        ),
    ),
    FarmingVideoItem(
        id = "video_tractor_rotavator",
        youtubeVideoId = "5M_wK_bE8aQ",
        category = VideoCategory.MACHINERY,
        titleTe = "ట్రాక్టర్ రోటవేటర్ సరైన సెట్టింగ్ మరియు డీజిల్ ఆదా",
        titleEn = "Rotavator PTO Adjustment and Diesel Fuel Saving",
        descriptionTe = "పొలం దున్నేటప్పుడు బ్లేడ్ల వేగం, డెప్త్ వీల్ సెట్టింగ్ మరియు డీజిల్ ఖర్చు తగ్గించుకునే సులభమైన మెలకువలు.",
        descriptionEn = "Correct PTO speed selection, depth wheel adjustment, and diesel saving tips during tilling.",
        durationMinutes = 5,
        sourceAgency = "వ్యవసాయ ఇంజనీరింగ్ విభాగం",
        keyTakeawaysTe = listOf(
            "భూమి తేమను బట్టి రోటవేటర్ డెప్త్ స్కిడ్స్ సర్దుబాటు చేయండి.",
            "540 RPM స్టాండర్డ్ PTO స్పీడ్ లేదా 540 Economy మోడ్ లో నడపండి.",
            "రోటవేటర్ తిరుగుతున్నప్పుడు ఇంజిన్ ను అతిగా రేస్ చేయకండి, గేర్ 1st Low వాడండి.",
        ),
    ),
)

@Composable
fun FarmingVideosScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val useTe = LocalAppLanguage.current == AppLanguage.TELUGU
    val speakHandler = LocalFarmSpeakHandler.current

    var selectedCategory by remember { mutableStateOf(VideoCategory.ALL) }
    var activeVideo by remember { mutableStateOf<FarmingVideoItem?>(null) }
    val isOnline = remember(context) { isNetworkAvailable(context) }

    val filteredVideos = remember(selectedCategory) {
        if (selectedCategory == VideoCategory.ALL) {
            CURATED_FARMING_VIDEOS
        } else {
            CURATED_FARMING_VIDEOS.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (useTe) "సాగు వీడియో పాఠాలు" else "Farming Video Guides",
                subtitle = if (useTe) "రైతులకు ఉపయోగపడే వీడియోలు" else "Curated video advisories",
                onBackClick = onBack,
                backContentDescription = if (useTe) "వెనుకకు" else "Back",
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = FarmSpacing.dp24),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            // 1. Embedded Video Player (if a video is selected)
            if (activeVideo != null) {
                item(key = "active_player_box") {
                    ActiveVideoPlayerCard(
                        video = activeVideo!!,
                        useTe = useTe,
                        isOnline = isOnline,
                        onClose = { activeVideo = null },
                    )
                }
            }

            // 2. Offline notice if not connected
            if (!isOnline) {
                item(key = "offline_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FarmSpacing.dp16),
                        shape = FarmShapeTokens.card,
                        color = FarmColors.AmberContainer,
                        border = BorderStroke(1.dp, FarmColors.HarvestAmber),
                    ) {
                        Row(
                            modifier = Modifier.padding(FarmSpacing.dp12),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = FarmColors.Warning,
                            )
                            Spacer(Modifier.width(FarmSpacing.dp8))
                            Text(
                                text = if (useTe) {
                                    "ఆఫ్‌లైన్: వీడియో ప్లే చేయడానికి ఇంటర్నెట్ కావాలి. అయితే క్రింద ఉన్న సూచనలు మరియు మాటల వివరణ ఇప్పుడే చదువుకోవచ్చు/వినవచ్చు."
                                } else {
                                    "Offline: Connect to internet to play video. Key steps and voice summary are available offline."
                                },
                                style = FarmTypography.bodySmall,
                                color = FarmColors.MainText,
                            )
                        }
                    }
                }
            }

            // 3. Category Filter Chips
            item(key = "category_filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = FarmSpacing.dp16),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    VideoCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = if (useTe) category.teluguName else category.englishName,
                                    style = FarmTypography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryForest,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }

            // 4. Video Cards List
            items(
                items = filteredVideos,
                key = { it.id },
            ) { video ->
                FarmingVideoCard(
                    video = video,
                    useTe = useTe,
                    isPlaying = activeVideo?.id == video.id,
                    onPlayClick = {
                        activeVideo = video
                    },
                    onListenClick = {
                        val listenText = if (useTe) {
                            "${video.titleTe}. ${video.descriptionTe}. ముఖ్య విషయాలు: " +
                                video.keyTakeawaysTe.joinToString(". ")
                        } else {
                            "${video.titleEn}. ${video.descriptionEn}"
                        }
                        speakHandler(listenText)
                    },
                    modifier = Modifier.padding(horizontal = FarmSpacing.dp16),
                )
            }
        }
    }
}

@Composable
private fun ActiveVideoPlayerCard(
    video: FarmingVideoItem,
    useTe: Boolean,
    isOnline: Boolean,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FarmSpacing.dp16),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.5.dp, FarmColors.PrimaryForest),
        shadowElevation = 4.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FarmColors.PrimaryForest)
                    .padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartDisplay,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(FarmSpacing.dp8))
                    Text(
                        text = if (useTe) video.titleTe else video.titleEn,
                        style = FarmTypography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = if (useTe) "మూసివేయి" else "Close",
                        tint = Color.White,
                    )
                }
            }

            if (isOnline) {
                YouTubeEmbedPlayer(
                    videoId = video.youtubeVideoId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(FarmSpacing.dp16),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(FarmSpacing.dp8))
                        Text(
                            text = if (useTe) "ఇంటర్నెట్ కనెక్షన్ లేదు" else "No Internet Connection",
                            style = FarmTypography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = if (useTe) "వీడియో కోసం డేటా ఆన్ చేయండి" else "Turn on Wi-Fi or mobile data to stream",
                            style = FarmTypography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
) {
    val embedHtml = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body { margin: 0; padding: 0; background-color: #000; overflow: hidden; }
                iframe { width: 100vw; height: 100vh; border: none; }
            </style>
        </head>
        <body>
            <iframe
                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&playsinline=1&controls=1"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen>
            </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(videoId) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "utf-8", null)
                webViewRef = this
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "utf-8", null)
        },
        modifier = modifier,
    )
}

@Composable
private fun FarmingVideoCard(
    video: FarmingVideoItem,
    useTe: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onListenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(
            if (isPlaying) 2.dp else 1.dp,
            if (isPlaying) FarmColors.PrimaryForest else FarmColors.Outline,
        ),
        shadowElevation = if (isPlaying) 3.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(FarmSpacing.dp16),
        ) {
            // Category & Source Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = FarmColors.SurfaceVariant,
                ) {
                    Text(
                        text = if (useTe) video.category.teluguName else video.category.englishName,
                        style = FarmTypography.labelSmall,
                        color = FarmColors.PrimaryForest,
                        modifier = Modifier.padding(horizontal = FarmSpacing.dp8, vertical = 2.dp),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${video.durationMinutes} ${if (useTe) "నిమిషాలు" else "min"}",
                        style = FarmTypography.labelSmall,
                        color = FarmColors.SecondaryText,
                    )
                    Spacer(Modifier.width(FarmSpacing.dp4))
                    Icon(
                        imageVector = Icons.Rounded.FiberManualRecord,
                        contentDescription = null,
                        modifier = Modifier.size(6.dp),
                        tint = FarmColors.SecondaryText,
                    )
                    Spacer(Modifier.width(FarmSpacing.dp4))
                    Text(
                        text = video.sourceAgency,
                        style = FarmTypography.labelSmall,
                        color = FarmColors.SecondaryText,
                    )
                }
            }

            Spacer(Modifier.height(FarmSpacing.dp8))

            // Title
            Text(
                text = if (useTe) video.titleTe else video.titleEn,
                style = FarmTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FarmColors.MainText,
            )

            Spacer(Modifier.height(FarmSpacing.dp4))

            // Description
            Text(
                text = if (useTe) video.descriptionTe else video.descriptionEn,
                style = FarmTypography.bodyMedium,
                color = FarmColors.SecondaryText,
                maxLines = if (expanded) 10 else 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Key Practical Steps
            if (expanded) {
                Spacer(Modifier.height(FarmSpacing.dp8))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FarmColors.SurfaceVariant.copy(alpha = 0.5f), FarmShapeTokens.inputField)
                        .padding(FarmSpacing.dp12),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp4),
                ) {
                    Text(
                        text = if (useTe) "ఆచరించాల్సిన ముఖ్య పద్ధతులు:" else "Key Actionable Steps:",
                        style = FarmTypography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmColors.PrimaryForest,
                    )
                    video.keyTakeawaysTe.forEach { step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", color = FarmColors.PrimaryForest, fontWeight = FontWeight.Bold)
                            Text(
                                text = step,
                                style = FarmTypography.bodySmall,
                                color = FarmColors.MainText,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(FarmSpacing.dp12))

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Play / Watch Button
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) FarmColors.AmberContainer else FarmColors.PrimaryForest,
                    modifier = Modifier.clickable(onClick = onPlayClick),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.PlayCircle else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (isPlaying) FarmColors.Warning else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(FarmSpacing.dp8))
                        Text(
                            text = if (isPlaying) {
                                if (useTe) "నడుస్తోంది" else "Playing"
                            } else {
                                if (useTe) "వీడియో చూడండి" else "Watch Video"
                            },
                            style = FarmTypography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) FarmColors.MainText else Color.White,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    // Read more toggle
                    Text(
                        text = if (expanded) {
                            if (useTe) "తక్కువ చూపించు" else "Show Less"
                        } else {
                            if (useTe) "వివరాలు..." else "Details..."
                        },
                        style = FarmTypography.labelMedium,
                        color = FarmColors.PrimaryForest,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(FarmSpacing.dp4),
                    )

                    // 1-Tap Telugu Voice Readout
                    IconButton(
                        onClick = onListenClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(FarmColors.SurfaceVariant, CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (useTe) "వినండి" else "Listen",
                            tint = FarmColors.PrimaryForest,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
