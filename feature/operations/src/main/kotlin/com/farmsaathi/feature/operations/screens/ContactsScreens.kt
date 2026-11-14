package com.farmsaathi.feature.operations.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmAudioButton
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmImageGridPicker
import com.farmsaathi.core.designsystem.components.FarmImagePickerOption
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmMoreDetailsAccordion
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSearchField
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmVoiceNameBadge
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.feature.operations.viewmodel.FieldOpsAndContactsViewModel

@Composable
fun ContactListScreen(
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    onOpenContact: (Long) -> Unit,
    onAddContact: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val context = LocalContext.current

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (useTe) "పరిచయాలు" else farmString(R.string.con_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.CONTACTS_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.CONTACTS_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.con_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = onAddContact,
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FarmSearchField(
                query = state.contactQuery,
                onQueryChange = viewModel::setContactQuery,
                placeholder = farmString(R.string.common_search),
                modifier = Modifier.padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp8),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = FarmSpacing.dp16),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                item(key = "contact_filter_all", contentType = "contact_filter_chip") {
                    FilterChip(
                        selected = state.contactFilter == null,
                        onClick = { viewModel.setContactFilter(null) },
                        label = { Text(farmString(R.string.common_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryContainer,
                            selectedLabelColor = FarmColors.PrimaryForest,
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
                items(
                    ContactType.entries,
                    key = { it.name },
                    contentType = { "contact_filter_chip" },
                ) { type ->
                    FilterChip(
                        selected = state.contactFilter == type,
                        onClick = { viewModel.setContactFilter(type) },
                        label = { Text(FarmLabels.contactType(type, lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryContainer,
                            selectedLabelColor = FarmColors.PrimaryForest,
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
            }

            if (state.contacts.isEmpty()) {
                FarmEmptyState(
                    title = if (useTe) "పరిచయాలు" else farmString(R.string.con_title),
                    message =
                    if (useTe) {
                        "ట్రాక్టర్, మందుల షాప్, వ్యాపారుల నంబర్లు సేవ్ చేయండి. కింద ఉన్న బటన్ నొక్కండి."
                    } else {
                        "Save tractor owners, shops, and traders for 1-tap calling."
                    },
                    icon = Icons.Rounded.Person,
                    imageRes = FarmImages.contacts,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(FarmSpacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    items(
                        state.contacts,
                        key = { it.id },
                        contentType = { "contact_record_card" },
                    ) { contact ->
                        val roleLabel = FarmLabels.contactType(contact.contactType, lang)
                        FarmRecordCard(
                            title = contact.name,
                            subtitle = roleLabel,
                            metadata = contact.phone,
                            leadingIcon = Icons.Rounded.Person,
                            leadingImageRes = FarmImages.forContactType(contact.contactType),
                            speakDescription =
                            if (useTe) {
                                "${contact.name}, $roleLabel. వివరాలకు నొక్కండి."
                            } else {
                                "${contact.name}, $roleLabel. Tap for details."
                            },
                            onClick = { onOpenContact(contact.id) },
                            trailingContent = {
                                FarmVoiceNameBadge(
                                    hasVoiceRecording = !contact.voiceNamePath.isNullOrBlank(),
                                    isPlaying =
                                    contact.voiceNamePath != null &&
                                        state.playingVoicePath == contact.voiceNamePath,
                                    isRecording = false,
                                    onPlayClick = {
                                        viewModel.togglePlayVoicePath(contact.voiceNamePath, contact.name)
                                    },
                                    onRecordClick = {},
                                    useTelugu = useTe,
                                    showRecordAction = false,
                                )
                                if (!contact.phone.isNullOrBlank()) {
                                    Spacer(Modifier.width(FarmSpacing.dp8))
                                    FarmIconButton(
                                        icon = Icons.Rounded.Call,
                                        contentDescription = farmString(R.string.con_call_btn),
                                        tint = FarmColors.OnPrimary,
                                        containerColor = FarmColors.PrimaryForest,
                                        speakDescription =
                                        if (useTe) {
                                            "${contact.name} కు ఫోన్ చేయడానికి ఈ ఆకుపచ్చ బటన్ నొక్కండి."
                                        } else {
                                            "Tap green button to call ${contact.name}."
                                        },
                                        onClick = {
                                            val dialIntent =
                                                Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.parse("tel:${contact.phone}"),
                                                )
                                            context.startActivity(dialIntent)
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactDetailScreen(
    contactId: Long,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contact = state.contacts.firstOrNull { it.id == contactId }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val context = LocalContext.current
    var showArchive by remember { mutableStateOf(false) }

    if (contact == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }
    val roleLabel = FarmLabels.contactType(contact.contactType, lang)

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = contact.name,
                subtitle = roleLabel,
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                trailingAction = {
                    Row {
                        FarmIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = farmString(R.string.common_edit),
                            onClick = { onEdit(contactId) },
                        )
                        FarmIconButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = if (useTe) "పరిచయం తీసేయండి" else farmString(R.string.common_archive),
                            tint = FarmColors.Error,
                            onClick = { showArchive = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                Image(
                    painter = painterResource(FarmImages.forContactType(contact.contactType)),
                    contentDescription = roleLabel,
                    contentScale = ContentScale.Crop,
                    modifier =
                    Modifier
                        .size(64.dp)
                        .clip(FarmShapeTokens.card)
                        .border(1.dp, FarmColors.Outline, FarmShapeTokens.card),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(contact.name, style = FarmTypography.titleLarge, color = FarmColors.MainText)
                    Text(roleLabel, style = FarmTypography.bodyMedium, color = FarmColors.SecondaryText)
                }
                FarmAudioButton(
                    audioCueKey = "contact_detail_${contact.id}",
                    isPlaying = state.isAudioPlaying,
                    onTogglePlay = {
                        viewModel.speakContactDetails(contact.name, roleLabel, contact.phone)
                    },
                    label = if (useTe) "వినండి" else "Listen",
                    contentDescription = if (useTe) "వివరాలు వినండి" else "Listen to contact details",
                )
            }
            contact.phone?.let { phone ->
                Text(phone, style = FarmTypography.headlineMedium, color = FarmColors.MainText)
                FarmPrimaryButton(
                    text = farmString(R.string.con_call_btn),
                    leadingIcon = Icons.Rounded.Call,
                    isHero = true,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    },
                )
            }
            contact.villageOrMandi?.let { Text(it, style = FarmTypography.bodyLarge, color = FarmColors.SecondaryText) }
            contact.notes?.let { Text(it, style = FarmTypography.bodyLarge, color = FarmColors.MainText) }
        }
    }

    if (showArchive) {
        FarmConfirmationDialog(
            title = if (useTe) "పరిచయం తీసేయాలా?" else "Archive contact?",
            message = if (useTe) "పాత బిల్లుల రికార్డులు పోవు." else "Linked transaction history stays safe.",
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                showArchive = false
                viewModel.archiveContact(contactId, onBack)
            },
            onDismiss = { showArchive = false },
        )
    }
}

@Composable
fun ContactFormScreen(
    contactId: Long?,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing =
        remember(contactId, state.contacts) {
            contactId?.let { id -> state.contacts.firstOrNull { it.id == id } }
        }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val context = LocalContext.current

    val primaryRoles =
        remember {
            listOf(
                ContactType.TRACTOR_OWNER,
                ContactType.FERTILIZER_SHOP,
                ContactType.LABOUR_CONTRACTOR,
                ContactType.BUYER_TRADER,
            )
        }
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember(existing) { mutableStateOf(existing?.phone.orEmpty()) }
    var type by remember(existing) { mutableStateOf(existing?.contactType ?: ContactType.TRACTOR_OWNER) }
    var showAllRoles by remember(existing) {
        mutableStateOf(existing != null && existing.contactType !in primaryRoles)
    }
    var village by remember(existing) { mutableStateOf(existing?.villageOrMandi.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var voiceNamePath by remember(existing) { mutableStateOf(existing?.voiceNamePath) }
    var showMore by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var bannerError by remember { mutableStateOf<String?>(null) }

    val micLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.toggleRecordContactVoice {
                    voiceNamePath = it
                    phoneError = null
                    bannerError = null
                }
            }
        }

    val phonebookPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@rememberLauncherForActivityResult
                try {
                    val projection =
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                        )
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val pickedName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                            val pickedPhone = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                            if (pickedName.isNotBlank() && name.isBlank()) name = pickedName
                            if (pickedPhone.isNotBlank()) {
                                phone = pickedPhone.replace(" ", "")
                                phoneError = null
                                bannerError = null
                            }
                        }
                    }
                } catch (_: SecurityException) {
                    bannerError =
                        if (useTe) {
                            "ఫోన్ బుక్ నుండి చదవలేకపోయాము. నంబర్ నేరుగా టైప్ చేయండి."
                        } else {
                            "Could not read contact details. Please enter number manually."
                        }
                } catch (_: Exception) {
                    bannerError =
                        if (useTe) {
                            "నంబర్ చదవడం కుదరలేదు. దయచేసి టైప్ చేయండి."
                        } else {
                            "Could not import selected contact."
                        }
                }
            }
        }

    val visibleTypes =
        remember(showAllRoles, primaryRoles) {
            if (showAllRoles) ContactType.entries else primaryRoles
        }
    val roleOptions =
        remember(lang, visibleTypes) {
            visibleTypes.map { ct ->
                FarmImagePickerOption(
                    value = ct,
                    label = FarmLabels.contactType(ct, lang),
                    imageRes = FarmImages.forContactType(ct),
                )
            }
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (existing == null) farmString(R.string.con_add_btn) else farmString(R.string.common_edit),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.CONTACT_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.CONTACT_FORM_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        if (name.isBlank() && phone.isBlank() && voiceNamePath.isNullOrBlank()) {
                            val msg =
                                if (useTe) {
                                    "ఫోన్ నంబర్ లేదా పేరు రాయండి (లేదా మాటలో పేరు రికార్డ్ చేయండి)"
                                } else {
                                    "Enter a phone number, name, or record a voice name"
                                }
                            phoneError = if (useTe) "ఫోన్ నంబర్ లేదా పేరు అవసరం" else "Phone or name required"
                            bannerError = msg
                            viewModel.playErrorCue()
                        } else {
                            phoneError = null
                            bannerError = null
                            viewModel.saveContact(
                                existing = existing,
                                name = name,
                                phone = phone,
                                type = type,
                                village = village,
                                notes = notes,
                                voiceNamePath = voiceNamePath,
                            ) { onBack() }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(FarmSpacing.dp16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            bannerError?.let { msg ->
                FarmStatusIndicator(
                    type = FarmStatusType.ERROR,
                    label = msg,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FarmImageGridPicker(
                label = if (useTe) "వీరు ఎవరు?" else "Category",
                options = roleOptions,
                selected = type,
                onSelect = { type = it },
                columns = 2,
            )
            FarmSecondaryButton(
                text =
                if (showAllRoles) {
                    if (useTe) "తక్కువ చూపించు" else "Show Fewer Roles"
                } else {
                    if (useTe) "ఇంకా చూపించు (మొత్తం 11 రకాలు)" else "Show All 11 Roles"
                },
                useMutedOutline = true,
                onClick = { showAllRoles = !showAllRoles },
            )
            FarmTextInput(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = null
                    bannerError = null
                },
                label = if (useTe) "ఫోన్ నంబర్" else "Phone Number",
                keyboardType = KeyboardType.Phone,
                errorText = phoneError,
            )
            FarmSecondaryButton(
                text = if (useTe) "ఫోన్ బుక్ నుండి ఎంచుకోండి" else "Pick from Phonebook",
                leadingIcon = Icons.Rounded.Contacts,
                useMutedOutline = true,
                onClick = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    phonebookPickerLauncher.launch(intent)
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                FarmTextInput(
                    value = name,
                    onValueChange = {
                        name = it
                        phoneError = null
                        bannerError = null
                    },
                    label = if (useTe) "పేరు (ఐచ్ఛికం)" else "Name (optional if phone/voice added)",
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FarmVoiceNameBadge(
                        hasVoiceRecording = !voiceNamePath.isNullOrBlank(),
                        isPlaying = voiceNamePath != null && state.playingVoicePath == voiceNamePath,
                        isRecording = state.isRecordingVoiceName,
                        onPlayClick = {
                            if (voiceNamePath.isNullOrBlank() && name.isBlank()) {
                                viewModel.speakBlankContactVoiceHint()
                            } else {
                                viewModel.togglePlayVoicePath(voiceNamePath, name)
                            }
                        },
                        onRecordClick = {
                            val ok =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                            if (ok) {
                                viewModel.toggleRecordContactVoice {
                                    voiceNamePath = it
                                    phoneError = null
                                    bannerError = null
                                }
                            } else {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        useTelugu = useTe,
                    )
                    Text(
                        text = if (useTe) "మాటలో పేరు రికార్డ్" else "Record Voice Name",
                        style = FarmTypography.labelSmall,
                        color = FarmColors.SecondaryText,
                    )
                }
            }
            FarmMoreDetailsAccordion(
                expanded = showMore,
                onToggle = { showMore = !showMore },
                useTelugu = useTe,
            ) {
                FarmTextInput(
                    value = village,
                    onValueChange = { village = it },
                    label = if (useTe) "ఊరు / మార్కెట్ (ఐచ్ఛికం)" else "Village / Mandi (optional)",
                )
                FarmTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = farmString(R.string.common_notes_optional),
                    singleLine = false,
                )
            }
        }
    }
}
