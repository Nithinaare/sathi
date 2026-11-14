package com.farmsaathi.app.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.farmsaathi.core.common.navigation.FarmRoute
import com.farmsaathi.feature.operations.screens.ContactDetailScreen
import com.farmsaathi.feature.operations.screens.ContactFormScreen
import com.farmsaathi.feature.operations.screens.ContactListScreen
import com.farmsaathi.feature.operations.screens.DailyAttendanceScreen
import com.farmsaathi.feature.operations.screens.HarvestAndStorageListScreen
import com.farmsaathi.feature.operations.screens.HarvestFormScreen
import com.farmsaathi.feature.operations.screens.InventoryListScreen
import com.farmsaathi.feature.operations.screens.InventoryProductDetailScreen
import com.farmsaathi.feature.operations.screens.InventoryProductFormScreen
import com.farmsaathi.feature.operations.screens.InventoryPurchaseFormScreen
import com.farmsaathi.feature.operations.screens.InventoryUsageFormScreen
import com.farmsaathi.feature.operations.screens.IrrigationFormScreen
import com.farmsaathi.feature.operations.screens.IrrigationListScreen
import com.farmsaathi.feature.operations.screens.MachineryDetailScreen
import com.farmsaathi.feature.operations.screens.MachineryListScreen
import com.farmsaathi.feature.operations.screens.MachineryManualFormScreen
import com.farmsaathi.feature.operations.screens.MachineryStartScreen
import com.farmsaathi.feature.operations.screens.SoilTestDetailScreen
import com.farmsaathi.feature.operations.screens.SoilTestFormScreen
import com.farmsaathi.feature.operations.screens.SoilTestListScreen
import com.farmsaathi.feature.operations.screens.WorkerDetailScreen
import com.farmsaathi.feature.operations.screens.WorkerFormScreen
import com.farmsaathi.feature.operations.screens.WorkerListScreen
import com.farmsaathi.feature.operations.screens.WorkerPaymentFormScreen
import com.farmsaathi.feature.operations.viewmodel.FieldOpsAndContactsViewModel
import com.farmsaathi.feature.operations.viewmodel.InventoryViewModel
import com.farmsaathi.feature.operations.viewmodel.LabourViewModel
import com.farmsaathi.feature.operations.viewmodel.MachineryViewModel

fun NavGraphBuilder.operationsNavGraph(nav: NavController) {
    labourNavRoutes(nav)
    machineryAndInventoryNavRoutes(nav)
    fieldOpsNavRoutes(nav)
    contactNavRoutes(nav)
}

private fun NavGraphBuilder.labourNavRoutes(nav: NavController) {
    composable<FarmRoute.WorkerList> {
        val vm: LabourViewModel = hiltViewModel()
        WorkerListScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onOpenDailyAttendance = { nav.navigate(FarmRoute.DailyAttendance()) },
            onOpenWorker = { id -> nav.navigate(FarmRoute.WorkerDetail(id)) },
            onAddWorker = { nav.navigate(FarmRoute.WorkerForm()) },
        )
    }
    composable<FarmRoute.DailyAttendance> {
        val vm: LabourViewModel = hiltViewModel()
        DailyAttendanceScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onAddWorker = { nav.navigate(FarmRoute.WorkerForm()) },
            onOpenWorkerList = { nav.navigate(FarmRoute.WorkerList) },
        )
    }
    composable<FarmRoute.WorkerDetail> { backStack ->
        val route: FarmRoute.WorkerDetail = backStack.toRoute()
        val vm: LabourViewModel = hiltViewModel()
        WorkerDetailScreen(
            workerId = route.workerId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onEditWorker = { id -> nav.navigate(FarmRoute.WorkerForm(id)) },
            onAddPayment = { id, isAdv -> nav.navigate(FarmRoute.WorkerPaymentForm(id, isAdv)) },
        )
    }
    composable<FarmRoute.WorkerForm> { backStack ->
        val route: FarmRoute.WorkerForm = backStack.toRoute()
        val vm: LabourViewModel = hiltViewModel()
        WorkerFormScreen(
            workerId = route.workerId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            initialPhotoPath = route.initialPhotoPath,
        )
    }
    composable<FarmRoute.WorkerPaymentForm> { backStack ->
        val route: FarmRoute.WorkerPaymentForm = backStack.toRoute()
        val vm: LabourViewModel = hiltViewModel()
        WorkerPaymentFormScreen(
            workerId = route.workerId,
            isAdvance = route.isAdvance,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.machineryAndInventoryNavRoutes(nav: NavController) {
    composable<FarmRoute.MachineryList> { backStack ->
        val route: FarmRoute.MachineryList = backStack.toRoute()
        val vm: MachineryViewModel = hiltViewModel()
        MachineryListScreen(
            cropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onStartTimer = { cId -> nav.navigate(FarmRoute.MachineryStartSession(cId)) },
            onOpenSession = { id -> nav.navigate(FarmRoute.MachineryDetail(id)) },
            onAddManualLog = { cId -> nav.navigate(FarmRoute.MachineryManualForm(cropCycleId = cId)) },
        )
    }
    composable<FarmRoute.MachineryStartSession> { backStack ->
        val route: FarmRoute.MachineryStartSession = backStack.toRoute()
        val vm: MachineryViewModel = hiltViewModel()
        MachineryStartScreen(
            initialCropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onStarted = { id ->
                nav.navigate(FarmRoute.MachineryDetail(id)) {
                    popUpTo<FarmRoute.MachineryStartSession> { inclusive = true }
                }
            },
        )
    }
    composable<FarmRoute.MachineryDetail> { backStack ->
        val route: FarmRoute.MachineryDetail = backStack.toRoute()
        val vm: MachineryViewModel = hiltViewModel()
        MachineryDetailScreen(sessionId = route.sessionId, viewModel = vm, onBack = { nav.popBackStack() })
    }
    composable<FarmRoute.MachineryManualForm> { backStack ->
        val route: FarmRoute.MachineryManualForm = backStack.toRoute()
        val vm: MachineryViewModel = hiltViewModel()
        MachineryManualFormScreen(
            initialCropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }
    composable<FarmRoute.InventoryList> {
        val vm: InventoryViewModel = hiltViewModel()
        InventoryListScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onOpenProduct = { id -> nav.navigate(FarmRoute.InventoryProductDetail(id)) },
            onAddProduct = { nav.navigate(FarmRoute.InventoryProductForm()) },
            onAddPurchase = { nav.navigate(FarmRoute.InventoryPurchaseForm()) },
            onRecordSpray = { nav.navigate(FarmRoute.InventoryUsageForm(productId = 0L)) },
        )
    }
    composable<FarmRoute.InventoryProductDetail> { backStack ->
        val route: FarmRoute.InventoryProductDetail = backStack.toRoute()
        val vm: InventoryViewModel = hiltViewModel()
        InventoryProductDetailScreen(
            productId = route.productId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onAddPurchase = { id -> nav.navigate(FarmRoute.InventoryPurchaseForm(id)) },
            onRecordUsage = { id -> nav.navigate(FarmRoute.InventoryUsageForm(id)) },
        )
    }
    composable<FarmRoute.InventoryProductForm> { backStack ->
        val route: FarmRoute.InventoryProductForm = backStack.toRoute()
        val vm: InventoryViewModel = hiltViewModel()
        InventoryProductFormScreen(productId = route.productId, viewModel = vm, onBack = { nav.popBackStack() })
    }
    composable<FarmRoute.InventoryPurchaseForm> { backStack ->
        val route: FarmRoute.InventoryPurchaseForm = backStack.toRoute()
        val vm: InventoryViewModel = hiltViewModel()
        InventoryPurchaseFormScreen(
            initialProductId = route.productId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
        )
    }
    composable<FarmRoute.InventoryUsageForm> { backStack ->
        val route: FarmRoute.InventoryUsageForm = backStack.toRoute()
        val vm: InventoryViewModel = hiltViewModel()
        InventoryUsageFormScreen(
            productId = route.productId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onOpenPurchase = { id -> nav.navigate(FarmRoute.InventoryPurchaseForm(id)) },
            initialPlotId = route.plotId,
            initialCropCycleId = route.cropCycleId,
            initialPhotoPath = route.initialPhotoPath,
            initialDateMillis = route.initialDateMillis,
            onAddProduct = { nav.navigate(FarmRoute.InventoryProductForm()) },
        )
    }
}

private fun NavGraphBuilder.fieldOpsNavRoutes(nav: NavController) {
    composable<FarmRoute.IrrigationList> { backStack ->
        val route: FarmRoute.IrrigationList = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        IrrigationListScreen(
            plotId = route.plotId,
            cropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onAddIrrigation = { pId, cId -> nav.navigate(FarmRoute.IrrigationForm(plotId = pId, cropCycleId = cId)) },
            onEditIrrigation = { id -> nav.navigate(FarmRoute.IrrigationForm(logId = id)) },
        )
    }
    composable<FarmRoute.IrrigationForm> { backStack ->
        val route: FarmRoute.IrrigationForm = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        IrrigationFormScreen(
            initialPlotId = route.plotId,
            initialCropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            initialPhotoPath = route.initialPhotoPath,
            initialDateMillis = route.initialDateMillis,
            logId = route.logId,
        )
    }
    composable<FarmRoute.HarvestList> { backStack ->
        val route: FarmRoute.HarvestList = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        HarvestAndStorageListScreen(
            cropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onAddHarvest = { cId -> nav.navigate(FarmRoute.HarvestForm(cropCycleId = cId)) },
            onAddCrop = { nav.navigate(FarmRoute.CropForm()) },
            onEditHarvest = { id -> nav.navigate(FarmRoute.HarvestForm(harvestId = id)) },
        )
    }
    composable<FarmRoute.HarvestForm> { backStack ->
        val route: FarmRoute.HarvestForm = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        HarvestFormScreen(
            initialCropCycleId = route.cropCycleId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            harvestId = route.harvestId,
            initialPhotoPath = route.initialPhotoPath,
            initialDateMillis = route.initialDateMillis,
            onAddCrop = { nav.navigate(FarmRoute.CropForm()) },
        )
    }
    composable<FarmRoute.SoilTestList> { backStack ->
        val route: FarmRoute.SoilTestList = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        SoilTestListScreen(
            plotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onAddSoilTest = { pId -> nav.navigate(FarmRoute.SoilTestForm(plotId = pId)) },
            onOpenDetail = { id -> nav.navigate(FarmRoute.SoilTestDetail(id)) },
        )
    }
    composable<FarmRoute.SoilTestDetail> { backStack ->
        val route: FarmRoute.SoilTestDetail = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        SoilTestDetailScreen(testId = route.testId, viewModel = vm, onBack = { nav.popBackStack() })
    }
    composable<FarmRoute.SoilTestForm> { backStack ->
        val route: FarmRoute.SoilTestForm = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        SoilTestFormScreen(
            initialPlotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            testId = route.testId,
            initialPhotoPath = route.initialPhotoPath,
            initialDateMillis = route.initialDateMillis,
            onAddPlot = { fId -> nav.navigate(FarmRoute.PlotForm(farmId = fId)) },
        )
    }
}

private fun NavGraphBuilder.contactNavRoutes(nav: NavController) {
    composable<FarmRoute.ContactList> {
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        ContactListScreen(
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onOpenContact = { id -> nav.navigate(FarmRoute.ContactDetail(id)) },
            onAddContact = { nav.navigate(FarmRoute.ContactForm()) },
        )
    }
    composable<FarmRoute.ContactDetail> { backStack ->
        val route: FarmRoute.ContactDetail = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        ContactDetailScreen(
            contactId = route.contactId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            onEdit = { id -> nav.navigate(FarmRoute.ContactForm(id)) },
        )
    }
    composable<FarmRoute.ContactForm> { backStack ->
        val route: FarmRoute.ContactForm = backStack.toRoute()
        val vm: FieldOpsAndContactsViewModel = hiltViewModel()
        ContactFormScreen(contactId = route.contactId, viewModel = vm, onBack = { nav.popBackStack() })
    }
}
