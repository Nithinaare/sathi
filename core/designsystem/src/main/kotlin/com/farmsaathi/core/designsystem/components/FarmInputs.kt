package com.farmsaathi.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.localization.LocalAppLanguage
import com.farmsaathi.core.designsystem.localization.safeFarmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import java.util.Calendar
import java.util.Locale

data class DropdownOption<T>(
    val value: T,
    val label: String,
    val supportingText: String? = null,
    val icon: ImageVector? = null,
)

@Composable
private fun farmTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FarmColors.PrimaryForest,
        unfocusedBorderColor = FarmColors.Outline,
        errorBorderColor = FarmColors.Error,
        focusedContainerColor = FarmColors.Surface,
        unfocusedContainerColor = FarmColors.Surface,
        disabledContainerColor = FarmColors.SurfaceVariant,
        focusedTextColor = FarmColors.MainText,
        unfocusedTextColor = FarmColors.MainText,
        focusedLabelColor = FarmColors.PrimaryForest,
        unfocusedLabelColor = FarmColors.SecondaryText,
        cursorColor = FarmColors.PrimaryForest,
    )

@Composable
private fun ExternalFieldLabel(label: String) {
    Text(
        text = label,
        style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = FarmColors.SecondaryText,
    )
    Spacer(Modifier.height(FarmSpacing.dp4))
}

@Composable
fun FarmTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    helperText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExternalFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = FarmTypography.bodyLarge, color = FarmColors.SecondaryText) },
            isError = !errorText.isNullOrBlank(),
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            textStyle = FarmTypography.bodyLarge,
            shape = FarmShapeTokens.inputField,
            colors = farmTextFieldColors(),
            keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions =
            KeyboardActions(
                onAny = { onImeAction?.invoke() },
            ),
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics { contentDescription = label },
        )
        FieldSupportingMessage(errorText = errorText, helperText = helperText)
    }
}

@Composable
fun FarmAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    errorText: String? = null,
    helperText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExternalFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val filtered = sanitizeDecimalInput(raw, maxDecimals = 2)
                if (filtered != null) onValueChange(filtered)
            },
            placeholder = { Text(placeholder, style = FarmTypography.headlineMedium, color = FarmColors.Disabled) },
            prefix = {
                Text(
                    text = "₹ ",
                    style = FarmTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = FarmColors.PrimaryForest,
                )
            },
            isError = !errorText.isNullOrBlank(),
            singleLine = true,
            enabled = enabled,
            textStyle = FarmTypography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            shape = FarmShapeTokens.inputField,
            colors = farmTextFieldColors(),
            keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics { contentDescription = label },
        )
        FieldSupportingMessage(errorText = errorText, helperText = helperText)
    }
}

@Composable
fun FarmNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    unitSuffix: String? = null,
    placeholder: String = "0",
    allowDecimal: Boolean = true,
    errorText: String? = null,
    helperText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExternalFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val filtered =
                    if (allowDecimal) {
                        sanitizeDecimalInput(raw, maxDecimals = 3)
                    } else {
                        raw.filter { it.isDigit() }
                    }
                if (filtered != null) onValueChange(filtered)
            },
            placeholder = { Text(placeholder, style = FarmTypography.titleLarge, color = FarmColors.Disabled) },
            suffix =
            unitSuffix?.let { suffix ->
                {
                    Text(
                        text = " $suffix",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FarmColors.SecondaryText,
                    )
                }
            },
            isError = !errorText.isNullOrBlank(),
            singleLine = true,
            enabled = enabled,
            textStyle = FarmTypography.titleLarge.copy(lineHeight = 24.sp, fontFeatureSettings = "tnum"),
            shape = FarmShapeTokens.inputField,
            colors = farmTextFieldColors(),
            keyboardOptions =
            KeyboardOptions(
                keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics { contentDescription = label },
        )
        FieldSupportingMessage(errorText = errorText, helperText = helperText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmDatePicker(
    selectedDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    label: String,
    formattedDate: String,
    modifier: Modifier = Modifier,
    showQuickTodayYesterday: Boolean = true,
    todayLabel: String = "ఈరోజు",
    yesterdayLabel: String = "నిన్న",
    confirmLabel: String = "OK",
    dismissLabel: String = "Cancel",
    errorText: String? = null,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ExternalFieldLabel(label)
        Surface(
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(enabled = enabled) { showDialog = true },
            shape = FarmShapeTokens.inputField,
            color = if (enabled) FarmColors.Surface else FarmColors.SurfaceVariant,
            border = BorderStroke(1.dp, if (errorText != null) FarmColors.Error else FarmColors.Outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formattedDate,
                    style = FarmTypography.bodyLarge,
                    color = FarmColors.MainText,
                )
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = label,
                    tint = FarmColors.PrimaryForest,
                    modifier = Modifier.size(FarmDimensions.iconStandard),
                )
            }
        }
        if (showQuickTodayYesterday && enabled) {
            Spacer(Modifier.height(FarmSpacing.dp8))
            val now = System.currentTimeMillis()
            val oneDayMs = 86_400_000L
            Row(horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                FilterChip(
                    selected = selectedDateMillis != null && kotlin.math.abs(selectedDateMillis - now) < oneDayMs / 2,
                    onClick = { onDateSelected(now) },
                    label = { Text(todayLabel, style = FarmTypography.bodySmall) },
                    colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmColors.PrimaryContainer,
                        selectedLabelColor = FarmColors.OnPrimaryContainer,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                )
                FilterChip(
                    selected = false,
                    onClick = { onDateSelected(now - oneDayMs) },
                    label = { Text(yesterdayLabel, style = FarmTypography.bodySmall) },
                    modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                )
            }
        }
        FieldSupportingMessage(errorText = errorText, helperText = null)
    }

    if (showDialog) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let(onDateSelected)
                        showDialog = false
                    },
                ) {
                    Text(confirmLabel, color = FarmColors.PrimaryForest)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(dismissLabel, color = FarmColors.SecondaryText)
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FarmDropdown(
    label: String,
    options: List<DropdownOption<T>>,
    selectedValue: T?,
    onSelect: (DropdownOption<T>) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.value == selectedValue }

    Column(modifier = modifier.fillMaxWidth()) {
        ExternalFieldLabel(label)
        Surface(
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(enabled = enabled) { expanded = true },
            shape = FarmShapeTokens.inputField,
            color = if (enabled) FarmColors.Surface else FarmColors.SurfaceVariant,
            border = BorderStroke(1.dp, if (errorText != null) FarmColors.Error else FarmColors.Outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedOption?.icon != null) {
                    Icon(
                        imageVector = selectedOption.icon,
                        contentDescription = null,
                        tint = FarmColors.PrimaryForest,
                        modifier = Modifier.size(FarmDimensions.iconStandard),
                    )
                    Spacer(Modifier.width(FarmSpacing.dp12))
                }
                Text(
                    text = selectedOption?.label ?: placeholder,
                    style = FarmTypography.bodyLarge,
                    color = if (selectedOption != null) FarmColors.MainText else FarmColors.SecondaryText,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = label,
                    tint = FarmColors.SecondaryText,
                )
            }
        }
        FieldSupportingMessage(errorText = errorText, helperText = null)
    }

    if (expanded) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = sheetState,
            containerColor = FarmColors.Surface,
        ) {
            Column(modifier = Modifier.padding(bottom = FarmSpacing.dp24)) {
                Text(
                    text = label,
                    style = FarmTypography.titleLarge,
                    color = FarmColors.MainText,
                    modifier = Modifier.padding(horizontal = FarmSpacing.dp20, vertical = FarmSpacing.dp12),
                )
                LazyColumn {
                    items(options) { option ->
                        val isSelected = option.value == selectedValue
                        FarmListItem(
                            title = option.label,
                            subtitle = option.supportingText,
                            leadingIcon = option.icon,
                            leadingTint = if (isSelected) FarmColors.PrimaryForest else FarmColors.SecondaryText,
                            selected = isSelected,
                            modifier = Modifier.semantics { selected = isSelected },
                            trailingContent = {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) FarmColors.PrimaryForest else FarmColors.SecondaryText,
                                )
                            },
                            onClick = {
                                onSelect(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmTimeSlotPicker(
    label: String,
    epochMillis: Long,
    onEpochMillisChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTelugu = LocalAppLanguage.current == AppLanguage.TELUGU
    val cal = remember(epochMillis) { Calendar.getInstance().apply { timeInMillis = epochMillis } }
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = cal.get(Calendar.MINUTE)
    val formattedTime = formatTimeSlotDisplay(currentHour, currentMinute, isTelugu)

    val morningLabel = safeFarmString(R.string.time_morning_slot)
    val afternoonLabel = safeFarmString(R.string.time_afternoon_slot)
    val eveningLabel = safeFarmString(R.string.time_evening_slot)
    val minus30Label = safeFarmString(R.string.time_minus_30m)
    val plus30Label = safeFarmString(R.string.time_plus_30m)

    val chipColors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = FarmColors.PrimaryContainer,
            selectedLabelColor = FarmColors.OnPrimaryContainer,
        )

    Column(modifier = modifier.fillMaxWidth()) {
        ExternalFieldLabel(label)
        Surface(
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp),
            shape = FarmShapeTokens.inputField,
            color = FarmColors.Surface,
            border = BorderStroke(1.dp, FarmColors.Outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = label,
                        tint = FarmColors.PrimaryForest,
                        modifier = Modifier.size(FarmDimensions.iconStandard),
                    )
                    Spacer(Modifier.width(FarmSpacing.dp12))
                    Text(
                        text = formattedTime,
                        style = FarmTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        color = FarmColors.MainText,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    FilterChip(
                        selected = false,
                        onClick = { onEpochMillisChange(shiftTimeMinutes(epochMillis, -30)) },
                        label = { Text(minus30Label, style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold)) },
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onEpochMillisChange(shiftTimeMinutes(epochMillis, 30)) },
                        label = { Text(plus30Label, style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold)) },
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
            }
        }
        Spacer(Modifier.height(FarmSpacing.dp8))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            listOf(
                Triple(morningLabel, 7, 0),
                Triple(afternoonLabel, 13, 0),
                Triple(eveningLabel, 17, 30),
            ).forEach { (slotLabel, hour, minute) ->
                val isSelected = currentHour == hour && currentMinute == minute
                FilterChip(
                    selected = isSelected,
                    onClick = { onEpochMillisChange(withHourMinute(epochMillis, hour, minute)) },
                    label = { Text(slotLabel, style = FarmTypography.bodySmall) },
                    colors = chipColors,
                    modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                )
            }
        }
    }
}

@Composable
fun FarmSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearchAction: (() -> Unit)? = null,
    clearContentDescription: String = safeFarmString(R.string.common_clear_search),
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, style = FarmTypography.bodyLarge, color = FarmColors.SecondaryText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = FarmColors.SecondaryText,
            )
        },
        trailingIcon =
        if (query.isNotEmpty()) {
            {
                FarmIconButton(
                    icon = Icons.Rounded.Clear,
                    contentDescription = clearContentDescription,
                    onClick = { onQueryChange("") },
                )
            }
        } else {
            null
        },
        singleLine = true,
        textStyle = FarmTypography.bodyLarge,
        shape = FarmShapeTokens.inputField,
        colors = farmTextFieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction?.invoke() }),
        modifier =
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp),
    )
}

@Composable
private fun FieldSupportingMessage(
    errorText: String?,
    helperText: String?,
) {
    if (!errorText.isNullOrBlank()) {
        Spacer(Modifier.height(FarmSpacing.dp4))
        Box(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Text(
                text = errorText,
                style = FarmTypography.bodySmall,
                color = FarmColors.Error,
            )
        }
    } else if (!helperText.isNullOrBlank()) {
        Spacer(Modifier.height(FarmSpacing.dp4))
        Text(
            text = helperText,
            style = FarmTypography.bodySmall,
            color = FarmColors.SecondaryText,
        )
    }
}

private fun sanitizeDecimalInput(
    raw: String,
    maxDecimals: Int,
): String? {
    var dotSeen = false
    var decimals = 0
    val sb = StringBuilder(raw.length)
    for (ch in raw) {
        if (ch == '.' && !dotSeen) {
            dotSeen = true
            if (sb.isEmpty()) sb.append('0')
            sb.append('.')
        } else if (ch.isDigit() && (!dotSeen || decimals < maxDecimals)) {
            if (dotSeen) decimals++
            sb.append(ch)
        } else {
            return null
        }
    }
    return sb.toString()
}

private fun withHourMinute(
    epochMillis: Long,
    hourOfDay: Int,
    minute: Int,
): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun shiftTimeMinutes(
    epochMillis: Long,
    deltaMinutes: Int,
): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val totalMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val nextMinutes = (totalMinutes + deltaMinutes).mod(24 * 60)
    return cal.apply {
        set(Calendar.HOUR_OF_DAY, nextMinutes / 60)
        set(Calendar.MINUTE, nextMinutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatTimeSlotDisplay(
    hourOfDay: Int,
    minute: Int,
    isTelugu: Boolean,
): String {
    val hour12 =
        when (val h = hourOfDay % 12) {
            0 -> 12
            else -> h
        }
    val hhmm = String.format(Locale.US, "%02d:%02d", hour12, minute)
    return if (isTelugu) {
        val period =
            when (hourOfDay) {
                in 4..11 -> "ఉదయం"
                in 12..15 -> "మధ్యాహ్నం"
                in 16..19 -> "సాయంత్రం"
                else -> "రాత్రి"
            }
        "$period $hhmm"
    } else {
        val amPm = if (hourOfDay < 12) "AM" else "PM"
        "$hhmm $amPm"
    }
}
