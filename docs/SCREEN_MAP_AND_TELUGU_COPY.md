# Farm Saathi — Stage 1 Screen Map & Conversational Telugu/English Copy

**Scope:** Stage 1 Offline-First Android App (Onboarding + 17 Core Farmer Modules)  
**Navigation Rule:** Max **4 bottom navigation tabs** (`Home / హోమ్`, `Farms / పొలాలు`, `Accounts / ఖాతాలు`, `More / మరిన్ని`). **No hamburger drawer.**  
**Tone:** Warm, respectful, everyday rural Telangana / Andhra Pradesh Telugu (avoids formal Sanskritized bookish phrasing) paired with 1-tap English equivalents.

---

## 1. Bottom Navigation & Type-Safe Route Hierarchy

### 1.1 Bottom Navigation Bar (4 Tabs Only)

| Order | Route | Tab Label (TE) | Tab Label (EN) | Icon (`Icons.Rounded`) | Tab Root Content |
|---|---|---|---|---|---|
| 1 | `FarmRoute.HomeTab` | హోమ్ | Home | `Home` | Today's greeting, quick tiles, season summary, today/overdue tasks |
| 2 | `FarmRoute.FarmsTab` | పొలాలు | Farms | `Landscape` | Farms list + quick access to Plots, Crops, Irrigation, Activities |
| 3 | `FarmRoute.AccountsTab` | ఖాతాలు | Accounts | `AccountBalanceWallet` | Season Expense/Income KPI + Expenses, Sales, Labour, Machinery hubs |
| 4 | `FarmRoute.MoreTab` | మరిన్ని | More | `GridView` | Inventory, Photo/Voice Diary, Harvest, Soil Tests, Contacts, Reports, Settings |

**Bottom Bar Visibility Rule:** Visible ONLY on the 4 top-level tab roots (`HomeTab`, `FarmsTab`, `AccountsTab`, `MoreTab`). Hidden on all Add/Edit forms, Detail screens, Onboarding, and Photo Viewer to preserve thumb & soft-keyboard space on 360dp portrait screens.

### 1.2 Complete Type-Safe Kotlin Routes (`FarmRoute.kt`)

```kotlin
package com.farmsaathi.core.common.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface FarmRoute {

    // 1. Onboarding
    @Serializable data object OnboardingWelcome : FarmRoute
    @Serializable data object OnboardingProfile : FarmRoute

    // 2. Home (Tab 1)
    @Serializable data object HomeTab : FarmRoute

    // 3. Farms (Tab 2)
    @Serializable data object FarmsTab : FarmRoute
    @Serializable data class FarmDetail(val farmId: Long) : FarmRoute
    @Serializable data class FarmForm(val farmId: Long? = null) : FarmRoute

    // 4. Plots
    @Serializable data class PlotList(val farmId: Long? = null) : FarmRoute
    @Serializable data class PlotDetail(val plotId: Long) : FarmRoute
    @Serializable data class PlotForm(val farmId: Long, val plotId: Long? = null) : FarmRoute

    // 5. Crops
    @Serializable data class CropList(val plotId: Long? = null) : FarmRoute
    @Serializable data class CropDetail(val cropId: Long) : FarmRoute
    @Serializable data class CropForm(val plotId: Long? = null, val cropId: Long? = null) : FarmRoute

    // 6. Calendar / Activities
    @Serializable data class ActivityList(val cropId: Long? = null, val plotId: Long? = null) : FarmRoute
    @Serializable data class ActivityForm(
        val activityId: Long? = null,
        val cropId: Long? = null,
        val plotId: Long? = null
    ) : FarmRoute

    // 7. Photo & Voice Diary (Offline storage & playback only)
    @Serializable data class DiaryList(val cropId: Long? = null, val plotId: Long? = null) : FarmRoute
    @Serializable data class DiaryForm(val entryId: Long? = null, val cropId: Long? = null, val plotId: Long? = null) : FarmRoute
    @Serializable data class DiaryPhotoView(val entryId: Long, val photoPath: String) : FarmRoute

    // 8. Accounts Hub (Tab 3) & Expenses
    @Serializable data object AccountsTab : FarmRoute
    @Serializable data class ExpenseList(val cropId: Long? = null) : FarmRoute
    @Serializable data class ExpenseDetail(val expenseId: Long) : FarmRoute
    @Serializable data class ExpenseForm(val expenseId: Long? = null, val cropId: Long? = null) : FarmRoute

    // 9. Income / Sales
    @Serializable data class SaleList(val cropId: Long? = null) : FarmRoute
    @Serializable data class SaleDetail(val saleId: Long) : FarmRoute
    @Serializable data class SaleForm(val saleId: Long? = null, val cropId: Long? = null) : FarmRoute

    // 10. Labour
    @Serializable data class LabourList(val cropId: Long? = null) : FarmRoute
    @Serializable data class LabourForm(val labourId: Long? = null, val cropId: Long? = null) : FarmRoute

    // 11. Machinery Rental
    @Serializable data class MachineryList(val cropId: Long? = null) : FarmRoute
    @Serializable data class MachineryForm(val rentalId: Long? = null, val cropId: Long? = null) : FarmRoute

    // 12. Input Inventory
    @Serializable data object InventoryList : FarmRoute
    @Serializable data class InventoryForm(val itemId: Long? = null) : FarmRoute
    @Serializable data class InventoryUsageForm(val itemId: Long) : FarmRoute

    // 13. Irrigation
    @Serializable data class IrrigationList(val plotId: Long? = null, val cropId: Long? = null) : FarmRoute
    @Serializable data class IrrigationForm(val logId: Long? = null, val plotId: Long? = null, val cropId: Long? = null) : FarmRoute

    // 14. Harvest & Storage
    @Serializable data class HarvestList(val cropId: Long? = null) : FarmRoute
    @Serializable data class HarvestForm(val harvestId: Long? = null, val cropId: Long? = null) : FarmRoute

    // 15. Soil Test Records (Manual + Photo only)
    @Serializable data class SoilTestList(val plotId: Long? = null) : FarmRoute
    @Serializable data class SoilTestDetail(val testId: Long) : FarmRoute
    @Serializable data class SoilTestForm(val testId: Long? = null, val plotId: Long? = null) : FarmRoute

    // 16. Contacts
    @Serializable data object ContactList : FarmRoute
    @Serializable data class ContactDetail(val contactId: Long) : FarmRoute
    @Serializable data class ContactForm(val contactId: Long? = null) : FarmRoute

    // 17. Reports
    @Serializable data object ReportsOverview : FarmRoute

    // 18. More Hub (Tab 4) & Settings / Backup
    @Serializable data object MoreTab : FarmRoute
    @Serializable data object SettingsHome : FarmRoute
    @Serializable data object BackupRestore : FarmRoute
    @Serializable data object OfflineHelp : FarmRoute
}
```

### 1.3 Tap-Depth Guarantee (Max 2 Taps for Daily Actions)
- **1 Tap from Home:** Add Expense, Add Sale, Log Activity, Add Photo/Voice Note, View Today's Tasks, Open Accounts/Farms/More.
- **2 Taps from Bottom Nav:** Any of the 17 core modules via `Farms` (Plots, Crops, Activities, Irrigation), `Accounts` (Expenses, Sales, Labour, Machinery, Reports), or `More` (Inventory, Diary, Harvest, Soil Tests, Contacts, Settings/Backup).

---

## 2. Shared Common Copy & Offline/Permission Strings

Use these shared strings across all modules so forms, confirmations, and audio cues stay consistent.

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `common_save` | సేవ్ చేయండి | Save |
| `common_cancel` | వద్దు | Cancel |
| `common_edit` | మార్చండి | Edit |
| `common_delete` | తీసేయండి | Delete |
| `common_confirm` | సరే, చేయండి | Yes, confirm |
| `common_back` | వెనక్కి | Back |
| `common_listen` | వినండి | Listen |
| `common_stop_audio` | ఆపండి | Stop |
| `common_today` | ఈరోజు | Today |
| `common_yesterday` | నిన్న | Yesterday |
| `common_all` | అన్నీ | All |
| `common_see_all` | అన్నీ చూడండి | See all |
| `common_search` | వెతకండి | Search |
| `common_retry` | మళ్ళీ చూడండి | Try again |
| `common_loading` | వివరాలు వస్తున్నాయి... | Loading details... |
| `common_offline_badge` | ఫోన్‌లోనే సేవ్ అవుతుంది (నెట్ అక్కర్లేదు) | Saved on phone (No internet needed) |
| `common_saved_toast` | వివరాలన్నీ సేవ్ అయ్యాయి | Saved safely on phone |
| `common_deleted_toast` | వివరం తీసేశాము | Record deleted |
| `common_delete_title` | ఇది తీసేయాలా? | Delete this record? |
| `common_delete_msg` | తీసేస్తే మళ్ళీ వెనక్కి రాదు. నిజంగానే తీసేయాలా? | Once deleted, it cannot be recovered. Are you sure? |
| `common_void_title` | ఈ లెక్క రద్దు చేయాలా? | Void this financial entry? |
| `common_void_msg` | రద్దు చేస్తే ఖర్చు/ఆదాయం లెక్కలో నుండి తీసేస్తాము, కానీ రికార్డు చరిత్ర భద్రంగా ఉంటుంది. | Voiding removes it from totals while keeping your ledger history intact. |
| `common_discard_title` | రాసిన వివరాలు వదిలేయాలా? | Discard unsaved details? |
| `common_discard_msg` | సేవ్ చేయకుండా వెనక్కి వెళ్తే ఇవి పోతాయి. | Changes not saved will be lost if you go back. |
| `common_required_field` | ఇది తప్పకుండా రాయండి | Please enter this detail |
| `common_invalid_number` | సరైన నంబర్ రాయండి | Please enter a valid number |
| `perm_camera_title` | ఫోటో తీయడానికి అనుమతి కావాలి | Camera permission needed |
| `perm_camera_msg` | పొలం, బిల్లుల ఫోటోలు ఫోన్‌లోనే దాచుకోవడానికి కెమెరా అనుమతి ఇవ్వండి. | Allow camera access to save farm and bill photos offline on your phone. |
| `perm_mic_title` | మాట రికార్డ్ చేయడానికి అనుమతి కావాలి | Microphone permission needed |
| `perm_mic_msg` | మీ మాటలను డైరీలో రికార్డ్ చేసి దాచుకోవడానికి మైక్ అనుమతి ఇవ్వండి. | Allow microphone access to record offline voice notes in your diary. |
| `perm_storage_title` | బ్యాకప్ దాచుకోవడానికి అనుమతి కావాలి | Storage permission needed |
| `perm_storage_msg` | మీ ఫోన్‌లో బ్యాకప్ ఫైల్ సేవ్ చేయడానికి లేదా ఎంచుకోవడానికి అనుమతి ఇవ్వండి. | Allow storage access to save or restore your backup file on this phone. |
| `perm_grant` | అనుమతి ఇవ్వండి | Allow |
| `perm_deny` | ఇప్పుడు వద్దు | Not now |
| `perm_settings` | సెట్టింగ్స్ తెరవండి | Open Settings |

---

## 3. Screen Groups, States, Audio Cues & Copy (All 18 Groups)

### Group 1: Onboarding
- **Routes:** `OnboardingWelcome`, `OnboardingProfile`
- **Key Screens:** Language & Offline Assurance Welcome -> Farmer Name, Village & Optional First Farm quick setup
- **Primary Action:** `మొదలుపెట్టండి` (`Get Started`) / `సేవ్ చేసి లోపలికి వెళ్ళండి` (`Save & Continue`)
- **Spoken Audio Cue:** `cue_onboarding_welcome` — **TE:** *"నమస్కారం రైతన్నా! ఫామ్ సాథీకి స్వాగతం. మీ పొలం ఖర్చులు, పంటలు, ఆదాయం అన్నీ ఇంటర్నెట్ లేకుండా మీ ఫోన్‌లోనే భద్రంగా రాసుకోవచ్చు. ముందు మీ భాష ఎంచుకోండి."* | **EN:** *"Welcome to Farm Saathi! You can safely track all your farm expenses, crops, and income right on your phone without internet. First choose your language."*
- **States:**
  - `Loading`: *"యాప్ సిద్ధమవుతోంది..."* / *"Setting up your farm book..."*
  - `Empty`: N/A (pre-filled defaults: `తెలుగు` selected)
  - `Error`: *"పేరు సేవ్ కాలేదు. మళ్ళీ ప్రయత్నించండి."* / *"Could not save profile. Please try again."*
  - `Offline`: *"ఇంటర్నెట్ లేకుండానే పనిచేస్తుంది — మీ వివరాలు మీ ఫోన్‌లోనే ఉంటాయి."* / *"Works 100% offline — your data stays on your phone."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `onb_title` | ఫామ్ సాథీకి స్వాగతం | Welcome to Farm Saathi |
| `onb_subtitle` | మీ పొలం ఖర్చులు, పంటలు, అమ్మకాల లెక్కలన్నీ సులువుగా ఫోన్‌లోనే రాసుకోండి. | Easily track your farm expenses, crops, and sales right on your phone. |
| `onb_lang_label` | మీకు నచ్చిన భాష ఎంచుకోండి | Choose your language |
| `onb_name_label` | మీ పేరు | Your name |
| `onb_name_hint` | ఉదా: రాములు / వెంకటేశ్వర్లు | e.g., Ramulu / Venkateswarlu |
| `onb_village_label` | మీ ఊరు పేరు (ఐచ్ఛికం) | Village name (optional) |
| `onb_village_hint` | ఉదా: నర్సాపూర్ | e.g., Narsapur |
| `onb_skip` | తర్వాత రాస్తాను | Skip for now |

---

### Group 2: Home (Tab 1)
- **Routes:** `HomeTab`
- **Key Screens:** Today's Farmer Dashboard
- **Primary Action:** 4 large 2×2 `FarmActionTile` triggers (`ఖర్చు రాయండి`, `అమ్మకం రాయండి`, `పొలం పని`, `ఫోటో / మాట`)
- **Spoken Audio Cue:** `cue_home_summary` — **TE:** *"నమస్కారం. ఇక్కడ పైనుండే కార్డులో ఈ సీజన్ మొత్తం ఖర్చు, ఆదాయం చూడొచ్చు. కింద ఉండే పెద్ద బటన్లతో కొత్త ఖర్చు, అమ్మకం, పని లేదా ఫోటో త్వరగా రాసుకోవచ్చు."* | **EN:** *"Hello. At the top you can see this season's total expense and income. Use the large buttons below to quickly log an expense, sale, farm work, or photo."*
- **States:**
  - `Loading`: *"ఈరోజు వివరాలు వస్తున్నాయి..."* / *"Loading today's summary..."*
  - `Empty` (New farmer, zero tasks/farms): *"ఇంకా పొలాలు లేదా పనులు కలపలేదు. మీ మొదటి పొలం లేదా ఖర్చు రాయండి!"* / *"No farms or tasks yet. Add your first farm or expense!"*
  - `Error`: *"హోమ్ వివరాలు లోడ్ కాలేదు."* / *"Could not load home summary."*
  - `Offline`: Subtle badge in header: *"ఆఫ్‌లైన్ లో భద్రంగా ఉంది"* / *"Safe offline on phone"*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `home_greeting` | నమస్కారం, %s గారు | Namaskaram, %s |
| `home_greeting_default` | నమస్కారం రైతన్నా | Namaskaram Farmer |
| `home_season_card` | ఈ సీజన్ లెక్క | This Season Summary |
| `home_expense_short` | ఖర్చు | Expense |
| `home_income_short` | ఆదాయం | Income |
| `home_quick_actions` | త్వరగా రాయండి | Quick Actions |
| `home_tile_expense` | ఖర్చు రాయండి | Add Expense |
| `home_tile_sale` | అమ్మకం రాయండి | Add Sale |
| `home_tile_activity` | పొలం పని | Log Activity |
| `home_tile_diary` | ఫోటో / మాట | Photo / Voice |
| `home_today_tasks` | ఈరోజు & బాకీ పనులు | Today & Due Tasks |
| `home_irrigation_due` | నీటి తడి సమయం అయింది | Watering due |

---

### Group 3: Farms (Tab 2)
- **Routes:** `FarmsTab`, `FarmDetail(farmId)`, `FarmForm(farmId?)`
- **Key Screens:** My Farms List, Farm Detail Hub (acres, plots summary, direct tiles into Plots/Crops/Irrigation/Soil), Add/Edit Farm Form
- **Primary Action:** `కొత్త పొలం కలపండి` (`Add New Farm`)
- **Spoken Audio Cue:** `cue_farms_overview` — **TE:** *"ఇక్కడ మీ దగ్గర ఉన్న పొలాలన్నీ ఉంటాయి. కొత్త పొలం కలపడానికి కింద ఉన్న ఆకుపచ్చ బటన్ నొక్కండి. పొలం మీద నొక్కితే అందులోని మడులు, పంటలు కనిపిస్తాయి."* | **EN:** *"Here are all your farms. Tap the green button below to add a farm. Tap any farm to see its plots and crops."*
- **States:**
  - `Loading`: *"మీ పొలాల వివరాలు వస్తున్నాయి..."* / *"Loading your farms..."*
  - `Empty`: *"ఇంకా ఏ పొలం కలపలేదు. ముందు మీ పొలం పేరు, ఎకరాలు రాసుకోండి."* / *"No farms added yet. Add your farm name and acres to start."*
  - `Error`: *"పొలాల వివరాలు చూపించలేకపోయాము."* / *"Unable to load farms."*
  - `Offline`: *"పొలాల వివరాలన్నీ ఫోన్‌లోనే సేవ్ అయ్యాయి."* / *"All farm details are stored offline on phone."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `farms_title` | నా పొలాలు | My Farms |
| `farms_add_btn` | కొత్త పొలం కలపండి | Add New Farm |
| `farms_edit_title` | పొలం వివరాలు మార్చండి | Edit Farm |
| `farms_name_label` | పొలం పేరు | Farm name |
| `farms_name_hint` | ఉదా: చెరువు కింద పొలం / పెద్ద చేను | e.g., Tank-side farm / Big field |
| `farms_acres_label` | మొత్తం విస్తీర్ణం | Total area |
| `farms_acres_unit` | ఎకరాలు | Acres |
| `farms_guntas_unit` | గుంటలు | Guntas |
| `farms_cents_unit` | సెంట్లు | Cents |
| `farms_water_label` | ప్రధాన నీటి వసతి | Main water source |
| `farms_location_label` | ఊరు / సర్వే నంబర్ (ఐచ్ఛికం) | Village / Survey no. (optional) |
| `farms_plots_count` | %d మడులు | %d Plots |
| `farms_active_crops` | %d పంటలు ఉన్నాయి | %d Active crops |

---

### Group 4: Plots
- **Routes:** `PlotList(farmId?)`, `PlotDetail(plotId)`, `PlotForm(farmId, plotId?)`
- **Key Screens:** Plots List (filtered by Farm or All), Plot Detail Hub (Current Crop, Irrigation Log, Soil Test, Activities), Add/Edit Plot Form
- **Primary Action:** `కొత్త మడి కలపండి` (`Add Plot`)
- **Spoken Audio Cue:** `cue_plots_help` — **TE:** *"ఒక్కో పొలంలో ఉండే మడులు లేదా భాగాలను ఇక్కడ రాసుకోవచ్చు. ప్రతి మడికి మట్టి రకం, విస్తీర్ణం వేరువేరుగా దాచుకోవచ్చు."* | **EN:** *"Record separate plots or sub-fields inside your farm here, along with each plot's area and soil type."*
- **States:**
  - `Loading`: *"మడుల వివరాలు వస్తున్నాయి..."* / *"Loading plots..."*
  - `Empty`: *"ఈ పొలంలో ఇంకా మడులు రాయలేదు. మడి పేరు, ఎకరాలు కలపండి."* / *"No plots added to this farm yet. Add plot name and acres."*
  - `Error`: *"మడుల వివరాలు రావడం లేదు."* / *"Could not load plots."*
  - `Offline`: *"మడుల వివరాలు ఫోన్‌లోనే ఉన్నాయి."* / *"Plot records saved locally."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `plots_title` | మడులు / భాగాలు | Plots / Fields |
| `plots_add_btn` | కొత్త మడి కలపండి | Add Plot |
| `plots_name_label` | మడి పేరు లేదా నంబర్ | Plot name or number |
| `plots_name_hint` | ఉదా: తూర్పు మడి / మడి 1 | e.g., East plot / Plot 1 |
| `plots_soil_label` | మట్టి రకం | Soil type |
| `soil_black` | నల్ల రేగడి | Black cotton soil |
| `soil_red` | ఎర్ర నేల | Red soil |
| `soil_sandy` | ఇసుక నేల | Sandy soil |
| `soil_loam` | గరప / ఒండ్రు నేల | Loamy / Alluvial soil |
| `plots_quick_crop` | ఈ మడిలో పంట వేయండి | Add Crop in this Plot |

---

### Group 5: Crops
- **Routes:** `CropList(plotId?)`, `CropDetail(cropId)`, `CropForm(plotId?, cropId?)`
- **Key Screens:** Active & Past Crops List, Crop Detail (Expense vs Income badge, Activities, Irrigation, Harvest, Diary links), Add/Edit Crop Form
- **Primary Action:** `కొత్త పంట వేయండి` (`Add Crop`)
- **Spoken Audio Cue:** `cue_crops_overview` — **TE:** *"మీరు వేసిన పంటలు, నాటిన తేదీలు ఇక్కడ ఉంటాయి. ఏదైనా పంట మీద నొక్కితే ఆ పంటకు అయిన ఖర్చు, పనులు, అమ్మకాలు అన్నీ ఒకే దగ్గర కనిపిస్తాయి."* | **EN:** *"View your active and past crops here. Tap any crop to see all expenses, activities, and sales for that crop in one place."*
- **States:**
  - `Loading`: *"పంటల వివరాలు వస్తున్నాయి..."* / *"Loading crops..."*
  - `Empty`: *"ఇంకా ఏ పంట రాయలేదు. మీరు ఇప్పుడు వేసిన పంట పేరు, నాటిన తేదీ కలపండి."* / *"No crops added yet. Add your current crop and sowing date."*
  - `Error`: *"పంటల జాబితా లోడ్ కాలేదు."* / *"Could not load crops list."*
  - `Offline`: *"పంటల రికార్డు ఫోన్‌లోనే సేవ్ అవుతుంది."* / *"Crop records saved on device."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `crops_title` | పంటలు | Crops |
| `crops_add_btn` | కొత్త పంట కలపండి | Add Crop |
| `crops_name_label` | పంట పేరు | Crop name |
| `crops_name_hint` | ఉదా: వరి, పత్తి, మిర్చి, కంది | e.g., Paddy, Cotton, Chilli, Redgram |
| `crops_variety_label` | విత్తనం రకం (ఐచ్ఛికం) | Seed variety (optional) |
| `crops_season_label` | సీజన్ | Season |
| `season_kharif` | వానాకాలం (ఖరీఫ్) | Kharif (Monsoon) |
| `season_rabi` | యాసంగి (రబీ) | Rabi (Winter) |
| `season_summer` | ఎండాకాలం | Summer |
| `crops_sowing_date` | నాటిన / విత్తిన తేదీ | Sowing / Planting date |
| `crops_status_active` | పెరుగుతోంది | Growing |
| `crops_status_harvested` | కోత పూర్తయింది | Harvested |
| `crops_mark_harvested` | కోత పూర్తయినట్లు గుర్తు పెట్టండి | Mark as Harvested |
| `crops_template_disclaimer` | ఇది మార్గదర్శక ప్లాన్ మాత్రమే — మీ రకం, మట్టి, వాతావరణం బట్టి తేదీలు మార్చుకోవచ్చు. | Editable planning template — adjust timings to your variety, soil, and weather. |

---

### Group 6: Calendar / Activities
- **Routes:** `ActivityList(cropId?, plotId?)`, `ActivityForm(activityId?, cropId?, plotId?)`
- **Key Screens:** Filterable Upcoming / Overdue / Done Activities Timeline, Add/Edit Activity Form
- **Primary Action:** `కొత్త పని రాయండి` (`Add Activity`)
- **Spoken Audio Cue:** `cue_activities_help` — **TE:** *"దున్నుడు, కలుపు, మందు కొట్టడం లాంటి పనులను ఇక్కడ రాసుకోవచ్చు. పని పూర్తయ్యాక టిక్ నొక్కితే చాలు."* | **EN:** *"Track ploughing, weeding, spraying, and other farm tasks here. Tap the check button once a task is done."*
- **States:**
  - `Loading`: *"పొలం పనులు వస్తున్నాయి..."* / *"Loading farm tasks..."*
  - `Empty`: *"ఇంకా ఏ పని రాసుకోలేదు. రేపు లేదా ఈ వారం చేయవలసిన పనిని ఇక్కడ రాసుకోండి."* / *"No activities logged yet. Add upcoming or completed farm work."*
  - `Error`: *"పనుల వివరాలు లోడ్ కాలేదు."* / *"Unable to load activities."*
  - `Offline`: *"రిమైండర్లు ఫోన్ లోకల్ అలారంతోనే పనిచేస్తాయి."* / *"Reminders work offline on your phone."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `act_title` | పొలం పనులు | Farm Activities |
| `act_add_btn` | కొత్త పని రాయండి | Add Activity |
| `act_type_label` | పని రకం | Work type |
| `act_plough` | దున్నుడు / తయారీ | Ploughing / Land prep |
| `act_sow` | విత్తడం / నాటు | Sowing / Transplanting |
| `act_weed` | కలుపు తీయడం | Weeding |
| `act_fert` | ఎరువు వేయడం | Fertilizer application |
| `act_spray` | మందు కొట్టడం | Spraying |
| `act_harvest` | కోత పని | Harvesting |
| `act_other` | ఇతర పని | Other work |
| `act_date_label` | పని తేదీ | Activity date |
| `act_status_todo` | చేయాలి | To do |
| `act_status_done` | పూర్తయింది | Done |
| `act_status_overdue` | ఆలస్యమైంది | Overdue |
| `act_mark_done` | పూర్తయింది అని గుర్తుపెట్టండి | Mark Done |

---

### Group 7: Photo & Voice Diary
- **Routes:** `DiaryList(cropId?, plotId?)`, `DiaryForm(entryId?, cropId?, plotId?)`, `DiaryPhotoView(entryId, photoPath)`
- **Key Screens:** Visual Photo & Voice Timeline, Add Entry (Photo + Voice Recording + Optional Short Text), Full-Screen Photo Viewer + Audio Player
- **Primary Action:** `ఫోటో / మాట దాచండి` (`Save Photo / Voice Note`)
- **Spoken Audio Cue:** `cue_diary_help` — **TE:** *"రాయడానికి సమయం లేకపోతే మైక్ బటన్ నొక్కి మీ మాటలోనే పంట స్థితి రికార్డ్ చేయండి, లేదా పొలం ఫోటో తీసి దాచుకోండి. ఇవన్నీ మీ ఫోన్‌లోనే ఉంటాయి."* | **EN:** *"No time to type? Tap the mic button to record a voice note or snap a farm photo to remember crop progress. Everything stays offline on your phone."*
- **Note:** **Stage 1 stores and plays raw audio only — zero Speech-to-Text (ASR) and zero AI crop/disease scanning.**
- **States:**
  - `Loading`: *"డైరీ ఫోటోలు, మాటలు వస్తున్నాయి..."* / *"Loading diary notes..."*
  - `Empty`: *"ఇంకా ఫోటోలు లేదా మాటలు దాచలేదు. మీ పొలం ఫోటో తీసి లేదా మాట రికార్డ్ చేసి పెట్టుకోండి."* / *"No photo or voice notes yet. Capture a field photo or record a voice memo."*
  - `Error`: *"ఫోటో లేదా ఆడియో ఫైల్ ఓపెన్ కాలేదు."* / *"Could not open photo or voice recording."*
  - `Offline`: *"ఫోటోలు, ఆడియో అన్నీ ఫోన్‌లోనే సేవ్ అవుతాయి."* / *"Photos and voice notes are stored on phone."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `diary_title` | ఫోటో & మాట డైరీ | Photo & Voice Diary |
| `diary_add_btn` | కొత్త ఫోటో / మాట | Add Photo / Voice |
| `diary_take_photo` | ఫోటో తీయండి | Take Photo |
| `diary_pick_gallery` | గ్యాలరీ నుండి | Pick from Gallery |
| `diary_voice_idle` | మాట్లాడటానికి మైక్ నొక్కండి (గరిష్టంగా 2 నిమిషాలు) | Tap mic to record voice note (max 2 min) |
| `diary_voice_recording` | రికార్డ్ అవుతోంది... ఆపడానికి నొక్కండి | Recording... tap to stop |
| `diary_note_label` | చిన్న గమనిక (ఐచ్ఛికం) | Short note (optional) |
| `diary_note_hint` | ఉదా: తెల్ల దోమ కనిపించింది / ఎరువు వేశాము | e.g., Whitefly noticed / Applied urea |
| `diary_play_voice` | మాట వినండి | Play voice note |
| `diary_pause_voice` | మాట ఆపండి | Pause voice note |

---

### Group 8: Accounts Hub (Tab 3) & Expenses
- **Routes:** `AccountsTab`, `ExpenseList(cropId?)`, `ExpenseDetail(expenseId)`, `ExpenseForm(expenseId?, cropId?)`
- **Key Screens:** Accounts Summary Hub (Net Balance + 4 sub-module tiles: Expenses, Sales, Labour, Machinery + Reports link), Expense Ledger List, Expense Detail (with bill photo), Add/Edit Expense Form
- **Primary Action:** `కొత్త ఖర్చు రాయండి` (`Add Expense`)
- **Spoken Audio Cue:** `cue_accounts_summary` — **TE:** *"ఇక్కడ మీ పొలం డబ్బుల లెక్కలన్నీ ఉంటాయి. పైన మొత్తం ఖర్చు, ఆదాయం చూసి, కింద ఖర్చులు, అమ్మకాలు, కూలీలు, ట్రాక్టర్ అద్దె విడివిడిగా చూడొచ్చు."* | **EN:** *"Here are all your farm money accounts. Check total expense and income at the top, or open Expenses, Sales, Labour, and Machinery below."*
- **States:**
  - `Loading`: *"ఖర్చుల లెక్క వస్తోంది..."* / *"Loading expense records..."*
  - `Empty`: *"ఇంకా ఖర్చులు ఏమీ రాయలేదు. విత్తనాలు, ఎరువులు లేదా మందుల ఖర్చు రాయండి."* / *"No expenses recorded yet. Log seeds, fertilizer, or other farm spending."*
  - `Error`: *"ఖర్చుల వివరాలు లోడ్ కాలేదు."* / *"Could not load expenses."*
  - `Offline`: *"ఖర్చుల పద్దులన్నీ ఫోన్‌లోనే సేవ్ అవుతున్నాయి."* / *"All expense entries saved offline."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `acc_title` | ఖాతాలు & లెక్కలు | Accounts & Ledger |
| `acc_net_profit` | చేతికి మిగిలింది | Net Balance |
| `acc_expenses_tile` | ఖర్చులు | Expenses |
| `acc_sales_tile` | అమ్మకాలు / ఆదాయం | Sales & Income |
| `acc_labour_tile` | కూలీల లెక్క | Labour Wages |
| `acc_machine_tile` | యంత్రాల అద్దె | Machinery Hire |
| `exp_title` | ఖర్చుల పద్దు | Expenses |
| `exp_add_btn` | కొత్త ఖర్చు రాయండి | Add Expense |
| `exp_cat_label` | ఖర్చు దేనికి అయింది? | Expense category |
| `exp_cat_seeds` | విత్తనాలు | Seeds |
| `exp_cat_fert` | ఎరువులు | Fertilizers |
| `exp_cat_pest` | పురుగు / కలుపు మందు | Pesticides / Herbicides |
| `exp_cat_labour` | కూలీలు | Labour |
| `exp_cat_machine` | ట్రాక్టర్ / యంత్రం | Tractor / Machinery |
| `exp_cat_transport` | రవాణా / బాడుగ | Transport |
| `exp_cat_other` | ఇతర ఖర్చులు | Other expenses |
| `exp_amount_label` | ఎంత ఖర్చయింది? (₹) | Amount spent (₹) |
| `exp_vendor_label` | షాప్ / ఎవరికి ఇచ్చారు (ఐచ్ఛికం) | Shop / Paid to (optional) |
| `exp_bill_photo` | బిల్లు ఫోటో పెట్టండి (ఐచ్ఛికం) | Attach bill photo (optional) |

---

### Group 9: Income / Sales
- **Routes:** `SaleList(cropId?)`, `SaleDetail(saleId)`, `SaleForm(saleId?, cropId?)`
- **Key Screens:** Sales Ledger (with Received vs Pending Due totals), Sale Detail, Add/Edit Sale Form (auto-calculates Quantity × Rate = Total, editable)
- **Primary Action:** `కొత్త అమ్మకం రాయండి` (`Add Sale`)
- **Spoken Audio Cue:** `cue_sales_summary` — **TE:** *"పంట అమ్మిన వివరాలు ఇక్కడ రాసుకోవచ్చు. డబ్బులు ఇంకా రావలసి ఉంటే 'బాకీ ఉంది' అని పెట్టుకుంటే తర్వాత మర్చిపోకుండా గుర్తుంటుంది."* | **EN:** *"Log crop sales here. Mark unpaid amounts as 'Pending due' so you never forget money owed to you."*
- **States:**
  - `Loading`: *"అమ్మకాల వివరాలు వస్తున్నాయి..."* / *"Loading sales ledger..."*
  - `Empty`: *"ఇంకా అమ్మకాలు రాయలేదు. పంట అమ్మినప్పుడు డబ్బులు, బస్తాల వివరం రాయండి."* / *"No sales logged yet. Record crop sales and pending payments here."*
  - `Error`: *"అమ్మకాల వివరాలు లోడ్ కాలేదు."* / *"Unable to load sales."*
  - `Offline`: *"అమ్మకాల లెక్క ఫోన్‌లోనే భద్రంగా ఉంది."* / *"Sales records stored offline."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `sale_title` | అమ్మకాలు & ఆదాయం | Sales & Income |
| `sale_add_btn` | కొత్త అమ్మకం రాయండి | Add Sale |
| `sale_qty_label` | అమ్మిన పరిమాణం | Quantity sold |
| `sale_unit_label` | కొలత రకం | Unit |
| `unit_quintal` | క్వింటాళ్ళు | Quintals |
| `unit_bag` | బస్తాలు | Bags |
| `unit_kg` | కిలోలు | Kg |
| `unit_ton` | టన్నులు | Tonnes |
| `sale_rate_label` | ఒక్కో దానికి ధర (₹) | Rate per unit (₹) |
| `sale_total_label` | మొత్తం డబ్బులు (₹) | Total amount (₹) |
| `sale_buyer_label` | కొనుగోలుదారు / వ్యాపారి | Buyer / Trader |
| `sale_status_label` | డబ్బులు వచ్చాయా? | Payment status |
| `sale_paid` | డబ్బులు అందింది | Received |
| `sale_partial` | కొంత అందింది | Partly paid |
| `sale_pending` | బాకీ ఉంది | Pending due |
| `sale_mark_received` | డబ్బులు అందినట్లు మార్చండి | Mark Received |

---

### Group 10: Labour
- **Routes:** `LabourList(cropId?)`, `LabourForm(labourId?, cropId?)`
- **Key Screens:** Labour Entries List (with Unpaid Wages badge & 1-tap Mark Paid action), Add/Edit Labour Form
- **Primary Action:** `కూలీల వివరం రాయండి` (`Add Labour Entry`)
- **Spoken Audio Cue:** `cue_labour_help` — **TE:** *"రోజూ ఎంత మంది కూలీలు వచ్చారు, ఎంత కూలీ అయింది, ఎవరికి ఇంకా డబ్బులు ఇవ్వాలి అనేది ఇక్కడ సులువుగా రాసుకోవచ్చు."* | **EN:** *"Track daily worker count, wages, and unpaid dues easily here."*
- **States:**
  - `Loading`: *"కూలీల లెక్క వస్తోంది..."* / *"Loading labour records..."*
  - `Empty`: *"ఇంకా కూలీల వివరాలు రాయలేదు. కూలీల సంఖ్య, కూలీ రేటు రాసుకోండి."* / *"No labour records yet. Add worker count and daily wage."*
  - `Error`: *"కూలీల లెక్క లోడ్ కాలేదు."* / *"Unable to load labour records."*
  - `Offline`: *"కూలీల లెక్క ఫోన్‌లోనే ఉంది."* / *"Labour ledger saved offline."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `lab_title` | కూలీల లెక్క | Labour Ledger |
| `lab_add_btn` | కూలీల వివరం రాయండి | Add Labour |
| `lab_workers_label` | ఎంత మంది కూలీలు? | Number of workers |
| `lab_wage_label` | ఒక్కొక్కరికి కూలీ (₹) | Wage per worker (₹) |
| `lab_total_label` | మొత్తం కూలీ (₹) | Total wage (₹) |
| `lab_work_label` | చేసిన పని | Work done |
| `lab_gang_label` | మేస్త్రి / కూలీ పేరు (ఐచ్ఛికం) | Leader / Worker name (optional) |
| `lab_paid` | ఇచ్చేశాను | Paid |
| `lab_unpaid` | ఇంకా ఇవ్వాలి | To pay |
| `lab_att_full` | పూర్తి రోజు | Full day |
| `lab_att_half` | సగం రోజు | Half day |
| `lab_att_absent` | రాలేదు | Absent |
| `lab_att_piece` | గుత్త / పని లెక్క | Piece rate |
| `lab_advance` | అడ్వాన్స్ ఇచ్చాము | Advance paid |
| `lab_mark_pay_today` | హాజరు & ఈరోజు కూలీ ఇచ్చేయండి | Mark & Pay Today |

---

### Group 11: Machinery Rental
- **Routes:** `MachineryList(cropId?)`, `MachineryForm(rentalId?, cropId?)`
- **Key Screens:** Machinery Hire Ledger, Add/Edit Machine Rental Form
- **Primary Action:** `యంత్రం అద్దె రాయండి` (`Add Machine Hire`)
- **Spoken Audio Cue:** `cue_machinery_help` — **TE:** *"ట్రాక్టర్, హార్వెస్టర్ లేదా మందు పిచికారీ యంత్రం అద్దె గంటలు, బాడుగ ఇక్కడ దాచుకోవచ్చు."* | **EN:** *"Record tractor, harvester, or sprayer hours and rental dues here."*
- **States:**
  - `Loading`: *"యంత్రాల అద్దె వివరాలు వస్తున్నాయి..."* / *"Loading machinery rentals..."*
  - `Empty`: *"ఇంకా యంత్రం అద్దె రాయలేదు. ట్రాక్టర్ లేదా హార్వెస్టర్ ఖర్చు కలపండి."* / *"No machinery rentals logged yet. Add tractor or harvester hire."*
  - `Error`: *"యంత్రాల వివరాలు లోడ్ కాలేదు."* / *"Could not load machinery records."*
  - `Offline`: *"యంత్రాల అద్దె ఫోన్‌లోనే సేవ్ అయింది."* / *"Machinery hire saved on device."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `mach_title` | యంత్రాల అద్దె | Machinery Rental |
| `mach_add_btn` | యంత్రం అద్దె రాయండి | Add Machine Hire |
| `mach_type_label` | యంత్రం రకం | Machine type |
| `mach_tractor` | ట్రాక్టర్ | Tractor |
| `mach_harvester` | హార్వెస్టర్ | Harvester |
| `mach_rotavator` | రోటావేటర్ | Rotavator |
| `mach_sprayer` | పవర్ స్ప్రేయర్ | Power Sprayer |
| `mach_other` | ఇతర యంత్రం | Other machine |
| `mach_usage_label` | ఎన్ని గంటలు / ఎకరాలు | Hours or Acres |
| `mach_owner_label` | యజమాని పేరు | Owner name |
| `mach_rate_label` | అద్దె రేటు (₹) | Rate (₹) |
| `mach_timer_start` | టైమర్ మొదలుపెట్టండి | Start Timer |
| `mach_timer_stop` | టైమర్ ఆపి లెక్క కట్టండి | Stop & Calculate |
| `mach_use_own` | మన సొంత యంత్రం | Own farm use |
| `mach_use_hired_in` | అద్దెకు తెచ్చాము | Hired in (Expense) |
| `mach_use_hired_out` | అద్దెకు ఇచ్చాము | Hired out (Income) |

---

### Group 12: Input Inventory
- **Routes:** `InventoryList`, `InventoryForm(itemId?)`, `InventoryUsageForm(itemId)`
- **Key Screens:** Stock List (Seeds, Fertilizers, Pesticides with Low-Stock warning chip), Add/Edit Stock Purchase Form, Quick Log Usage sheet/screen
- **Primary Action:** `కొత్త సరుకు కలపండి` (`Add Stock`)
- **Spoken Audio Cue:** `cue_inventory_help` — **TE:** *"ఇంట్లో ఉన్న విత్తనాలు, ఎరువులు, మందుల నిల్వ ఇక్కడ చూడొచ్చు. వాడినప్పుడు 'వాడాను' అని నొక్కితే మిగిలిన నిల్వ ఆటోమేటిక్‌గా మారుతుంది."* | **EN:** *"Check remaining seeds, fertilizers, and pesticides here. Tap 'Use' whenever you apply stock to update the balance automatically."*
- **States:**
  - `Loading`: *"నిల్వ వివరాలు వస్తున్నాయి..."* / *"Loading input stock..."*
  - `Empty`: *"ఇంకా విత్తనాలు, ఎరువుల నిల్వ రాయలేదు. కొన్న సరుకును ఇక్కడ కలపండి."* / *"No stock items added yet. Add purchased seeds or fertilizers."*
  - `Error`: *"నిల్వ వివరాలు రావడం లేదు."* / *"Could not load inventory."*
  - `Offline`: *"నిల్వ లెక్క ఫోన్‌లోనే ఉంది."* / *"Inventory stored locally."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `inv_title` | విత్తనాలు & మందుల నిల్వ | Input Inventory |
| `inv_add_btn` | కొత్త సరుకు కలపండి | Add Stock |
| `inv_use_btn` | వాడినది రాయండి | Log Usage |
| `inv_item_name` | సరుకు పేరు | Item name |
| `inv_item_hint` | ఉదా: యూరియా, డీఏపీ, పత్తి విత్తనాలు | e.g., Urea, DAP, Cotton seeds |
| `inv_qty_avail` | మిగిలిన నిల్వ | Available stock |
| `inv_used_qty` | ఎంత వాడారు? | Quantity used |
| `inv_low_stock` | నిల్వ తక్కువగా ఉంది | Low stock |
| `inv_in_stock` | నిల్వ ఉంది | In stock |
| `inv_out_stock` | అయిపోయింది | Out of stock |

---

### Group 13: Irrigation
- **Routes:** `IrrigationList(plotId?, cropId?)`, `IrrigationForm(logId?, plotId?, cropId?)`
- **Key Screens:** Watering History & Due Reminders List, Log Watering Form
- **Primary Action:** `నీరు పెట్టినట్లు రాయండి` (`Log Watering`)
- **Spoken Audio Cue:** `cue_irrigation_help` — **TE:** *"ఏ మడికి ఎప్పుడు నీరు పెట్టారు, మళ్ళీ ఎప్పుడు పెట్టాలో ఇక్కడ రాసుకోండి. టైం అయినప్పుడు హోమ్ పేజీలో మీకు గుర్తుచేస్తుంది."* | **EN:** *"Log when each plot was watered and set the next watering date. Reminders appear right on your Home screen."*
- **States:**
  - `Loading`: *"నీటి తడుల వివరాలు వస్తున్నాయి..."* / *"Loading watering logs..."*
  - `Empty`: *"ఇంకా నీటి తడులు రాయలేదు. ఈరోజు నీరు పెట్టిన మడి వివరం రాయండి."* / *"No irrigation logs yet. Record your latest plot watering."*
  - `Error`: *"నీటి తడుల వివరాలు లోడ్ కాలేదు."* / *"Unable to load irrigation records."*
  - `Offline`: *"నీటి తడుల రిమైండర్ ఫోన్‌లోనే పనిచేస్తుంది."* / *"Watering reminders work offline."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `irr_title` | నీటి తడులు | Irrigation |
| `irr_add_btn` | నీరు పెట్టినట్లు రాయండి | Log Watering |
| `irr_source_label` | నీటి వనరు | Water source |
| `irr_bore` | బోరు బావి | Borewell |
| `irr_canal` | కాలువ / చెరువు | Canal / Tank |
| `irr_drip` | బిందు సేద్యం (డ్రిప్) | Drip irrigation |
| `irr_rain` | వర్షం పడింది | Rainfed |
| `irr_duration_label` | ఎంత సమయం పెట్టారు? (గంటలు) | Duration (Hours) |
| `irr_next_date` | మళ్ళీ నీరు పెట్టే తేదీ | Next watering date |
| `irr_due_badge` | నీరు పెట్టాలి | Watering due |
| `irr_ok_badge` | తడి బాగుంది | Watered |

---

### Group 14: Harvest & Storage
- **Routes:** `HarvestList(cropId?)`, `HarvestForm(harvestId?, cropId?)`
- **Key Screens:** Harvest Yield & Stored Stock List (shows total harvested vs in-store vs sold), Add/Edit Harvest Form
- **Primary Action:** `కోత దిగుబడి రాయండి` (`Add Harvest`)
- **Spoken Audio Cue:** `cue_harvest_help` — **TE:** *"కోత తర్వాత ఎన్ని బస్తాలు లేదా క్వింటాళ్ళు వచ్చాయో, అవి ఇంట్లో ఉన్నాయా లేదా గోడౌన్‌లో పెట్టారా అనేది ఇక్కడ రాసుకోవచ్చు."* | **EN:** *"Record bags or quintals harvested and track where your produce is stored."*
- **States:**
  - `Loading`: *"కోత & నిల్వ వివరాలు వస్తున్నాయి..."* / *"Loading harvest and storage..."*
  - `Empty`: *"ఇంకా కోత వివరాలు రాయలేదు. పంట కోసిన తర్వాత వచ్చిన దిగుబడిని కలపండి."* / *"No harvest records yet. Log crop yield after harvesting."*
  - `Error`: *"కోత వివరాలు లోడ్ కాలేదు."* / *"Could not load harvest records."*
  - `Offline`: *"కోత రికార్డు ఫోన్‌లోనే భద్రంగా ఉంది."* / *"Harvest data saved locally."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `harv_title` | కోత & నిల్వ | Harvest & Storage |
| `harv_add_btn` | కోత దిగుబడి రాయండి | Add Harvest |
| `harv_yield_label` | వచ్చిన దిగుబడి | Harvested quantity |
| `harv_date_label` | కోత తేదీ | Harvest date |
| `harv_place_label` | ఎక్కడ నిల్వ చేశారు? | Storage location |
| `harv_home` | ఇంటి దగ్గర | At home |
| `harv_godown` | గోడౌన్ / మార్కెట్ యార్డ్ | Godown / Yard |
| `harv_sold` | కల్లంలోనే అమ్మేశాము | Sold immediately |
| `harv_remaining` | నిల్వలో మిగిలింది | Remaining in store |

---

### Group 15: Soil Test Records
- **Routes:** `SoilTestList(plotId?)`, `SoilTestDetail(testId)`, `SoilTestForm(testId?, plotId?)`
- **Key Screens:** Soil Test History List, Soil Card Detail (pH, N/P/K status badges + photo viewer), Manual Add/Edit Soil Test Form (**100% manual + photo attachment — ZERO OCR**)
- **Primary Action:** `కొత్త మట్టి పరీక్ష రాయండి` (`Add Soil Test`)
- **Spoken Audio Cue:** `cue_soil_help` — **TE:** *"మీ మడి మట్టి పరీక్ష కార్డు ఫోటో తీసి ఇందులో దాచుకోండి. pH, నత్రజని, భాస్వరం విలువలు కూడా చేత్తో రాసి పెట్టుకోవచ్చు."* | **EN:** *"Snap a photo of your soil health card to store safely here, and manually type pH or N-P-K levels if you wish."*
- **States:**
  - `Loading`: *"మట్టి పరీక్ష వివరాలు వస్తున్నాయి..."* / *"Loading soil test records..."*
  - `Empty`: *"ఇంకా మట్టి పరీక్ష కార్డు కలపలేదు. మీ మట్టి పరీక్ష కాగితం ఫోటో తీసి దాచుకోండి."* / *"No soil test records yet. Add your soil card photo and values."*
  - `Error`: *"మట్టి పరీక్ష వివరం లోడ్ కాలేదు."* / *"Unable to load soil test."*
  - `Offline`: *"మట్టి కార్డు ఫోటో ఫోన్‌లోనే సేవ్ అవుతుంది."* / *"Soil card photo saved on phone."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `soil_title` | మట్టి పరీక్షలు | Soil Test Records |
| `soil_add_btn` | మట్టి పరీక్ష కలపండి | Add Soil Test |
| `soil_date_label` | పరీక్ష చేసిన తేదీ | Test date |
| `soil_lab_label` | ల్యాబ్ / కేంద్రం పేరు | Lab / Center name |
| `soil_ph_label` | pH విలువ (ఉదా: 7.2) | pH value (e.g. 7.2) |
| `soil_n_label` | నత్రజని (N) | Nitrogen (N) |
| `soil_p_label` | భాస్వరం (P) | Phosphorus (P) |
| `soil_k_label` | పొటాష్ (K) | Potassium (K) |
| `soil_lvl_low` | తక్కువ | Low |
| `soil_lvl_med` | సరిపడా | Medium |
| `soil_lvl_high` | ఎక్కువ | High |
| `soil_card_photo` | మట్టి కార్డు ఫోటో పెట్టండి | Attach soil card photo |

---

### Group 16: Contacts
- **Routes:** `ContactList`, `ContactDetail(contactId)`, `ContactForm(contactId?)`
- **Key Screens:** Searchable Contacts List (filter chips: Traders, Input Shops, Labour Leaders, Tractor Owners, Officers), Contact Detail (linked dues + 1-tap Phone Call button), Add/Edit Contact Form
- **Primary Action:** `కొత్త పేరు కలపండి` (`Add Contact`)
- **Spoken Audio Cue:** `cue_contacts_help` — **TE:** *"మందుల షాప్ వాళ్ళు, పంట కొనే వ్యాపారులు, ట్రాక్టర్ యజమానుల నంబర్లు ఇక్కడ పెట్టుకుంటే ఒకే నొక్కుతో ఫోన్ చేయొచ్చు."* | **EN:** *"Save trusted input dealers, crop buyers, and tractor owners here for 1-tap calling."*
- **States:**
  - `Loading`: *"పరిచయాల జాబితా వస్తోంది..."* / *"Loading contacts..."*
  - `Empty`: *"ఇంకా ఎవరి నంబర్లు కలపలేదు. వ్యాపారి లేదా ట్రాక్టర్ యజమాని నంబర్ సేవ్ చేసుకోండి."* / *"No contacts saved yet. Add buyers, dealers, or tractor owners."*
  - `Error`: *"ఫోన్ కాల్ యాప్ తెరవలేకపోయాము."* / *"Could not open phone dialer."*
  - `Offline`: *"పరిచయాలన్నీ ఫోన్‌లోనే ఉన్నాయి; కాల్ చేయడానికి ఫోన్ సిగ్నల్ సరిపోతుంది."* / *"Contacts stored locally; standard phone network used for calls."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `con_title` | పరిచయాలు & వ్యాపారులు | Contacts |
| `con_add_btn` | కొత్త పేరు కలపండి | Add Contact |
| `con_name_label` | పేరు | Name |
| `con_phone_label` | ఫోన్ నంబర్ | Phone number |
| `con_role_label` | వీరు ఎవరు? | Contact type |
| `con_buyer` | పంట వ్యాపారి | Crop Buyer / Trader |
| `con_dealer` | ఎరువులు / మందుల షాప్ | Input Dealer |
| `con_labour` | కూలీ మేస్త్రి | Labour Leader |
| `con_machine` | ట్రాక్టర్ యజమాని | Machinery Owner |
| `con_officer` | వ్యవసాయ అధికారి | Agri Officer |
| `con_call_btn` | ఫోన్ చేయండి | Call Now |

---

### Group 17: Reports
- **Routes:** `ReportsOverview`
- **Key Screens:** Offline Season & Crop-Wise Profit/Loss Report (Expense vs Income summary cards, simple category bars, pending receivables/payables, offline text/CSV/PDF share via Android Share Sheet)
- **Primary Action:** `లెక్క షేర్ చేయండి` (`Share Summary`)
- **Spoken Audio Cue:** `cue_reports_summary` — **TE:** *"ఇక్కడ మీ పంటల వారీ లాభం లేదా నష్టం పూర్తిగా చూడొచ్చు. పైన మొత్తం ఖర్చు, వచ్చిన ఆదాయం, చేతికి మిగిలిన డబ్బులు కనిపిస్తాయి."* | **EN:** *"See your complete crop-wise profit or loss here. Top cards show total expense, total income, and your net balance."*
- **States:**
  - `Loading`: *"లాభ-నష్టాల లెక్క తయారవుతోంది..."* / *"Preparing profit & loss report..."*
  - `Empty`: *"నివేదిక చూపించడానికి ఇంకా ఖర్చులు లేదా అమ్మకాలు రాయలేదు."* / *"Add some expenses or sales to see your profit & loss report."*
  - `Error`: *"నివేదిక ఫైల్ తయారు చేయలేకపోయాము."* / *"Could not generate report summary."*
  - `Offline`: *"ఈ లెక్క మొత్తం మీ ఫోన్‌లోనే లెక్కించబడింది."* / *"Calculated 100% offline on your phone."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `rep_title` | లాభ-నష్టాల నివేదిక | Profit & Loss Report |
| `rep_season_filter` | సీజన్ ఎంచుకోండి | Select season |
| `rep_total_exp` | మొత్తం ఖర్చు | Total Expense |
| `rep_total_inc` | మొత్తం ఆదాయం | Total Income |
| `rep_net_profit` | నికర లాభం | Net Profit |
| `rep_net_loss` | నికర నష్టం | Net Loss |
| `rep_pending_in` | రావలసిన బాకీ | Money to Receive |
| `rep_pending_out` | ఇవ్వవలసిన బాకీ | Money to Pay |
| `rep_crop_breakdown` | పంటల వారీ లెక్క | Crop-wise Breakdown |
| `rep_share_btn` | వివరాలు షేర్ చేయండి | Share Summary |

---

### Group 18: More Hub (Tab 4) & Settings / Backup
- **Routes:** `MoreTab`, `SettingsHome`, `BackupRestore`, `OfflineHelp`
- **Key Screens:** More Services Tile Grid, Settings (1-Tap Telugu/English Switch, Spoken Audio Guide toggle + Test Voice), Local Backup Export (ZIP/JSON to device Downloads/USB) & Restore File Picker (with overwrite confirmation dialog), Offline Help Guide
- **Primary Action:** `ఫోన్‌లో బ్యాకప్ తీయండి` (`Save Backup to Phone`)
- **Spoken Audio Cue:** `cue_settings_backup` — **TE:** *"ఇక్కడ భాషను తెలుగు నుండి ఇంగ్లీష్‌కు మార్చుకోవచ్చు. మీ ఫోన్ మారినా డేటా పోకుండా బ్యాకప్ ఫైల్ తీసి దాచుకోవచ్చు."* | **EN:** *"Switch between Telugu and English here, turn spoken audio help on or off, and save a backup file of all your records."*
- **States:**
  - `Loading`: *"బ్యాకప్ ఫైల్ సిద్ధమవుతోంది... ఆగండి"* / *"Preparing backup file... please wait"*
  - `Empty` (No backups done yet): *"మీరు ఇంకా బ్యాకప్ తీయలేదు. వారానికోసారి బ్యాకప్ తీసి దాచుకోండి."* / *"No backup saved yet. Save a backup once a week for safety."*
  - `Error` (Corrupt/wrong backup file): *"ఈ బ్యాకప్ ఫైల్ సరిగా లేదు. ఫామ్ సాథీ బ్యాకప్ ఫైల్ మాత్రమే ఎంచుకోండి."* / *"Invalid backup file. Please choose a valid Farm Saathi backup file."*
  - `Offline`: *"బ్యాకప్ మీ ఫోన్ మెమరీలో సేవ్ అవుతుంది — ఇంటర్నెట్ అక్కర్లేదు."* / *"Backup saves to phone storage — no internet required."*

| Key | Telugu (Conversational AP/TS) | English |
|---|---|---|
| `more_title` | మరిన్ని వివరాలు | More Tools |
| `set_title` | సెట్టింగ్స్ & బ్యాకప్ | Settings & Backup |
| `set_lang_title` | యాప్ భాష | App Language |
| `set_lang_te` | తెలుగు | Telugu |
| `set_lang_en` | English (ఇంగ్లీష్) | English |
| `set_audio_title` | మాటలతో సాయం (ఆడియో గైడ్) | Spoken Audio Help |
| `set_audio_sub` | ప్రతి స్క్రీన్ వివరం వినిపిస్తుంది | Reads screen guidance aloud |
| `set_audio_test` | మాట పరీక్షించండి | Test Voice |
| `bak_title` | బ్యాకప్ & రీస్టోర్ | Backup & Restore |
| `bak_export_btn` | ఫోన్‌లో బ్యాకప్ తీయండి | Save Backup to Phone |
| `bak_restore_btn` | బ్యాకప్ తిరిగి తెచ్చుకోండి | Restore from Backup |
| `bak_last_date` | చివరి బ్యాకప్: %s | Last backup: %s |
| `bak_restore_confirm_title` | పాత బ్యాకప్ ఎక్కించాలా? | Restore from backup? |
| `bak_restore_confirm_msg` | ఇలా చేస్తే ఇప్పుడున్న వివరాల బదులు బ్యాకప్ ఫైల్‌లోని వివరాలు వస్తాయి. కొనసాగించాలా? | Current app data will be replaced by the selected backup file. Continue? |
| `bak_success` | బ్యాకప్ ఫైల్ డౌన్‌లోడ్స్‌లో సేవ్ అయింది | Backup saved in Downloads |
| `bak_restored` | మీ పాత వివరాలన్నీ తిరిగి వచ్చాయి | All records restored successfully |
| `help_title` | వాడే విధానం (సాయం) | How to Use (Offline Help) |

---

## 4. Strict Stage 1 Scope Verification Matrix

Confirming **ZERO Stage 2 or Stage 3 features** exist in routes, bottom tabs, tiles, form controls, settings, or copy strings:

| Excluded Stage 2 / Stage 3 Feature | Verification Status in Stage 1 UI & Routes |
|---|---|
| Weather / Rain Forecast API widgets | **ABSENT** — Zero weather cards, API calls, or location-weather permissions. |
| Crop Disease / Pest AI Camera Scanner | **ABSENT** — `FarmPhotoCapture` strictly saves & views raw photos offline; no "Scan leaf" / "Diagnose" UI. |
| Telugu Voice-to-Text (ASR) Dictation / Voice Search | **ABSENT** — `FarmSearchField` is text-only; `FarmVoiceRecorder` saves/plays local `.m4a` audio only with zero transcription. |
| OCR / Automatic Bill or Soil Card Scanning | **ABSENT** — Soil Tests & Expenses use manual numeric inputs + raw photo attachment only. |
| Facial Recognition / Biometric / Cloud OTP Login | **ABSENT** — Zero login screens, phone OTP flows, cloud accounts, or camera face-unlock prompts. |
| Live Mandi Price Feeds / Online Store / Loan Ads | **ABSENT** — Sales & Reports operate purely on farmer-entered prices and local Room SQL totals. |
| Cloud Sync / Server Backup Status | **ABSENT** — Backup is strictly local device ZIP/JSON file export & restore via Android Storage Access Framework (SAF). |
