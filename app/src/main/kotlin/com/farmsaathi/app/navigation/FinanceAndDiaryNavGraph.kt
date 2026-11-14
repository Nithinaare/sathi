package com.farmsaathi.app.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.farmsaathi.core.common.navigation.FarmRoute
import com.farmsaathi.feature.diary.screens.DiaryFormScreen
import com.farmsaathi.feature.diary.screens.DiaryListScreen
import com.farmsaathi.feature.diary.viewmodel.DiaryViewModel
import com.farmsaathi.feature.finance.screens.AccountsHubScreen
import com.farmsaathi.feature.finance.screens.ExpenseFormScreen
import com.farmsaathi.feature.finance.screens.LedgerDetailScreen
import com.farmsaathi.feature.finance.screens.SaleFormScreen
import com.farmsaathi.feature.finance.viewmodel.FinanceViewModel

fun NavGraphBuilder.financeAndDiaryNavGraph(nav: NavController) {
    composable<FarmRoute.AccountsTab> {
        val vm: FinanceViewModel = hiltViewModel()
        AccountsHubScreen(
            viewModel = vm,
            onAddExpense = { nav.navigate(FarmRoute.ExpenseForm()) },
            onAddSale = { nav.navigate(FarmRoute.SaleForm()) },
            onOpenLabour = { nav.navigate(FarmRoute.WorkerList) },
            onOpenMachinery = { nav.navigate(FarmRoute.MachineryList()) },
            onOpenReports = { nav.navigate(FarmRoute.ReportsOverview) },
            onOpenEntryDetail = { id -> nav.navigate(FarmRoute.ExpenseDetail(id)) },
        )
    }

    composable<FarmRoute.ExpenseForm> { backStack ->
        val route: FarmRoute.ExpenseForm = backStack.toRoute()
        val vm: FinanceViewModel = hiltViewModel()
        ExpenseFormScreen(
            initialCropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            initialPhotoPath = route.initialPhotoPath,
            initialDateMillis = route.initialDateMillis,
        )
    }

    composable<FarmRoute.SaleForm> { backStack ->
        val route: FarmRoute.SaleForm = backStack.toRoute()
        val vm: FinanceViewModel = hiltViewModel()
        SaleFormScreen(
            initialCropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.ExpenseDetail> { backStack ->
        val route: FarmRoute.ExpenseDetail = backStack.toRoute()
        val vm: FinanceViewModel = hiltViewModel()
        LedgerDetailScreen(
            ledgerEntryId = route.ledgerEntryId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.SaleDetail> { backStack ->
        val route: FarmRoute.SaleDetail = backStack.toRoute()
        val vm: FinanceViewModel = hiltViewModel()
        LedgerDetailScreen(
            ledgerEntryId = route.saleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.DiaryList> { backStack ->
        val route: FarmRoute.DiaryList = backStack.toRoute()
        val vm: DiaryViewModel = hiltViewModel()
        DiaryListScreen(
            cropCycleId = route.cropCycleId,
            plotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onAddEntry = { cId, pId -> nav.navigate(FarmRoute.DiaryForm(cropCycleId = cId, plotId = pId)) },
            onEditEntry = { id -> nav.navigate(FarmRoute.DiaryForm(entryId = id)) },
        )
    }

    composable<FarmRoute.DiaryForm> { backStack ->
        val route: FarmRoute.DiaryForm = backStack.toRoute()
        val vm: DiaryViewModel = hiltViewModel()
        DiaryFormScreen(
            entryId = route.entryId,
            initialCropCycleId = route.cropCycleId,
            initialPlotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            initialPhotoPath = route.initialPhotoPath,
            initialDateMillis = route.initialDateMillis,
        )
    }
}
