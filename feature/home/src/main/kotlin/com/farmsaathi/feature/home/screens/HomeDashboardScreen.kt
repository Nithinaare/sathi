package com.farmsaathi.feature.home.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmAudioButton
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSectionHeader
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmSummaryCard
import com.farmsaathi.core.designsystem.components.FarmVisualGridTile
import com.farmsaathi.core.designsystem.components.rememberFarmPhotoLaunchers
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmCombinedClickable
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.localization.rememberHoldToSpeakAction
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.feature.home.components.HomePanchangKarteBanner
import com.farmsaathi.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeDashboardScreen(
    viewModel: HomeViewModel,
    onAddExpense: () -> Unit,
    onAddSale: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenMachinery: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenMachinerySession: (Long) -> Unit,
    onOpenContacts: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenIrrigation: () -> Unit = {},
    onOpenExtras: () -> Unit = {},
    onOpenBackup: () -> Unit = onOpenSettings,
    onSnapPhotoReady: (String, Long?) -> Unit = { _, _ -> },
    onOpenTeluguCalendar: () -> Unit = {},
    onOpenLandMeasurement: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val context = LocalContext.current

    val snapLaunchers =
        rememberFarmPhotoLaunchers(
            createTempFile = viewModel::createTempCaptureFile,
            onCapturedFile = { file ->
                viewModel.capturePhotoWithExif(file, onSnapPhotoReady)
            },
            onGalleryUri = { uri ->
                viewModel.importGalleryWithExif(uri, onSnapPhotoReady)
            },
        )

    if (state.isLoading) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeaderBar(
            farmerName = state.prefs.farmerName,
            todayFormatted = state.todayFormatted,
            isAudioPlaying = state.isAudioPlaying,
            useTe = useTe,
            onOpenContacts = onOpenContacts,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings,
            onToggleAudio = viewModel::toggleHomeAudio,
            modifier = Modifier.zIndex(1f),
        )
        HorizontalDivider(thickness = 1.dp, color = FarmColors.Outline)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            if (state.runningMachinery.isNotEmpty()) {
                items(
                    state.runningMachinery,
                    key = { "timer_${it.id}" },
                    contentType = { "running_machinery_banner" },
                ) { session ->
                    ActiveMachineryBanner(
                        session = session,
                        elapsedSecondsProvider = { viewModel.currentElapsedSeconds(session) },
                        onClick = { onOpenMachinerySession(session.id) },
                        useTe = useTe,
                    )
                }
            }

            if (state.showBackupBanner) {
                item(key = "backup_safety_banner", contentType = "backup_safety_banner") {
                    BackupSafetyBanner(
                        useTe = useTe,
                        onOpenBackup = onOpenBackup,
                    )
                }
            }

            if (state.todayCalendarDay != null && state.todayAlert != null) {
                item(key = "panchang_karte_banner", contentType = "panchang_karte_banner") {
                    HomePanchangKarteBanner(
                        calendarDay = state.todayCalendarDay,
                        karteInfo = state.todayKarteInfo,
                        alert = state.todayAlert,
                        isPlayingAudio = state.isPanchangAudioPlaying,
                        onPlayAudio = viewModel::togglePanchangAudio,
                        onClick = onOpenTeluguCalendar,
                    )
                }
            }

            item(key = "season_summary_card", contentType = "summary_card") {
                val totals = state.seasonTotals
                FarmSummaryCard(
                    title = farmString(R.string.home_season_card),
                    primaryLabel = farmString(R.string.home_net_short),
                    primaryValue = totals.netBalance.formatInr(),
                    secondaryMetrics =
                    listOf(
                        farmString(R.string.home_expense_short) to totals.totalExpense.formatInr(),
                        farmString(R.string.home_income_short) to totals.totalIncome.formatInr(),
                    ),
                    onClick = onOpenAccounts,
                )
            }

            item(key = "snap_photo_hero", contentType = "snap_photo_hero") {
                SnapPhotoHeroCard(
                    title = farmString(R.string.home_snap_photo),
                    useTe = useTe,
                    onCameraClick = snapLaunchers.launchCamera,
                    onGalleryClick = snapLaunchers.launchGallery,
                )
            }

            item(key = "action_row_finance", contentType = "two_col_action_row") {
                TwoColActionRow(
                    first = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_expense),
                            icon = Icons.Rounded.Payments,
                            imageRes = FarmImages.expense,
                            fullBleedImage = true,
                            borderColor = FarmColors.Terracotta,
                            speakDescription =
                            if (useTe) {
                                "ఖర్చు. విత్తనాలు, ఎరువులు, మందులు లేదా కూలీల డబ్బు రాయండి."
                            } else {
                                "Expense. Record money spent on seeds, inputs, or labour."
                            },
                            onClick = onAddExpense,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                    second = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_sale),
                            icon = Icons.Rounded.AccountBalanceWallet,
                            imageRes = FarmImages.income,
                            fullBleedImage = true,
                            borderColor = FarmColors.PrimaryForest,
                            speakDescription =
                            if (useTe) {
                                "అమ్మకం. పంట అమ్మగా వచ్చిన డబ్బులు రాయండి."
                            } else {
                                "Sale. Record crop sale earnings and pending dues."
                            },
                            onClick = onAddSale,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                )
            }

            item(key = "action_row_labour_irrigation", contentType = "two_col_action_row") {
                TwoColActionRow(
                    first = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_labour),
                            icon = Icons.Rounded.Groups,
                            imageRes = FarmImages.wageCalc,
                            fullBleedImage = true,
                            borderColor = FarmColors.HarvestAmber,
                            speakDescription =
                            if (useTe) {
                                "కూలీలు. రోజుల లెక్క, కూలీల ఫోటోలు మరియు వేతన క్యాలిక్యులేటర్ ఇక్కడ ఉంటాయి."
                            } else {
                                "Labour. Track worker day counters, photos, and wage calculator."
                            },
                            onClick = onOpenAttendance,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                    second = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_irrigation),
                            icon = Icons.Rounded.WaterDrop,
                            imageRes = FarmImages.irrigation,
                            fullBleedImage = true,
                            borderColor = FarmColors.Info,
                            speakDescription =
                            if (useTe) {
                                "నీళ్లు. పంట ఫోటో ఎంచుకుని నీరు పెట్టిన సమయం రాయండి."
                            } else {
                                "Watering. Pick crop photo and snap camera to log irrigation time."
                            },
                            onClick = onOpenIrrigation,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                )
            }

            item(key = "action_row_activity_tractor", contentType = "two_col_action_row") {
                TwoColActionRow(
                    first = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_activity),
                            icon = Icons.AutoMirrored.Rounded.EventNote,
                            imageRes = FarmImages.farmWork,
                            fullBleedImage = true,
                            borderColor = FarmColors.PrimaryForest,
                            speakDescription =
                            if (useTe) {
                                "పనులు. దున్నుడు, కలుపు తీత, మందు కొట్టే పనులు గుర్తుంచుకోండి."
                            } else {
                                "Farm Work. Track ploughing, weeding, and spraying tasks."
                            },
                            onClick = onOpenActivities,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                    second = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_tractor),
                            icon = Icons.Rounded.Agriculture,
                            imageRes = FarmImages.tractor,
                            fullBleedImage = true,
                            borderColor = FarmColors.Terracotta,
                            speakDescription =
                            if (useTe) {
                                "ట్రాక్టర్. టైమర్ ఆన్ చేసి గంటల అద్దె లెక్క వేసుకోండి."
                            } else {
                                "Tractor. Live stopwatch and hourly hire calculator."
                            },
                            onClick = onOpenMachinery,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                )
            }

            item(key = "contacts_header", contentType = "section_header") {
                FarmSectionHeader(
                    title = if (useTe) "ఫోన్ పరిచయాలు" else "Quick Call Contacts",
                    actionLabel = farmString(R.string.common_see_all),
                    onActionClick = onOpenContacts,
                )
            }

            if (state.contacts.isEmpty()) {
                item(key = "contacts_empty_tile", contentType = "action_tile") {
                    FarmVisualGridTile(
                        title = farmString(R.string.con_title),
                        icon = Icons.Rounded.Call,
                        imageRes = FarmImages.contacts,
                        fullBleedImage = true,
                        borderColor = FarmColors.Info,
                        speakDescription =
                        if (useTe) {
                            "ఫోన్ పరిచయాలు. ట్రాక్టర్, షాప్ నంబర్లు సేవ్ చేసి ఒక్క నొక్కుతో ఫోన్ చేయండి."
                        } else {
                            "Contacts. Save tractor owners and dealers for 1-tap dialing."
                        },
                        onClick = onOpenContacts,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item(key = "contacts_row", contentType = "contacts_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                    ) {
                        items(
                            state.contacts,
                            key = { "c_${it.id}" },
                            contentType = { "quick_contact_card" },
                        ) { contact ->
                            QuickContactCallCard(
                                contact = contact,
                                isPlayingVoice = state.playingContactId == contact.id,
                                useTe = useTe,
                                onSpeakClick = { viewModel.playContactBadge(contact) },
                                onDialClick = {
                                    val phone = contact.phone
                                    if (!phone.isNullOrBlank()) {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")),
                                        )
                                    } else {
                                        onOpenContacts()
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (state.dueTasks.isNotEmpty()) {
                item(key = "due_tasks_header", contentType = "section_header") {
                    FarmSectionHeader(
                        title = farmString(R.string.home_today_tasks),
                        actionLabel = farmString(R.string.common_see_all),
                        onActionClick = onOpenActivities,
                    )
                }
                items(
                    state.dueTasks.take(2),
                    key = { "task_${it.id}" },
                    contentType = { "due_task_card" },
                ) { task ->
                    FarmRecordCard(
                        title = task.title,
                        subtitle = FarmLabels.taskCategory(task.category, lang),
                        leadingIcon = Icons.Rounded.CheckCircle,
                        leadingImageRes = FarmImages.forTaskCategory(task.category),
                        statusType = FarmStatusType.WARNING,
                        statusLabel = FarmLabels.taskStatus(task.status, lang),
                        onClick = { onOpenTask(task.id) },
                    )
                }
            }

            item(key = "action_row_extras_measurement", contentType = "two_col_action_row") {
                TwoColActionRow(
                    first = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_land_measurement),
                            icon = Icons.Rounded.Terrain,
                            borderColor = FarmColors.PrimaryForest,
                            speakDescription = farmString(R.string.home_tile_land_measurement_desc),
                            onClick = onOpenLandMeasurement,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                    second = {
                        FarmVisualGridTile(
                            title = farmString(R.string.home_extras_tile),
                            icon = Icons.Rounded.Extension,
                            imageRes = FarmImages.extrasShed,
                            fullBleedImage = true,
                            borderColor = FarmColors.SecondaryText,
                            speakDescription =
                            if (useTe) {
                                "అదనపు సౌకర్యాలు. డైరీ, మందుల నిల్వ, కోత, మట్టి పరీక్షలు మరియు నివేదికలు ఇక్కడ ఉంటాయి."
                            } else {
                                "Advanced Extras. Open field diary, input stock, harvest, soil health, and reports."
                            },
                            onClick = onOpenExtras,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SnapPhotoHeroCard(
    title: String,
    useTe: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    val speakTarget =
        if (useTe) {
            "ఫోటో తీసి రాయండి. ఏ పంటకైనా లేదా బిల్లుకైనా ఫోటో తీస్తే తేదీ సమయం ఆటోమేటిక్‌గా పడుతుంది."
        } else {
            "Snap photo to log. Automatically extracts date and time from your photo."
        }
    val holdSpeak = rememberHoldToSpeakAction(speakTarget)

    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(FarmShapeTokens.actionTile)
            .border(1.5.dp, FarmColors.PrimaryForest, FarmShapeTokens.actionTile)
            .farmCombinedClickable(
                role = Role.Button,
                speakLabel = speakTarget,
                onLongPressSpeak = holdSpeak,
                onClick = onCameraClick,
            ),
        shape = FarmShapeTokens.actionTile,
        color = FarmColors.Surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(FarmImages.snapCamera),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops =
                            arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.84f),
                                0.65f to Color.Black.copy(alpha = 0.62f),
                                1.0f to Color.Black.copy(alpha = 0.38f),
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = FarmSpacing.dp16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = FarmShapeTokens.circular,
                        color = FarmColors.PrimaryForest,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(FarmSpacing.dp12))
                    Text(
                        text = title,
                        style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FarmIconButton(
                    icon = Icons.Rounded.PhotoLibrary,
                    contentDescription = farmString(R.string.diary_pick_gallery),
                    tint = Color.White,
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    speakDescription = if (useTe) "గ్యాలరీ ఫోటో" else "Pick photo",
                    onClick = onGalleryClick,
                )
            }
        }
    }
}

@Composable
private fun QuickContactCallCard(
    contact: Contact,
    isPlayingVoice: Boolean,
    useTe: Boolean,
    onSpeakClick: () -> Unit,
    onDialClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(196.dp),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(modifier = Modifier.padding(FarmSpacing.dp12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                Image(
                    painter = painterResource(FarmImages.forContactType(contact.contactType)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(42.dp).clip(FarmShapeTokens.circular),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.MainText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = FarmLabels.contactType(contact.contactType, if (useTe) AppLanguage.TELUGU else AppLanguage.ENGLISH),
                        style = FarmTypography.bodySmall,
                        color = FarmColors.SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(FarmSpacing.dp8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FarmIconButton(
                    icon = if (isPlayingVoice) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = farmString(R.string.common_listen),
                    tint = if (isPlayingVoice) Color.White else FarmColors.OnPrimaryContainer,
                    containerColor = if (isPlayingVoice) FarmColors.PrimaryForest else FarmColors.PrimaryContainer,
                    onClick = onSpeakClick,
                )
                Surface(
                    modifier =
                    Modifier
                        .weight(1f)
                        .height(FarmDimensions.minTouchTarget)
                        .clip(FarmShapeTokens.inputField)
                        .farmCombinedClickable(
                            role = Role.Button,
                            speakLabel = if (useTe) "${contact.name} కు ఫోన్" else "Call ${contact.name}",
                            onLongPressSpeak =
                            rememberHoldToSpeakAction(
                                if (useTe) "${contact.name} కు ఫోన్" else "Call ${contact.name}",
                            ),
                            onClick = onDialClick,
                        ),
                    shape = FarmShapeTokens.inputField,
                    color = FarmColors.PrimaryForest,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = FarmSpacing.dp8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(FarmSpacing.dp4))
                        Text(
                            text = if (useTe) "కాల్" else "Call",
                            style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderBar(
    farmerName: String,
    todayFormatted: String,
    isAudioPlaying: Boolean,
    useTe: Boolean,
    onOpenContacts: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = FarmColors.Surface) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12),
        ) {
            val greeting =
                if (farmerName.isNotBlank()) {
                    farmString(R.string.home_greeting, farmerName)
                } else {
                    farmString(R.string.home_greeting_default)
                }
            Text(
                text = greeting,
                style = FarmTypography.headlineMedium,
                color = FarmColors.MainText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(FarmSpacing.dp4))
            Text(todayFormatted, style = FarmTypography.bodySmall, color = FarmColors.SecondaryText)
            Spacer(Modifier.height(FarmSpacing.dp8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                HeaderUtilityPill(
                    label = if (useTe) "ఫోన్లు" else "Calls",
                    icon = Icons.Rounded.Call,
                    speakText = if (useTe) "పరిచయాలకు ఫోన్" else "Phone Contacts",
                    onClick = onOpenContacts,
                    modifier = Modifier.weight(1f),
                )
                HeaderUtilityPill(
                    label = farmString(R.string.common_search),
                    icon = Icons.Rounded.Search,
                    speakText = if (useTe) "వెతకండి" else "Search",
                    onClick = onOpenSearch,
                    modifier = Modifier.weight(1f),
                )
                HeaderUtilityPill(
                    label = if (useTe) "సెట్టింగ్స్" else "Settings",
                    icon = Icons.Rounded.Settings,
                    speakText = if (useTe) "సెట్టింగ్స్ & బ్యాకప్" else "Settings and backup",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                )
                FarmAudioButton(
                    audioCueKey = AudioCueCatalog.HOME_SUMMARY,
                    isPlaying = isAudioPlaying,
                    onTogglePlay = { onToggleAudio() },
                    label = farmString(R.string.common_listen),
                    contentDescription = farmString(R.string.common_listen),
                    speakDescription = if (useTe) "సహాయం వినండి" else "Listen guide",
                    modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderUtilityPill(
    label: String,
    icon: ImageVector,
    speakText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onHoldToSpeak = rememberHoldToSpeakAction(speakText)
    Surface(
        modifier =
        modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(FarmShapeTokens.inputField)
            .border(1.dp, FarmColors.Outline, FarmShapeTokens.inputField)
            .farmCombinedClickable(
                role = Role.Button,
                speakLabel = speakText,
                onLongPressSpeak = onHoldToSpeak,
                onClick = onClick,
            ),
        shape = FarmShapeTokens.inputField,
        color = FarmColors.SurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FarmColors.PrimaryForest,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = FarmTypography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = FarmColors.MainText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BackupSafetyBanner(
    useTe: Boolean,
    onOpenBackup: () -> Unit,
) {
    val title = if (useTe) "డేటా భద్రత: బ్యాకప్ తీసుకోండి" else "Back up your farm records"
    val body =
        if (useTe) {
            "మీ లెక్కలు ఫోన్‌లోనే ఉన్నాయి. ఫోన్ మారినా పోకుండా బ్యాకప్ ఫైల్ సేవ్ చేయండి."
        } else {
            "Keep a safe offline backup copy so your farm records are never lost."
        }
    val actionText = if (useTe) "బ్యాకప్" else "Backup"
    val holdSpeak = rememberHoldToSpeakAction("$title. $body")

    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(FarmShapeTokens.card)
            .border(1.dp, FarmColors.HarvestAmber, FarmShapeTokens.card)
            .farmCombinedClickable(
                role = Role.Button,
                speakLabel = "$title. $body",
                onLongPressSpeak = holdSpeak,
                onClick = onOpenBackup,
            ),
        shape = FarmShapeTokens.card,
        color = FarmColors.AmberContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            Icon(
                imageVector = Icons.Rounded.Backup,
                contentDescription = null,
                tint = FarmColors.Warning,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FarmColors.MainText,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    style = FarmTypography.bodySmall,
                    color = FarmColors.OnAmberContainer,
                )
            }
            Surface(
                shape = FarmShapeTokens.inputField,
                color = FarmColors.PrimaryForest,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp8),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = actionText,
                        style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.OnPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TwoColActionRow(
    first: @Composable RowScope.() -> Unit,
    second: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
    ) {
        first()
        second()
    }
}

@Composable
private fun ActiveMachineryBanner(
    session: MachinerySession,
    elapsedSecondsProvider: () -> Long,
    onClick: () -> Unit,
    useTe: Boolean,
) {
    var elapsedSecs by remember(session.id) { mutableLongStateOf(elapsedSecondsProvider()) }
    LaunchedEffect(session.id) {
        while (true) {
            elapsedSecs = elapsedSecondsProvider()
            delay(1_000L)
        }
    }
    val hrs = elapsedSecs / 3600
    val mins = (elapsedSecs % 3600) / 60
    val secs = elapsedSecs % 60
    val timeText = "%02d:%02d:%02d".format(hrs, mins, secs)

    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = FarmShapeTokens.card,
        color = FarmColors.AmberContainer,
        border = BorderStroke(1.5.dp, FarmColors.HarvestAmber),
    ) {
        Row(
            modifier = Modifier.padding(FarmSpacing.dp16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = FarmColors.Warning)
            Spacer(Modifier.width(FarmSpacing.dp12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${session.machineName} • ${session.operationType}",
                    style = FarmTypography.titleMedium,
                    color = FarmColors.MainText,
                )
                Text(
                    text = if (useTe) "నడుస్తోంది — ఆపడానికి నొక్కండి" else "Running — tap to stop & bill",
                    style = FarmTypography.bodySmall,
                    color = FarmColors.Warning,
                )
            }
            Text(
                text = timeText,
                style = FarmTypography.titleLarge.copy(fontFeatureSettings = "tnum"),
                color = FarmColors.MainText,
            )
        }
    }
}
