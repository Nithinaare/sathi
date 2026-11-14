#Requires -Version 5.1
<#!
.SYNOPSIS
    Single-function whole-app live ADB screen capture automation for Farm Saathi.
.DESCRIPTION
    Dot-source this file and call `Invoke-FarmSaathiFullCapture` (or execute the script directly).
    Captures screenshots (PNG), UI Automator hierarchy (XML), and >=48dp / contentDescription
    telemetry across Zone A (Accessibility), Zone B (UI Alignment), and Zone C (UX Journeys)
    into disjoint folders plus `capture_manifest.json` so all child audit agents run in parallel
    with zero ADB collisions and zero duplicated work.
#>

function Invoke-FarmSaathiFullCapture {
    [CmdletBinding()]
    param(
        [string]$OutRoot = "C:\Users\nithi\.gemini\antigravity\brain\10b008e7-4e30-42dd-aafb-3cafee65ec3d\scratch\audit\full_capture",
        [int]$WaitMs = 650,
        [switch]$KeepAppStateAtEnd
    )

    $ErrorActionPreference = "Stop"
    $Pkg = "com.farmsaathi.app"
    $Activity = "com.farmsaathi.app/.MainActivity"
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

    function Run-Adb {
        param([Parameter(ValueFromRemainingArguments = $true)][object[]]$CmdArgs)
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $out = & adb.exe @CmdArgs 2>&1
            return ($out | ForEach-Object { "$_" })
        } finally {
            $ErrorActionPreference = $prevEap
        }
    }

    [void](Run-Adb start-server)

    $zoneDirs = @{
        zone_a = Join-Path $OutRoot "zone_a"
        zone_b = Join-Path $OutRoot "zone_b"
        zone_c = Join-Path $OutRoot "zone_c"
    }
    foreach ($z in $zoneDirs.Values) {
        if (Test-Path $z) {
            Remove-Item -Path (Join-Path $z "*") -Recurse -Force -ErrorAction SilentlyContinue
        }
        New-Item -ItemType Directory -Path $z -Force | Out-Null
    }

    function Get-DeviceDpi {
        $raw = (Run-Adb shell wm density) -join " "
        if ($raw -match 'Override density:\s*(\d+)') { return [double]$Matches[1] }
        if ($raw -match 'Physical density:\s*(\d+)') { return [double]$Matches[1] }
        return 450.0
    }

    $dpi = Get-DeviceDpi
    $dpScale = $dpi / 160.0
    $manifestItems = New-Object System.Collections.Generic.List[object]
    $skippedOptional = New-Object System.Collections.Generic.List[object]
    $zoneNotes = @{
        zone_a = New-Object System.Collections.Generic.List[string]
        zone_b = New-Object System.Collections.Generic.List[string]
        zone_c = New-Object System.Collections.Generic.List[string]
    }

    function Get-FocusInfo {
        return (((Run-Adb shell dumpsys window) | Select-String -Pattern "mCurrentFocus|mFocusedApp" | Select-Object -First 2) -join " | ").Trim()
    }

    function Assert-InApp {
        $f = Get-FocusInfo
        if ($f -and ($f -notmatch [regex]::Escape($Pkg))) {
            Write-Warning "Focus left $Pkg ($f). Pressing KEYCODE_BACK."
            [void](Run-Adb shell input keyevent KEYCODE_BACK)
            Start-Sleep -Milliseconds 600
            $f2 = Get-FocusInfo
            if ($f2 -and ($f2 -notmatch [regex]::Escape($Pkg))) {
                [void](Run-Adb shell am start -n $Activity)
                Start-Sleep -Milliseconds 1200
            }
        }
    }

    function Reset-LiveApp {
        param([int]$DelayMs = 2000)
        [void](Run-Adb shell svc power stayon usb)
        [void](Run-Adb shell input keyevent KEYCODE_WAKEUP)
        [void](Run-Adb shell am force-stop $Pkg)
        [void](Run-Adb shell am start -n $Activity)
        Start-Sleep -Milliseconds $DelayMs
    }

    function Dump-UiXml {
        param([string]$LocalXmlPath)
        $devXml = "/sdcard/fs_cap_dump.xml"
        for ($try = 0; $try -lt 3; $try++) {
            [void](Run-Adb shell uiautomator dump $devXml)
            [void](Run-Adb pull $devXml $LocalXmlPath)
            if (Test-Path $LocalXmlPath) {
                try {
                    [xml]$doc = Get-Content $LocalXmlPath -Encoding UTF8
                    return $doc
                } catch {
                    Start-Sleep -Milliseconds 350
                }
            }
        }
        throw "Failed to dump valid UI XML to $LocalXmlPath"
    }

    function Capture-DevicePng {
        param([string]$ScreenId, [string]$PngLocal)
        $tmpPng = "/data/local/tmp/fs_cap_$ScreenId.png"
        $sdPng = "/sdcard/fs_cap_$ScreenId.png"
        if (Test-Path $PngLocal) { Remove-Item $PngLocal -Force }

        $cap1 = (Run-Adb shell screencap -p $tmpPng) -join "`n"
        $pull1 = (Run-Adb pull $tmpPng $PngLocal) -join "`n"
        [void](Run-Adb shell rm -f $tmpPng)

        if (-not (Test-Path $PngLocal) -or (Get-Item $PngLocal).Length -lt 4096) {
            $cap2 = (Run-Adb shell screencap -p $sdPng) -join "`n"
            $pull2 = (Run-Adb pull $sdPng $PngLocal) -join "`n"
            [void](Run-Adb shell rm -f $sdPng)
            if (-not (Test-Path $PngLocal) -or (Get-Item $PngLocal).Length -lt 4096) {
                throw "Capture-DevicePng failed for '$ScreenId' -> '$PngLocal'. tmp=($cap1 | $pull1) sd=($cap2 | $pull2)"
            }
        }
    }

    function Save-CaptureScreen {
        param(
            [string]$Zone,
            [string]$ScreenId,
            [string]$Description
        )
        Assert-InApp
        $dir = $zoneDirs[$Zone]
        $pngLocal = Join-Path $dir "$ScreenId.png"
        $xmlLocal = Join-Path $dir "$ScreenId.xml"

        Capture-DevicePng -ScreenId $ScreenId -PngLocal $pngLocal
        $doc = Dump-UiXml -LocalXmlPath $xmlLocal
        $focus = Get-FocusInfo

        $unlabeled = 0
        $smallTargets = @()
        $nodeLines = New-Object System.Collections.Generic.List[string]

        foreach ($n in $doc.SelectNodes("//node")) {
            $txt = $n.text
            $desc = $n.'content-desc'
            $clk = $n.clickable
            $b = $n.bounds
            if (($clk -eq "true" -or $txt -ne "" -or $desc -ne "") -and $b -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
                $x1 = [int]$Matches[1]; $y1 = [int]$Matches[2]
                $x2 = [int]$Matches[3]; $y2 = [int]$Matches[4]
                $wDp = [math]::Round(($x2 - $x1) / $dpScale, 1)
                $hDp = [math]::Round(($y2 - $y1) / $dpScale, 1)
                $cx = [int](($x1 + $x2) / 2); $cy = [int](($y1 + $y2) / 2)
                if ($clk -eq "true") {
                    $hasLabel = ($desc -ne "") -or ($txt -ne "")
                    if (-not $hasLabel) {
                        $childLabel = ($n.SelectNodes(".//node") | ForEach-Object {
                            if ($_.text -ne "") { $_.text } elseif ($_.'content-desc' -ne "") { $_.'content-desc' }
                        }) -join " "
                        if (-not $childLabel) { $unlabeled++ }
                    }
                    if ($wDp -lt 48.0 -or $hDp -lt 48.0) {
                        $smallTargets += "${wDp}x${hDp}dp@($cx,$cy) text='$txt' desc='$desc'"
                    }
                }
                $cls = $n.class.Split('.')[-1]
                $nodeLines.Add(("[{0}] click={1} en={2} center=({3},{4}) size={5}x{6}dp bounds={7} text='{8}' desc='{9}'" -f $cls, $clk, $n.enabled, $cx, $cy, $wDp, $hDp, $b, $txt, $desc))
            }
        }

        $zoneNotes[$Zone].Add("[$ScreenId] $Description | focus=$focus | unlabeled=$unlabeled | small_lt_48dp=$($smallTargets.Count)")
        foreach ($st in $smallTargets) {
            $zoneNotes[$Zone].Add("   SMALL_TARGET: $st")
        }

        $txtSummaryPath = Join-Path $dir "$ScreenId.nodes.txt"
        $nodeLines -join "`r`n" | Set-Content -Path $txtSummaryPath -Encoding UTF8

        $item = [ordered]@{
            id                     = $ScreenId
            zone                   = $Zone
            description            = $Description
            png                    = $pngLocal
            xml                    = $xmlLocal
            nodes_txt              = $txtSummaryPath
            dpi                    = $dpi
            focus                  = $focus
            unlabeled_clickable    = $unlabeled
            small_targets_count    = $smallTargets.Count
            small_targets          = $smallTargets
        }
        $manifestItems.Add([pscustomobject]$item)
        Write-Host "CAPTURED [$Zone] $ScreenId -> $Description (unlabeled=$unlabeled small=$($smallTargets.Count))"
    }

    function Find-NodeCenter {
        param(
            [string]$Pattern,
            [int]$MinY = 0,
            [int]$MaxY = 99999,
            [int]$Index = 0,
            [switch]$RequireEnabled
        )
        $tmpXml = Join-Path $OutRoot "_probe.xml"
        $doc = Dump-UiXml -LocalXmlPath $tmpXml
        $hits = @()
        foreach ($n in $doc.SelectNodes("//node")) {
            if (($n.text -match $Pattern -or $n.'content-desc' -match $Pattern) -and $n.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
                $y1 = [int]$Matches[2]; $y2 = [int]$Matches[4]
                if ($y1 -ge $MinY -and $y2 -le $MaxY) {
                    if ($RequireEnabled -and $n.enabled -ne "true") { continue }
                    $cx = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
                    $cy = [int](($y1 + $y2) / 2)
                    $hits += [pscustomobject]@{ X = $cx; Y = $cy; Text = $n.text; Desc = $n.'content-desc'; Enabled = $n.enabled }
                }
            }
        }
        if ($hits.Count -le $Index) { return $null }
        return $hits[$Index]
    }

    function Tap-Match {
        param(
            [string]$Pattern,
            [int]$MinY = 0,
            [int]$MaxY = 99999,
            [int]$Index = 0,
            [switch]$Optional,
            [switch]$RequireEnabled,
            [int]$DelayMs = $WaitMs
        )
        $hit = Find-NodeCenter -Pattern $Pattern -MinY $MinY -MaxY $MaxY -Index $Index -RequireEnabled:$RequireEnabled
        if ($null -eq $hit) {
            if ($Optional) { return $false }
            throw "Tap-Match failed: no node matched '$Pattern' within Y=[$MinY,$MaxY] (index=$Index)"
        }
        [void](Run-Adb shell input tap $hit.X $hit.Y)
        Start-Sleep -Milliseconds $DelayMs
        return $true
    }

    function Tap-LargeCard {
        param(
            [int]$MinY = 500,
            [int]$MaxY = 1850,
            [double]$MinWidthDp = 240.0,
            [int]$Index = 0,
            [switch]$AvoidRightSideAction,
            [int]$DelayMs = $WaitMs
        )
        $tmpXml = Join-Path $OutRoot "_probe.xml"
        $doc = Dump-UiXml -LocalXmlPath $tmpXml
        $hits = @()
        foreach ($n in $doc.SelectNodes("//node")) {
            if ($n.clickable -eq "true" -and $n.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
                $x1 = [int]$Matches[1]; $y1 = [int]$Matches[2]
                $x2 = [int]$Matches[3]; $y2 = [int]$Matches[4]
                $wDp = ($x2 - $x1) / $dpScale
                $hDp = ($y2 - $y1) / $dpScale
                if ($y1 -ge $MinY -and $y2 -le $MaxY -and $wDp -ge $MinWidthDp -and $hDp -ge 55.0) {
                    $cx = if ($AvoidRightSideAction) { [int]($x1 + ($x2 - $x1) * 0.35) } else { [int](($x1 + $x2) / 2) }
                    $cy = [int](($y1 + $y2) / 2)
                    $hits += [pscustomobject]@{ X = $cx; Y = $cy; Bounds = $n.bounds }
                }
            }
        }
        if ($hits.Count -le $Index) { return $false }
        $t = $hits[$Index]
        [void](Run-Adb shell input tap $t.X $t.Y)
        Start-Sleep -Milliseconds $DelayMs
        return $true
    }

    function Scroll-Dir {
        param([ValidateSet("down", "up")][string]$Dir = "down", [int]$DelayMs = 600)
        if ($Dir -eq "down") {
            [void](Run-Adb shell input swipe 540 1650 540 680 320)
        } else {
            [void](Run-Adb shell input swipe 540 680 540 1650 320)
        }
        Start-Sleep -Milliseconds $DelayMs
    }

    function Back-App {
        param([int]$DelayMs = 600)
        $ime = ((Run-Adb shell dumpsys input_method) | Select-String -Pattern "mInputShown=true")
        if ($ime) {
            [void](Run-Adb shell input keyevent KEYCODE_BACK)
            Start-Sleep -Milliseconds 350
        }
        [void](Run-Adb shell input keyevent KEYCODE_BACK)
        Start-Sleep -Milliseconds $DelayMs
        Assert-InApp
    }

    function Record-Skip {
        param([string]$Zone, [string]$ScreenId, [string]$Reason)
        $skippedOptional.Add([pscustomobject]@{ zone = $Zone; id = $ScreenId; reason = $Reason })
        $zoneNotes[$Zone].Add("[SKIPPED $ScreenId] $Reason")
        Write-Host "SKIPPED [$Zone] $ScreenId : $Reason"
    }

    # =========================================================================
    # PASS 1: ZONE A (Accessibility Scope)
    # =========================================================================
    Reset-LiveApp -DelayMs 2400
    Save-CaptureScreen -Zone "zone_a" -ScreenId "a01_home_top" -Description "Home top viewport (Header pills, Season card, Snap hero card, top grid)"

    Scroll-Dir -Dir "down"
    Save-CaptureScreen -Zone "zone_a" -ScreenId "a02_home_mid" -Description "Home scrolled mid (Quick contacts / tasks / tiles)"

    Scroll-Dir -Dir "down"
    Save-CaptureScreen -Zone "zone_a" -ScreenId "a03_home_bottom" -Description "Home scrolled bottom (Extras card + bottom nav)"

    Scroll-Dir -Dir "up"
    Scroll-Dir -Dir "up"

    if (Tap-Match -Pattern "^ఫోన్లు$" -MinY 280 -MaxY 490) {
        Save-CaptureScreen -Zone "zone_a" -ScreenId "a04_contacts_list" -Description "Contacts list + search + category chips"

        if (Tap-LargeCard -MinY 650 -MaxY 1800 -MinWidthDp 250.0 -AvoidRightSideAction) {
            Save-CaptureScreen -Zone "zone_a" -ScreenId "a05_contact_detail" -Description "Contact detail screen"
            Back-App
        } else {
            Record-Skip -Zone "zone_a" -ScreenId "a05_contact_detail" -Reason "No saved contact rows present"
        }

        if (Tap-Match -Pattern "కొత్త పేరు కలపండి|Add.*Contact" -MinY 1800 -MaxY 2300 -Optional) {
            Save-CaptureScreen -Zone "zone_a" -ScreenId "a06_contact_add_top" -Description "Add Contact initial form (roles + name/phone/voice)"
            if (Tap-Match -Pattern "సేవ్ చేయండి|Save" -MinY 1900 -MaxY 2300 -Optional) {
                Save-CaptureScreen -Zone "zone_a" -ScreenId "a07_contact_add_empty_save" -Description "Add Contact validation state after tapping Save empty"
            }
            if (Tap-Match -Pattern "మరిన్ని వివరాలు|More details" -MinY 900 -MaxY 2050 -Optional) {
                Save-CaptureScreen -Zone "zone_a" -ScreenId "a08_contact_add_more" -Description "Add Contact More details expanded"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_a" -ScreenId "a09_contact_add_more_bottom" -Description "Add Contact scrolled bottom"
            }
            Back-App
        }
        Back-App
    }

    # Search
    if (Tap-Match -Pattern "^వెతకండి$" -MinY 280 -MaxY 490 -Optional) {
        Save-CaptureScreen -Zone "zone_a" -ScreenId "a10_search_empty" -Description "Offline Search initial screen (visual quick filters)"
        $searchBox = Find-NodeCenter -Pattern "పేరు.*ఫోన్|వెతకండి" -MinY 320 -MaxY 700
        if ($null -ne $searchBox) {
            [void](Run-Adb shell input tap $searchBox.X $searchBox.Y)
            Start-Sleep -Milliseconds 450
            [void](Run-Adb shell input text "Nithin")
            Start-Sleep -Milliseconds 650
            $ime = ((Run-Adb shell dumpsys input_method) | Select-String -Pattern "mInputShown=true")
            if ($ime) {
                [void](Run-Adb shell input keyevent KEYCODE_BACK)
                Start-Sleep -Milliseconds 400
            }
            Save-CaptureScreen -Zone "zone_a" -ScreenId "a11_search_results_nithin" -Description "Offline Search results for query Nithin"
        }
        Back-App
    }

    # Settings + Area Unit bottom sheet
    if (Tap-Match -Pattern "^సెట్టింగ్స్$" -MinY 280 -MaxY 490 -Optional) {
        Save-CaptureScreen -Zone "zone_a" -ScreenId "a12_settings_top" -Description "Settings top viewport (Language, Audio, Reminder, Area Unit)"
        if (Tap-Match -Pattern "ఎకరాలు \+|సెంట్లు|గుంటలు" -MinY 900 -MaxY 1400 -Optional) {
            Save-CaptureScreen -Zone "zone_a" -ScreenId "a13_settings_area_sheet" -Description "Area unit selector modal sheet"
            Back-App
        }
        Scroll-Dir -Dir "down"
        Save-CaptureScreen -Zone "zone_a" -ScreenId "a14_settings_bottom" -Description "Settings scrolled bottom (Counts, Backup CTA, Version)"
        Back-App
    }

    # =========================================================================
    # PASS 2: ZONE B (UI Alignment Scope — 100% Zone B Parity)
    # =========================================================================
    Reset-LiveApp -DelayMs 2200
    if (Tap-Match -Pattern "^డబ్బు$" -MinY 1980 -MaxY 2300) {
        Save-CaptureScreen -Zone "zone_b" -ScreenId "b01_money_tab_top" -Description "Money Tab top (Header, Summary card, 2x2 grid)"

        Scroll-Dir -Dir "down"
        Save-CaptureScreen -Zone "zone_b" -ScreenId "b02_money_tab_bottom" -Description "Money Tab scrolled mid (P&L card, Ledger search & entries)"

        Scroll-Dir -Dir "down"
        Save-CaptureScreen -Zone "zone_b" -ScreenId "b03_money_tab_bottom2" -Description "Money Tab scrolled lower (checks status-bar overlap)"

        Scroll-Dir -Dir "up"
        Save-CaptureScreen -Zone "zone_b" -ScreenId "b04_money_tab_mid_return" -Description "Money Tab mid-scroll return (pinned header check)"

        # Capture P&L Report (now near top/mid on AccountsHub)
        Scroll-Dir -Dir "down"
        if (Tap-Match -Pattern "లాభ-నష్టాల నివేదిక" -MinY 250 -MaxY 2000 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b05_reports_pnl_top" -Description "P&L Report top (Crop breakdown + Reports icon)"
            Scroll-Dir -Dir "down"
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b06_reports_pnl_bottom" -Description "P&L Report scrolled bottom"
            Back-App
        }

        # Capture first ledger entry detail if available
        if (Tap-LargeCard -MinY 850 -MaxY 1950 -MinWidthDp 250.0) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b07_ledger_detail" -Description "Ledger Entry Detail screen"
            Back-App
        } else {
            Record-Skip -Zone "zone_b" -ScreenId "b07_ledger_detail" -Reason "No ledger row tapped within Y=[850,1950]"
        }

        # Return to top of Money Tab
        Scroll-Dir -Dir "up"
        Scroll-Dir -Dir "up"

        # New Expense Form (top, mid, more expanded, more bottom)
        if (Tap-Match -Pattern "^ఖర్చు$" -MinY 900 -MaxY 1550 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b08_expense_top" -Description "New Expense initial view (8-tile grid, amount, chips)"
            Scroll-Dir -Dir "down"
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b09_expense_mid" -Description "New Expense scrolled (Camera/Gallery buttons, Voice card, More toggle)"
            if (Tap-Match -Pattern "మరిన్ని వివరాలు" -MinY 400 -MaxY 2000 -Optional) {
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b10_expense_more" -Description "New Expense More Details expanded"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b11_expense_more_bottom" -Description "New Expense More Details scrolled bottom"
            }
            Back-App
        }

        # New Sale Form (top, mid, more expanded, more bottom)
        if (Tap-Match -Pattern "^అమ్మకం$" -MinY 900 -MaxY 1650 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b12_sale_top" -Description "New Sale initial view (Aligned Quantity/Unit row, Amount, Crop)"
            Scroll-Dir -Dir "down"
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b13_sale_mid" -Description "New Sale scrolled (Sold date, Payment mode, More toggle)"
            if (Tap-Match -Pattern "మరిన్ని వివరాలు" -MinY 350 -MaxY 2050 -Optional) {
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b14_sale_more" -Description "New Sale More Details expanded (Buyer + Voice + Photo)"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b15_sale_more_bottom" -Description "New Sale More Details scrolled bottom"
            }
            Back-App
        }

        # Tractor / Machinery List + Manual + Start Job
        if (Tap-Match -Pattern "^ట్రాక్టర్$" -MinY 1450 -MaxY 1950 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b16_tractor_list" -Description "Tractor list top"
            Scroll-Dir -Dir "down"
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b17_tractor_bottom" -Description "Tractor list scrolled"

            if (Tap-Match -Pattern "పాత అద్దె చేత్తో రాయండి" -MinY 1900 -MaxY 2300 -Optional) {
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b18_tractor_manual_top" -Description "Tractor manual entry form top"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b19_tractor_manual_bottom" -Description "Tractor manual entry form scrolled bottom"
                Back-App
            }

            if (Tap-Match -Pattern "పని మొదలు" -MinY 1700 -MaxY 2020 -Optional) {
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b20_tractor_start_top" -Description "Tractor start live job form top"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b21_tractor_start_bottom" -Description "Tractor start live job form scrolled bottom"
                Back-App
            }
            Back-App
        }
    }

    # Streamlined 4-Tile Extras Hub (Diary, Reports, Backup, Help)
    Reset-LiveApp -DelayMs 2000
    Scroll-Dir -Dir "down"
    Scroll-Dir -Dir "down"
    if (Tap-Match -Pattern "అదనపు సౌకర్యాలు" -MinY 350 -MaxY 2050 -Optional) {
        Save-CaptureScreen -Zone "zone_b" -ScreenId "b22_extras_top" -Description "Streamlined 4-tile Extras Hub (Diary, Reports, Backup, Guide)"

        if (Tap-Match -Pattern "డైరీ" -MinY 300 -MaxY 1100 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b23_diary_list" -Description "Diary list (Single border, clickable photo/edit sheet)"
            if (Tap-Match -Pattern "కొత్త ఫోటో / మాట" -MinY 1900 -MaxY 2300 -Optional) {
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b24_diary_add_top" -Description "Diary add form top (Photo & Voice first)"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b25_diary_add_bottom" -Description "Diary add form scrolled bottom (Crop/Plot chips & sticky Save)"
                Back-App
            }
            Back-App
        }

        if (Tap-Match -Pattern "^బ్యాకప్$" -MinY 600 -MaxY 2150 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b36_backup_restore" -Description "Backup & Restore screen"
            Back-App
        }
        if (Tap-Match -Pattern "వాడే విధానం" -MinY 600 -MaxY 2150 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b37_help_guide" -Description "Offline Audio App Guide screen (Hold-to-speak tip)"
            Back-App
        }
        Scroll-Dir -Dir "down"
        Save-CaptureScreen -Zone "zone_b" -ScreenId "b35_extras_bottom" -Description "Extras Hub scrolled bottom"
    }

    # Crop-Lifecycle Tools now under Crops Tab (Spray/Inventory, Harvest, Soil Card)
    Reset-LiveApp -DelayMs 2000
    if (Tap-Match -Pattern "^పంటలు$" -MinY 1980 -MaxY 2300 -Optional) {
        if (Tap-Match -Pattern "మందు కొట్టాము" -MinY 500 -MaxY 1800 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b26_spray_form_top" -Description "Crop Spray / Usage quick 3-tap form top"
            Scroll-Dir -Dir "down"
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b27_spray_form_bottom" -Description "Crop Spray / Usage form bottom"
            Back-App
        }

        if (Tap-Match -Pattern "మందుల నిల్వ చూడండి|మందుల నిల్వ" -MinY 500 -MaxY 1900 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b28_inventory_list" -Description "Inventory Stock list screen"
            Back-App
        }

        if (Tap-Match -Pattern "కోత & నిల్వ" -MinY 500 -MaxY 1800 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b29_harvest_list" -Description "Harvest & Storage list"
            if (Tap-Match -Pattern "కోత దిగుబడి రాయండి" -MinY 1900 -MaxY 2300 -Optional) {
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b30_harvest_add_top" -Description "Harvest add form top (Aligned Qty/Unit)"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b31_harvest_add_bottom" -Description "Harvest add form bottom (Photo + storage chips + sticky Save)"
                Back-App
            }
            Back-App
        }

        if (Tap-Match -Pattern "మట్టి కార్డు" -MinY 500 -MaxY 1900 -Optional) {
            Save-CaptureScreen -Zone "zone_b" -ScreenId "b32_soil_list" -Description "Soil card list"
            if (Tap-Match -Pattern "మట్టి పరీక్ష కలపండి" -MinY 1850 -MaxY 2300 -Optional) {
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b33_soil_add_top" -Description "Soil add form top (Photo-first + Traffic light ratings)"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_b" -ScreenId "b34_soil_add_bottom" -Description "Soil add form bottom (Collapsed optional lab N/P/K)"
                Back-App
            }
            Back-App
        }
    }

    # =========================================================================
    # PASS 3: ZONE C (UX Journeys: Labour + Crops + Templates + Tasks + Watering)
    # =========================================================================
    Reset-LiveApp -DelayMs 2200
    Scroll-Dir -Dir "down"
    if (Tap-Match -Pattern "^కూలీలు$" -MinY 400 -MaxY 1950 -Optional) {
        Save-CaptureScreen -Zone "zone_c" -ScreenId "c01_labour_hub_top" -Description "Labour Hub top (Crew Wage Calculator + sticky Save CTA + Rupee chips)"

        if (Tap-Match -Pattern "కూలీ బాకీ" -MinY 350 -MaxY 1100 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c02_labour_worker_balance" -Description "Labour Worker Balance mode (clickable empty card or worker balances)"
            Tap-Match -Pattern "గుంపు లెక్క" -MinY 350 -MaxY 1100 -Optional | Out-Null
        }

        Scroll-Dir -Dir "down"
        Save-CaptureScreen -Zone "zone_c" -ScreenId "c03_labour_workers_bottom" -Description "Labour Hub scrolled Sun-stamp workers/roll call"

        if (Tap-Match -Pattern "రోజువారీ హాజరు" -MinY 250 -MaxY 1950 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c04_labour_daily_attendance" -Description "Unified Sun/Half-Sun Daily Attendance Roll Call screen"
            Back-App
        }

        Scroll-Dir -Dir "up"
        if (Tap-Match -Pattern "కొత్త కూలీ పేరు కలపండి|కొత్త కూలీ" -MinY 100 -MaxY 2300 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c05_worker_add_quick" -Description "Add Worker quick Photo + Voice Name cards"
            if (Tap-Match -Pattern "పూర్తి వివరాలు" -MinY 250 -MaxY 750 -Optional) {
                Save-CaptureScreen -Zone "zone_c" -ScreenId "c06_worker_add_full_top" -Description "Add Worker Full Form top (Rupee half-day chips)"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_c" -ScreenId "c07_worker_add_full_bottom" -Description "Add Worker Full Form bottom"
            }
            Back-App
        }
        Back-App
    }

    # Crops / Farms Tab (visible 2x2 crop tools + editable templates)
    Reset-LiveApp -DelayMs 2200
    if (Tap-Match -Pattern "^పంటలు$" -MinY 1980 -MaxY 2300) {
        Save-CaptureScreen -Zone "zone_c" -ScreenId "c08_crops_tab_top" -Description "Crops/Farms Tab top (Tiles + visible 2x2 Crop Tools)"

        if (Tap-LargeCard -MinY 850 -MaxY 1750 -MinWidthDp 250.0) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c09_farm_detail" -Description "Farm Detail screen (with PLOTS_HELP)"
            Back-App
        } else {
            Record-Skip -Zone "zone_c" -ScreenId "c09_farm_detail" -Reason "No clickable Farm card found in Y=[850,1750]"
        }

        Scroll-Dir -Dir "down"
        Save-CaptureScreen -Zone "zone_c" -ScreenId "c10_crops_tab_tools_scrolled" -Description "Crops Tab scrolled (Farm cards + visible Crop Tools)"
        if (Tap-Match -Pattern "పంట పట్టికలు" -MinY 350 -MaxY 1950 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c12_crop_templates" -Description "Editable Room-backed Crop Calendar Templates screen"
            Back-App
        }
        Scroll-Dir -Dir "up"

        if (Tap-Match -Pattern "కొత్త పొలం కలపండి" -MinY 1850 -MaxY 2300 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c13_farm_add_form" -Description "Add Farm Form"
            Back-App
        }

        if (Tap-Match -Pattern "కొత్త మడి కలపండి" -MinY 500 -MaxY 2050 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c14_plot_add_form" -Description "Add Plot Form (Soil chips + unit toggle)"
            Back-App
        }

        if (Tap-Match -Pattern "కొత్త పంట కలపండి" -MinY 500 -MaxY 2050 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c15_crop_add_form_top" -Description "Add Crop Cycle Form top (Surfaced Plot pills + Template + Duration)"
            Scroll-Dir -Dir "down"
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c16_crop_add_form_bottom" -Description "Add Crop Cycle Form scrolled bottom"
            Back-App
        }

        if (Tap-Match -Pattern "^పంటలు$" -MinY 300 -MaxY 1000 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c17_crops_list" -Description "Crop Cycles list screen"
            Back-App
        }

        if (Tap-Match -Pattern "^పనులు$" -MinY 300 -MaxY 1000 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c18_tasks_list" -Description "Farm Tasks & Calendar list (Clickable tasks)"
            if (Tap-Match -Pattern "కొత్త పని రాయండి" -MinY 1850 -MaxY 2300 -Optional) {
                Save-CaptureScreen -Zone "zone_c" -ScreenId "c19_task_add_top" -Description "Add Farm Task form top (Activity + Future date chips)"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_c" -ScreenId "c20_task_add_bottom" -Description "Add Farm Task form scrolled bottom"
                Back-App
            }
            Back-App
        }

        if (Tap-Match -Pattern "^నీళ్లు$" -MinY 300 -MaxY 1000 -Optional) {
            Save-CaptureScreen -Zone "zone_c" -ScreenId "c21_watering_list" -Description "Watering / Irrigation list (Clickable + Telugu min)"
            if (Tap-Match -Pattern "నీరు పెట్టినట్లు రాయండి" -MinY 1850 -MaxY 2300 -Optional) {
                Save-CaptureScreen -Zone "zone_c" -ScreenId "c22_watering_add_top" -Description "Add Watering Form top (Crop + Photo EXIF + Source)"
                Scroll-Dir -Dir "down"
                Save-CaptureScreen -Zone "zone_c" -ScreenId "c23_watering_add_bottom" -Description "Add Watering Form scrolled (Method + Time + Next-watering)"
                if (Tap-Match -Pattern "మరిన్ని వివరాలు" -MinY 400 -MaxY 2000 -Optional) {
                    Save-CaptureScreen -Zone "zone_c" -ScreenId "c24_watering_add_more" -Description "Add Watering More Details expanded"
                    Scroll-Dir -Dir "down"
                    Save-CaptureScreen -Zone "zone_c" -ScreenId "c25_watering_add_more_bottom" -Description "Add Watering More Details bottom"
                }
                Back-App
            }
            Back-App
        }
    }

    # Cleanup temp device files & return app cleanly to Home
    [void](Run-Adb shell rm -f /sdcard/fs_cap_dump.xml)
    $probeXml = Join-Path $OutRoot "_probe.xml"
    if (Test-Path $probeXml) { Remove-Item $probeXml -Force }

    foreach ($zName in @("zone_a", "zone_b", "zone_c")) {
        $zoneSummaryPath = Join-Path $zoneDirs[$zName] "zone_summary.txt"
        $zoneNotes[$zName] -join "`r`n" | Set-Content -Path $zoneSummaryPath -Encoding UTF8
    }

    $manifest = [ordered]@{
        generated_at     = (Get-Date).ToString("o")
        package          = $Pkg
        device_dpi       = $dpi
        total_captured   = $manifestItems.Count
        counts_by_zone   = [ordered]@{
            zone_a = @($manifestItems | Where-Object { $_.zone -eq "zone_a" }).Count
            zone_b = @($manifestItems | Where-Object { $_.zone -eq "zone_b" }).Count
            zone_c = @($manifestItems | Where-Object { $_.zone -eq "zone_c" }).Count
        }
        skipped_optional = $skippedOptional
        screens          = $manifestItems
    }
    $manifestPath = Join-Path $OutRoot "capture_manifest.json"
    $manifest | ConvertTo-Json -Depth 6 | Set-Content -Path $manifestPath -Encoding UTF8

    if (-not $KeepAppStateAtEnd) {
        Reset-LiveApp -DelayMs 1800
    }

    Write-Output "FULL_CAPTURE_COMPLETE: total=$($manifestItems.Count) manifest=$manifestPath"
    return $manifest
}

if ($MyInvocation.InvocationName -ne '.') {
    Invoke-FarmSaathiFullCapture
}
