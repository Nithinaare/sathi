---
name: live-app-audit
description: >-
  Use this skill to run a multi-agent live ADB + UX + Accessibility + UI Alignment audit
  on Farm Saathi with non-overlapping screen slices across child agents, automated
  screenshot/UI-hierarchy inspection, rural/low-literacy research benchmarks, and a
  prioritized Keep / Change / Remove / Add report.
---

# Farm Saathi Live App Audit Playbook

## 1. Master Audit Prompt (For Orchestrator / Standalone Lead Agent)

Copy or invoke with this prompt when auditing Farm Saathi on a live Android device:

> Audit the **live running Farm Saathi app** (`com.farmsaathi.app`) for its real target users: **rural smallholder farmers**, including **illiterate, semi-literate, and first-time smartphone users** working outdoors in bright sunlight and noisy field conditions.
>
> **Strict Rules:**
> - Inspect **real device screenshots and live UI trees** using `scripts/adb_inspect.ps1` (do not guess layout from memory). Never run two ADB child agents against the same USB device concurrently — run each zone turn sequentially.
> - Every finding MUST cite **Screen -> Observed Evidence -> Why It Matters (Cause/Impact) -> Concrete Fix**.
>
> **Audit 4 Dimensions:**
> 1. **Accessibility & Inclusive Usability (with Reasons):** Touch targets (`>= 48dp`), TalkBack/screen-reader `contentDescription` coverage, click-hold / audio button discoverability, self-voiced Telugu clarity, outdoor sunlight contrast, error recovery when tapping wrong items, and zero-text fallback paths for non-readers. Explain **why** each issue blocks or slows a farmer.
> 2. **Live UI Alignment & Visual Craft (with Reasons):** Asymmetric controls, label wrapping/ellipsizing (`...`), overlapping cards/badges, image-to-meaning clarity (reused/confusing photos vs recognizable real-world objects), density/clutter per viewport, keyboard/bottom-bar occlusion. Explain **why** each defect harms comprehension or trust.
> 3. **Deep UX Research & Out-of-Box Thinking:** Benchmark the live journeys against rural/low-literacy ICT4D & Indian vernacular app references (**Indrani Medhi** *Text-Free User Interfaces*, **Digital Green FarmerChat** multimodal voice+photo flows, **Khatabook** color-coded zero-training ledgers). Count taps for core daily tasks and propose bold, offline-friendly (Stage 1: zero `INTERNET` permission) out-of-the-box ideas.
> 4. **Missing, Change, and Remove Matrix:**
>    - **Missing:** High-value capabilities farmers actually expect on-device offline.
>    - **Change:** Flows/labels/visuals that exist today but need reshaping.
>    - **Remove / Merge:** Redundant screens, dead controls, developer jargon, or low-value forms causing cognitive load.

---

## 2. Two Execution Modes (Parallel Full-Capture Recommended vs Sequential Live ADB)

### Mode A (Recommended): One-Function Full-App Capture + Parallel Child Audits

Avoid ADB USB lock contention and cut audit time by ~3x:
1. **Run the single-function capture script ONCE on the phone:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\skills\live-app-audit\scripts\capture_all_screens.ps1"
   ```
   Or dot-source and call the single exported function `Invoke-FarmSaathiFullCapture`:
   ```powershell
   . "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\skills\live-app-audit\scripts\capture_all_screens.ps1"
   Invoke-FarmSaathiFullCapture -OutRoot "...\scratch\audit\full_capture"
   ```
   This walks **Zones A, B, and C cleanly in one pass**, writes `capture_manifest.json` + PNG screenshots + UI XML + pre-parsed `*.nodes.txt` (`dp` dimensions, `clickable`, `content-desc`) + `zone_summary.txt` (`A11Y_SUMMARY`) into disjoint `zone_a/`, `zone_b/`, and `zone_c/` subdirectories, cleans device `/sdcard/fs_cap_*` temp files, and resets the phone back to Home.
2. **Invoke all 3 child agents IN PARALLEL in the same tool batch:**
   Pass each child agent ONLY its disjoint `full_capture\zone_<x>` folder and `capture_manifest.json` (plus tell them NOT to run ADB commands during parallel mode). Each agent reads every PNG visually with `read_file`, checks `zone_summary.txt` / `*.nodes.txt`, verifies Kotlin Compose source only where root cause is ambiguous, and writes its report independently.

### Mode B: Interactive Single-Screen ADB Follow-up (`adb_inspect.ps1`)

Use ONLY sequentially (one agent on the phone at a time) when testing dynamic interactive transitions not included in the static capture:
```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\skills\live-app-audit\scripts\adb_inspect.ps1" -Action <action> ...
```
- Never pipe raw `adb exec-out screencap -p >` in PowerShell (PowerShell UTF-16LE corrupts PNG bytes). Always pull via `adb_inspect.ps1`.
- `-Action reset | tap-text | tap | scroll | back | text | snapshot | focus`

### Shared Child Agent Rules
1. **Read Every Captured PNG Visually (`read_file`):** Do NOT rely on XML bounds alone — visually inspect alignment, text truncation, image clarity, scrim contrast, and empty states.
2. **Do NOT Modify App Source Code During Audit:** Read code/screens and write ONLY markdown reports in `...\scratch\audit\report_<zone>.md`.
3. **Stay Strictly Inside Your Exclusive Zone:** Never re-audit another agent's screens.

---

## 3. Child Agent Partition Map (3 Non-Overlapping Agents)

| Agent Name | Primary Audit Lens | Exclusive Screens (`full_capture/zone_*`) | Output Artifact |
| :--- | :--- | :--- | :--- |
| **`live-accessibility-auditor`** | A11y (`>=48dp`, TalkBack `contentDesc`, hold-to-speak, audio cues, illiterate zero-text path, sunlight contrast) | **Zone A (`zone_a/`):** Home Header/Hero/Quick Contacts/Bottom, Contacts List + Detail + Add/Empty-Save/More, Offline Search + Query Results, Settings + Area Unit Sheet | `report_accessibility.md` |
| **`live-ui-alignment-auditor`** | Pixel alignment, symmetry, label wrapping/truncation, scrim contrast, stock photo reuse/clarity, sticky-bar clipping, notch artifacts | **Zone B (`zone_b/`):** Money Tab (top/bottom/scrolled), P&L Report, Ledger Detail, New Expense + More, New Sale + More, Tractor List + Manual + Start Job, Extras Hub, Diary + Add, Inventory + Add, Harvest + Add, Soil + Add, Backup, Help Guide | `report_ui_alignment.md` |
| **`live-ux-journey-auditor`** | Deep farmer UX journeys, tap counts, cognitive load, Missing / Change / Remove audit, out-of-box offline agri-UX ideas | **Zone C (`zone_c/`):** Labour Hub (Wage Calculator tabs + Attendance + Workers), Add Worker (`ఫోటో + రేటు` & `పూర్తి వివరాలు`), Crops/Farms Tab + Farm Detail + `అదనపు సాగు సాధనాలు` Accordion + Crop Templates, Add Farm/Plot/Crop, Crops List, Tasks + Add Task, Watering + Add Watering + More | `report_ux_journeys.md` |

## 4. Zero-Collision Rule

Run `capture_all_screens.ps1` (`Invoke-FarmSaathiFullCapture`) first on the connected USB device (`adb_inspect.ps1` also must never run concurrently across agents). Once `capture_manifest.json` exists, `live-accessibility-auditor`, `live-ui-alignment-auditor`, and `live-ux-journey-auditor` run **in parallel** over their disjoint `zone_a`, `zone_b`, and `zone_c` capture folders with zero repeated work.
