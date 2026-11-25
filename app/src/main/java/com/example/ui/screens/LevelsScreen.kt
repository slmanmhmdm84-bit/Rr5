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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PlayerStats
import com.example.model.AiDifficulty
import com.example.model.CampaignLevel
import com.example.model.FighterRoster
import com.example.model.LevelCatalog
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

@Composable
fun LevelsScreen(
    stats: PlayerStats,
    onSelectLevel: (CampaignLevel) -> Unit,
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
                    .testTag("levels_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.btnBack,
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = strings.levelsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = strings.levelsSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(LevelCatalog.all, key = { it.levelNumber }) { level ->
                val isUnlocked = level.levelNumber <= stats.highestUnlockedLevel
                val isCleared = level.levelNumber in stats.clearedStages
                val isNext = level.levelNumber == stats.highestUnlockedLevel && !isCleared
                val opponent = FighterRoster.byId(level.opponentId)

                val diffColor = when (level.difficulty) {
                    AiDifficulty.EASY -> ElectricCyan
                    AiDifficulty.NORMAL -> SolarGold
                    AiDifficulty.HARD -> CrimsonFlame
                    AiDifficulty.EXTREME -> VioletSurge
                }

                Card(
                    onClick = { onSelectLevel(level) },
                    enabled = isUnlocked,
                    colors = CardDefaults.cardColors(
                        containerColor = ObsidianCard,
                        disabledContainerColor = ObsidianCard
                    ),
                    border = BorderStroke(
                        width = if (isNext || level.isBoss) 2.dp else 1.dp,
                        color = when {
                            !isUnlocked -> ObsidianBorder
                            isNext -> SolarGold
                            level.isBoss -> CrimsonFlame.copy(alpha = 0.8f)
                            isCleared -> EmeraldHealth.copy(alpha = 0.65f)
                            else -> opponent.primaryColor.copy(alpha = 0.6f)
                        }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isUnlocked) 1f else 0.52f)
                        .testTag("level_card_${level.levelNumber}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        if (isUnlocked) {
                                            (if (level.isBoss) CrimsonFlame else opponent.primaryColor).copy(alpha = 0.16f)
                                        } else Color.Transparent,
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 68.dp, height = 84.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianBg.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            FighterPortraitCanvas(
                                profile = opponent,
                                facingRight = false,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (!isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xAA0B0A12)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = strings.levelLocked,
                                        tint = TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${strings.levelLabel} ${level.levelNumber}: ${strings.levelTitle(level.levelNumber)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (level.isBoss) {
                                    Surface(color = CrimsonFlame, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = strings.bossBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (level.hasCoordinatedAlly) {
                                    Surface(color = VioletSurge, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = strings.allyBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (isCleared) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = strings.levelCleared,
                                        tint = EmeraldHealth,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isNext) {
                                    Surface(
                                        color = SolarGold,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = strings.levelNextBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ObsidianBg,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            val oppLabel = if (level.isBoss) strings.bossName(level.bossType) else strings.fighterName(opponent.id)
                            Text(
                                text = "$oppLabel • ${strings.arenaName(level.arenaId)} • ${strings.difficultyLabel(level.difficulty)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = diffColor
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = SolarGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${strings.modifierName(level.modifier)}: ${strings.modifierDesc(level.modifier)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
