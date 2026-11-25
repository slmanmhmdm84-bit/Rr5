package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class FighterId {
    SAQR,
    SAIF,
    JABBAR,
    LAYLA
}

enum class FighterStyle {
    MARTIAL_ARTIST,
    CYBER_RONIN,
    HEAVY_TITAN,
    SHADOW_ASSASSIN
}

data class FighterProfile(
    val id: FighterId,
    val style: FighterStyle,
    val primaryColor: Color,
    val secondaryColor: Color,
    val auraColor: Color,
    val skinColor: Color,
    val speedRating: Int,
    val powerRating: Int,
    val defenseRating: Int,
    val moveSpeed: Float,
    val damageScale: Float,
    val defenseScale: Float,
    val starterUnlocked: Boolean = true,
    val unlockCost: Int = 0,
    val unlockStageHint: Int? = null
)

object FighterRoster {
    val all: List<FighterProfile> = listOf(
        FighterProfile(
            id = FighterId.SAQR,
            style = FighterStyle.MARTIAL_ARTIST,
            primaryColor = Color(0xFFFF3B5C),
            secondaryColor = Color(0xFFFFC107),
            auraColor = Color(0xFFFF7A00),
            skinColor = Color(0xFFE6B18A),
            speedRating = 8,
            powerRating = 8,
            defenseRating = 7,
            moveSpeed = 385f,
            damageScale = 1.0f,
            defenseScale = 1.0f,
            starterUnlocked = true
        ),
        FighterProfile(
            id = FighterId.SAIF,
            style = FighterStyle.CYBER_RONIN,
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF1F2B47),
            auraColor = Color(0xFF38F8FF),
            skinColor = Color(0xFFD9B29C),
            speedRating = 10,
            powerRating = 7,
            defenseRating = 6,
            moveSpeed = 445f,
            damageScale = 0.94f,
            defenseScale = 0.92f,
            starterUnlocked = true
        ),
        FighterProfile(
            id = FighterId.JABBAR,
            style = FighterStyle.HEAVY_TITAN,
            primaryColor = Color(0xFFFF9800),
            secondaryColor = Color(0xFF455A64),
            auraColor = Color(0xFFFFD54F),
            skinColor = Color(0xFFC58C68),
            speedRating = 5,
            powerRating = 10,
            defenseRating = 10,
            moveSpeed = 320f,
            damageScale = 1.18f,
            defenseScale = 1.22f,
            starterUnlocked = false,
            unlockCost = 180,
            unlockStageHint = 3
        ),
        FighterProfile(
            id = FighterId.LAYLA,
            style = FighterStyle.SHADOW_ASSASSIN,
            primaryColor = Color(0xFFB5179E),
            secondaryColor = Color(0xFF7209B7),
            auraColor = Color(0xFFE0AAFF),
            skinColor = Color(0xFFEBC3AB),
            speedRating = 9,
            powerRating = 8,
            defenseRating = 6,
            moveSpeed = 430f,
            damageScale = 1.04f,
            defenseScale = 0.95f,
            starterUnlocked = false,
            unlockCost = 320,
            unlockStageHint = 6
        )
    )

    fun byId(id: FighterId): FighterProfile = all.first { it.id == id }
}

enum class ArenaId {
    NEON_DOJO,
    DESERT_CITADEL,
    VOLCANIC_FORGE
}

data class ArenaStage(
    val id: ArenaId,
    @DrawableRes val backgroundRes: Int,
    val floorColor: Color,
    val accentColor: Color
)

object ArenaCatalog {
    val all: List<ArenaStage> = listOf(
        ArenaStage(
            id = ArenaId.NEON_DOJO,
            backgroundRes = R.drawable.img_arena_neon_dojo,
            floorColor = Color(0xFF131829),
            accentColor = Color(0xFF00E5FF)
        ),
        ArenaStage(
            id = ArenaId.DESERT_CITADEL,
            backgroundRes = R.drawable.img_arena_desert_citadel,
            floorColor = Color(0xFF2B1D14),
            accentColor = Color(0xFFFFC107)
        ),
        ArenaStage(
            id = ArenaId.VOLCANIC_FORGE,
            backgroundRes = R.drawable.img_arena_volcanic_forge,
            floorColor = Color(0xFF1F1114),
            accentColor = Color(0xFFFF3B5C)
        )
    )

    fun byId(id: ArenaId): ArenaStage = all.first { it.id == id }
}

enum class AiDifficulty {
    EASY,
    NORMAL,
    HARD,
    EXTREME
}

enum class StageModifier {
    STANDARD,
    FAST_ENERGY,
    ARMORED_BOSS,
    HYPER_SPEED,
    CRITICAL_STRIKE,
    COORDINATED_AMBUSH,
    OVERLORD_RAGE
}

enum class BossType {
    NONE,
    IRON_COLOSSUS,
    PHANTOM_EMPRESS,
    OMEGA_SHOGUN
}

data class CampaignLevel(
    val levelNumber: Int,
    val opponentId: FighterId,
    val arenaId: ArenaId,
    val difficulty: AiDifficulty,
    val modifier: StageModifier,
    val bossType: BossType = BossType.NONE,
    val hasCoordinatedAlly: Boolean = false,
    val enemyHpMultiplier: Float,
    val enemyDamageMultiplier: Float,
    val aiAggression: Float,
    val rewardHonor: Int
) {
    val isBoss: Boolean get() = bossType != BossType.NONE
}

object LevelCatalog {
    val all: List<CampaignLevel> = listOf(
        CampaignLevel(
            levelNumber = 1,
            opponentId = FighterId.SAIF,
            arenaId = ArenaId.NEON_DOJO,
            difficulty = AiDifficulty.EASY,
            modifier = StageModifier.STANDARD,
            enemyHpMultiplier = 0.88f,
            enemyDamageMultiplier = 0.82f,
            aiAggression = 0.42f,
            rewardHonor = 60
        ),
        CampaignLevel(
            levelNumber = 2,
            opponentId = FighterId.SAQR,
            arenaId = ArenaId.DESERT_CITADEL,
            difficulty = AiDifficulty.NORMAL,
            modifier = StageModifier.FAST_ENERGY,
            enemyHpMultiplier = 1.0f,
            enemyDamageMultiplier = 0.95f,
            aiAggression = 0.60f,
            rewardHonor = 85
        ),
        CampaignLevel(
            levelNumber = 3,
            opponentId = FighterId.JABBAR,
            arenaId = ArenaId.VOLCANIC_FORGE,
            difficulty = AiDifficulty.NORMAL,
            modifier = StageModifier.ARMORED_BOSS,
            bossType = BossType.IRON_COLOSSUS,
            enemyHpMultiplier = 1.35f,
            enemyDamageMultiplier = 1.05f,
            aiAggression = 0.72f,
            rewardHonor = 140
        ),
        CampaignLevel(
            levelNumber = 4,
            opponentId = FighterId.LAYLA,
            arenaId = ArenaId.NEON_DOJO,
            difficulty = AiDifficulty.HARD,
            modifier = StageModifier.HYPER_SPEED,
            enemyHpMultiplier = 1.12f,
            enemyDamageMultiplier = 1.05f,
            aiAggression = 0.82f,
            rewardHonor = 120
        ),
        CampaignLevel(
            levelNumber = 5,
            opponentId = FighterId.SAQR,
            arenaId = ArenaId.DESERT_CITADEL,
            difficulty = AiDifficulty.HARD,
            modifier = StageModifier.COORDINATED_AMBUSH,
            hasCoordinatedAlly = true,
            enemyHpMultiplier = 1.18f,
            enemyDamageMultiplier = 1.08f,
            aiAggression = 0.88f,
            rewardHonor = 150
        ),
        CampaignLevel(
            levelNumber = 6,
            opponentId = FighterId.LAYLA,
            arenaId = ArenaId.NEON_DOJO,
            difficulty = AiDifficulty.HARD,
            modifier = StageModifier.CRITICAL_STRIKE,
            bossType = BossType.PHANTOM_EMPRESS,
            hasCoordinatedAlly = true,
            enemyHpMultiplier = 1.40f,
            enemyDamageMultiplier = 1.12f,
            aiAggression = 0.94f,
            rewardHonor = 200
        ),
        CampaignLevel(
            levelNumber = 7,
            opponentId = FighterId.JABBAR,
            arenaId = ArenaId.VOLCANIC_FORGE,
            difficulty = AiDifficulty.EXTREME,
            modifier = StageModifier.CRITICAL_STRIKE,
            hasCoordinatedAlly = true,
            enemyHpMultiplier = 1.32f,
            enemyDamageMultiplier = 1.16f,
            aiAggression = 0.96f,
            rewardHonor = 220
        ),
        CampaignLevel(
            levelNumber = 8,
            opponentId = FighterId.SAIF,
            arenaId = ArenaId.VOLCANIC_FORGE,
            difficulty = AiDifficulty.EXTREME,
            modifier = StageModifier.OVERLORD_RAGE,
            bossType = BossType.OMEGA_SHOGUN,
            hasCoordinatedAlly = true,
            enemyHpMultiplier = 1.65f,
            enemyDamageMultiplier = 1.22f,
            aiAggression = 1.0f,
            rewardHonor = 350
        )
    )

    fun byNumber(levelNumber: Int): CampaignLevel =
        all.firstOrNull { it.levelNumber == levelNumber } ?: all.first()
}

enum class CombatPerk(val unlockCost: Int) {
    SURGE_BATTERY(100),
    IRON_BASTION(140),
    COMBO_CATALYST(180),
    PHANTOM_REFLEX(220)
}

enum class AchievementId(val rewardHonor: Int) {
    FIRST_VICTORY(50),
    COMBO_SAVANT(90),
    PERFECT_ROUND(120),
    COLOSSUS_SLAYER(150),
    PHANTOM_BREAKER(200),
    APEX_CHAMPION(300),
    EXTREME_SURVIVOR(250),
    ARSENAL_COLLECTOR(180)
}

enum class MissionId(val target: Int, val rewardHonor: Int) {
    WIN_MATCHES(3, 120),
    LAND_COMBOS(5, 100),
    DEFEAT_BOSSES(2, 180),
    WIN_EXTREME_OR_HARD(2, 200)
}

enum class GameMode {
    CAMPAIGN,
    VERSUS,
    TRAINING
}

enum class FighterAction {
    IDLE,
    WALK_FORWARD,
    WALK_BACKWARD,
    JUMP,
    GUARD,
    DASH,
    LIGHT_PUNCH,
    HEAVY_KICK,
    SPECIAL_ATTACK,
    BOSS_SLAM,
    BOSS_TELEPORT,
    BOSS_BARRAGE,
    BOSS_SLASH,
    HITSTUN,
    KNOCKDOWN,
    VICTORY
}

data class Afterimage(
    val x: Float,
    val y: Float,
    val facingRight: Boolean,
    val action: FighterAction,
    val alpha: Float
)

data class FighterState(
    val profile: FighterProfile,
    val x: Float,
    val y: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val facingRight: Boolean,
    val hp: Float = 100f,
    val trailHp: Float = 100f,
    val maxHp: Float = 100f,
    val energy: Float = 35f,
    val action: FighterAction = FighterAction.IDLE,
    val actionTimer: Float = 0f,
    val actionDuration: Float = 0.4f,
    val hitRegistered: Boolean = false,
    val comboCount: Int = 0,
    val comboTimer: Float = 0f,
    val roundsWon: Int = 0,
    val flashTimer: Float = 0f,
    val animPhase: Float = 0f,
    val dashCooldown: Float = 0f,
    val attackVariant: Int = 0,
    val bossType: BossType = BossType.NONE,
    val bossPhase: Int = 1,
    val superArmorTimer: Float = 0f,
    val bossCooldown: Float = 2.4f,
    val guardFreshTimer: Float = 0f,
    val afterimages: List<Afterimage> = emptyList()
)

data class AllyStrikerState(
    val active: Boolean = false,
    val x: Float = 850f,
    val y: Float = 0f,
    val facingRight: Boolean = false,
    val color: Color = Color(0xFFB5179E),
    val timer: Float = 0f,
    val duration: Float = 1.25f,
    val strikeFired: Boolean = false,
    val isAerialDrone: Boolean = false
)

data class Projectile(
    val id: Long,
    val ownerIsPlayer: Boolean,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float = 0f,
    val radius: Float,
    val damage: Float,
    val primaryColor: Color,
    val coreColor: Color,
    val style: FighterStyle,
    val life: Float = 2.0f
)

data class HitParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val life: Float,
    val maxLife: Float
)

data class ImpactRing(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float
)

data class FloatingCallout(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float
)

enum class RoundPhase {
    INTRO,
    FIGHTING,
    KO_SLOWMO,
    ROUND_OVER,
    MATCH_OVER
}

data class SavedMatchSnapshot(
    val mode: GameMode,
    val difficulty: AiDifficulty,
    val playerId: FighterId,
    val opponentId: FighterId,
    val arenaId: ArenaId,
    val campaignLevelNumber: Int?,
    val roundNumber: Int,
    val roundTimeRemaining: Float,
    val playerHp: Float,
    val playerEnergy: Float,
    val playerRoundsWon: Int,
    val cpuHp: Float,
    val cpuEnergy: Float,
    val cpuRoundsWon: Int,
    val bossPhase: Int,
    val highestComboInMatch: Int,
    val kosInMatch: Int,
    val savedAtEpochMs: Long
)

data class MatchState(
    val mode: GameMode,
    val difficulty: AiDifficulty,
    val arena: ArenaStage,
    val player: FighterState,
    val cpu: FighterState,
    val allyStriker: AllyStrikerState? = null,
    val allyCooldown: Float = 5.5f,
    val roundNumber: Int = 1,
    val campaignLevel: CampaignLevel? = null,
    val activeModifier: StageModifier = StageModifier.STANDARD,
    val roundTimeRemaining: Float = 60f,
    val phase: RoundPhase = RoundPhase.INTRO,
    val phaseTimer: Float = 1.3f,
    val bannerText: String = "",
    val bannerSubtext: String = "",
    val bossWarningText: String = "",
    val aiTacticLabel: String = "",
    val projectiles: List<Projectile> = emptyList(),
    val particles: List<HitParticle> = emptyList(),
    val impactRings: List<ImpactRing> = emptyList(),
    val callouts: List<FloatingCallout> = emptyList(),
    val screenShake: Float = 0f,
    val hitStopTimer: Float = 0f,
    val playerWonMatch: Boolean? = null,
    val highestComboInMatch: Int = 0,
    val kosInMatch: Int = 0,
    val isPaused: Boolean = false
)
