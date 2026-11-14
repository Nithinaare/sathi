param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("reset", "snapshot", "tap", "tap-text", "scroll", "back", "text", "focus")]
    [string]$Action,

    [string]$Name = "screen",
    [int]$X = 0,
    [int]$Y = 0,
    [string]$Pattern = "",
    [int]$MinY = 0,
    [int]$MaxY = 99999,
    [int]$Index = 0,
    [ValidateSet("down", "up", "left", "right")]
    [string]$Direction = "down",
    [string]$InputText = "",
    [string]$OutDir = "C:\Users\nithi\.gemini\antigravity\brain\10b008e7-4e30-42dd-aafb-3cafee65ec3d\scratch\audit",
    [int]$WaitMs = 900
)

$ErrorActionPreference = "Stop"
$Pkg = "com.farmsaathi.app"
$Activity = "com.farmsaathi.app/.MainActivity"
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

function Get-DeviceDensity {
    $raw = (adb shell wm density 2>$null) -join " "
    if ($raw -match 'Override density:\s*(\d+)') { return [double]$Matches[1] }
    if ($raw -match 'Physical density:\s*(\d+)') { return [double]$Matches[1] }
    return 440.0
}

function Get-FocusWindow {
    $focus = (adb shell dumpsys window 2>$null | Select-String -Pattern "mCurrentFocus|mFocusedApp" | Select-Object -First 2) -join " | "
    return $focus
}

function Save-Snapshot([string]$Tag, [string]$Dir) {
    $pngDevice = "/sdcard/fs_audit_$Tag.png"
    $xmlDevice = "/sdcard/fs_audit_$Tag.xml"
    $pngLocal = Join-Path $Dir "$Tag.png"
    $xmlLocal = Join-Path $Dir "$Tag.xml"

    adb shell screencap -p $pngDevice | Out-Null
    adb pull $pngDevice $pngLocal | Out-Null
    adb shell uiautomator dump $xmlDevice | Out-Null
    adb pull $xmlDevice $xmlLocal | Out-Null

    $dpi = Get-DeviceDensity
    $dpScale = $dpi / 160.0
    $focus = Get-FocusWindow

    Write-Output "SNAPSHOT: png=$pngLocal xml=$xmlLocal dpi=$dpi focus=$focus"
    if ($focus -and ($focus -notmatch [regex]::Escape($Pkg))) {
        Write-Warning "FOCUS_LEFT_APP: $focus (consider running -Action back or -Action reset)"
    }

    if (Test-Path $xmlLocal) {
        [xml]$doc = Get-Content $xmlLocal -Encoding UTF8
        $unlabeledClickable = 0
        $smallTouchTargets = @()
        Write-Output "--- NODES ($Tag) ---"
        foreach ($n in $doc.SelectNodes("//node")) {
            $txt = $n.text
            $desc = $n.'content-desc'
            $clk = $n.clickable
            $b = $n.bounds
            if ($clk -eq "true" -or $txt -ne "" -or $desc -ne "") {
                if ($b -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
                    $x1 = [int]$Matches[1]; $y1 = [int]$Matches[2]
                    $x2 = [int]$Matches[3]; $y2 = [int]$Matches[4]
                    $wPx = $x2 - $x1; $hPx = $y2 - $y1
                    $wDp = [math]::Round($wPx / $dpScale, 1)
                    $hDp = [math]::Round($hPx / $dpScale, 1)
                    $cx = [int](($x1 + $x2) / 2); $cy = [int](($y1 + $y2) / 2)
                    if ($clk -eq "true") {
                        $hasDesc = $desc -ne "" -or $txt -ne ""
                        if (-not $hasDesc) {
                            $childText = ($n.SelectNodes(".//node") | ForEach-Object { if ($_.text -ne "") { $_.text } elseif ($_.'content-desc' -ne "") { $_.'content-desc' } }) -join " "
                            if (-not $childText) { $unlabeledClickable++ }
                        }
                        if ($wDp -lt 48.0 -or $hDp -lt 48.0) {
                            $smallTouchTargets += "${wDp}x${hDp}dp@($cx,$cy) text='$txt' desc='$desc'"
                        }
                    }
                    $cls = $n.class.Split('.')[-1]
                    Write-Output ("[{0}] click={1} center=({2},{3}) size={4}x{5}dp bounds={6} text='{7}' desc='{8}'" -f $cls, $clk, $cx, $cy, $wDp, $hDp, $b, $txt, $desc)
                }
            }
        }
        Write-Output "A11Y_SUMMARY: unlabeled_clickable=$unlabeledClickable small_targets_lt_48dp=$($smallTouchTargets.Count)"
        foreach ($st in $smallTouchTargets) {
            Write-Output "  SMALL_TARGET: $st"
        }
    }
}

switch ($Action) {
    "reset" {
        adb shell svc power stayon usb | Out-Null
        adb shell input keyevent KEYCODE_WAKEUP | Out-Null
        adb shell am force-stop $Pkg | Out-Null
        adb shell am start -n $Activity | Out-Null
        Start-Sleep -Milliseconds 2200
        Save-Snapshot -Tag $Name -Dir $OutDir
    }
    "snapshot" {
        Save-Snapshot -Tag $Name -Dir $OutDir
    }
    "focus" {
        Write-Output (Get-FocusWindow)
    }
    "tap" {
        adb shell input tap $X $Y
        Start-Sleep -Milliseconds $WaitMs
        Save-Snapshot -Tag $Name -Dir $OutDir
    }
    "tap-text" {
        $latestXml = Join-Path $OutDir ($Name + "_src.xml")
        adb shell uiautomator dump /sdcard/fs_audit_src.xml | Out-Null
        adb pull /sdcard/fs_audit_src.xml $latestXml | Out-Null
        [xml]$doc = Get-Content $latestXml -Encoding UTF8
        $matchesList = @()
        foreach ($n in $doc.SelectNodes("//node")) {
            if (($n.text -match $Pattern -or $n.'content-desc' -match $Pattern) -and $n.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
                $y1 = [int]$Matches[2]; $y2 = [int]$Matches[4]
                if ($y1 -ge $MinY -and $y2 -le $MaxY) {
                    $cx = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
                    $cy = [int](($y1 + $y2) / 2)
                    $matchesList += [pscustomobject]@{ X = $cx; Y = $cy; Text = $n.text; Desc = $n.'content-desc' }
                }
            }
        }
        if ($matchesList.Count -le $Index) {
            throw "No node matching '$Pattern' within Y=[$MinY,$MaxY] at index $Index (found $($matchesList.Count))."
        }
        $target = $matchesList[$Index]
        Write-Output "TAPPING: '$($target.Text)' desc='$($target.Desc)' at ($($target.X), $($target.Y))"
        adb shell input tap $target.X $target.Y
        Start-Sleep -Milliseconds $WaitMs
        Save-Snapshot -Tag $Name -Dir $OutDir
    }
    "scroll" {
        switch ($Direction) {
            "down"  { adb shell input swipe 540 1650 540 700 320 }
            "up"    { adb shell input swipe 540 700 540 1650 320 }
            "left"  { adb shell input swipe 900 1100 180 1100 300 }
            "right" { adb shell input swipe 180 1100 900 1100 300 }
        }
        Start-Sleep -Milliseconds $WaitMs
        Save-Snapshot -Tag $Name -Dir $OutDir
    }
    "back" {
        adb shell input keyevent KEYCODE_BACK
        Start-Sleep -Milliseconds $WaitMs
        Save-Snapshot -Tag $Name -Dir $OutDir
    }
    "text" {
        $escaped = $InputText -replace ' ', '%s'
        adb shell input text $escaped
        Start-Sleep -Milliseconds $WaitMs
        Save-Snapshot -Tag $Name -Dir $OutDir
    }
}
