package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.engine.CombatConstants
import com.example.model.BossType
import com.example.model.FighterState
import com.example.model.GameMode
import com.example.model.LevelCatalog
import com.example.model.RoundPhase
import com.example.ui.canvas.FighterRenderScratch
import com.example.ui.canvas.createCalloutPaint
import com.example.ui.canvas.drawAllyStriker
import com.example.ui.canvas.drawArenaFloorEffects
import com.example.ui.canvas.drawFighter
import com.example.ui.canvas.drawProjectilesParticlesAndCallouts
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CrimsonFlame
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSurge
import com.example.viewmodel.CombatHudState
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.min
import kotlin.random.Random

@Composable
fun CombatScreen(
    viewModel: GameViewModel,
    soundEnabled: Boolean,
    largeControls: Boolean,
    manualSaveToast: Boolean,
    onChangeFighter: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scratch = remember { FighterRenderScratch() }
    val calloutPaint = remember(context) {
        val tf = runCatching { ResourcesCompat.getFont(context, R.font.changa) }.getOrNull()
        createCalloutPaint(tf)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.saveCheckpointNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.saveCheckpointNow()
        }
    }

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler {
        if (viewModel.hudState.value?.phase != RoundPhase.MATCH_OVER) {
            viewModel.togglePause()
        } else {
            onExitToMenu()
        }
    }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    viewModel.stepFrame(dt) { heavy ->
                        haptic.performHapticFeedback(
                            if (heavy) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                        )
                    }
                }
                lastNanos = frameNanos
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
        ) {
            ArenaBackgroundAndCanvas(
                viewModel = viewModel,
                scratch = scratch,
                calloutPaint = calloutPaint
            )

            CombatHudSection(
                viewModel = viewModel,
                soundEnabled = soundEnabled
            )

            CombatBannerAndComboSection(viewModel = viewModel)

            FloatingArcadeControlPad(
                energyFlow = viewModel.playerEnergyPct,
                largeControls = largeControls,
                onMoveLeft = { viewModel.setMoveLeft(it) },
                onMoveRight = { viewModel.setMoveRight(it) },
                onGuard = { viewModel.setGuard(it) },
                onJump = { viewModel.triggerJump() },
                onDash = { viewModel.triggerDash() },
                onPunch = { viewModel.triggerPunch() },
                onKick = { viewModel.triggerKick() },
                onSpecial = { viewModel.triggerSpecial() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )

            CombatOverlaysSection(
                viewModel = viewModel,
                manualSaveToast = manualSaveToast,
                onChangeFighter = onChangeFighter,
                onExitToMenu = onExitToMenu
            )
        }
    }
}

@Composable
private fun ArenaBackgroundAndCanvas(
    viewModel: GameViewModel,
    scratch: FighterRenderScratch,
    calloutPaint: android.graphics.Paint
) {
    val strings = LocalStrings.current
    val initialBg = remember(viewModel) {
        viewModel.hudState.value?.arenaBackgroundRes ?: R.drawable.img_arena_neon_dojo
    }
    val bgRes by remember(viewModel) {
        viewModel.hudState
            .map { it?.arenaBackgroundRes ?: R.drawable.img_arena_neon_dojo }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = initialBg)

    val scrimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x990B0A12),
                Color(0x180B0A12),
                Color(0x770B0A12)
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val isPortrait = heightPx > widthPx

        val floorY = if (isPortrait) heightPx * 0.77f else heightPx * 0.85f
        val fighterScale = min(widthPx / 680f, heightPx / 440f).coerceIn(0.65f, 1.85f)

        Image(
            painter = painterResource(id = bgRes),
            contentDescription = strings.cdArenaBg,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush)
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
        ) {
            val matchState = viewModel.renderMatchState ?: return@Canvas
            val shakeX = if (matchState.screenShake > 0.5f) {
                (Random.nextFloat() * 2f - 1f) * matchState.screenShake
            } else 0f
            val shakeY = if (matchState.screenShake > 0.5f) {
                (Random.nextFloat() * 2f - 1f) * matchState.screenShake * 0.6f
            } else 0f

            withTransform({
                if (shakeX != 0f || shakeY != 0f) {
                    translate(left = shakeX, top = shakeY)
                }
            }) {
                drawArenaFloorEffects(
                    floorY = floorY,
                    accentColor = matchState.arena.accentColor,
                    floorColor = matchState.arena.floorColor
                )
                drawAllyStriker(
                    ally = matchState.allyStriker,
                    widthPx = widthPx,
                    floorY = floorY,
                    scale = fighterScale,
                    scratch = scratch
                )
                drawFighter(
                    fighter = matchState.cpu,
                    widthPx = widthPx,
                    floorY = floorY,
                    scale = fighterScale,
                    scratch = scratch
                )
                drawFighter(
                    fighter = matchState.player,
                    widthPx = widthPx,
                    floorY = floorY,
                    scale = fighterScale,
                    scratch = scratch
                )
                drawProjectilesParticlesAndCallouts(
                    projectiles = matchState.projectiles,
                    particles = matchState.particles,
                    impactRings = matchState.impactRings,
                    callouts = matchState.callouts,
                    widthPx = widthPx,
                    floorY = floorY,
                    scale = fighterScale,
                    calloutPaint = calloutPaint,
                    scratch = scratch
                )
            }
        }
    }
}

@Composable
private fun BoxScope.CombatHudSection(
    viewModel: GameViewModel,
    soundEnabled: Boolean
) {
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val hud = hudState ?: return

    CombatHudOverlay(
        hud = hud,
        soundEnabled = soundEnabled,
        playerStateProvider = { viewModel.renderMatchState?.player },
        cpuStateProvider = { viewModel.renderMatchState?.cpu },
        onToggleSound = { viewModel.toggleSound() },
        onPause = { viewModel.togglePause() },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )

    if (hud.bossWarningText.isNotEmpty() && hud.phase == RoundPhase.FIGHTING) {
        Surface(
            color = CrimsonFlame.copy(alpha = 0.90f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, SolarGold),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 114.dp)
        ) {
            Text(
                text = hud.bossWarningText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}

private data class BannerComboSnapshot(
    val playerCombo: Int = 0,
    val phase: RoundPhase = RoundPhase.INTRO,
    val bannerText: String = "",
    val bannerSubtext: String = ""
)

@Composable
private fun BoxScope.CombatBannerAndComboSection(
    viewModel: GameViewModel
) {
    val strings = LocalStrings.current
    val bannerState by remember(viewModel) {
        viewModel.hudState
            .map { h ->
                if (h == null) null
                else BannerComboSnapshot(h.playerCombo, h.phase, h.bannerText, h.bannerSubtext)
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val hud = bannerState ?: return

    if (hud.playerCombo >= 2 && hud.phase == RoundPhase.FIGHTING) {
        Surface(
            color = CrimsonFlame.copy(alpha = 0.88f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, SolarGold),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, bottom = 70.dp)
        ) {
            Text(
                text = "${hud.playerCombo}× ${strings.comboLabel}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }

    Box(modifier = Modifier.align(Alignment.Center)) {
        AnimatedVisibility(
            visible = hud.bannerText.isNotEmpty() && hud.phase != RoundPhase.MATCH_OVER,
            enter = fadeIn() + scaleIn(initialScale = 0.72f),
            exit = fadeOut()
        ) {
            Surface(
                color = ObsidianBg.copy(alpha = 0.80f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, SolarGold.copy(alpha = 0.85f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = hud.bannerText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            shadow = Shadow(CrimsonFlame, Offset(0f, 3f), 10f)
                        ),
                        color = SolarGold,
                        textAlign = TextAlign.Center
                    )
                    if (hud.bannerSubtext.isNotEmpty()) {
                        Text(
                            text = hud.bannerSubtext,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private data class OverlaySnapshot(
    val isPaused: Boolean = false,
    val phase: RoundPhase = RoundPhase.INTRO,
    val playerWonMatch: Boolean? = null,
    val campaignLevelNumber: Int? = null,
    val mode: GameMode = GameMode.VERSUS,
    val playerRoundsWon: Int = 0,
    val cpuRoundsWon: Int = 0,
    val highestComboInMatch: Int = 0
)

@Composable
private fun CombatOverlaysSection(
    viewModel: GameViewModel,
    manualSaveToast: Boolean,
    onChangeFighter: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val overlayState by remember(viewModel) {
        viewModel.hudState
            .map { h ->
                if (h == null) null
                else OverlaySnapshot(
                    isPaused = h.isPaused,
                    phase = h.phase,
                    playerWonMatch = h.playerWonMatch,
                    campaignLevelNumber = h.campaignLevelNumber,
                    mode = h.mode,
                    playerRoundsWon = h.playerRoundsWon,
                    cpuRoundsWon = h.cpuRoundsWon,
                    highestComboInMatch = h.highestComboInMatch
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val hud = overlayState ?: return

    if (hud.isPaused) {
        PauseOverlay(
            manualSaveToast = manualSaveToast,
            onResume = { viewModel.setPaused(false) },
            onManualSave = { viewModel.manualSaveProgress() },
            onRematch = { viewModel.rematch() },
            onExit = {
                viewModel.saveCheckpointNow()
                onExitToMenu()
            }
        )
    }

    if (hud.phase == RoundPhase.MATCH_OVER) {
        MatchOverOverlay(
            hud = hud,
            onRematch = { viewModel.rematch() },
            onNextLevel = { viewModel.advanceNextLevel() },
            onChangeFighter = onChangeFighter,
            onExit = onExitToMenu
        )
    }
}

@Composable
private fun CombatHudOverlay(
    hud: CombatHudState,
    soundEnabled: Boolean,
    playerStateProvider: () -> FighterState?,
    cpuStateProvider: () -> FighterState?,
    onToggleSound: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FighterHudColumn(
                name = hud.playerName,
                hp = hud.playerHp,
                maxHp = hud.playerMaxHp,
                roundsWon = hud.playerRoundsWon,
                isLeft = true,
                barTag = "player_health_bar",
                fighterStateProvider = playerStateProvider,
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = ObsidianCard.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, SolarGold.copy(alpha = 0.9f)),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = if (hud.mode == GameMode.TRAINING) {
                        "∞"
                    } else {
                        hud.timeRemainingSec.toString().padStart(2, '0')
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (hud.timeRemainingSec <= 10 && hud.mode != GameMode.TRAINING) {
                        CrimsonFlame
                    } else {
                        SolarGold
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            FighterHudColumn(
                name = hud.cpuName,
                hp = hud.cpuHp,
                maxHp = hud.cpuMaxHp,
                roundsWon = hud.cpuRoundsWon,
                isLeft = false,
                barTag = "cpu_health_bar",
                fighterStateProvider = cpuStateProvider,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ObsidianCard.copy(alpha = 0.72f),
                shape = CircleShape,
                border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.8f))
            ) {
                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("combat_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = strings.cdPause,
                        tint = TextPrimary
                    )
                }
            }
            if (hud.bossType != BossType.NONE) {
                Surface(
                    color = if (hud.bossPhase >= 2) CrimsonFlame.copy(alpha = 0.85f) else ObsidianCard.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (hud.bossPhase >= 2) SolarGold else CrimsonFlame)
                ) {
                    Text(
                        text = strings.bossPhaseLabel(hud.bossPhase),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hud.bossPhase >= 2) Color.White else SolarGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else if (hud.campaignLevelNumber != null) {
                Surface(
                    color = ObsidianCard.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "${strings.levelLabel} ${hud.campaignLevelNumber} • ${strings.modifierName(hud.activeModifier)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = ObsidianCard.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = strings.difficultyLabel(hud.difficulty),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Surface(
                color = ObsidianCard.copy(alpha = 0.72f),
                shape = CircleShape,
                border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.8f))
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("combat_sound_button")
                ) {
                    Icon(
                        imageVector = if (soundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = strings.cdSound,
                        tint = if (soundEnabled) ElectricCyan else TextSecondary
                    )
                }
            }
        }

        if (hud.aiTacticLabel.isNotEmpty() && hud.phase == RoundPhase.FIGHTING) {
            Surface(
                color = ObsidianBg.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, VioletSurge.copy(alpha = 0.7f))
            ) {
                Text(
                    text = hud.aiTacticLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = VioletSurge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FighterHudColumn(
    name: String,
    hp: Int,
    maxHp: Int,
    roundsWon: Int,
    isLeft: Boolean,
    barTag: String,
    fighterStateProvider: () -> FighterState?,
    modifier: Modifier = Modifier
) {
    val hpFractionStatic = if (maxHp > 0) (hp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f) else 0f
    val hpColor = when {
        hpFractionStatic > 0.5f -> EmeraldHealth
        hpFractionStatic > 0.25f -> SolarGold
        else -> CrimsonFlame
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isLeft) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    RoundGems(wins = roundsWon, color = SolarGold)
                }
                Text(
                    text = "$hp/$maxHp",
                    style = MaterialTheme.typography.labelSmall,
                    color = hpColor,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Text(
                    text = "$hp/$maxHp",
                    style = MaterialTheme.typography.labelSmall,
                    color = hpColor,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RoundGems(wins = roundsWon, color = SolarGold)
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Smooth 60/120fps GPU-drawn health + trail bar (reads renderMatchState in draw phase only)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .testTag(barTag)
        ) {
            val f = fighterStateProvider()
            val curHpPct = if (f != null && f.maxHp > 0f) (f.hp / f.maxHp).coerceIn(0f, 1f) else hpFractionStatic
            val curTrailPct = if (f != null && f.maxHp > 0f) (f.trailHp / f.maxHp).coerceIn(0f, 1f) else hpFractionStatic
            val isTakingHit = curTrailPct > curHpPct + 0.01f
            val curHpColor = when {
                curHpPct > 0.5f -> EmeraldHealth
                curHpPct > 0.25f -> SolarGold
                else -> CrimsonFlame
            }

            val outerCorner = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            drawRoundRect(
                color = ObsidianBg.copy(alpha = 0.88f),
                cornerRadius = outerCorner
            )

            val pad = 2.dp.toPx()
            val innerW = (size.width - pad * 2f).coerceAtLeast(0f)
            val innerH = (size.height - pad * 2f).coerceAtLeast(0f)
            val innerCorner = CornerRadius(7.dp.toPx(), 7.dp.toPx())

            val trailW = innerW * curTrailPct
            if (trailW > 0.5f) {
                val trailX = if (isLeft) pad else size.width - pad - trailW
                drawRoundRect(
                    color = CrimsonFlame.copy(alpha = 0.78f),
                    topLeft = Offset(trailX, pad),
                    size = Size(trailW, innerH),
                    cornerRadius = innerCorner
                )
            }

            val fillW = innerW * curHpPct
            if (fillW > 0.5f) {
                val fillX = if (isLeft) pad else size.width - pad - fillW
                drawRoundRect(
                    color = curHpColor,
                    topLeft = Offset(fillX, pad),
                    size = Size(fillW, innerH),
                    cornerRadius = innerCorner
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.24f),
                    topLeft = Offset(fillX, pad),
                    size = Size(fillW, innerH * 0.40f),
                    cornerRadius = innerCorner
                )
            }

            // Tactical 25% / 50% / 75% divider notches
            val tickColor = ObsidianBg.copy(alpha = 0.50f)
            for (step in 1..3) {
                val tx = pad + innerW * (step * 0.25f)
                drawLine(
                    color = tickColor,
                    start = Offset(tx, pad + 2f),
                    end = Offset(tx, size.height - pad - 2f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            val isCritical = curHpPct in 0.001f..0.25f
            drawRoundRect(
                color = when {
                    isTakingHit -> CrimsonFlame
                    isCritical -> CrimsonFlame.copy(alpha = 0.85f)
                    else -> TextPrimary.copy(alpha = 0.62f)
                },
                cornerRadius = outerCorner,
                style = Stroke(width = (if (isTakingHit || isCritical) 2.dp else 1.5.dp).toPx())
            )
        }

        // Smooth 60/120fps GPU-drawn energy bar with 50% Special-Ready threshold notch
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(9.dp)
        ) {
            val f = fighterStateProvider()
            val energyVal = f?.energy ?: 0f
            val energyFraction = (energyVal / 100f).coerceIn(0f, 1f)
            val ready = energyVal >= CombatConstants.SPECIAL_COST
            val corner = CornerRadius(5.dp.toPx(), 5.dp.toPx())

            drawRoundRect(
                color = ObsidianBg.copy(alpha = 0.85f),
                cornerRadius = corner
            )
            val fillW = size.width * energyFraction
            if (fillW > 0.5f) {
                val fillX = if (isLeft) 0f else size.width - fillW
                drawRoundRect(
                    color = if (ready) SolarGold else ElectricCyan,
                    topLeft = Offset(fillX, 0f),
                    size = Size(fillW, size.height),
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.28f),
                    topLeft = Offset(fillX, 0f),
                    size = Size(fillW, size.height * 0.42f),
                    cornerRadius = corner
                )
            }
            val midX = size.width * 0.5f
            drawLine(
                color = if (ready) Color.White.copy(alpha = 0.8f) else TextPrimary.copy(alpha = 0.45f),
                start = Offset(midX, 1f),
                end = Offset(midX, size.height - 1f),
                strokeWidth = 1.5.dp.toPx()
            )
            drawRoundRect(
                color = if (ready) SolarGold.copy(alpha = 0.9f) else ObsidianBorder,
                cornerRadius = corner,
                style = Stroke(width = (if (ready) 1.4.dp else 1.dp).toPx())
            )
        }
    }
}

@Composable
private fun RoundGems(wins: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) { idx ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (idx < wins) color else ObsidianBorder,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun FloatingArcadeControlPad(
    energyFlow: StateFlow<Int>,
    largeControls: Boolean,
    onMoveLeft: (Boolean) -> Unit,
    onMoveRight: (Boolean) -> Unit,
    onGuard: (Boolean) -> Unit,
    onJump: () -> Unit,
    onDash: () -> Unit,
    onPunch: () -> Unit,
    onKick: () -> Unit,
    onSpecial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val scale = if (largeControls) 1.15f else 1f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TapControlButton(
                    icon = Icons.Filled.KeyboardArrowUp,
                    label = strings.ctrlJump,
                    accent = ElectricCyan,
                    size = 52.dp * scale,
                    tag = "ctrl_jump",
                    onTap = onJump
                )
                TapControlButton(
                    icon = Icons.Filled.FlashOn,
                    label = strings.ctrlDash,
                    accent = VioletSurge,
                    size = 52.dp * scale,
                    tag = "ctrl_dash",
                    onTap = onDash
                )
            }
            SlideDirectionPad(
                width = 136.dp * scale,
                height = 62.dp * scale,
                onMoveLeft = onMoveLeft,
                onMoveRight = onMoveRight
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HoldControlButton(
                    icon = Icons.Filled.Shield,
                    contentDesc = strings.ctrlGuard,
                    label = strings.ctrlGuard,
                    accent = ElectricCyan,
                    size = 54.dp * scale,
                    tag = "ctrl_guard",
                    onHeldChanged = onGuard
                )
                SpecialControlButton(
                    energyFlow = energyFlow,
                    specialLabel = strings.ctrlSpecial,
                    size = 56.dp * scale,
                    onTap = onSpecial
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TapAttackButton(
                    label = strings.ctrlPunch,
                    accent = CrimsonFlame,
                    size = 62.dp * scale,
                    tag = "ctrl_punch",
                    onTap = onPunch
                )
                TapAttackButton(
                    label = strings.ctrlKick,
                    accent = Color(0xFFFF7A00),
                    size = 62.dp * scale,
                    tag = "ctrl_kick",
                    onTap = onKick
                )
            }
        }
    }
}

@Composable
private fun SpecialControlButton(
    energyFlow: StateFlow<Int>,
    specialLabel: String,
    size: Dp,
    onTap: () -> Unit
) {
    val energy by energyFlow.collectAsStateWithLifecycle()
    val specialReady = energy >= CombatConstants.SPECIAL_COST.toInt()
    Box(contentAlignment = Alignment.Center) {
        if (specialReady) {
            Box(
                modifier = Modifier
                    .size(size + 6.dp)
                    .clip(CircleShape)
                    .background(SolarGold.copy(alpha = 0.26f))
            )
        }
        TapControlButton(
            icon = Icons.Filled.Bolt,
            label = if (specialReady) specialLabel else "$energy%",
            accent = if (specialReady) SolarGold else TextSecondary,
            filled = specialReady,
            size = size,
            tag = "ctrl_special",
            onTap = onTap
        )
    }
}

@Composable
private fun SlideDirectionPad(
    width: Dp,
    height: Dp,
    onMoveLeft: (Boolean) -> Unit,
    onMoveRight: (Boolean) -> Unit
) {
    val strings = LocalStrings.current
    var activeDir by remember { mutableIntStateOf(0) }
    val leftScale by animateFloatAsState(
        targetValue = if (activeDir == -1) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "dpad_left_scale"
    )
    val rightScale by animateFloatAsState(
        targetValue = if (activeDir == 1) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "dpad_right_scale"
    )

    Row(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianCard.copy(alpha = 0.58f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun updateDir(x: Float) {
                        val half = size.width / 2f
                        val next = if (x < half) -1 else 1
                        if (next != activeDir) {
                            activeDir = next
                            onMoveLeft(next == -1)
                            onMoveRight(next == 1)
                        }
                    }
                    updateDir(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        updateDir(change.position.x)
                    }
                    activeDir = 0
                    onMoveLeft(false)
                    onMoveRight(false)
                }
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = if (activeDir == -1) ElectricCyan.copy(alpha = 0.35f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = if (activeDir == -1) 2.dp else 1.dp,
                color = if (activeDir == -1) ElectricCyan else ObsidianBorder.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = leftScale
                    scaleY = leftScale
                }
                .testTag("ctrl_left")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings.cdMoveLeft,
                    tint = if (activeDir == -1) ElectricCyan else TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Surface(
            color = if (activeDir == 1) ElectricCyan.copy(alpha = 0.35f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = if (activeDir == 1) 2.dp else 1.dp,
                color = if (activeDir == 1) ElectricCyan else ObsidianBorder.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = rightScale
                    scaleY = rightScale
                }
                .testTag("ctrl_right")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = strings.cdMoveRight,
                    tint = if (activeDir == 1) ElectricCyan else TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun HoldControlButton(
    icon: ImageVector,
    contentDesc: String,
    accent: Color,
    size: Dp,
    tag: String,
    label: String? = null,
    onHeldChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "${tag}_scale"
    )

    Surface(
        color = if (pressed) accent.copy(alpha = 0.38f) else ObsidianCard.copy(alpha = 0.62f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (pressed) 2.dp else 1.5.dp, if (pressed) accent else ObsidianBorder.copy(alpha = 0.75f)),
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .testTag(tag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onHeldChanged(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onHeldChanged(false)
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = if (pressed) accent else TextPrimary,
                modifier = Modifier.size(if (label != null) 20.dp else 28.dp)
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun TapControlButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    size: Dp,
    tag: String,
    filled: Boolean = false,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "${tag}_scale"
    )

    Surface(
        color = when {
            filled -> accent.copy(alpha = 0.92f)
            pressed -> accent.copy(alpha = 0.38f)
            else -> ObsidianCard.copy(alpha = 0.62f)
        },
        shape = CircleShape,
        border = BorderStroke(1.5.dp, accent.copy(alpha = if (filled) 1f else 0.75f)),
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .testTag(tag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onTap()
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (filled) ObsidianBg else accent,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (filled) ObsidianBg else TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TapAttackButton(
    label: String,
    accent: Color,
    size: Dp,
    tag: String,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.87f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "${tag}_scale"
    )

    Surface(
        color = if (pressed) accent.copy(alpha = 0.92f) else accent.copy(alpha = 0.28f),
        shape = CircleShape,
        border = BorderStroke(2.dp, accent),
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .testTag(tag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onTap()
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (pressed) ObsidianBg else TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    manualSaveToast: Boolean,
    onResume: () -> Unit,
    onManualSave: () -> Unit,
    onRematch: () -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalStrings.current
    val dir = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides dir) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xBB07060B)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.5.dp, ObsidianBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(0.86f)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.pauseTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = SolarGold
                    )
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pause_resume_button")
                    ) {
                        Text(text = strings.btnResume, style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(
                        onClick = onManualSave,
                        border = BorderStroke(1.dp, SolarGold),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pause_manual_save_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, tint = SolarGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.btnSaveCheckpoint, style = MaterialTheme.typography.titleMedium, color = SolarGold)
                    }
                    if (manualSaveToast) {
                        Text(
                            text = strings.savedVerifiedMsg,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldHealth,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onRematch,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pause_rematch_button")
                    ) {
                        Text(text = strings.btnRematch, style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(
                        onClick = onExit,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pause_exit_button")
                    ) {
                        Text(text = strings.btnMainMenu, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchOverOverlay(
    hud: OverlaySnapshot,
    onRematch: () -> Unit,
    onNextLevel: () -> Unit,
    onChangeFighter: () -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalStrings.current
    val dir = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val playerWon = hud.playerWonMatch == true
    val currentLevelNum = hud.campaignLevelNumber ?: 1
    val hasNextLevel = playerWon &&
        hud.mode == GameMode.CAMPAIGN &&
        currentLevelNum < LevelCatalog.all.size
    val campaignChampion = playerWon &&
        hud.mode == GameMode.CAMPAIGN &&
        currentLevelNum >= LevelCatalog.all.size

    CompositionLocalProvider(LocalLayoutDirection provides dir) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC07060B)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(2.dp, if (playerWon) SolarGold else CrimsonFlame),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(0.88f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when {
                            campaignChampion -> strings.campaignChampionTitle
                            playerWon -> strings.youWinTitle
                            else -> strings.youLoseTitle
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (playerWon) SolarGold else CrimsonFlame,
                        textAlign = TextAlign.Center
                    )
                    if (campaignChampion) {
                        Text(
                            text = strings.campaignChampionSub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "${hud.playerRoundsWon} - ${hud.cpuRoundsWon} • ${strings.bestComboLabel}: ${hud.highestComboInMatch}×",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }

                    if (hasNextLevel) {
                        Button(
                            onClick = onNextLevel,
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("result_next_stage_button")
                        ) {
                            Text(text = strings.btnNextLevel, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Button(
                        onClick = onRematch,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonFlame, contentColor = TextPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_rematch_button")
                    ) {
                        Text(text = strings.btnRematch, style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = onChangeFighter,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("result_change_fighter_button")
                    ) {
                        Text(text = strings.btnChangeFighter, style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = onExit,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("result_main_menu_button")
                    ) {
                        Text(text = strings.btnMainMenu, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
