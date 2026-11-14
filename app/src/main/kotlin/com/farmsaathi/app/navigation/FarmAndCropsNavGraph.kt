package com.farmsaathi.app.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.farmsaathi.core.common.navigation.FarmRoute
import com.farmsaathi.feature.farm.measurement.ui.LandMeasurementScreen
import com.farmsaathi.feature.farm.screens.ActivityFormScreen
import com.farmsaathi.feature.farm.screens.ActivityListScreen
import com.farmsaathi.feature.farm.screens.CropDetailScreen
import com.farmsaathi.feature.farm.screens.CropFormScreen
import com.farmsaathi.feature.farm.screens.CropListScreen
import com.farmsaathi.feature.farm.screens.CropTemplatesScreen
import com.farmsaathi.feature.farm.screens.FarmDetailScreen
import com.farmsaathi.feature.farm.screens.FarmFormScreen
import com.farmsaathi.feature.farm.screens.FarmsListScreen
import com.farmsaathi.feature.farm.screens.KarteGuideScreen
import com.farmsaathi.feature.farm.screens.PlotDetailScreen
import com.farmsaathi.feature.farm.screens.PlotFormScreen
import com.farmsaathi.feature.farm.screens.TeluguCalendarScreen
import com.farmsaathi.feature.farm.viewmodel.CropAndCalendarViewModel
import com.farmsaathi.feature.farm.viewmodel.FarmAndPlotViewModel
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarViewModel

fun NavGraphBuilder.farmAndCropsNavGraph(nav: NavController) {
    farmAndPlotDestinations(nav)
    cropAndActivityDestinations(nav)
}

private fun NavGraphBuilder.farmAndPlotDestinations(nav: NavController) {
    composable<FarmRoute.FarmsTab> {
        val vm: FarmAndPlotViewModel = hiltViewModel()
        FarmsListScreen(
            viewModel = vm,
            onOpenFarm = { id -> nav.navigate(FarmRoute.FarmDetail(id)) },
            onAddFarm = { nav.navigate(FarmRoute.FarmForm()) },
            onOpenTemplates = { nav.navigate(FarmRoute.CropTemplateList) },
            onOpenCrops = { nav.navigate(FarmRoute.CropList()) },
            onAddPlot = { fId -> nav.navigate(FarmRoute.PlotForm(farmId = fId)) },
            onAddCropDirect = { nav.navigate(FarmRoute.CropForm()) },
            onOpenActivities = { nav.navigate(FarmRoute.ActivityList()) },
            onOpenIrrigation = { nav.navigate(FarmRoute.IrrigationList()) },
            onOpenInventory = { nav.navigate(FarmRoute.InventoryList) },
            onOpenHarvest = { nav.navigate(FarmRoute.HarvestList()) },
            onOpenSoilTests = { nav.navigate(FarmRoute.SoilTestList()) },
            onOpenSpray = { nav.navigate(FarmRoute.InventoryUsageForm(productId = 0L)) },
            onAddCropForFarm = { fId -> nav.navigate(FarmRoute.CropForm(farmId = fId)) },
        )
    }

    composable<FarmRoute.FarmDetail> { backStack ->
        val route: FarmRoute.FarmDetail = backStack.toRoute()
        val vm: FarmAndPlotViewModel = hiltViewModel()
        FarmDetailScreen(
            farmId = route.farmId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onEditFarm = { id -> nav.navigate(FarmRoute.FarmForm(id)) },
            onAddPlot = { fId -> nav.navigate(FarmRoute.PlotForm(farmId = fId)) },
            onOpenPlot = { pId -> nav.navigate(FarmRoute.PlotDetail(pId)) },
        )
    }

    composable<FarmRoute.FarmForm> { backStack ->
        val route: FarmRoute.FarmForm = backStack.toRoute()
        val vm: FarmAndPlotViewModel = hiltViewModel()
        FarmFormScreen(
            farmId = route.farmId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.PlotDetail> { backStack ->
        val route: FarmRoute.PlotDetail = backStack.toRoute()
        val vm: FarmAndPlotViewModel = hiltViewModel()
        PlotDetailScreen(
            plotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onEditPlot = { fId, pId -> nav.navigate(FarmRoute.PlotForm(fId, pId)) },
            onAddCrop = { pId -> nav.navigate(FarmRoute.CropForm(plotId = pId)) },
            onOpenCrop = { cId -> nav.navigate(FarmRoute.CropDetail(cId)) },
            onOpenIrrigation = { pId -> nav.navigate(FarmRoute.IrrigationList(plotId = pId)) },
            onOpenSoilTests = { pId -> nav.navigate(FarmRoute.SoilTestList(plotId = pId)) },
            onMeasurePlot = { pId -> nav.navigate(FarmRoute.LandMeasurement(plotId = pId)) },
        )
    }

    composable<FarmRoute.LandMeasurement> { backStack ->
        val route: FarmRoute.LandMeasurement = backStack.toRoute()
        LandMeasurementScreen(
            plotId = route.plotId,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.PlotForm> { backStack ->
        val route: FarmRoute.PlotForm = backStack.toRoute()
        val vm: FarmAndPlotViewModel = hiltViewModel()
        PlotFormScreen(
            farmId = route.farmId,
            plotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.cropAndActivityDestinations(nav: NavController) {
    composable<FarmRoute.CropList> { backStack ->
        val route: FarmRoute.CropList = backStack.toRoute()
        val vm: CropAndCalendarViewModel = hiltViewModel()
        CropListScreen(
            plotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onOpenCrop = { cId -> nav.navigate(FarmRoute.CropDetail(cId)) },
            onAddCrop = { pId -> nav.navigate(FarmRoute.CropForm(plotId = pId)) },
        )
    }

    composable<FarmRoute.CropDetail> { backStack ->
        val route: FarmRoute.CropDetail = backStack.toRoute()
        val vm: CropAndCalendarViewModel = hiltViewModel()
        CropDetailScreen(
            cropCycleId = route.cropId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onEditCrop = { cId -> nav.navigate(FarmRoute.CropForm(cropCycleId = cId)) },
            onOpenActivities = { cId -> nav.navigate(FarmRoute.ActivityList(cropCycleId = cId)) },
            onOpenDiary = { cId -> nav.navigate(FarmRoute.DiaryList(cropCycleId = cId)) },
            onAddExpense = { cId -> nav.navigate(FarmRoute.ExpenseForm(cropCycleId = cId)) },
            onAddHarvest = { cId -> nav.navigate(FarmRoute.HarvestForm(cropCycleId = cId)) },
        )
    }

    composable<FarmRoute.CropForm> { backStack ->
        val route: FarmRoute.CropForm = backStack.toRoute()
        val vm: CropAndCalendarViewModel = hiltViewModel()
        CropFormScreen(
            initialPlotId = route.plotId,
            cropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            initialFarmId = route.farmId,
        )
    }

    composable<FarmRoute.CropTemplateList> {
        val vm: CropAndCalendarViewModel = hiltViewModel()
        CropTemplatesScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }

    composable<FarmRoute.ActivityList> { backStack ->
        val route: FarmRoute.ActivityList = backStack.toRoute()
        val vm: CropAndCalendarViewModel = hiltViewModel()
        ActivityListScreen(
            cropCycleId = route.cropCycleId,
            plotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onAddActivity = { cId, pId -> nav.navigate(FarmRoute.ActivityForm(cropCycleId = cId, plotId = pId)) },
            onEditActivity = { tId -> nav.navigate(FarmRoute.ActivityForm(taskId = tId)) },
        )
    }

    composable<FarmRoute.ActivityForm> { backStack ->
        val route: FarmRoute.ActivityForm = backStack.toRoute()
        val vm: CropAndCalendarViewModel = hiltViewModel()
        ActivityFormScreen(
            initialCropCycleId = route.cropCycleId,
            initialPlotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            taskId = route.taskId,
            initialDueDateEpochDay = route.initialDueDateEpochDay,
        )
    }

    composable<FarmRoute.TeluguCalendar> { backStack ->
        val route: FarmRoute.TeluguCalendar = backStack.toRoute()
        val vm: TeluguCalendarViewModel = hiltViewModel()
        TeluguCalendarScreen(
            viewModel = vm,
            initialEpochDay = route.initialEpochDay,
            onBack = { nav.popBackStack() },
            onOpenKarteGuide = { karteId -> nav.navigate(FarmRoute.KarteGuide(highlightKarteId = karteId)) },
            onAddTask = { epochDay -> nav.navigate(FarmRoute.ActivityForm(initialDueDateEpochDay = epochDay)) },
        )
    }

    composable<FarmRoute.KarteGuide> { backStack ->
        val route: FarmRoute.KarteGuide = backStack.toRoute()
        val vm: TeluguCalendarViewModel = hiltViewModel()
        KarteGuideScreen(
            viewModel = vm,
            highlightKarteId = route.highlightKarteId,
            onBack = { nav.popBackStack() },
            onAddTask = { nav.navigate(FarmRoute.ActivityForm()) },
        )
    }
}
