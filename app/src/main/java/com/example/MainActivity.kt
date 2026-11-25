package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.GameMode
import com.example.ui.i18n.GameStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.screens.CombatScreen
import com.example.ui.screens.FighterSelectScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelsScreen
import com.example.ui.screens.MoveListScreen
import com.example.ui.screens.ProgressionScreen
import com.example.ui.theme.ArenaChampionsTheme
import com.example.viewmodel.GameViewModel

private const val ROUTE_HOME = "home"
private const val ROUTE_LEVELS = "levels"
private const val ROUTE_SELECT = "fighter_select"
private const val ROUTE_ARENA = "combat_arena"
private const val ROUTE_MOVELIST = "move_list"
private const val ROUTE_PROGRESSION = "progression"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            ArenaChampionsTheme {
                val gameViewModel: GameViewModel = viewModel()
                val stats by gameViewModel.playerStats.collectAsStateWithLifecycle()
                val selection by gameViewModel.selection.collectAsStateWithLifecycle()
                val hasActiveMatch by gameViewModel.hasActiveMatch.collectAsStateWithLifecycle()
                val manualSaveToast by gameViewModel.manualSaveToast.collectAsStateWithLifecycle()
                val strings = GameStrings.forLanguage(stats.language)
                val layoutDir = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                val navController = rememberNavController()

                CompositionLocalProvider(
                    LocalStrings provides strings,
                    LocalLayoutDirection provides layoutDir
                ) {
                    val navigateSafe: (String) -> Unit = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        enterTransition = {
                            fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 14 }
                        },
                        exitTransition = {
                            fadeOut(tween(140, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            fadeIn(tween(180, easing = FastOutSlowInEasing))
                        },
                        popExitTransition = {
                            fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(tween(150, easing = FastOutSlowInEasing)) { it / 14 }
                        }
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                stats = stats,
                                manualSaveToast = manualSaveToast,
                                onPlayNow = {
                                    gameViewModel.playNow()
                                    navigateSafe(ROUTE_ARENA)
                                },
                                onResumeSavedMatch = {
                                    if (gameViewModel.resumeSavedMatch()) {
                                        navigateSafe(ROUTE_ARENA)
                                    } else {
                                        gameViewModel.playNow()
                                        navigateSafe(ROUTE_ARENA)
                                    }
                                },
                                onSelectMode = { mode ->
                                    gameViewModel.setGameMode(mode)
                                    if (mode == GameMode.CAMPAIGN) {
                                        navigateSafe(ROUTE_LEVELS)
                                    } else {
                                        navigateSafe(ROUTE_SELECT)
                                    }
                                },
                                onOpenProgression = {
                                    navigateSafe(ROUTE_PROGRESSION)
                                },
                                onOpenMoveList = {
                                    navigateSafe(ROUTE_MOVELIST)
                                },
                                onSelectDifficulty = { gameViewModel.selectDifficulty(it) },
                                onManualSave = { gameViewModel.manualSaveProgress() },
                                onToggleSound = { gameViewModel.toggleSound() },
                                onSetHaptics = { gameViewModel.setHapticsEnabled(it) },
                                onSetLargeControls = { gameViewModel.setLargeControls(it) },
                                onSetLanguage = { gameViewModel.setLanguage(it) }
                            )
                        }

                        composable(ROUTE_LEVELS) {
                            LevelsScreen(
                                stats = stats,
                                onSelectLevel = { level ->
                                    gameViewModel.selectCampaignLevel(level)
                                    navigateSafe(ROUTE_SELECT)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_SELECT) {
                            FighterSelectScreen(
                                config = selection,
                                stats = stats,
                                onSelectPlayer = { gameViewModel.selectPlayerFighter(it) },
                                onUnlockFighter = { id, cost -> gameViewModel.unlockFighter(id, cost) },
                                onSelectOpponent = { gameViewModel.selectOpponentFighter(it) },
                                onSelectArena = { gameViewModel.selectArena(it) },
                                onSelectDifficulty = { gameViewModel.selectDifficulty(it) },
                                onStartFight = {
                                    gameViewModel.startMatch()
                                    navigateSafe(ROUTE_ARENA)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ARENA) {
                            if (hasActiveMatch) {
                                CombatScreen(
                                    viewModel = gameViewModel,
                                    soundEnabled = stats.soundEnabled,
                                    largeControls = stats.largeControls,
                                    manualSaveToast = manualSaveToast,
                                    onChangeFighter = {
                                        if (!navController.popBackStack(ROUTE_SELECT, inclusive = false)) {
                                            navController.navigate(ROUTE_SELECT) {
                                                popUpTo(ROUTE_HOME) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onExitToMenu = {
                                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    if (!gameViewModel.resumeSavedMatch()) {
                                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                                    }
                                }
                            }
                        }

                        composable(ROUTE_PROGRESSION) {
                            ProgressionScreen(
                                stats = stats,
                                manualSaveToast = manualSaveToast,
                                onUnlockFighter = { id, cost -> gameViewModel.unlockFighter(id, cost) },
                                onUnlockOrEquipPerk = { perk -> gameViewModel.unlockOrEquipPerk(perk) },
                                onManualSave = { gameViewModel.manualSaveProgress() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_MOVELIST) {
                            MoveListScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
