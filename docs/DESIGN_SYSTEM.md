# Farm Saathi — UI/UX Design System (Stage 1)

**Target Device:** Realme 5s (Android 10 / API 29+, 4 GB RAM, 360dp portrait width, 720×1600 HD+ LCD)  
**Environment:** Outdoor bright sunlight, one-handed thumb operation, 100% offline-first  
**Language Priority:** Conversational Rural Telugu (Telangana / Andhra Pradesh) primary, instant 1-tap English toggle  
**Theme Policy:** **Light theme only.** `dynamicColor = false` hardcoded (never inherit Android wallpaper tints).

---

## 1. Color Tokens (`FarmColors`)

### 1.1 Exact Palette & Contrast Rules

| Token | HEX | Compose Role | Outdoor / Contrast Usage Rule |
|---|---|---|---|
| `Primary` | `#245A3A` | Primary CTA fills, active tab indicator, key headings | 8.1:1 with `#FFFFFF` text (AAA). Safe for filled buttons & dark-on-light icons. |
| `PrimaryDark` | `#18432A` | Pressed / focused state for Primary CTA & active tile borders | High-contrast pressed feedback under sunlight. |
| `OnPrimary` | `#FFFFFF` | Text & icons on `Primary` / `Success` / `Error` / `Info` fills | Never use on `AccentAmber` (`#B7791F`). |
| `PrimaryContainer` | `#E2F0E5` | Selected chips, active farm pill, soft positive highlights | Always pair with `OnPrimaryContainer` (`#163C27`). |
| `OnPrimaryContainer` | `#163C27` | Text & icons inside `PrimaryContainer` | >10:1 contrast on `#E2F0E5`. |
| `Secondary` | `#637A43` | Secondary badges, crop/plot accents, subtle icons | Pair with `#FFFFFF` only for large icons/bold 18sp+ or use container pair. |
| `SecondaryContainer` | `#E9F0DD` | Plot tags, activity category chips, subtle info panels | Pair with `OnSecondaryContainer` (`#344326`). |
| `OnSecondaryContainer` | `#344326` | Text & icons inside `SecondaryContainer` | >8:1 contrast on `#E9F0DD`. |
| `AccentAmber` | `#B7791F` | Harvest/gold highlights, tile borders, pending/due accents | **CRITICAL:** 3.66:1 vs `#FFFFFF` — **never place white body text on `#B7791F`**. Use `#1E2B22` on `#F7EBD3`, or `#9A6200` for warning text on white. |
| `AccentContainer` | `#F7EBD3` | Pending dues banner, harvest/storage highlight card | Pair with `MainText` (`#1E2B22`) or `Warning` (`#9A6200`). |
| `Terracotta` | `#B65D42` | Soil/machinery/expense visual accent & border cue | Use for icons/borders/illustrations or dark-text containers; use `#B3261E` for error text. |
| `Background` | `#F7F8F2` | Screen scaffold background (warm paper cream, cuts glare) | Eliminates harsh pure-white screen bounce in direct sun. |
| `Surface` | `#FFFFFF` | Elevated cards, form inputs, dialogs, bottom bar | Always give cards a `1.dp` `#D5DED4` border over `#F7F8F2`. |
| `SurfaceVariant` | `#EEF1E8` | Unselected tiles, secondary input fill, list dividers/headers | Subtle separation without heavy shadows. |
| `MainText` | `#1E2B22` | Primary titles, body text, form input values, KPI numbers | ~14:1 on `#FFFFFF`, ~13:1 on `#F7F8F2`. |
| `SecondaryText` | `#536156` | Subtitles, metadata, field hints, inactive nav labels | ~6.4:1 on `#FFFFFF`, ~6.0:1 on `#F7F8F2` (passes AA for 14sp+). |
| `Outline` | `#D5DED4` | Card borders, input borders, horizontal dividers | Minimum `1.dp` stroke (`1.5.dp` when focused/selected). |
| `Disabled` | `#A2AAA1` | Disabled button text/icons, disabled borders | Pair disabled fill with `SurfaceVariant` (`#EEF1E8`) + clear reason text. |
| `Success` | `#2F6F44` | Paid, harvested, synced locally, good soil status | 5.78:1 with `#FFFFFF`. Always pair with check icon + text. |
| `Warning` | `#9A6200` | Payment due, irrigation due, low stock | 5.17:1 on `#FFFFFF`. Text/icon safe on white & `#F7EBD3`. |
| `Error` | `#B3261E` | Validation errors, destructive delete, overdue alert | 5.9:1 with `#FFFFFF`. Pair with alert icon + corrective text. |
| `Info` | `#2E6385` | Offline saved indicator, audio cue active, neutral tips | 6.03:1 with `#FFFFFF`. |

### 1.2 Kotlin Color & Material 3 Scheme

```kotlin
package com.farmsaathi.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object FarmColors {
    val Primary = Color(0xFF245A3A)
    val PrimaryDark = Color(0xFF18432A)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE2F0E5)
    val OnPrimaryContainer = Color(0xFF163C27)

    val Secondary = Color(0xFF637A43)
    val SecondaryContainer = Color(0xFFE9F0DD)
    val OnSecondaryContainer = Color(0xFF344326)

    val AccentAmber = Color(0xFFB7791F)
    val AccentContainer = Color(0xFFF7EBD3)
    val Terracotta = Color(0xFFB65D42)

    val Background = Color(0xFFF7F8F2)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEEF1E8)

    val MainText = Color(0xFF1E2B22)
    val SecondaryText = Color(0xFF536156)
    val Outline = Color(0xFFD5DED4)
    val Disabled = Color(0xFFA2AAA1)

    val Success = Color(0xFF2F6F44)
    val Warning = Color(0xFF9A6200)
    val Error = Color(0xFFB3261E)
    val Info = Color(0xFF2E6385)

    // Derived soft containers for semantic status pills (dark text/icon always used inside)
    val ErrorContainer = Color(0xFFFCE8E6)
    val InfoContainer = Color(0xFFE4EFF6)
}

val FarmLightColorScheme = lightColorScheme(
    primary = FarmColors.Primary,
    onPrimary = FarmColors.OnPrimary,
    primaryContainer = FarmColors.PrimaryContainer,
    onPrimaryContainer = FarmColors.OnPrimaryContainer,
    secondary = FarmColors.Secondary,
    onSecondary = FarmColors.OnPrimary,
    secondaryContainer = FarmColors.SecondaryContainer,
    onSecondaryContainer = FarmColors.OnSecondaryContainer,
    tertiary = FarmColors.AccentAmber,
    onTertiary = FarmColors.MainText,
    tertiaryContainer = FarmColors.AccentContainer,
    onTertiaryContainer = FarmColors.MainText,
    background = FarmColors.Background,
    onBackground = FarmColors.MainText,
    surface = FarmColors.Surface,
    onSurface = FarmColors.MainText,
    surfaceVariant = FarmColors.SurfaceVariant,
    onSurfaceVariant = FarmColors.SecondaryText,
    outline = FarmColors.Outline,
    outlineVariant = FarmColors.Outline,
    error = FarmColors.Error,
    onError = FarmColors.OnPrimary,
    errorContainer = FarmColors.ErrorContainer,
    onErrorContainer = FarmColors.Error
)
```

---

## 2. Typography Scale & Font Setup (`FarmTypography`)

### 2.1 Font Family Strategy (Zero APK Bloat)
- **Font Stack:** Use Android's `FontFamily.Default` (`FontFamily.SansSerif`), which resolves Latin/numerals to **Roboto** and Telugu script (`U+0C00–U+0C7F`) to Android 10's system **Noto Sans Telugu** automatically via `/system/etc/fonts.xml`.
- **Telugu Diacritic Protection:** Telugu top/bottom vowel signs (`గుడి`, `కొమ్ము`, `వత్తు`) clip if line heights are tight. Every token below maintains **+4sp to +8sp line-height headroom** and sets `PlatformTextStyle(includeFontPadding = true)` for safe vertical bounds.
- **Numeric Clarity:** Use `fontFeatureSettings = "tnum"` (tabular numerals) on all `₹` amounts, acres, dates, and weights so digits align cleanly.

### 2.2 Exact Scale Specification

| Role Token | M3 Slot | Size | Line Height | Weight | Usage |
|---|---|---|---|---|---|
| `Display` | `displayLarge` | `28.sp` | `36.sp` | `Bold (700)` | Hero KPI numbers (`₹42,500`, total acres, net balance) |
| `Heading` | `headlineMedium` | `24.sp` | `32.sp` | `Bold (700)` | Page header titles, onboarding titles |
| `Section` | `titleLarge` | `20.sp` | `28.sp` | `SemiBold (600)` | Section headings, dialog titles, empty/error titles |
| `Card` | `titleMedium` | `18.sp` | `26.sp` | `SemiBold (600)` | Card titles, primary CTA labels, action tile titles |
| `Body` | `bodyLarge` | `16.sp` | `24.sp` | `Normal (400)` | Primary body copy, input field values, list titles |
| `Secondary` | `bodyMedium` | `15.sp` | `22.sp` | `Medium (500)` | Subtitles, secondary button labels, dropdown labels |
| `Supporting` | `labelLarge` | `14.sp` | `20.sp` | `Medium (500)` | Helper text, status badge text, filter chips |
| `Metadata` | `labelSmall` | `12.sp` | `16.sp` | `Medium (500)` | Timestamps, bottom nav labels, audio duration `00:18` |

### 2.3 Kotlin Typography Definition

```kotlin
package com.farmsaathi.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val FarmLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun farmTextStyle(
    sizeSp: Int,
    lineHeightSp: Int,
    weight: FontWeight,
    tabularNums: Boolean = false
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    fontWeight = weight,
    fontFeatureSettings = if (tabularNums) "tnum" else null,
    platformStyle = PlatformTextStyle(includeFontPadding = true),
    lineHeightStyle = FarmLineHeightStyle
)

object FarmTextStyles {
    val Display = farmTextStyle(28, 36, FontWeight.Bold, tabularNums = true)
    val Heading = farmTextStyle(24, 32, FontWeight.Bold)
    val Section = farmTextStyle(20, 28, FontWeight.SemiBold)
    val Card = farmTextStyle(18, 26, FontWeight.SemiBold)
    val Body = farmTextStyle(16, 24, FontWeight.Normal)
    val Secondary = farmTextStyle(15, 22, FontWeight.Medium)
    val Supporting = farmTextStyle(14, 20, FontWeight.Medium)
    val Metadata = farmTextStyle(12, 16, FontWeight.Medium, tabularNums = true)
    val NumericValue = farmTextStyle(18, 26, FontWeight.Bold, tabularNums = true)
}

val FarmTypography = Typography(
    displayLarge = FarmTextStyles.Display,
    headlineMedium = FarmTextStyles.Heading,
    titleLarge = FarmTextStyles.Section,
    titleMedium = FarmTextStyles.Card,
    bodyLarge = FarmTextStyles.Body,
    bodyMedium = FarmTextStyles.Secondary,
    labelLarge = FarmTextStyles.Supporting,
    labelSmall = FarmTextStyles.Metadata
)
```

---

## 3. Spacing, Shapes & Touch Targets (`FarmDimens`, `FarmShapes`)

### 3.1 Spacing Scale (4dp Base)

| Token | Value | Primary Usage |
|---|---|---|
| `SpaceXs` | `4.dp` | Tight icon-to-badge gap, vertical micro separation |
| `SpaceSm` | `8.dp` | Between label and input, inside chips, between row elements |
| `SpaceMd` | `12.dp` | Grid gutter between 2-column action tiles, vertical list item spacing |
| `SpaceLg` | `16.dp` | Standard screen horizontal padding, internal card padding |
| `SpaceXl` | `20.dp` | Dialog internal padding, summary card vertical padding |
| `SpaceXxl` | `24.dp` | Between major sections on a screen |
| `SpaceXxxl` | `32.dp` | Top/bottom empty-state breathing room |
| `SpaceSection` | `40.dp` | Onboarding hero separation, bottom scroll clearance above sticky CTA |

### 3.2 Shape Tokens

| Token | Radius | Components |
|---|---|---|
| `TileLarge` | `20.dp` | Home hero quick-action tiles, Onboarding choice tiles |
| `Tile` | `18.dp` | Secondary grid action tiles, `FarmVoiceRecorder` bar |
| `Card` | `16.dp` | `FarmSummaryCard`, `FarmRecordCard`, `FarmConfirmationDialog`, `FarmSnackbar` |
| `Field` | `12.dp` | `FarmPrimaryButton`, `FarmSecondaryButton`, all inputs, dropdowns, search field |
| `Tag` | `8.dp` | `FarmStatusIndicator`, category pills, audio duration chip, photo thumbnails |

### 3.3 Touch Targets & Icon Sizes (Realme 5s 360dp Ergonomics)

| Element | Minimum Size | Rule |
|---|---|---|
| Any tappable control | `48.dp × 48.dp` | Enforce via `Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)` |
| Primary CTA (`FarmPrimaryButton`) | `56.dp`–`64.dp` height | `56.dp` default inside forms; `64.dp` on Onboarding / primary bottom action |
| Primary Action Tile (`FarmActionTile`) | `64.dp`–`80.dp` height | `80.dp` for 2-column Home grid tiles; `64.dp` for horizontal list-style tiles |
| Main / Tile Icons | `32.dp`–`40.dp` | `36.dp` default in `FarmActionTile` and state illustrations (`48.dp` in Empty/Error hero) |
| Standard Icons | `24.dp` | Bottom nav, back button, list trailing icons, status icons (`16.dp–20.dp` inside small tags) |
| Bottom Navigation Bar | `72.dp` height | Roomy for `24.dp` icon + `12.sp` Telugu label with zero clipping |
| Card / Border Stroke | `1.dp` (`1.5.dp` selected) | High-sunlight separation using `FarmColors.Outline` (`#D5DED4`) |

```kotlin
package com.farmsaathi.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object FarmDimens {
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 20.dp
    val SpaceXxl = 24.dp
    val SpaceXxxl = 32.dp
    val SpaceSection = 40.dp

    val MinTouchTarget = 48.dp
    val ButtonHeight = 56.dp
    val ButtonHeroHeight = 64.dp
    val TileCompactHeight = 64.dp
    val TileHeroHeight = 80.dp
    val BottomNavHeight = 72.dp

    val IconStandard = 24.dp
    val IconStatus = 18.dp
    val IconTileMain = 36.dp
    val IconHero = 48.dp

    val BorderWidth = 1.dp
    val BorderSelectedWidth = 1.5.dp
}

object FarmShapes {
    val TileLarge = RoundedCornerShape(20.dp)
    val Tile = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(16.dp)
    val Field = RoundedCornerShape(12.dp)
    val Tag = RoundedCornerShape(8.dp)

    val MaterialShapes = Shapes(
        extraSmall = Tag,
        small = Field,
        medium = Card,
        large = Tile,
        extraLarge = TileLarge
    )
}
```

---

## 4. Jetpack Compose Component API Contracts

**Package:** `com.farmsaathi.core.designsystem.components`  
All components below consume `FarmTheme` (`FarmLightColorScheme`, `FarmTypography`, `FarmShapes.MaterialShapes`) and include explicit `Modifier = Modifier` as the first optional parameter.

### 4.1 Supporting UI Models & Enums

```kotlin
package com.farmsaathi.core.designsystem.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FarmStatusType {
    SUCCESS,   // Paid, Completed, Harvested, Active, Good
    WARNING,   // Due, Low Stock, Irrigation Needed
    ERROR,     // Overdue, Deficit, Failed
    INFO,      // Saved Offline, Audio Ready, Tip
    NEUTRAL    // Draft, Archived, Inactive
}

enum class FarmBadgeStyle {
    FILLED_SOFT, // Soft tinted container + dark high-contrast text/icon (default)
    OUTLINED     // White surface + 1.5dp semantic border + dark text/icon
}

data class DropdownOption<T>(
    val value: T,
    val label: String,
    val supportingText: String? = null,
    val icon: ImageVector? = null
)

data class FarmNavTab(
    val routeId: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val contentDescription: String = label
)

sealed interface VoiceRecordingState {
    data object Idle : VoiceRecordingState
    data class Recording(val elapsedMillis: Long, val amplitudeNormalized: Float = 0f) : VoiceRecordingState
    data class Recorded(val filePath: String, val durationMillis: Long) : VoiceRecordingState
}

enum class SnackbarTone { INFO, SUCCESS, WARNING, ERROR }
```

---

### 4.2 Buttons & Action Triggers

#### `FarmPrimaryButton`
- **Role:** Single dominant thumb CTA per screen/form (Save, Add Crop, Confirm).
- **Visual Spec:** `56.dp` height (`64.dp` if `isHero = true`), `FarmShapes.Field` (`12.dp`), `FarmColors.Primary` fill (`#245A3A`), `FarmColors.PrimaryDark` (`#18432A`) ripple/pressed, `FarmColors.OnPrimary` (`#FFFFFF`) text (`Card` 18sp SemiBold). Disabled uses `SurfaceVariant` fill + `Disabled` text.

```kotlin
@Composable
fun FarmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isHero: Boolean = false,
    isDestructive: Boolean = false // Uses FarmColors.Error (#B3261E) only for confirmed delete
)
```

#### `FarmSecondaryButton`
- **Role:** Secondary/cancel/export action next to or below `FarmPrimaryButton`.
- **Visual Spec:** `52.dp` height (min `48.dp`), `FarmShapes.Field` (`12.dp`), `FarmColors.Surface` (`#FFFFFF`) fill, `1.5.dp` border (`FarmColors.Primary` or `FarmColors.Outline`), `FarmColors.Primary` / `FarmColors.MainText` label (`Secondary` 15sp–16sp Medium).

```kotlin
@Composable
fun FarmSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    useMutedOutline: Boolean = false
)
```

#### `FarmActionTile`
- **Role:** Large icon-first farmer tile (Home 2-column grid, More hub, Onboarding choices).
- **Visual Spec:** `80.dp` height default (`64.dp` compact row mode), `FarmShapes.TileLarge` (`20.dp`) or `FarmShapes.Tile` (`18.dp`), `36.dp` (`32–40dp`) top/leading icon, `1.dp` `#D5DED4` border (or `1.5.dp` accent border + soft container tint). Max 2 lines Telugu title (`Card` 18sp or `Body` 16sp SemiBold) + optional 1-line subtitle (`Supporting` 14sp).

```kotlin
@Composable
fun FarmActionTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color = FarmColors.Surface,
    iconTint: Color = FarmColors.Primary,
    borderColor: Color = FarmColors.Outline,
    compact: Boolean = false,
    badgeText: String? = null,
    enabled: Boolean = true
)
```

#### `FarmIconButton`
- **Role:** Back arrow, edit, delete, filter, add-photo icon actions.
- **Visual Spec:** Mandatory `48.dp × 48.dp` minimum touch box (`24.dp` icon centered). Supports optional subtle `SurfaceVariant` circle/rounded fill for outdoor visibility.

```kotlin
@Composable
fun FarmIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FarmColors.MainText,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true
)
```

#### `FarmAudioButton`
- **Role:** Tap-to-hear spoken Telugu/English summary or instruction for low-literacy farmers (uses local offline TTS / bundled Stage 1 audio cue key — **no ASR**).
- **Visual Spec:** `48.dp` min height pill or square-rounded button (`FarmShapes.Field` / Circle). Idle: `PrimaryContainer` (`#E2F0E5`) + speaker icon (`24.dp`) + optional short label ("వినండి" / "Listen"). Playing: `Primary` (`#245A3A`) fill + stop/pause icon + `#FFFFFF` text + live `stateDescription` for TalkBack.

```kotlin
@Composable
fun FarmAudioButton(
    audioCueKey: String,
    isPlaying: Boolean,
    onTogglePlay: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null, // Null renders 48dp compact speaker button; non-null renders pill
    contentDescription: String
)
```

---

### 4.3 Headers, Cards & Status Display

#### `FarmPageHeader`
- **Role:** Top bar for every screen (replaces cluttered Material top bars).
- **Visual Spec:** `FarmColors.Background` (`#F7F8F2`) or `FarmColors.Surface` with `1.dp` bottom outline. Left optional `48.dp` back `FarmIconButton`, center-left `Heading` (`24sp`) or `Section` (`20sp`) title + optional 1-line subtitle (`SecondaryText`), right optional `FarmAudioButton` + single action icon. Marked `semantics { heading() }`.

```kotlin
@Composable
fun FarmPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null
)
```

#### `FarmSectionHeader`
- **Role:** Separates logical groups on Home, Plot Detail, Accounts, More.
- **Visual Spec:** `20.sp` SemiBold (`Section`), `FarmColors.MainText`, optional right action text button (`48.dp` touch height) such as "అన్నీ చూడండి" / "See all". Marked `semantics { heading() }`.

```kotlin
@Composable
fun FarmSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
)
```

#### `FarmSummaryCard`
- **Role:** Top KPI summary on Home, Farms, Accounts, Reports (e.g., Total Expense vs Income, Net Balance, Active Plots).
- **Visual Spec:** `FarmShapes.Card` (`16.dp`), `1.dp` `#D5DED4` border, `16.dp–20.dp` padding, supports primary `Display` (`28sp` Bold) figure, optional secondary comparison row (2 columns wrapped safely on 360dp), optional `FarmAudioButton`, and optional `FarmStatusIndicator`.

```kotlin
@Composable
fun FarmSummaryCard(
    title: String,
    primaryValue: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    secondaryMetrics: List<Pair<String, String>> = emptyList(), // Max 2-3 pairs; wraps cleanly
    status: Pair<FarmStatusType, String>? = null,
    containerColor: Color = FarmColors.Surface,
    accentStripColor: Color? = null, // Optional 4dp left indicator strip
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null
)
```

#### `FarmRecordCard`
- **Role:** Primary ledger/entity card for Expenses, Sales, Crops, Activities, Labour, Inventory, Irrigation, Harvest, Soil Tests.
- **Visual Spec:** `FarmShapes.Card` (`16.dp`), `#FFFFFF` surface, `1.dp` `#D5DED4` border, `16.dp` padding, merged TalkBack semantics. Left leading category icon (`24dp` inside `40dp` soft-tinted circle), center title (`Card` 18sp or `Body` 16sp SemiBold) + 1–2 metadata lines (`Supporting` 14sp), right-aligned `NumericValue` (`18sp` Bold) + optional `FarmStatusIndicator` or audio/photo micro indicator icon.

```kotlin
@Composable
fun FarmRecordCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    amountOrMetric: String? = null,
    metricColor: Color = FarmColors.MainText,
    leadingIcon: ImageVector? = null,
    leadingIconContainerColor: Color = FarmColors.PrimaryContainer,
    leadingIconTint: Color = FarmColors.Primary,
    statusType: FarmStatusType? = null,
    statusLabel: String? = null,
    hasPhoto: Boolean = false,
    hasAudio: Boolean = false
)
```

#### `FarmListItem`
- **Role:** Lightweight selectable row inside Settings, Contacts, Dropdown sheets, Plot sub-lists.
- **Visual Spec:** Minimum `56.dp` height, `16.dp` horizontal padding, `12.dp` vertical padding, optional `24.dp` leading icon, title (`Body` 16sp Medium), optional subtitle (`Supporting` 14sp), optional trailing chevron/switch/value.

```kotlin
@Composable
fun FarmListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = FarmColors.Primary,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    enabled: Boolean = true
)
```

#### `FarmStatusIndicator`
- **Role:** Non-color-only badge communicating status (Paid/Pending/Overdue/Offline/Low Stock).
- **Visual Spec:** `FarmShapes.Tag` (`8.dp`), ALWAYS pairs **semantic container/border + distinct icon (`16.dp`) + explicit text (`Supporting` 14sp)**. Never renders as an unlabeled colored dot.

```kotlin
@Composable
fun FarmStatusIndicator(
    type: FarmStatusType,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null, // Defaults mapped per FarmStatusType if null
    style: FarmBadgeStyle = FarmBadgeStyle.FILLED_SOFT
)
```

---

### 4.4 Form Inputs (360dp Keyboard-Safe)

#### `FarmAmountInput`
- **Role:** Rupee (`₹`) entry for Expenses, Sales, Wages, Machinery Rental, Inventory cost. Maps strictly to `MoneyPaise` (`Long` minor units, max 2 decimal places, Indian digit grouping `₹1,25,000` — never `Double`/`Float`).
- **Visual Spec:** `FarmShapes.Field` (`12.dp`), `56.dp` minimum field height, `#FFFFFF` fill, `1.dp` `#D5DED4` border (`2.dp` `Primary` when focused, `2.dp` `Error` on error), fixed `₹` prefix in `NumericValue` (`18sp` Bold), `KeyboardType.Decimal` with digits + single decimal filter (max 2 decimals), tabular numerals (`tnum`), clear supporting error text below.

```kotlin
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
    enabled: Boolean = true
)
```

#### `FarmNumberInput`
- **Role:** Acres / Guntas (`microAcres`), quintals / bags / kg (`milliUnits`), hours, worker count, pH, N/P/K values, irrigation duration.
- **Visual Spec:** Same field shell as `FarmAmountInput` (`12.dp`, `56.dp` height), supports trailing unit suffix (e.g., `"ఎకరాలు"`, `"గుంటలు"`, `"క్వింటాళ్ళు"`, `"గంటలు"`, `"pH"`) and `allowDecimal` toggle (`KeyboardType.Decimal` vs `KeyboardType.Number`).

```kotlin
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
    enabled: Boolean = true
)
```

#### `FarmDatePicker`
- **Role:** Reliable 1-tap date selector for expenses, sowing, irrigation, harvest.
- **Visual Spec:** Read-only field button (`56.dp` height, `12.dp` radius) displaying formatted Telugu/local date (e.g. `10 నవంబర్ 2026` / `10 Nov 2026`) + calendar icon (`24.dp`), plus optional quick-chip row above/below (`ఈరోజు` / `నిన్న`) so farmers rarely need the full calendar dialog! Opens M3 `DatePickerDialog` styled with `FarmLightColorScheme`.

```kotlin
@Composable
fun FarmDatePicker(
    selectedDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    formattedDate: String,
    showQuickTodayYesterday: Boolean = true,
    todayLabel: String = "ఈరోజు",
    yesterdayLabel: String = "నిన్న",
    errorText: String? = null,
    enabled: Boolean = true
)
```

#### `FarmDropdown`
- **Role:** Farm, Plot, Crop, Expense Category, Contact, Machine Type, Soil Type selector.
- **Visual Spec:** `56.dp` read-only trigger field (`12.dp` radius) with trailing chevron. Opens a **ModalBottomSheet** (more thumb-friendly & readable on 360dp than cramped pop-up menus!) listing `FarmListItem` options (`56.dp` rows) with optional icons and active checkmark.

```kotlin
@Composable
fun <T> FarmDropdown(
    label: String,
    options: List<DropdownOption<T>>,
    selectedValue: T?,
    onSelect: (DropdownOption<T>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    errorText: String? = null,
    enabled: Boolean = true
)
```

#### `FarmSearchField`
- **Role:** Offline local Room SQL search on Contacts, Expenses, Plots, Crops, Inventory.
- **Visual Spec:** `52.dp` height (`12.dp` radius), `#FFFFFF` fill, `1.dp` `#D5DED4` border, leading `24.dp` search icon, trailing `48.dp` clear (`X`) button when `query.isNotEmpty()`, `ImeAction.Search`. **Note:** Text search only — no Stage 2 voice ASR button.

```kotlin
@Composable
fun FarmSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearchAction: (() -> Unit)? = null,
    clearContentDescription: String = "Clear search"
)
```

---

### 4.5 Offline Photo & Voice Media Components (Stage 1 Only)

#### `FarmPhotoCapture`
- **Role:** Attach local camera or gallery photo to Crop Diary, Expense bill, Soil Health Card, Farm/Plot record (**storage & viewing only — zero OCR/disease AI**).
- **Visual Spec:** Dashed or solid `1.5.dp` `#D5DED4` box (`16.dp` radius). Empty state shows large `FarmActionTile`-style dual buttons ("ఫోటో తీయండి" / "గ్యాలరీ నుండి") >= `56.dp` height. Populated state renders horizontally wrapped thumbnail strip (`88.dp × 88.dp`, `8.dp` radius) with `48.dp` remove `X` target and tap-to-open `FarmPhotoViewer`.

```kotlin
@Composable
fun FarmPhotoCapture(
    photoUris: List<String>,
    onTakePhotoClick: () -> Unit,
    onPickFromGalleryClick: () -> Unit,
    onRemovePhotoClick: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    cameraButtonLabel: String,
    galleryButtonLabel: String,
    maxPhotos: Int = 5
)
```

#### `FarmPhotoViewer`
- **Role:** Full-screen offline image inspection (bills, soil test paper, crop growth).
- **Visual Spec:** High-contrast `#1E2B22` full-screen backdrop, pinch-to-zoom & pan `Image` (`ContentScale.Fit`), top safe bar with `48.dp` Close button (`#FFFFFF` icon on dark `#1E2B22` 85% scrim), bottom caption bar (date, plot/crop label, optional `FarmAudioPlayer` if photo diary has paired voice note).

```kotlin
@Composable
fun FarmPhotoViewer(
    photoUri: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    timestampLabel: String? = null,
    onDeleteClick: (() -> Unit)? = null
)
```

#### `FarmVoiceRecorder`
- **Role:** Record offline AAC/M4A voice notes on device for Photo & Voice Diary, Crop notes, Expense reminders (**audio storage & playback only — zero ASR transcription**).
- **Visual Spec:** `FarmShapes.Tile` (`18.dp`), `SurfaceVariant` (`#EEF1E8`) container with `1.dp` `#D5DED4` border. Idle: large `56.dp` mic button (`Primary`) + prompt text. Recording: `Error` (`#B3261E`) stop button (`56.dp`) + pulsing waveform bar + tabular `MM:SS` timer + clear "రికార్డింగ్ ఆపండి" label. Recorded: switches seamlessly to `FarmAudioPlayer` + `48.dp` re-record/delete icon.

```kotlin
@Composable
fun FarmVoiceRecorder(
    state: VoiceRecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDeleteRecording: () -> Unit,
    idleLabel: String,
    recordingLabel: String,
    modifier: Modifier = Modifier,
    maxDurationSeconds: Int = 120,
    audioPlayerContent: (@Composable (filePath: String, durationMillis: Long) -> Unit)? = null
)
```

#### `FarmAudioPlayer`
- **Role:** Play back recorded farmer voice notes offline.
- **Visual Spec:** `56.dp` minimum height bar (`12.dp` radius, `#E2F0E5` or `#EEF1E8`), `48.dp` Play/Pause icon button (`Primary`), interactive progress `Slider` (or scrub bar with `48.dp` touch height), tabular `currentTime / totalTime` (`Metadata` 12sp).

```kotlin
@Composable
fun FarmAudioPlayer(
    isPlaying: Boolean,
    currentPositionMillis: Long,
    totalDurationMillis: Long,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    playContentDescription: String,
    pauseContentDescription: String
)
```

---

### 4.6 Navigation, Dialogs & Feedback

#### `FarmBottomNavigation`
- **Role:** Persistent 4-tab root navigation (**strictly max 4 tabs**: Home, Farms, Accounts, More; no hamburger menu).
- **Visual Spec:** `72.dp` height, `#FFFFFF` surface with `1.dp` top `#D5DED4` border (zero elevation shadow muddiness). Active tab uses `PrimaryContainer` (`#E2F0E5`) pill + `Primary` (`#245A3A`) `24.dp` icon + bold `12.sp` Telugu label (`maxLines = 1`, `softWrap = false` with `overflow = TextOverflow.Ellipsis`). Inactive uses `SecondaryText` (`#536156`).

```kotlin
@Composable
fun FarmBottomNavigation(
    tabs: List<FarmNavTab>, // Must be <= 4 items
    selectedRouteId: String,
    onTabSelected: (FarmNavTab) -> Unit,
    modifier: Modifier = Modifier
)
```

#### `FarmConfirmationDialog`
- **Role:** Explicit confirmation before deleting a farm/plot/record, restoring a local backup (overwriting current data), or discarding unsaved form edits.
- **Visual Spec:** `FarmShapes.Card` (`16.dp`), `#FFFFFF` surface, `20.dp` padding, optional top warning icon (`36.dp`), `Section` (`20sp`) title, `Body` (`16sp`) clear Telugu explanation, stacked or 50/50 split `FarmSecondaryButton` (Cancel) + `FarmPrimaryButton` (`isDestructive = true` for delete/overwrite).

```kotlin
@Composable
fun FarmConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    icon: ImageVector? = null
)
```

#### `FarmSnackbar`
- **Role:** Non-blocking save/backup/validation feedback (positioned safely above `FarmBottomNavigation` or keyboard).
- **Visual Spec:** `12.dp`–`16.dp` radius, high-contrast semantic pair (Info/Success/Warning/Error icon + `Body` `16sp` message + optional `48.dp`-height Undo/Action button), `LiveRegionMode.Polite` semantics.

```kotlin
@Composable
fun FarmSnackbar(
    message: String,
    tone: SnackbarTone = SnackbarTone.INFO,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

---

### 4.7 Empty, Loading, Error & Permission States

#### `FarmEmptyState`
- **Role:** Friendly zero-data screen for every list/tab with direct first-step CTA.
- **Visual Spec:** Centered column with `32.dp` padding, `48.dp` soft-circled illustration icon, `Section` (`20sp`) title, `Body` (`16sp`) `SecondaryText` explanation, optional `FarmAudioButton`, and prominent `FarmPrimaryButton`.

```kotlin
@Composable
fun FarmEmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null
)
```

#### `FarmLoadingState`
- **Role:** Clean loading feedback while Room queries / local JSON backup ZIP export runs.
- **Visual Spec:** Centered `CircularProgressIndicator` (`40.dp`, `FarmColors.Primary`, `4.dp` stroke) + friendly `Body` (`16sp`) Telugu label (`"వివరాలు వస్తున్నాయి..."`). Never shows empty white flashes or heavy shimmer skeletons that strain 4 GB RAM devices.

```kotlin
@Composable
fun FarmLoadingState(
    message: String,
    modifier: Modifier = Modifier
)
```

#### `FarmErrorState`
- **Role:** Recoverable local error state (e.g., corrupted backup file, storage full, mic/camera failure).
- **Visual Spec:** `ErrorContainer` soft icon circle + `Error` (`#B3261E`) `40.dp` icon, `Section` (`20sp`) error title, `Body` (`16sp`) clear actionable reason (never technical stack traces), `FarmPrimaryButton` ("మళ్ళీ చూడండి" / "Try again") + optional secondary help/back button.

```kotlin
@Composable
fun FarmErrorState(
    title: String,
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null
)
```

#### `FarmPermissionExplanation`
- **Role:** Pre-prompt Telugu explanation sheet/card BEFORE Android system runtime permission popup for **Camera**, **Microphone (voice notes)**, or **Storage (backup/gallery)**.
- **Visual Spec:** `FarmShapes.Card` (`16.dp`) or bottom sheet, `40.dp` icon in `PrimaryContainer`, `Section` (`20sp`) title ("కెమెరా అనుమతి కావాలి"), plain-language 2-line reason explaining data stays offline on phone, optional `FarmAudioButton`, `FarmPrimaryButton` ("అనుమతి ఇవ్వండి") + `FarmSecondaryButton` ("ఇప్పుడు వద్దు").

```kotlin
@Composable
fun FarmPermissionExplanation(
    title: String,
    explanation: String,
    icon: ImageVector,
    grantLabel: String,
    denyLabel: String,
    onGrantClick: () -> Unit,
    onDenyClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPermanentlyDenied: Boolean = false, // Changes grant action to Open App Settings
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null
)
```

---

## 5. Accessibility, Sunlight & Hardware Guardrails

### 5.1 Never Rely on Color Alone
Every visual status MUST combine at least **three channels**:
1. **Color:** Semantic token (`Success`, `Warning`, `Error`, `Info`).
2. **Icon + Border:** Distinct shape (`CheckCircle`, `Schedule`/`Warning`, `ErrorOutline`, `CloudOff`/`Info`) + `1.dp–1.5.dp` border.
3. **Explicit Text + Optional Audio:** Short conversational Telugu badge (`"అందింది"`, `"బాకీ ఉంది"`, `"నీరు పెట్టాలి"`) + `FarmAudioButton` on key screens.

### 5.2 TalkBack & Compose Semantics
- **Merged List Cards:** Apply `Modifier.semantics(mergeDescendants = true) {}` on `FarmRecordCard`, `FarmListItem`, and `FarmActionTile` so TalkBack announces `"విత్తనాలు, 2,400 రూపాయలు, ఈరోజు, అందింది"` as one continuous phrase.
- **Headings:** Set `semantics { heading() }` on `FarmPageHeader` title and `FarmSectionHeader`.
- **Stateful Controls:** Set `stateDescription` on `FarmAudioButton` (`"వినిపిస్తోంది"` / `"ఆగి ఉంది"`) and `FarmVoiceRecorder` (`"రికార్డింగ్ అవుతోంది"`).
- **Live Feedback:** Apply `semantics { liveRegion = LiveRegionMode.Polite }` on inline validation errors and `FarmSnackbar`.
- **Icons:** Decorative icons beside explicit labels use `contentDescription = null` to eliminate TalkBack double-reading; standalone icon buttons require explicit Telugu/English `contentDescription`.

### 5.3 Outdoor Sunlight & Low-Literacy Legibility
- **Contrast Floor:** Body/supporting text >= `4.5:1`; primary titles/KPIs >= `7:1`.
- **Zero Transparency:** Never render frosted glass, alpha `< 0.9` scrims, or light grey text over photos.
- **Crushed-Shadow Avoidance:** Use `1.dp` `#D5DED4` outlines + `#FFFFFF` over `#F7F8F2` instead of heavy drop shadows that disappear outdoors.
- **Scannable Hierarchy:** Max 3 data points per `FarmRecordCard` row; lead numbers with `₹` and units explicitly.

### 5.4 Keyboard-Safe Forms & 360dp Portrait Resilience
- Every form screen uses `Scaffold` with `Modifier.imePadding()` + scrollable `Column(Modifier.verticalScroll(rememberScrollState()))`.
- Keep the primary Save `FarmPrimaryButton` pinned in `bottomBar` with `imePadding()` OR inside the scroll column immediately after the final field so the soft keyboard never covers inputs or Save.
- Zero horizontal scroll tables: wrap 2-column grids using `Row(horizontalArrangement = Arrangement.spacedBy(12.dp))` + `Modifier.weight(1f)` and fall back cleanly up to `1.5x` Android system font scale.
- **Realme 5s Performance (4 GB RAM / Android 10):** Use `LazyColumn` with stable ID keys, decode local diary photos via sampled thumbnails (`max 512px` in lists, full size only in `FarmPhotoViewer`), and avoid `RenderEffect` blur (unsupported on API 29).
