package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.PlayerStats
import com.example.model.AiDifficulty
import com.example.model.GameMode
import com.example.model.LevelCatalog
import com.example.ui.i18n.AppLanguage
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    stats: PlayerStats,
    manualSaveToast: Boolean,
    onPlayNow: () -> Unit,
    onResumeSavedMatch: () -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onOpenProgression: () -> Unit,
    onOpenMoveList: () -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onManualSave: () -> Unit,
    onToggleSound: () -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetLargeControls: (Boolean) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit
) {
    val strings = LocalStrings.current
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBg, Color(0xFF130F24), ObsidianBg)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Banner Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, SolarGold.copy(alpha = 0.42f)),
                    color = ObsidianCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_clash),
                        contentDescription = strings.cdHeroBanner,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x550B0A12),
                                        Color(0xAA0B0A12),
                                        Color(0xEE0B0A12)
                                    )
                                )
                            )
                    )

                    // Top-end Sound & Settings buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = ObsidianCard.copy(alpha = 0.82f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            IconButton(
                                onClick = onToggleSound,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_sound_toggle")
                            ) {
                                Icon(
                                    imageVector = if (stats.soundEnabled) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                    contentDescription = strings.cdSound,
                                    tint = if (stats.soundEnabled) ElectricCyan else TextSecondary
                                )
                            }
                        }
                        Surface(
                            color = ObsidianCard.copy(alpha = 0.82f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = strings.cdSettings,
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = CrimsonFlame.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CrimsonFlame.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = strings.badgeArcade,
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonFlame,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.displayLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.appSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                }

                // Primary Play Button (Immediate Jump into Battle or Saved Match)
                val nextLevel = remember(stats.highestUnlockedLevel) {
                    LevelCatalog.byNumber(stats.highestUnlockedLevel)
                }
                val nextOppName = if (nextLevel.isBoss) {
                    strings.bossName(nextLevel.bossType)
                } else {
                    strings.fighterName(nextLevel.opponentId)
                }
                val hasResume = stats.activeSnapshot != null
                Button(
                    onClick = {
                        if (hasResume) onResumeSavedMatch() else onPlayNow()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasResume) EmeraldHealth else CrimsonFlame,
                        contentColor = if (hasResume) ObsidianBg else TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("home_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = if (hasResume) strings.btnResumeMatch else strings.btnPlayNow,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stats.activeSnapshot?.let { snap ->
                                strings.resumeMatchDescFormat(strings.fighterName(snap.opponentId), snap.roundNumber)
                            } ?: strings.playNowDescFormat(nextLevel.levelNumber, nextOppName),
                            style = MaterialTheme.typography.labelSmall,
                            color = (if (hasResume) ObsidianBg else TextPrimary).copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Player Stats Strip
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadge(
                            label = strings.statWins,
                            value = stats.totalWins.toString(),
                            color = ElectricCyan
                        )
                        StatBadge(
                            label = strings.statKos,
                            value = stats.totalKos.toString(),
                            color = CrimsonFlame
                        )
                        StatBadge(
                            label = strings.statBestCombo,
                            value = "${stats.bestCombo}×",
                            color = SolarGold
                        )
                        StatBadge(
                            label = strings.statLevelsCleared,
                            value = "${stats.levelsCleared}/${LevelCatalog.all.size}",
                            color = VioletSurge
                        )
                        StatBadge(
                            label = strings.honorLabel,
                            value = stats.honorPoints.toString(),
                            color = EmeraldHealth
                        )
                    }
                }

                // Game Modes
                ModeActionCard(
                    title = strings.modeCampaign,
                    description = strings.modeCampaignDesc,
                    icon = Icons.Filled.EmojiEvents,
                    accent = SolarGold,
                    tag = "mode_campaign_card",
                    onClick = { onSelectMode(GameMode.CAMPAIGN) }
                )

                ModeActionCard(
                    title = strings.modeVersus,
                    description = strings.modeVersusDesc,
                    icon = Icons.Filled.SportsMma,
                    accent = CrimsonFlame,
                    tag = "mode_versus_card",
                    onClick = { onSelectMode(GameMode.VERSUS) }
                )

                ModeActionCard(
                    title = strings.modeTraining,
                    description = strings.modeTrainingDesc,
                    icon = Icons.Filled.FitnessCenter,
                    accent = ElectricCyan,
                    tag = "mode_training_card",
                    onClick = { onSelectMode(GameMode.TRAINING) }
                )

                // Progression, Missions & Armory Card
                Button(
                    onClick = onOpenProgression,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletSurge.copy(alpha = 0.25f),
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.5.dp, VioletSurge),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("open_progression_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = SolarGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.btnProgression,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenMoveList,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ObsidianCard.copy(alpha = 0.6f),
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("open_movelist_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = SolarGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.btnMoveList,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { showSettings = true },
                        border = BorderStroke(1.dp, ObsidianBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ObsidianCard.copy(alpha = 0.6f),
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = strings.btnSettings,
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.btnSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            stats = stats,
            manualSaveToast = manualSaveToast,
            onDismiss = { showSettings = false },
            onSelectDifficulty = onSelectDifficulty,
            onManualSave = onManualSave,
            onToggleSound = onToggleSound,
            onSetHaptics = onSetHaptics,
            onSetLargeControls = onSetLargeControls,
            onSetLanguage = onSetLanguage
        )
    }
}

@Composable
private fun SettingsDialog(
    stats: PlayerStats,
    manualSaveToast: Boolean,
    onDismiss: () -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onManualSave: () -> Unit,
    onToggleSound: () -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetLargeControls: (Boolean) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit
) {
    val strings = LocalStrings.current
    val savedTime = remember(stats.lastSavedEpochMs) {
        if (stats.lastSavedEpochMs <= 0L) "—"
        else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(stats.lastSavedEpochMs))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.5.dp, ObsidianBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SolarGold
                )

                // Language Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.settingLanguage,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = stats.language == lang
                            Surface(
                                color = if (selected) ElectricCyan.copy(alpha = 0.22f) else ObsidianBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) ElectricCyan else ObsidianBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSetLanguage(lang) }
                                    .testTag("lang_${lang.code}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) ElectricCyan else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Default Difficulty Picker (4 levels)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.settingDifficulty,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AiDifficulty.entries.forEach { diff ->
                            val active = stats.preferredDifficulty == diff
                            val color = when (diff) {
                                AiDifficulty.EASY -> ElectricCyan
                                AiDifficulty.NORMAL -> SolarGold
                                AiDifficulty.HARD -> CrimsonFlame
                                AiDifficulty.EXTREME -> VioletSurge
                            }
                            Surface(
                                color = if (active) color.copy(alpha = 0.22f) else ObsidianBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(if (active) 2.dp else 1.dp, if (active) color else ObsidianBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectDifficulty(diff) }
                                    .testTag("setting_diff_${diff.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp)) {
                                    Text(
                                        text = strings.difficultyLabel(diff),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) color else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                SettingSwitchRow(
                    label = strings.settingSound,
                    checked = stats.soundEnabled,
                    tag = "setting_sound_switch",
                    onCheckedChange = { onToggleSound() }
                )

                SettingSwitchRow(
                    label = strings.settingHaptics,
                    checked = stats.hapticsEnabled,
                    tag = "setting_haptics_switch",
                    onCheckedChange = onSetHaptics
                )

                SettingSwitchRow(
                    label = strings.settingLargeControls,
                    checked = stats.largeControls,
                    tag = "setting_large_controls_switch",
                    onCheckedChange = onSetLargeControls
                )

                // Manual Save Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.saveLastFormat(savedTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (manualSaveToast) {
                        Text(
                            text = strings.savedVerifiedMsg,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldHealth,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onManualSave,
                        border = BorderStroke(1.dp, ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_manual_save_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.btnSaveNow, style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonFlame,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_close_button")
                ) {
                    Text(text = strings.btnClose, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = ElectricCyan
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
