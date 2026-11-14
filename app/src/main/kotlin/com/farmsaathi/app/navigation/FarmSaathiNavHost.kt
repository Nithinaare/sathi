package com.farmsaathi.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.farmsaathi.core.common.navigation.FarmRoute
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmBottomNavigation
import com.farmsaathi.core.designsystem.components.FarmNavTab
import com.farmsaathi.core.designsystem.localization.LocalAppLanguage
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.model.AppLanguage

@Composable
fun FarmSaathiAppRoot(onboardingCompleted: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val useTe = LocalAppLanguage.current == AppLanguage.TELUGU

    val selectedTabId =
        when {
            currentDest?.hasRoute<FarmRoute.HomeTab>() == true -> TAB_HOME
            currentDest?.hasRoute<FarmRoute.FarmsTab>() == true -> TAB_FARMS
            currentDest?.hasRoute<FarmRoute.AccountsTab>() == true -> TAB_ACCOUNTS
            else -> null
        }

    val homeLabel = farmString(R.string.nav_home)
    val farmsLabel = farmString(R.string.nav_farms)
    val accountsLabel = farmString(R.string.nav_accounts)
    val tabs =
        remember(useTe, homeLabel, farmsLabel, accountsLabel) {
            listOf(
                FarmNavTab(
                    routeId = TAB_HOME,
                    label = homeLabel,
                    icon = Icons.Rounded.Home,
                    speakText =
                    if (useTe) {
                        "హోమ్ తెర. ఇక్కడ ఈ సీజన్ ఖర్చు ఆదాయం, పరిచయాలు, ముఖ్యమైన బటన్లు ఉంటాయి."
                    } else {
                        "Home tab. View season balance, contacts, and daily quick actions."
                    },
                ),
                FarmNavTab(
                    routeId = TAB_FARMS,
                    label = farmsLabel,
                    icon = Icons.Rounded.Landscape,
                    speakText =
                    if (useTe) {
                        "పొలాలు తెర. మీ పొలాలు, మడులు, పంటలు, నీటి తడులు, మందులు ఇక్కడ ఉంటాయి."
                    } else {
                        "Farms tab. Manage farms, plots, crops, irrigation, and inputs."
                    },
                ),
                FarmNavTab(
                    routeId = TAB_ACCOUNTS,
                    label = accountsLabel,
                    icon = Icons.Rounded.AccountBalanceWallet,
                    speakText =
                    if (useTe) {
                        "ఖాతాలు తెర. మీ ఖర్చులు, అమ్మకాలు, కూలీల లెక్క, ట్రాక్టర్ అద్దె, నివేదికలు చూడొచ్చు."
                    } else {
                        "Accounts tab. View expenses, sales, labour, tractor hire, and reports."
                    },
                ),
            )
        }

    Scaffold(
        containerColor = FarmColors.Background,
        bottomBar = {
            if (selectedTabId != null) {
                FarmBottomNavigation(
                    tabs = tabs,
                    selectedRouteId = selectedTabId,
                    onTabSelected = { tab ->
                        val target: FarmRoute =
                            when (tab.routeId) {
                                TAB_HOME -> FarmRoute.HomeTab
                                TAB_FARMS -> FarmRoute.FarmsTab
                                else -> FarmRoute.AccountsTab
                            }
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        val startDest: FarmRoute =
            if (onboardingCompleted) {
                FarmRoute.HomeTab
            } else {
                FarmRoute.OnboardingWelcome
            }
        NavHost(
            navController = navController,
            startDestination = startDest,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(180)) },
            modifier = Modifier.padding(innerPadding),
        ) {
            onboardingHomeMoreNavGraph(navController)
            farmAndCropsNavGraph(navController)
            financeAndDiaryNavGraph(navController)
            operationsNavGraph(navController)
        }
    }
}

private const val TAB_HOME = "home"
private const val TAB_FARMS = "farms"
private const val TAB_ACCOUNTS = "accounts"
