package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PlayerStats
import com.example.model.AchievementId
import com.example.model.CombatPerk
import com.example.model.FighterId
import com.example.model.FighterRoster
import com.example.model.MissionId
import com.example.ui.canvas.FighterPortraitCanvas
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CrimsonFlame
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSurge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressionScreen(
    stats: PlayerStats,
    manualSaveToast: Boolean,
    onUnlockFighter: (FighterId, Int) -> Unit,
    onUnlockOrEquipPerk: (CombatPerk) -> Unit,
    onManualSave: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val formattedTime = remember(stats.lastSavedEpochMs) {
        if (stats.lastSavedEpochMs <= 0L) "—"
        else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(stats.lastSavedEpochMs))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("progression_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.btnBack,
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = strings.progressionTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = SolarGold.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SolarGold)
            ) {
                Text(
                    text = "${stats.honorPoints} ${strings.honorLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SolarGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Save Integrity & Manual Save Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldHealth,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = strings.savePanelTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = strings.saveLastFormat(formattedTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = if (stats.recoveredFromBackup) strings.saveRestoredBackup else strings.saveChecksumValid,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldHealth
                            )
                        }
                    }
                    Button(
                        onClick = onManualSave,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("progression_manual_save_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.btnSaveNow, style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (manualSaveToast) {
                    Text(
                        text = strings.savedVerifiedMsg,
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(strings.tabMissions, strings.tabAchievements, strings.tabArmory).forEachIndexed { idx, label ->
                val selected = selectedTab == idx
                Surface(
                    color = if (selected) CrimsonFlame.copy(alpha = 0.25f) else ObsidianCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CrimsonFlame else ObsidianBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedTab = idx }
                        .testTag("progression_tab_$idx")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) CrimsonFlame else TextSecondary
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    items(MissionId.entries, key = { it.name }) { mission ->
                        val prog = (stats.missionProgress[mission] ?: 0).coerceAtMost(mission.target)
                        val done = mission in stats.completedMissions
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(1.dp, if (done) EmeraldHealth else ObsidianBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.missionTitle(mission),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (done) EmeraldHealth.copy(alpha = 0.2f) else SolarGold.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (done) strings.missionCompleteBadge else strings.rewardHonorFormat(mission.rewardHonor),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (done) EmeraldHealth else SolarGold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = strings.missionDesc(mission),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { prog.toFloat() / mission.target },
                                        color = if (done) EmeraldHealth else ElectricCyan,
                                        trackColor = ObsidianBg,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Text(
                                        text = "$prog/${mission.target}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    items(AchievementId.entries, key = { it.name }) { ach ->
                        val unlocked = ach in stats.unlockedAchievements
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(1.dp, if (unlocked) SolarGold else ObsidianBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (unlocked) 1f else 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = if (unlocked) SolarGold else TextMuted,
                                    modifier = Modifier.size(30.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.achievementTitle(ach),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (unlocked) SolarGold else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.achievementDesc(ach),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = if (unlocked) strings.unlockedBadge else strings.rewardHonorFormat(ach.rewardHonor),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) EmeraldHealth else SolarGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                else -> {
                    item {
                        Text(
                            text = strings.armoryFightersHeader,
                            style = MaterialTheme.typography.titleLarge,
                            color = SolarGold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(FighterRoster.all.filter { !it.starterUnlocked }, key = { it.id.name }) { fighter ->
                        val unlocked = fighter.id in stats.unlockedFighters
                        val canAfford = stats.honorPoints >= fighter.unlockCost
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(1.dp, if (unlocked) EmeraldHealth else fighter.primaryColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FighterPortraitCanvas(
                                    profile = fighter,
                                    modifier = Modifier.size(width = 64.dp, height = 78.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${strings.fighterName(fighter.id)} — ${strings.fighterTitle(fighter.id)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = fighter.unlockStageHint?.let { strings.unlockHintStageFormat(it) }
                                            ?: strings.fighterBio(fighter.id),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                if (unlocked) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = strings.unlockedBadge,
                                        tint = EmeraldHealth
                                    )
                                } else {
                                    Button(
                                        onClick = { onUnlockFighter(fighter.id, fighter.unlockCost) },
                                        enabled = canAfford,
                                        colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = ObsidianBg),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text(
                                            text = strings.unlockWithHonorFormat(fighter.unlockCost),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = strings.armoryPerksHeader,
                            style = MaterialTheme.typography.titleLarge,
                            color = ElectricCyan,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(CombatPerk.entries, key = { it.name }) { perk ->
                        val unlocked = perk in stats.unlockedPerks
                        val equipped = stats.equippedPerk == perk
                        val canAfford = stats.honorPoints >= perk.unlockCost
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(
                                if (equipped) 2.dp else 1.dp,
                                when {
                                    equipped -> ElectricCyan
                                    unlocked -> VioletSurge.copy(alpha = 0.6f)
                                    else -> ObsidianBorder
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (equipped) Icons.Filled.Shield else Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = if (equipped) ElectricCyan else SolarGold,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.perkName(perk),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.perkDesc(perk),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Button(
                                    onClick = { onUnlockOrEquipPerk(perk) },
                                    enabled = unlocked || canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            equipped -> EmeraldHealth
                                            unlocked -> ElectricCyan
                                            else -> SolarGold
                                        },
                                        contentColor = ObsidianBg
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("perk_btn_${perk.name.lowercase()}")
                                ) {
                                    Text(
                                        text = when {
                                            equipped -> strings.equippedBadge
                                            unlocked -> strings.btnEquip
                                            else -> "${strings.btnUnlock} (${perk.unlockCost})"
                                        },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}
