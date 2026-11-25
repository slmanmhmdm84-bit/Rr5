package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlayerStats
import com.example.model.AiDifficulty
import com.example.model.ArenaCatalog
import com.example.model.ArenaStage
import com.example.model.FighterId
import com.example.model.FighterProfile
import com.example.model.FighterRoster
import com.example.model.GameMode
import com.example.ui.canvas.FighterPortraitCanvas
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CrimsonFlame
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSurge
import com.example.viewmodel.SelectionConfig

@Composable
fun FighterSelectScreen(
    config: SelectionConfig,
    stats: PlayerStats,
    onSelectPlayer: (FighterProfile) -> Unit,
    onUnlockFighter: (FighterId, Int) -> Unit,
    onSelectOpponent: (FighterProfile) -> Unit,
    onSelectArena: (ArenaStage) -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onStartFight: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    .testTag("select_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.btnBack,
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.selectTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = CrimsonFlame.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CrimsonFlame.copy(alpha = 0.6f))
            ) {
                Text(
                    text = strings.modeLabel(config.mode),
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonFlame,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campaign Stage Briefing Card
            if (config.mode == GameMode.CAMPAIGN) {
                val lvl = config.selectedLevel
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.5.dp, if (lvl.isBoss) CrimsonFlame else SolarGold.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${strings.levelLabel} ${lvl.levelNumber}: ${strings.levelTitle(lvl.levelNumber)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = SolarGold,
                                modifier = Modifier.weight(1f)
                            )
                            if (lvl.isBoss) {
                                Surface(color = CrimsonFlame, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = strings.bossBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        val oppName = if (lvl.isBoss) strings.bossName(lvl.bossType) else strings.fighterName(lvl.opponentId)
                        Text(
                            text = "${strings.selectOpponent}: $oppName • ${strings.arenaName(lvl.arenaId)} • ${strings.difficultyLabel(lvl.difficulty)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Text(
                            text = "${strings.modifierName(lvl.modifier)} — ${strings.modifierDesc(lvl.modifier)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Equipped Perk Banner (if any)
            stats.equippedPerk?.let { perk ->
                Surface(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Shield, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${strings.equippedBadge}: ${strings.perkName(perk)} — ${strings.perkDesc(perk)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.selectYourFighter,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${stats.honorPoints} ${strings.honorLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarGold,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(FighterRoster.all, key = { it.id }) { fighter ->
                    val unlocked = fighter.id in stats.unlockedFighters
                    val selected = fighter.id == config.playerFighter.id
                    FighterRosterCard(
                        fighter = fighter,
                        selected = selected,
                        unlocked = unlocked,
                        canUnlockWithHonor = !unlocked && stats.honorPoints >= fighter.unlockCost,
                        tag = "select_player_${fighter.id.name.lowercase()}",
                        onClick = {
                            if (unlocked) {
                                onSelectPlayer(fighter)
                            } else if (stats.honorPoints >= fighter.unlockCost) {
                                onUnlockFighter(fighter.id, fighter.unlockCost)
                            }
                        }
                    )
                }
            }

            SelectedFighterSummary(fighter = config.playerFighter)

            if (config.mode != GameMode.CAMPAIGN) {
                Text(
                    text = strings.selectOpponent,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FighterRoster.all.forEach { opp ->
                        val isOppSelected = opp.id == config.opponentFighter.id
                        Surface(
                            color = if (isOppSelected) opp.primaryColor.copy(alpha = 0.22f) else ObsidianCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isOppSelected) 2.dp else 1.dp,
                                color = if (isOppSelected) opp.primaryColor else ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectOpponent(opp) }
                                .testTag("select_opp_${opp.id.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    text = strings.fighterName(opp.id),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isOppSelected) TextPrimary else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (config.mode == GameMode.VERSUS) {
                Text(
                    text = strings.selectDifficulty,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiDifficulty.entries.forEach { diff ->
                        val active = diff == config.difficulty
                        val color = when (diff) {
                            AiDifficulty.EASY -> ElectricCyan
                            AiDifficulty.NORMAL -> SolarGold
                            AiDifficulty.HARD -> CrimsonFlame
                            AiDifficulty.EXTREME -> VioletSurge
                        }
                        Surface(
                            color = if (active) color.copy(alpha = 0.22f) else ObsidianCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(if (active) 2.dp else 1.dp, if (active) color else ObsidianBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectDifficulty(diff) }
                                .testTag("diff_${diff.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    text = strings.difficultyLabel(diff),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (active) color else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Text(
                    text = strings.difficultyDesc(config.difficulty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (config.mode != GameMode.CAMPAIGN) {
                Text(
                    text = strings.selectArena,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ArenaCatalog.all, key = { it.id }) { arena ->
                        val selected = arena.id == config.arena.id
                        ArenaCard(
                            arena = arena,
                            selected = selected,
                            onClick = { onSelectArena(arena) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onStartFight,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonFlame,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_fight_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.btnStartFight,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
private fun FighterRosterCard(
    fighter: FighterProfile,
    selected: Boolean,
    unlocked: Boolean,
    canUnlockWithHonor: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) fighter.primaryColor.copy(alpha = 0.16f) else ObsidianCard
        ),
        border = BorderStroke(
            width = if (selected) 2.5.dp else 1.dp,
            color = when {
                selected -> fighter.primaryColor
                canUnlockWithHonor -> SolarGold
                else -> ObsidianBorder
            }
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(148.dp)
            .alpha(if (unlocked || canUnlockWithHonor) 1f else 0.58f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = unlocked || canUnlockWithHonor, onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                contentAlignment = Alignment.Center
            ) {
                FighterPortraitCanvas(
                    profile = fighter,
                    modifier = Modifier.fillMaxSize()
                )
                if (!unlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x990B0A12), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = strings.lockedFighterBadge,
                            tint = if (canUnlockWithHonor) SolarGold else TextMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.fighterName(fighter.id),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = if (unlocked) {
                    strings.fighterTitle(fighter.id)
                } else {
                    strings.unlockWithHonorFormat(fighter.unlockCost)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) fighter.auraColor else SolarGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectedFighterSummary(fighter: FighterProfile) {
    val strings = LocalStrings.current
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, fighter.primaryColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = fighter.auraColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${strings.specialPrefix}: ${strings.fighterSpecialName(fighter.id)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = fighter.auraColor
                )
            }
            Text(
                text = strings.fighterSpecialDesc(fighter.id),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            StatBarRow(label = strings.statSpeed, value = fighter.speedRating, color = ElectricCyan)
            StatBarRow(label = strings.statPower, value = fighter.powerRating, color = CrimsonFlame)
            StatBarRow(label = strings.statDefense, value = fighter.defenseRating, color = SolarGold)
        }
    }
}

@Composable
private fun StatBarRow(label: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(56.dp)
        )
        LinearProgressIndicator(
            progress = { value / 10f },
            color = color,
            trackColor = ObsidianBg,
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = "$value/10",
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ArenaCard(
    arena: ArenaStage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        border = BorderStroke(
            width = if (selected) 2.5.dp else 1.dp,
            color = if (selected) arena.accentColor else ObsidianBorder
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(190.dp)
            .height(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("select_arena_${arena.id.name.lowercase()}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = arena.backgroundRes),
                contentDescription = strings.arenaName(arena.id),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xDD0B0A12))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = strings.arenaName(arena.id),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) arena.accentColor else TextPrimary
                )
                Text(
                    text = strings.arenaSubtitle(arena.id),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
