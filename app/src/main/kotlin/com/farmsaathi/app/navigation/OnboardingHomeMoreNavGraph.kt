package com.farmsaathi.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.farmsaathi.core.common.navigation.FarmRoute
import com.farmsaathi.core.common.repository.SearchHitType
import com.farmsaathi.core.designsystem.components.FarmSnackbar
import com.farmsaathi.core.designsystem.components.SnackbarTone
import com.farmsaathi.core.designsystem.localization.LocalAppLanguage
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.feature.home.screens.BackupRestoreScreen
import com.farmsaathi.feature.home.screens.FarmingVideosScreen
import com.farmsaathi.feature.home.screens.HomeDashboardScreen
import com.farmsaathi.feature.home.screens.MoreHubScreen
import com.farmsaathi.feature.home.screens.OfflineHelpScreen
import com.farmsaathi.feature.home.screens.OnboardingAudioMicTestScreen
import com.farmsaathi.feature.home.screens.OnboardingProfileScreen
import com.farmsaathi.feature.home.screens.OnboardingWelcomeScreen
import com.farmsaathi.feature.home.screens.SettingsScreen
import com.farmsaathi.feature.home.screens.SnapToLogScreen
import com.farmsaathi.feature.home.viewmodel.HomeViewModel
import com.farmsaathi.feature.home.viewmodel.OnboardingViewModel
import com.farmsaathi.feature.home.viewmodel.SettingsAndBackupViewModel
import com.farmsaathi.feature.reports.screens.OfflineSearchScreen
import com.farmsaathi.feature.reports.screens.ReportsOverviewScreen
import com.farmsaathi.feature.reports.viewmodel.ReportsAndSearchViewModel

fun NavGraphBuilder.onboardingHomeMoreNavGraph(nav: NavController) {
    onboardingDestinations(nav)
    homeAndMoreDestinations(nav)
    utilityAndReportDestinations(nav)
    snapToLogDestination(nav)
}

private fun NavGraphBuilder.onboardingDestinations(nav: NavController) {
    composable<FarmRoute.OnboardingWelcome> {
        val vm: OnboardingViewModel = hiltViewModel()
        OnboardingWelcomeScreen(
            viewModel = vm,
            onContinue = { nav.navigate(FarmRoute.OnboardingAudioMicTest) },
        )
    }

    composable<FarmRoute.OnboardingAudioMicTest> {
        val vm: OnboardingViewModel = hiltViewModel()
        OnboardingAudioMicTestScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onContinue = { nav.navigate(FarmRoute.OnboardingProfile) },
        )
    }

    composable<FarmRoute.OnboardingProfile> {
        val vm: OnboardingViewModel = hiltViewModel()
        OnboardingProfileScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onCompleted = {
                nav.navigate(FarmRoute.HomeTab) {
                    popUpTo<FarmRoute.OnboardingWelcome> { inclusive = true }
                }
            },
        )
    }
}

private fun NavGraphBuilder.homeAndMoreDestinations(nav: NavController) {
    composable<FarmRoute.HomeTab> {
        val vm: HomeViewModel = hiltViewModel()
        HomeDashboardScreen(
            viewModel = vm,
            onAddExpense = { nav.navigate(FarmRoute.ExpenseForm()) },
            onAddSale = { nav.navigate(FarmRoute.SaleForm()) },
            onOpenActivities = { nav.navigate(FarmRoute.ActivityList()) },
            onOpenAttendance = { nav.navigate(FarmRoute.WorkerList) },
            onOpenMachinery = { nav.navigate(FarmRoute.MachineryList()) },
            onOpenAccounts = { nav.navigate(FarmRoute.AccountsTab) },
            onOpenTask = { nav.navigate(FarmRoute.ActivityList()) },
            onOpenMachinerySession = { id -> nav.navigate(FarmRoute.MachineryDetail(id)) },
            onOpenContacts = { nav.navigate(FarmRoute.ContactList()) },
            onOpenSearch = { nav.navigate(FarmRoute.OfflineSearch) },
            onOpenSettings = { nav.navigate(FarmRoute.SettingsHome) },
            onOpenBackup = { nav.navigate(FarmRoute.BackupRestore) },
            onOpenIrrigation = { nav.navigate(FarmRoute.IrrigationList()) },
            onOpenExtras = { nav.navigate(FarmRoute.MoreTab) },
            onOpenTeluguCalendar = { nav.navigate(FarmRoute.TeluguCalendar()) },
            onOpenLandMeasurement = { nav.navigate(FarmRoute.LandMeasurement()) },
            onSnapPhotoReady = { path, exifTs ->
                nav.navigate(FarmRoute.SnapToLog(photoPath = path, exifEpochMillis = exifTs))
            },
        )
    }

    composable<FarmRoute.MoreTab> {
        MoreHubScreen(
            onOpenDiary = { nav.navigate(FarmRoute.DiaryList()) },
            onOpenReports = { nav.navigate(FarmRoute.ReportsOverview) },
            onOpenHelp = { nav.navigate(FarmRoute.OfflineHelp) },
            onOpenBackup = { nav.navigate(FarmRoute.BackupRestore) },
            onOpenLandMeasurement = { nav.navigate(FarmRoute.LandMeasurement()) },
            onOpenVideos = { nav.navigate(FarmRoute.FarmingVideos()) },
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.FarmingVideos> {
        FarmingVideosScreen(
            onBack = { nav.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.utilityAndReportDestinations(nav: NavController) {
    composable<FarmRoute.SettingsHome> {
        val vm: SettingsAndBackupViewModel = hiltViewModel()
        SettingsScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onOpenBackup = { nav.navigate(FarmRoute.BackupRestore) },
        )
    }

    composable<FarmRoute.BackupRestore> {
        val vm: SettingsAndBackupViewModel = hiltViewModel()
        val useTe = LocalAppLanguage.current == AppLanguage.TELUGU
        var showRestoreHint by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize()) {
            BackupRestoreScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onRestoredRestartHint = { showRestoreHint = true },
            )
            if (showRestoreHint) {
                FarmSnackbar(
                    message =
                    if (useTe) {
                        "రిస్టోర్ పూర్తయింది — యాప్‌ను ఒకసారి మూసి మళ్ళీ తెరవండి"
                    } else {
                        "Restore complete — restart the app if older data still appears"
                    },
                    tone = SnackbarTone.SUCCESS,
                    actionLabel = if (useTe) "హోమ్" else "Home",
                    onActionClick = {
                        showRestoreHint = false
                        nav.navigate(FarmRoute.HomeTab) {
                            popUpTo<FarmRoute.HomeTab> { inclusive = true }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(FarmSpacing.dp16),
                )
            }
        }
    }

    composable<FarmRoute.OfflineHelp> {
        val vm: SettingsAndBackupViewModel = hiltViewModel()
        OfflineHelpScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.ReportsOverview> {
        val vm: ReportsAndSearchViewModel = hiltViewModel()
        ReportsOverviewScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.OfflineSearch> {
        val vm: ReportsAndSearchViewModel = hiltViewModel()
        OfflineSearchScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onSelectHit = { type, id ->
                when (type) {
                    SearchHitType.FARM -> nav.navigate(FarmRoute.FarmDetail(id))
                    SearchHitType.PLOT -> nav.navigate(FarmRoute.PlotDetail(id))
                    SearchHitType.CROP -> nav.navigate(FarmRoute.CropDetail(id))
                    SearchHitType.DIARY -> nav.navigate(FarmRoute.DiaryForm(entryId = id))
                    SearchHitType.LEDGER -> nav.navigate(FarmRoute.ExpenseDetail(id))
                    SearchHitType.WORKER -> nav.navigate(FarmRoute.WorkerDetail(id))
                    SearchHitType.CONTACT -> nav.navigate(FarmRoute.ContactDetail(id))
                    SearchHitType.INVENTORY -> nav.navigate(FarmRoute.InventoryProductDetail(id))
                }
            },
        )
    }
}

private fun NavGraphBuilder.snapToLogDestination(nav: NavController) {
    composable<FarmRoute.SnapToLog> { backStack ->
        val route: FarmRoute.SnapToLog = backStack.toRoute()
        val vm: HomeViewModel = hiltViewModel()
        SnapToLogScreen(
            photoPath = route.photoPath,
            exifEpochMillis = route.exifEpochMillis,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onRouteExpense = { path, ts ->
                nav.popBackStack()
                nav.navigate(FarmRoute.ExpenseForm(initialPhotoPath = path, initialDateMillis = ts))
            },
            onRouteIrrigation = { path, ts ->
                nav.popBackStack()
                nav.navigate(FarmRoute.IrrigationForm(initialPhotoPath = path, initialDateMillis = ts))
            },
            onRouteDiary = { path, ts ->
                nav.popBackStack()
                nav.navigate(FarmRoute.DiaryForm(initialPhotoPath = path, initialDateMillis = ts))
            },
            onRouteWorker = { path ->
                nav.popBackStack()
                nav.navigate(FarmRoute.WorkerForm(initialPhotoPath = path))
            },
            onRouteSpray = { path, ts ->
                nav.popBackStack()
                nav.navigate(FarmRoute.InventoryUsageForm(productId = 0L, initialPhotoPath = path, initialDateMillis = ts))
            },
            onRouteSoilTest = { path, ts ->
                nav.popBackStack()
                nav.navigate(FarmRoute.SoilTestForm(initialPhotoPath = path, initialDateMillis = ts))
            },
            onRouteHarvest = { path, ts ->
                nav.popBackStack()
                nav.navigate(FarmRoute.HarvestForm(initialPhotoPath = path, initialDateMillis = ts))
            },
        )
    }
}
