package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundEffect
import com.example.model.Afterimage
import com.example.model.AiDifficulty
import com.example.model.AllyStrikerState
import com.example.model.ArenaCatalog
import com.example.model.ArenaStage
import com.example.model.BossType
import com.example.model.CampaignLevel
import com.example.model.CombatPerk
import com.example.model.FighterAction
import com.example.model.FighterProfile
import com.example.model.FighterRoster
import com.example.model.FighterState
import com.example.model.FighterStyle
import com.example.model.FloatingCallout
import com.example.model.GameMode
import com.example.model.HitParticle
import com.example.model.ImpactRing
import com.example.model.LevelCatalog
import com.example.model.MatchState
import com.example.model.Projectile
import com.example.model.RoundPhase
import com.example.model.SavedMatchSnapshot
import com.example.model.StageModifier
import com.example.ui.i18n.GameStrings
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.CrimsonFlame
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SolarGold
import com.example.ui.theme.VioletSurge
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class PlayerInputState(
    val moveLeft: Boolean = false,
    val moveRight: Boolean = false,
    val guardHeld: Boolean = false,
    val jumpQueued: Boolean = false,
    val punchQueued: Boolean = false,
    val kickQueued: Boolean = false,
    val specialQueued: Boolean = false,
    val dashQueued: Boolean = false
)

object CombatConstants {
    const val STAGE_MIN_X = 60f
    const val STAGE_MAX_X = 940f
    const val GRAVITY = -2250f
    const val JUMP_VELOCITY = 910f
    const val HURTBOX_HALF_WIDTH = 36f
    const val HURTBOX_HEIGHT = 162f
    const val SPECIAL_COST = 50f
}

class CombatEngine {
    private var nextId = 1L
    private val aiBrain = AdaptiveAiBrain()
    private var trainingIdleTimer = 0f
    private var warningTimer = 0f

    // Player Input Buffering (140ms)
    private var bufPunch = 0f
    private var bufKick = 0f
    private var bufSpecial = 0f
    private var bufJump = 0f
    private var bufDash = 0f

    fun createMatch(
        mode: GameMode,
        playerProfile: FighterProfile,
        opponentProfile: FighterProfile?,
        arena: ArenaStage,
        difficulty: AiDifficulty,
        campaignLevel: CampaignLevel? = null,
        equippedPerk: CombatPerk? = null,
        strings: LocalizedStrings = GameStrings.english
    ): MatchState {
        clearAllState()
        val activeLevel = if (mode == GameMode.CAMPAIGN) {
            campaignLevel ?: LevelCatalog.all.first()
        } else null

        val finalOpponent = activeLevel?.let { FighterRoster.byId(it.opponentId) }
            ?: opponentProfile
            ?: FighterRoster.all.first { it.id != playerProfile.id }
        val finalArena = activeLevel?.let { ArenaCatalog.byId(it.arenaId) } ?: arena
        val finalDiff = activeLevel?.difficulty ?: difficulty
        val modifier = activeLevel?.modifier ?: StageModifier.STANDARD
        val bossType = activeLevel?.bossType ?: BossType.NONE

        val cpuMaxHp = 100f * (activeLevel?.enemyHpMultiplier ?: when {
            mode == GameMode.VERSUS && finalDiff == AiDifficulty.EXTREME -> 1.15f
            else -> 1f
        })
        val initialCpuEnergy = if (modifier == StageModifier.OVERLORD_RAGE) 100f else 35f
        val initialPlayerEnergy = when {
            mode == GameMode.TRAINING -> 100f
            equippedPerk == CombatPerk.SURGE_BATTERY -> 55f
            modifier == StageModifier.FAST_ENERGY -> 50f
            else -> 35f
        }

        val bannerTitle = when {
            mode == GameMode.TRAINING -> strings.trainingBanner
            bossType != BossType.NONE -> strings.bossName(bossType)
            activeLevel != null -> "${strings.levelLabel} ${activeLevel.levelNumber} • ${strings.roundLabel} 1"
            else -> "${strings.roundLabel} 1"
        }
        val bannerSub = when {
            mode == GameMode.TRAINING -> strings.trainingSub
            bossType != BossType.NONE -> strings.bossTitle(bossType)
            activeLevel != null -> "${strings.levelTitle(activeLevel.levelNumber)} — ${strings.modifierName(modifier)}"
            else -> "${strings.readySub} (${strings.difficultyLabel(finalDiff)})"
        }

        return MatchState(
            mode = mode,
            difficulty = finalDiff,
            arena = finalArena,
            player = FighterState(
                profile = playerProfile,
                x = 270f,
                facingRight = true,
                hp = 100f,
                trailHp = 100f,
                maxHp = 100f,
                energy = initialPlayerEnergy
            ),
            cpu = FighterState(
                profile = finalOpponent,
                x = 730f,
                facingRight = false,
                hp = cpuMaxHp,
                trailHp = cpuMaxHp,
                maxHp = cpuMaxHp,
                energy = initialCpuEnergy,
                bossType = bossType,
                bossPhase = 1,
                bossCooldown = 2.6f
            ),
            allyCooldown = 4.8f,
            roundNumber = 1,
            campaignLevel = activeLevel,
            activeModifier = modifier,
            roundTimeRemaining = 60f,
            phase = RoundPhase.INTRO,
            phaseTimer = 0.95f,
            bannerText = bannerTitle,
            bannerSubtext = bannerSub
        )
    }

    fun createMatchFromSnapshot(
        snapshot: SavedMatchSnapshot,
        strings: LocalizedStrings = GameStrings.english
    ): MatchState {
        val level = snapshot.campaignLevelNumber?.let { LevelCatalog.byNumber(it) }
        val base = createMatch(
            mode = snapshot.mode,
            playerProfile = FighterRoster.byId(snapshot.playerId),
            opponentProfile = FighterRoster.byId(snapshot.opponentId),
            arena = ArenaCatalog.byId(snapshot.arenaId),
            difficulty = snapshot.difficulty,
            campaignLevel = level,
            strings = strings
        )
        return base.copy(
            roundNumber = snapshot.roundNumber,
            roundTimeRemaining = snapshot.roundTimeRemaining,
            phase = RoundPhase.INTRO,
            phaseTimer = 1.0f,
            bannerText = "${strings.roundLabel} ${snapshot.roundNumber}",
            bannerSubtext = strings.readySub,
            highestComboInMatch = snapshot.highestComboInMatch,
            kosInMatch = snapshot.kosInMatch,
            player = base.player.copy(
                hp = snapshot.playerHp.coerceIn(1f, base.player.maxHp),
                trailHp = snapshot.playerHp.coerceIn(1f, base.player.maxHp),
                energy = snapshot.playerEnergy.coerceIn(0f, 100f),
                roundsWon = snapshot.playerRoundsWon
            ),
            cpu = base.cpu.copy(
                hp = snapshot.cpuHp.coerceIn(1f, base.cpu.maxHp),
                trailHp = snapshot.cpuHp.coerceIn(1f, base.cpu.maxHp),
                energy = snapshot.cpuEnergy.coerceIn(0f, 100f),
                roundsWon = snapshot.cpuRoundsWon,
                bossPhase = snapshot.bossPhase
            )
        )
    }

    fun toSnapshot(state: MatchState): SavedMatchSnapshot? {
        if (state.mode == GameMode.TRAINING || state.phase == RoundPhase.MATCH_OVER) return null
        return SavedMatchSnapshot(
            mode = state.mode,
            difficulty = state.difficulty,
            playerId = state.player.profile.id,
            opponentId = state.cpu.profile.id,
            arenaId = state.arena.id,
            campaignLevelNumber = state.campaignLevel?.levelNumber,
            roundNumber = state.roundNumber,
            roundTimeRemaining = state.roundTimeRemaining,
            playerHp = state.player.hp.coerceAtLeast(1f),
            playerEnergy = state.player.energy,
            playerRoundsWon = state.player.roundsWon,
            cpuHp = state.cpu.hp.coerceAtLeast(1f),
            cpuEnergy = state.cpu.energy,
            cpuRoundsWon = state.cpu.roundsWon,
            bossPhase = state.cpu.bossPhase,
            highestComboInMatch = state.highestComboInMatch,
            kosInMatch = state.kosInMatch,
            savedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun clearAllState() {
        bufPunch = 0f
        bufKick = 0f
        bufSpecial = 0f
        bufJump = 0f
        bufDash = 0f
        trainingIdleTimer = 0f
        warningTimer = 0f
        aiBrain.reset()
    }

    fun step(
        state: MatchState,
        dtRaw: Float,
        input: PlayerInputState,
        equippedPerk: CombatPerk? = null,
        strings: LocalizedStrings = GameStrings.english,
        onSound: (SoundEffect) -> Unit,
        onHitImpact: (heavy: Boolean) -> Unit
    ): Pair<MatchState, PlayerInputState> {
        if (state.isPaused) return state to input
        val dt = dtRaw.coerceIn(0.0005f, 0.033f)

        bufPunch = if (input.punchQueued) 0.14f else max(0f, bufPunch - dt)
        bufKick = if (input.kickQueued) 0.14f else max(0f, bufKick - dt)
        bufSpecial = if (input.specialQueued) 0.14f else max(0f, bufSpecial - dt)
        bufJump = if (input.jumpQueued) 0.14f else max(0f, bufJump - dt)
        bufDash = if (input.dashQueued) 0.14f else max(0f, bufDash - dt)

        val consumedInput = input.copy(
            jumpQueued = false,
            punchQueued = false,
            kickQueued = false,
            specialQueued = false,
            dashQueued = false
        )

        val nextShake = state.screenShake * exp(-dt * 14f)
        var nextState = state.copy(screenShake = if (nextShake < 0.25f) 0f else nextShake)
        if (warningTimer > 0f) {
            warningTimer = max(0f, warningTimer - dt)
            if (warningTimer <= 0f) {
                nextState = nextState.copy(bossWarningText = "")
            }
        }

        if (nextState.hitStopTimer > 0f) {
            return nextState.copy(
                hitStopTimer = max(0f, nextState.hitStopTimer - dt),
                particles = updateParticles(nextState.particles, dt),
                impactRings = updateRings(nextState.impactRings, dt),
                callouts = updateCallouts(nextState.callouts, dt)
            ) to consumedInput
        }

        val speedMod = if (nextState.activeModifier == StageModifier.HYPER_SPEED) 1.22f else 1f
        val timeScale = (if (nextState.phase == RoundPhase.KO_SLOWMO) 0.35f else 1f) * speedMod
        val simDt = dt * timeScale

        when (nextState.phase) {
            RoundPhase.INTRO -> {
                val t = nextState.phaseTimer - dt
                if (t <= 0f) {
                    onSound(SoundEffect.ROUND_START)
                    nextState = nextState.copy(
                        phase = RoundPhase.FIGHTING,
                        phaseTimer = 0.75f,
                        bannerText = strings.fightBanner,
                        bannerSubtext = ""
                    )
                } else {
                    nextState = nextState.copy(phaseTimer = t)
                }
            }
            RoundPhase.FIGHTING -> {
                if (nextState.phaseTimer > 0f) {
                    val bannerT = nextState.phaseTimer - dt
                    nextState = nextState.copy(
                        phaseTimer = max(0f, bannerT),
                        bannerText = if (bannerT <= 0f) "" else nextState.bannerText
                    )
                }
                if (nextState.mode != GameMode.TRAINING) {
                    val timeLeft = max(0f, nextState.roundTimeRemaining - dt)
                    nextState = nextState.copy(roundTimeRemaining = timeLeft)
                    if (timeLeft <= 0f) {
                        nextState = resolveRoundEnd(nextState, timedOut = true, strings = strings, onSound = onSound)
                    }
                }
            }
            RoundPhase.KO_SLOWMO -> {
                val t = nextState.phaseTimer - dt
                if (t <= 0f) {
                    nextState = resolveRoundEnd(nextState, timedOut = false, strings = strings, onSound = onSound)
                } else {
                    nextState = nextState.copy(phaseTimer = t)
                }
            }
            RoundPhase.ROUND_OVER -> {
                val t = nextState.phaseTimer - dt
                if (t <= 0f) {
                    nextState = startNextRound(nextState, equippedPerk, strings)
                } else {
                    nextState = nextState.copy(phaseTimer = t)
                }
            }
            RoundPhase.MATCH_OVER -> {}
        }

        var p1 = updateTrailAndTimers(nextState.player, dt)
        var p2 = updateTrailAndTimers(nextState.cpu, dt)
        var particles = updateParticles(nextState.particles, dt)
        var rings = updateRings(nextState.impactRings, dt)
        var callouts = updateCallouts(nextState.callouts, dt)
        var projectiles = nextState.projectiles
        var ally = nextState.allyStriker
        var allyCd = max(0f, nextState.allyCooldown - dt)
        var bossWarn = nextState.bossWarningText
        var aiTactic = nextState.aiTacticLabel
        var shake = nextState.screenShake

        if (nextState.phase == RoundPhase.FIGHTING) {
            if (nextState.activeModifier == StageModifier.FAST_ENERGY) {
                p1 = p1.copy(energy = min(100f, p1.energy + dt * 9f))
                p2 = p2.copy(energy = min(100f, p2.energy + dt * 9f))
            } else if (nextState.activeModifier == StageModifier.OVERLORD_RAGE) {
                p2 = p2.copy(energy = min(100f, p2.energy + dt * 11f))
            }

            // Boss Phase 2 Enrage Transition at 50% HP
            if (p2.bossType != BossType.NONE && p2.bossPhase == 1 && p2.hp > 0f && p2.hp <= p2.maxHp * 0.50f) {
                onSound(SoundEffect.SPECIAL)
                p2 = p2.copy(
                    bossPhase = 2,
                    energy = min(100f, p2.energy + 45f),
                    bossCooldown = 0.6f,
                    superArmorTimer = 1.2f
                )
                rings = rings + ImpactRing(p2.x, 80f, 150f, SolarGold, 0.40f, 0.40f)
                particles = particles + spawnBurstParticles(p2.x, 85f, SolarGold, 24)
                callouts = callouts + FloatingCallout(nextId++, strings.bossPhase2Banner, p2.x, 195f, SolarGold, 1.3f, 1.3f)
                bossWarn = strings.bossPhase2Banner
                warningTimer = 1.6f
                shake = max(shake, 16f)
            }

            if (canChangeFacing(p1)) p1 = p1.copy(facingRight = p1.x <= p2.x)
            if (canChangeFacing(p2)) p2 = p2.copy(facingRight = p2.x < p1.x)

            val bufferedPlayerInput = input.copy(
                jumpQueued = bufJump > 0f,
                punchQueued = bufPunch > 0f,
                kickQueued = bufKick > 0f,
                specialQueued = bufSpecial > 0f,
                dashQueued = bufDash > 0f
            )
            val prevP1Action = p1.action
            val p1Result = applyPlayerControls(
                f = p1,
                input = bufferedPlayerInput,
                dt = simDt,
                speedMult = if (equippedPerk == CombatPerk.PHANTOM_REFLEX) 1.08f else 1f,
                dashCdMult = if (equippedPerk == CombatPerk.PHANTOM_REFLEX) 0.70f else 1f,
                onSound = onSound
            )
            p1 = p1Result

            if (p1.action != prevP1Action || p1.actionTimer == 0f) {
                when (p1.action) {
                    FighterAction.LIGHT_PUNCH -> bufPunch = 0f
                    FighterAction.HEAVY_KICK -> bufKick = 0f
                    FighterAction.SPECIAL_ATTACK -> bufSpecial = 0f
                    FighterAction.JUMP -> bufJump = 0f
                    FighterAction.DASH -> bufDash = 0f
                    else -> {}
                }
            }

            val supportsAlly = (nextState.campaignLevel?.hasCoordinatedAlly == true) ||
                (p2.bossType != BossType.NONE && p2.bossPhase >= 2) ||
                (nextState.difficulty == AiDifficulty.EXTREME && nextState.mode == GameMode.CAMPAIGN)
            val allyReady = supportsAlly && (ally == null || !ally.active) && allyCd <= 0f

            val aiDecision = aiBrain.decide(
                ai = p2,
                player = p1,
                difficulty = nextState.difficulty,
                aggression = nextState.campaignLevel?.aiAggression ?: defaultAggression(nextState.difficulty),
                mode = nextState.mode,
                allyReady = allyReady,
                dt = simDt,
                projectiles = projectiles,
                strings = strings
            )
            aiTactic = aiDecision.tacticLabel

            if (aiDecision.bossActionQueued != null) {
                val bossActRes = startBossAction(p2, p1, aiDecision.bossActionQueued, strings, onSound)
                p2 = bossActRes.first
                if (bossActRes.second.isNotEmpty()) {
                    bossWarn = bossActRes.second
                    warningTimer = 1.15f
                }
            } else {
                p2 = applyPlayerControls(
                    f = p2,
                    input = aiDecision.input,
                    dt = simDt,
                    speedMult = 1f,
                    dashCdMult = 1f,
                    onSound = onSound
                )
            }

            if (aiDecision.shouldSummonAlly && allyReady) {
                val spawnOnLeft = p1.x > 340f
                val spawnX = if (spawnOnLeft) max(CombatConstants.STAGE_MIN_X + 25f, p1.x - 190f)
                else min(CombatConstants.STAGE_MAX_X - 25f, p1.x + 190f)
                val isDrone = p2.bossType == BossType.OMEGA_SHOGUN
                ally = AllyStrikerState(
                    active = true,
                    x = spawnX,
                    y = if (isDrone) 140f else 0f,
                    facingRight = spawnX < p1.x,
                    color = if (isDrone) ElectricCyan else VioletSurge,
                    timer = 0f,
                    duration = 1.25f,
                    strikeFired = false,
                    isAerialDrone = isDrone
                )
                allyCd = if (p2.bossPhase >= 2 || nextState.difficulty == AiDifficulty.EXTREME) 5.5f else 8.0f
                bossWarn = strings.allyIncomingWarn
                warningTimer = 1.0f
                onSound(SoundEffect.DASH)
            }
        }

        // Update Coordinated Ally Striker
        if (ally != null && ally.active) {
            val nt = ally.timer + simDt
            if (nt >= ally.duration) {
                ally = null
            } else {
                var fired = ally.strikeFired
                if (!fired && nt >= 0.48f && nextState.phase == RoundPhase.FIGHTING) {
                    fired = true
                    val dir = if (p1.x >= ally.x) 1f else -1f
                    onSound(SoundEffect.SPECIAL)
                    projectiles = projectiles + Projectile(
                        id = nextId++,
                        ownerIsPlayer = false,
                        x = ally.x + dir * 26f,
                        y = if (ally.isAerialDrone) 128f else 78f,
                        vx = dir * 590f,
                        vy = if (ally.isAerialDrone) -135f else 0f,
                        radius = 22f,
                        damage = 11f * difficultyDamageScale(nextState.difficulty),
                        primaryColor = ally.color,
                        coreColor = Color.White,
                        style = if (ally.isAerialDrone) FighterStyle.CYBER_RONIN else FighterStyle.SHADOW_ASSASSIN,
                        life = 1.3f
                    )
                    rings = rings + ImpactRing(ally.x, ally.y + 65f, 45f, ally.color, 0.22f, 0.22f)
                }
                ally = ally.copy(timer = nt, strikeFired = fired, facingRight = p1.x >= ally.x)
            }
        }

        // Boss Mid-Action Effects (Teleport blink / Slam landing / Barrage / Dash Slash)
        val bossMid = stepBossSpecials(
            boss = p2,
            player = p1,
            projectiles = projectiles,
            particles = particles,
            rings = rings,
            callouts = callouts,
            shake = shake,
            strings = strings,
            onSound = onSound,
            onHitImpact = onHitImpact
        )
        p2 = bossMid.boss
        p1 = bossMid.player
        projectiles = bossMid.projectiles
        particles = bossMid.particles
        rings = bossMid.rings
        callouts = bossMid.callouts
        shake = bossMid.shake

        val p1WasAir = p1.y > 4f
        val p2WasAir = p2.y > 4f
        p1 = integratePhysics(p1, simDt)
        p2 = integratePhysics(p2, simDt)
        if (p1WasAir && p1.y <= 0.1f) particles = particles + spawnDustParticles(p1.x, Color(0xFFCCCCCC))
        if (p2WasAir && p2.y <= 0.1f) {
            particles = particles + spawnDustParticles(p2.x, if (p2.bossType == BossType.IRON_COLOSSUS) SolarGold else Color(0xFFCCCCCC))
        }

        // Ground pushbox separation
        if (abs(p1.y - p2.y) < 90f && p2.action != FighterAction.BOSS_TELEPORT) {
            val minDist = 68f
            val dx = p2.x - p1.x
            if (abs(dx) < minDist) {
                val push = (minDist - abs(dx)) * 0.5f
                if (dx >= 0f) {
                    p1 = p1.copy(x = (p1.x - push).coerceIn(CombatConstants.STAGE_MIN_X, CombatConstants.STAGE_MAX_X))
                    p2 = p2.copy(x = (p2.x + push).coerceIn(CombatConstants.STAGE_MIN_X, CombatConstants.STAGE_MAX_X))
                } else {
                    p1 = p1.copy(x = (p1.x + push).coerceIn(CombatConstants.STAGE_MIN_X, CombatConstants.STAGE_MAX_X))
                    p2 = p2.copy(x = (p2.x - push).coerceIn(CombatConstants.STAGE_MIN_X, CombatConstants.STAGE_MAX_X))
                }
            }
        }

        var hitStop = 0f
        var maxCombo = nextState.highestComboInMatch

        val globalDamageMult = if (nextState.activeModifier == StageModifier.CRITICAL_STRIKE) 1.35f else 1f
        val p1ComboMult = if (equippedPerk == CombatPerk.COMBO_CATALYST && p1.comboCount >= 1) 1.15f else 1f
        val p1EnergyMult = (if (nextState.activeModifier == StageModifier.FAST_ENERGY) 2f else 1f) *
            (if (equippedPerk == CombatPerk.SURGE_BATTERY) 1.2f else 1f)
        val p2EnergyMult = if (nextState.activeModifier == StageModifier.FAST_ENERGY) 2f else 1f

        val cpuDamageMult = (nextState.campaignLevel?.enemyDamageMultiplier ?: difficultyDamageScale(nextState.difficulty)) * globalDamageMult
        val heavyKnockbackBonus = if (nextState.activeModifier == StageModifier.ARMORED_BOSS) 1.3f else 1f

        if (nextState.phase == RoundPhase.FIGHTING) {
            val p1Spawn = maybeSpawnSpecialProjectile(p1, ownerIsPlayer = true, projectiles = projectiles, dmgMult = globalDamageMult * p1ComboMult)
            p1 = p1Spawn.first
            projectiles = p1Spawn.second

            val p2Spawn = maybeSpawnSpecialProjectile(p2, ownerIsPlayer = false, projectiles = projectiles, dmgMult = cpuDamageMult)
            p2 = p2Spawn.first
            projectiles = p2Spawn.second

            val p1DefenseBonus = if (equippedPerk == CombatPerk.IRON_BASTION) 1.12f else 1f
            val p1ChipScale = if (equippedPerk == CombatPerk.IRON_BASTION) 0.5f else 1f

            val melee1 = resolveMeleeHit(
                attacker = p1,
                defender = p2,
                damageMult = globalDamageMult * p1ComboMult,
                energyMult = p1EnergyMult,
                knockbackMult = 1f,
                defBonus = 1f,
                chipScale = 1f,
                particles = particles,
                rings = rings,
                callouts = callouts,
                strings = strings,
                onSound = onSound,
                onHitImpact = onHitImpact
            )
            p1 = melee1.attacker
            p2 = melee1.defender
            particles = melee1.particles
            rings = melee1.rings
            callouts = melee1.callouts
            hitStop = max(hitStop, melee1.hitStop)
            shake = max(shake, melee1.shake)

            val melee2 = resolveMeleeHit(
                attacker = p2,
                defender = p1,
                damageMult = cpuDamageMult,
                energyMult = p2EnergyMult,
                knockbackMult = heavyKnockbackBonus,
                defBonus = p1DefenseBonus,
                chipScale = p1ChipScale,
                particles = particles,
                rings = rings,
                callouts = callouts,
                strings = strings,
                onSound = onSound,
                onHitImpact = onHitImpact
            )
            p2 = melee2.attacker
            p1 = melee2.defender
            particles = melee2.particles
            rings = melee2.rings
            callouts = melee2.callouts
            hitStop = max(hitStop, melee2.hitStop)
            shake = max(shake, melee2.shake)

            val projStep = updateAndCollideProjectiles(
                projectiles = projectiles,
                player = p1,
                cpu = p2,
                p1EnergyMult = p1EnergyMult,
                p2EnergyMult = p2EnergyMult,
                p1DefBonus = p1DefenseBonus,
                p1ChipScale = p1ChipScale,
                particles = particles,
                rings = rings,
                callouts = callouts,
                strings = strings,
                dt = simDt,
                onSound = onSound,
                onHitImpact = onHitImpact
            )
            projectiles = projStep.projectiles
            p1 = projStep.player
            p2 = projStep.cpu
            particles = projStep.particles
            rings = projStep.rings
            callouts = projStep.callouts
            hitStop = max(hitStop, projStep.hitStop)
            shake = max(shake, projStep.shake)

            maxCombo = max(maxCombo, p1.comboCount)

            if (nextState.mode == GameMode.TRAINING) {
                if (p1.action == FighterAction.HITSTUN || p2.action == FighterAction.HITSTUN) {
                    trainingIdleTimer = 0f
                } else {
                    trainingIdleTimer += dt
                }
                if (trainingIdleTimer > 1.8f || p2.hp < 12f) {
                    p1 = p1.copy(hp = min(p1.maxHp, p1.hp + dt * 45f), energy = min(100f, p1.energy + dt * 25f))
                    p2 = p2.copy(hp = min(p2.maxHp, p2.hp + dt * 45f))
                } else {
                    p1 = p1.copy(energy = min(100f, p1.energy + dt * 12f))
                }
            } else if (p1.hp <= 0f || p2.hp <= 0f) {
                onSound(SoundEffect.KO)
                if (p1.hp <= 0f) {
                    p1 = p1.copy(hp = 0f, action = FighterAction.KNOCKDOWN, actionTimer = 0f, actionDuration = 2f, vx = if (p1.facingRight) -260f else 260f, vy = 340f)
                }
                if (p2.hp <= 0f) {
                    p2 = p2.copy(hp = 0f, action = FighterAction.KNOCKDOWN, actionTimer = 0f, actionDuration = 2f, vx = if (p2.facingRight) -260f else 260f, vy = 340f)
                }
                nextState = nextState.copy(
                    phase = RoundPhase.KO_SLOWMO,
                    phaseTimer = 1.25f,
                    bannerText = strings.koBanner,
                    bannerSubtext = strings.koSub,
                    screenShake = 22f,
                    kosInMatch = nextState.kosInMatch + if (p2.hp <= 0f) 1 else 0
                )
            }
        } else if (projectiles.isNotEmpty()) {
            projectiles = projectiles.mapNotNull { proj ->
                val nx = proj.x + proj.vx * simDt
                val ny = proj.y + proj.vy * simDt
                val nLife = proj.life - simDt
                if (nLife <= 0f || nx < 0f || nx > 1000f || ny < 0f) null else proj.copy(x = nx, y = ny, life = nLife)
            }
        }

        return nextState.copy(
            player = p1,
            cpu = p2,
            allyStriker = ally,
            allyCooldown = allyCd,
            bossWarningText = bossWarn,
            aiTacticLabel = aiTactic,
            projectiles = projectiles,
            particles = if (particles.size > 48) particles.takeLast(48) else particles,
            impactRings = if (rings.size > 16) rings.takeLast(16) else rings,
            callouts = if (callouts.size > 10) callouts.takeLast(10) else callouts,
            screenShake = shake,
            hitStopTimer = hitStop,
            highestComboInMatch = maxCombo
        ) to consumedInput
    }

    private fun defaultAggression(difficulty: AiDifficulty): Float = when (difficulty) {
        AiDifficulty.EASY -> 0.42f
        AiDifficulty.NORMAL -> 0.68f
        AiDifficulty.HARD -> 0.88f
        AiDifficulty.EXTREME -> 1.0f
    }

    private fun difficultyDamageScale(difficulty: AiDifficulty): Float = when (difficulty) {
        AiDifficulty.EASY -> 0.82f
        AiDifficulty.NORMAL -> 1.0f
        AiDifficulty.HARD -> 1.08f
        AiDifficulty.EXTREME -> 1.18f
    }

    private fun canChangeFacing(f: FighterState): Boolean {
        return f.y <= 1f && (f.action == FighterAction.IDLE || f.action == FighterAction.WALK_FORWARD || f.action == FighterAction.WALK_BACKWARD || f.action == FighterAction.GUARD)
    }

    private fun updateTrailAndTimers(f: FighterState, dt: Float): FighterState {
        val trailAlpha = 1f - exp(-dt * 4.8f)
        val newTrail = if (f.trailHp > f.hp) {
            max(f.hp, lerp(f.trailHp, f.hp, trailAlpha) - dt * 14f)
        } else f.hp
        val newComboTimer = max(0f, f.comboTimer - dt)
        val newComboCount = if (newComboTimer <= 0f) 0 else f.comboCount
        val newFlash = max(0f, f.flashTimer - dt)
        val newDashCd = max(0f, f.dashCooldown - dt)
        val newArmor = max(0f, f.superArmorTimer - dt)
        val newBossCd = max(0f, f.bossCooldown - dt)
        val newGuardFresh = if (f.action == FighterAction.GUARD) min(5f, f.guardFreshTimer + dt) else 0f
        val updatedAfterimages = if (f.afterimages.isEmpty()) {
            emptyList()
        } else {
            f.afterimages.mapNotNull {
                val a = it.alpha - dt * 5.2f
                if (a <= 0f) null else it.copy(alpha = a)
            }
        }
        return f.copy(
            trailHp = newTrail,
            comboTimer = newComboTimer,
            comboCount = newComboCount,
            flashTimer = newFlash,
            dashCooldown = newDashCd,
            superArmorTimer = newArmor,
            bossCooldown = newBossCd,
            guardFreshTimer = newGuardFresh,
            animPhase = f.animPhase + dt * 7.5f,
            afterimages = updatedAfterimages
        )
    }

    private fun applyPlayerControls(
        f: FighterState,
        input: PlayerInputState,
        dt: Float,
        speedMult: Float,
        dashCdMult: Float,
        onSound: (SoundEffect) -> Unit
    ): FighterState {
        var fighter = f
        if (fighter.action == FighterAction.HITSTUN ||
            fighter.action == FighterAction.KNOCKDOWN ||
            fighter.action == FighterAction.VICTORY ||
            fighter.action == FighterAction.BOSS_SLAM ||
            fighter.action == FighterAction.BOSS_TELEPORT ||
            fighter.action == FighterAction.BOSS_BARRAGE ||
            fighter.action == FighterAction.BOSS_SLASH
        ) {
            return fighter
        }

        val dashCancelWindow = fighter.action == FighterAction.DASH && fighter.actionTimer >= 0.10f
        val canCancelIntoKick = dashCancelWindow ||
            (fighter.action == FighterAction.LIGHT_PUNCH &&
                ((fighter.hitRegistered && fighter.actionTimer >= 0.06f) || fighter.actionTimer >= 0.17f))
        val canCancelIntoSpecial = dashCancelWindow ||
            ((fighter.action == FighterAction.LIGHT_PUNCH || fighter.action == FighterAction.HEAVY_KICK) &&
                ((fighter.hitRegistered && fighter.actionTimer >= 0.08f) || fighter.actionTimer >= fighter.actionDuration * 0.72f))
        val canCancelIntoDash = (fighter.action == FighterAction.LIGHT_PUNCH || fighter.action == FighterAction.HEAVY_KICK) &&
            fighter.hitRegistered && fighter.actionTimer >= 0.09f

        val isFreeAction = fighter.action == FighterAction.IDLE ||
            fighter.action == FighterAction.WALK_FORWARD ||
            fighter.action == FighterAction.WALK_BACKWARD ||
            fighter.action == FighterAction.GUARD ||
            fighter.action == FighterAction.JUMP

        if (input.specialQueued && fighter.energy >= CombatConstants.SPECIAL_COST && (isFreeAction || canCancelIntoSpecial) && fighter.y <= 4f) {
            onSound(SoundEffect.SPECIAL)
            return fighter.copy(
                action = FighterAction.SPECIAL_ATTACK,
                actionTimer = 0f,
                actionDuration = 0.44f,
                hitRegistered = false,
                vx = 0f,
                energy = fighter.energy - CombatConstants.SPECIAL_COST
            )
        }

        if (input.kickQueued && (isFreeAction || canCancelIntoKick)) {
            onSound(SoundEffect.PUNCH_SWING)
            val dir = if (fighter.facingRight) 1f else -1f
            val kickVx = if (fighter.y <= 2f) dir * 135f else if (abs(fighter.vx) < 220f) dir * 240f else fighter.vx
            val kickVy = if (fighter.y > 4f) min(fighter.vy, -140f) else fighter.vy
            return fighter.copy(
                action = FighterAction.HEAVY_KICK,
                actionTimer = 0f,
                actionDuration = 0.35f,
                hitRegistered = false,
                attackVariant = (fighter.attackVariant + 1) % 2,
                superArmorTimer = if (fighter.bossType == BossType.IRON_COLOSSUS) 0.28f else fighter.superArmorTimer,
                vx = kickVx,
                vy = kickVy
            )
        }

        if (input.punchQueued && (isFreeAction || dashCancelWindow)) {
            onSound(SoundEffect.PUNCH_SWING)
            val jabStep = if (fighter.y <= 2f) (if (fighter.facingRight) 95f else -95f) else fighter.vx
            return fighter.copy(
                action = FighterAction.LIGHT_PUNCH,
                actionTimer = 0f,
                actionDuration = 0.23f,
                hitRegistered = false,
                attackVariant = (fighter.attackVariant + 1) % 2,
                vx = jabStep
            )
        }

        if (input.dashQueued && (isFreeAction || canCancelIntoDash) && fighter.dashCooldown <= 0f && fighter.y <= 2f) {
            onSound(SoundEffect.DASH)
            val dir = when {
                input.moveLeft -> -1f
                input.moveRight -> 1f
                fighter.facingRight -> 1f
                else -> -1f
            }
            return fighter.copy(
                action = FighterAction.DASH,
                actionTimer = 0f,
                actionDuration = 0.17f,
                vx = dir * 880f * speedMult,
                dashCooldown = 0.36f * dashCdMult,
                afterimages = fighter.afterimages + Afterimage(fighter.x, fighter.y, fighter.facingRight, FighterAction.DASH, 0.75f)
            )
        }

        if (!isFreeAction && !dashCancelWindow) return fighter

        if (input.jumpQueued && fighter.y <= 2f) {
            onSound(SoundEffect.JUMP)
            val moveVx = when {
                input.moveLeft -> -fighter.profile.moveSpeed * speedMult * 0.95f
                input.moveRight -> fighter.profile.moveSpeed * speedMult * 0.95f
                else -> fighter.vx * 0.55f
            }
            return fighter.copy(
                action = FighterAction.JUMP,
                actionTimer = 0f,
                actionDuration = 0.9f,
                vy = CombatConstants.JUMP_VELOCITY,
                vx = moveVx
            )
        }

        if (input.guardHeld && fighter.y <= 2f) {
            return fighter.copy(
                action = FighterAction.GUARD,
                guardFreshTimer = if (fighter.action == FighterAction.GUARD) fighter.guardFreshTimer else 0f,
                vx = fighter.vx * exp(-dt * 20f)
            )
        }

        val moveDir = when {
            input.moveLeft && !input.moveRight -> -1f
            input.moveRight && !input.moveLeft -> 1f
            else -> 0f
        }
        val effectiveMoveSpeed = fighter.profile.moveSpeed * speedMult
        if (fighter.y <= 2f) {
            if (moveDir != 0f) {
                val walkingForward = (moveDir > 0f && fighter.facingRight) || (moveDir < 0f && !fighter.facingRight)
                val targetVx = moveDir * effectiveMoveSpeed * (if (walkingForward) 1.06f else 0.88f)
                val smoothVx = lerp(fighter.vx, targetVx, 1f - exp(-dt * 26f))
                fighter = fighter.copy(
                    action = if (walkingForward) FighterAction.WALK_FORWARD else FighterAction.WALK_BACKWARD,
                    vx = smoothVx
                )
            } else {
                val smoothVx = lerp(fighter.vx, 0f, 1f - exp(-dt * 28f))
                fighter = fighter.copy(
                    action = FighterAction.IDLE,
                    vx = if (abs(smoothVx) < 4f) 0f else smoothVx
                )
            }
        } else if (moveDir != 0f) {
            val targetAirVx = moveDir * effectiveMoveSpeed * 0.90f
            fighter = fighter.copy(vx = lerp(fighter.vx, targetAirVx, 1f - exp(-dt * 13f)))
        }

        return fighter
    }

    private fun startBossAction(
        boss: FighterState,
        player: FighterState,
        action: FighterAction,
        strings: LocalizedStrings,
        onSound: (SoundEffect) -> Unit
    ): Pair<FighterState, String> {
        val dir = if (player.x >= boss.x) 1f else -1f
        val cdBase = if (boss.bossPhase >= 2) 3.4f else 4.8f
        onSound(SoundEffect.SPECIAL)
        return when (action) {
            FighterAction.BOSS_SLAM -> {
                boss.copy(
                    action = FighterAction.BOSS_SLAM,
                    actionTimer = 0f,
                    actionDuration = 0.78f,
                    hitRegistered = false,
                    vy = 640f,
                    vx = dir * 320f,
                    facingRight = dir > 0f,
                    superArmorTimer = 0.78f,
                    bossCooldown = cdBase
                ) to strings.bossWarnSlam
            }
            FighterAction.BOSS_TELEPORT -> {
                boss.copy(
                    action = FighterAction.BOSS_TELEPORT,
                    actionTimer = 0f,
                    actionDuration = 0.56f,
                    hitRegistered = false,
                    vx = 0f,
                    bossCooldown = cdBase,
                    afterimages = boss.afterimages + Afterimage(boss.x, boss.y, boss.facingRight, FighterAction.IDLE, 0.85f)
                ) to strings.bossWarnTeleport
            }
            FighterAction.BOSS_BARRAGE -> {
                boss.copy(
                    action = FighterAction.BOSS_BARRAGE,
                    actionTimer = 0f,
                    actionDuration = 0.62f,
                    hitRegistered = false,
                    vx = 0f,
                    facingRight = dir > 0f,
                    bossCooldown = cdBase
                ) to strings.bossWarnBarrage
            }
            FighterAction.BOSS_SLASH -> {
                boss.copy(
                    action = FighterAction.BOSS_SLASH,
                    actionTimer = 0f,
                    actionDuration = 0.44f,
                    hitRegistered = false,
                    vx = dir * 820f,
                    facingRight = dir > 0f,
                    superArmorTimer = 0.40f,
                    bossCooldown = cdBase * 0.85f
                ) to strings.bossWarnSlash
            }
            else -> boss to ""
        }
    }

    private data class BossMidResult(
        val boss: FighterState,
        val player: FighterState,
        val projectiles: List<Projectile>,
        val particles: List<HitParticle>,
        val rings: List<ImpactRing>,
        val callouts: List<FloatingCallout>,
        val shake: Float
    )

    private fun stepBossSpecials(
        boss: FighterState,
        player: FighterState,
        projectiles: List<Projectile>,
        particles: List<HitParticle>,
        rings: List<ImpactRing>,
        callouts: List<FloatingCallout>,
        shake: Float,
        strings: LocalizedStrings,
        onSound: (SoundEffect) -> Unit,
        onHitImpact: (heavy: Boolean) -> Unit
    ): BossMidResult {
        var b = boss
        var p = player
        var projs = projectiles
        var parts = particles
        var nextRings = rings
        var calls = callouts
        var nextShake = shake

        when (b.action) {
            FighterAction.BOSS_SLAM -> {
                // On landing slam
                if (!b.hitRegistered && b.actionTimer > 0.25f && b.y <= 14f) {
                    b = b.copy(hitRegistered = true, vx = 0f)
                    onSound(SoundEffect.HIT_HEAVY)
                    nextShake = max(nextShake, 18f)
                    nextRings = nextRings + ImpactRing(b.x, 12f, 175f, SolarGold, 0.36f, 0.36f)
                    parts = parts + spawnBurstParticles(b.x, 18f, SolarGold, 22)
                    projs = projs + listOf(
                        Projectile(nextId++, false, b.x + 28f, 22f, 470f, 0f, 30f, 16f, b.profile.primaryColor, SolarGold, FighterStyle.HEAVY_TITAN, 1.1f),
                        Projectile(nextId++, false, b.x - 28f, 22f, -470f, 0f, 30f, 16f, b.profile.primaryColor, SolarGold, FighterStyle.HEAVY_TITAN, 1.1f)
                    )
                    if (abs(p.x - b.x) < 135f && p.y < 55f) {
                        onHitImpact(true)
                        val dmg = 18f / p.profile.defenseScale
                        calls = calls + FloatingCallout(nextId++, strings.guardBreakCallout, p.x, p.y + 170f, CrimsonFlame, 0.9f, 0.9f)
                        p = p.copy(
                            hp = max(0f, p.hp - dmg),
                            action = FighterAction.HITSTUN,
                            actionTimer = 0f,
                            actionDuration = 0.42f,
                            vx = (if (p.x >= b.x) 1f else -1f) * 320f,
                            vy = 290f,
                            flashTimer = 0.16f
                        )
                    }
                }
            }
            FighterAction.BOSS_TELEPORT -> {
                // Blink behind player halfway through
                if (!b.hitRegistered && b.actionTimer >= 0.22f) {
                    val behindOffset = if (p.facingRight) -92f else 92f
                    val newX = (p.x + behindOffset).coerceIn(CombatConstants.STAGE_MIN_X, CombatConstants.STAGE_MAX_X)
                    val newFacingRight = newX <= p.x
                    onSound(SoundEffect.DASH)
                    nextRings = nextRings + ImpactRing(b.x, 80f, 55f, VioletSurge, 0.24f, 0.24f) +
                        ImpactRing(newX, 80f, 75f, b.profile.auraColor, 0.28f, 0.28f)
                    b = b.copy(
                        x = newX,
                        facingRight = newFacingRight,
                        hitRegistered = true,
                        afterimages = b.afterimages + Afterimage(b.x, b.y, b.facingRight, FighterAction.IDLE, 0.85f)
                    )
                    if (abs(p.x - newX) < 115f && p.y < 120f) {
                        val facingStrike = p.facingRight == newFacingRight
                        val blocked = p.action == FighterAction.GUARD && !facingStrike
                        val dmg = (if (blocked) 3.5f else 15f) / p.profile.defenseScale
                        onSound(if (blocked) SoundEffect.BLOCK else SoundEffect.HIT_HEAVY)
                        onHitImpact(!blocked)
                        p = p.copy(
                            hp = max(0f, p.hp - dmg),
                            action = if (blocked) FighterAction.GUARD else FighterAction.HITSTUN,
                            actionTimer = 0f,
                            actionDuration = if (blocked) 0.16f else 0.34f,
                            vx = (if (newFacingRight) 1f else -1f) * 240f,
                            flashTimer = 0.14f
                        )
                    }
                }
            }
            FighterAction.BOSS_BARRAGE -> {
                if (!b.hitRegistered && b.actionTimer >= 0.24f) {
                    val dir = if (b.facingRight) 1f else -1f
                    onSound(SoundEffect.SPECIAL)
                    projs = projs + listOf(
                        Projectile(nextId++, false, b.x + dir * 55f, 92f, dir * 640f, 0f, 24f, 12f, ElectricCyan, Color.White, FighterStyle.CYBER_RONIN, 1.5f),
                        Projectile(nextId++, false, b.x + dir * 55f, 125f, dir * 610f, -95f, 22f, 11f, ElectricCyan, Color.White, FighterStyle.CYBER_RONIN, 1.5f),
                        Projectile(nextId++, false, b.x + dir * 45f, 65f, dir * 680f, 80f, 22f, 11f, b.profile.primaryColor, ElectricCyan, FighterStyle.CYBER_RONIN, 1.5f)
                    )
                    b = b.copy(hitRegistered = true)
                    nextShake = max(nextShake, 8f)
                }
            }
            FighterAction.BOSS_SLASH -> {
                if ((b.actionTimer * 100).toInt() % 3 == 0) {
                    b = b.copy(afterimages = (b.afterimages + Afterimage(b.x, b.y, b.facingRight, FighterAction.BOSS_SLASH, 0.7f)).takeLast(4))
                }
                if (!b.hitRegistered && b.actionTimer >= 0.10f && abs(p.x - b.x) < 95f && p.y < 120f) {
                    b = b.copy(hitRegistered = true)
                    val blocked = p.action == FighterAction.GUARD && p.facingRight != b.facingRight
                    val dmg = (if (blocked) 4f else 17f) / p.profile.defenseScale
                    onSound(if (blocked) SoundEffect.BLOCK else SoundEffect.HIT_HEAVY)
                    onHitImpact(!blocked)
                    nextRings = nextRings + ImpactRing((b.x + p.x) * 0.5f, 88f, 90f, ElectricCyan, 0.28f, 0.28f)
                    p = p.copy(
                        hp = max(0f, p.hp - dmg),
                        action = if (blocked) FighterAction.GUARD else FighterAction.HITSTUN,
                        actionTimer = 0f,
                        actionDuration = if (blocked) 0.16f else 0.36f,
                        vx = (if (b.facingRight) 1f else -1f) * 300f,
                        flashTimer = 0.15f
                    )
                }
            }
            else -> {}
        }

        return BossMidResult(b, p, projs, parts, nextRings, calls, nextShake)
    }

    private fun integratePhysics(f: FighterState, dt: Float): FighterState {
        var timer = f.actionTimer + dt
        var action = f.action
        var duration = f.actionDuration
        var vx = f.vx
        var vy = f.vy
        var x = f.x + vx * dt
        var y = f.y + vy * dt
        var afterimages = f.afterimages

        if (action == FighterAction.DASH && (timer * 100).toInt() % 4 == 0) {
            afterimages = (afterimages + Afterimage(x, y, f.facingRight, action, 0.55f)).takeLast(4)
        }

        if (y > 0f || vy > 0f) {
            vy += CombatConstants.GRAVITY * dt
            if (y <= 0f) {
                y = 0f
                vy = 0f
                if (action == FighterAction.JUMP) {
                    action = FighterAction.IDLE
                    timer = 0f
                    vx *= 0.4f
                }
            }
        } else {
            y = 0f
            vy = 0f
        }

        if (action == FighterAction.HITSTUN || action == FighterAction.KNOCKDOWN) {
            vx *= exp(-dt * 6.8f)
        }

        if (action != FighterAction.IDLE &&
            action != FighterAction.WALK_FORWARD &&
            action != FighterAction.WALK_BACKWARD &&
            action != FighterAction.GUARD &&
            action != FighterAction.VICTORY &&
            action != FighterAction.KNOCKDOWN
        ) {
            if (timer >= duration) {
                action = if (y > 0f) FighterAction.JUMP else FighterAction.IDLE
                timer = 0f
                duration = 0.4f
                if (y <= 0f) vx = 0f
            }
        } else if (action == FighterAction.KNOCKDOWN && y <= 0f && timer >= duration) {
            vx = 0f
        }

        x = x.coerceIn(CombatConstants.STAGE_MIN_X, CombatConstants.STAGE_MAX_X)
        return f.copy(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            action = action,
            actionTimer = timer,
            actionDuration = duration,
            afterimages = afterimages
        )
    }

    private fun maybeSpawnSpecialProjectile(
        f: FighterState,
        ownerIsPlayer: Boolean,
        projectiles: List<Projectile>,
        dmgMult: Float
    ): Pair<FighterState, List<Projectile>> {
        if (f.action != FighterAction.SPECIAL_ATTACK || f.hitRegistered) {
            return f to projectiles
        }
        if (f.actionTimer >= 0.13f) {
            val dir = if (f.facingRight) 1f else -1f
            val spawnX = f.x + dir * 68f
            val newProjectiles = when (f.profile.style) {
                FighterStyle.SHADOW_ASSASSIN -> listOf(
                    Projectile(
                        id = nextId++,
                        ownerIsPlayer = ownerIsPlayer,
                        x = spawnX,
                        y = f.y + 88f,
                        vx = dir * 700f,
                        radius = 28f,
                        damage = 13f * f.profile.damageScale * dmgMult,
                        primaryColor = f.profile.primaryColor,
                        coreColor = f.profile.auraColor,
                        style = f.profile.style,
                        life = 1.6f
                    ),
                    Projectile(
                        id = nextId++,
                        ownerIsPlayer = ownerIsPlayer,
                        x = spawnX - dir * 48f,
                        y = f.y + 62f,
                        vx = dir * 770f,
                        radius = 24f,
                        damage = 11f * f.profile.damageScale * dmgMult,
                        primaryColor = f.profile.secondaryColor,
                        coreColor = Color.White,
                        style = f.profile.style,
                        life = 1.6f
                    )
                )
                FighterStyle.HEAVY_TITAN -> listOf(
                    Projectile(
                        id = nextId++,
                        ownerIsPlayer = ownerIsPlayer,
                        x = spawnX,
                        y = 26f,
                        vx = dir * 530f,
                        radius = 40f,
                        damage = 25f * f.profile.damageScale * dmgMult,
                        primaryColor = f.profile.primaryColor,
                        coreColor = f.profile.auraColor,
                        style = f.profile.style,
                        life = 1.4f
                    )
                )
                else -> listOf(
                    Projectile(
                        id = nextId++,
                        ownerIsPlayer = ownerIsPlayer,
                        x = spawnX,
                        y = f.y + 84f,
                        vx = dir * 680f,
                        radius = 32f,
                        damage = 22f * f.profile.damageScale * dmgMult,
                        primaryColor = f.profile.primaryColor,
                        coreColor = f.profile.auraColor,
                        style = f.profile.style,
                        life = 1.7f
                    )
                )
            }
            return f.copy(hitRegistered = true) to (projectiles + newProjectiles)
        }
        return f to projectiles
    }

    private data class MeleeResult(
        val attacker: FighterState,
        val defender: FighterState,
        val particles: List<HitParticle>,
        val rings: List<ImpactRing>,
        val callouts: List<FloatingCallout>,
        val hitStop: Float,
        val shake: Float
    )

    private fun resolveMeleeHit(
        attacker: FighterState,
        defender: FighterState,
        damageMult: Float,
        energyMult: Float,
        knockbackMult: Float,
        defBonus: Float,
        chipScale: Float,
        particles: List<HitParticle>,
        rings: List<ImpactRing>,
        callouts: List<FloatingCallout>,
        strings: LocalizedStrings,
        onSound: (SoundEffect) -> Unit,
        onHitImpact: (heavy: Boolean) -> Unit
    ): MeleeResult {
        if (attacker.hitRegistered) return MeleeResult(attacker, defender, particles, rings, callouts, 0f, 0f)
        if (defender.action == FighterAction.KNOCKDOWN || defender.action == FighterAction.BOSS_TELEPORT) {
            return MeleeResult(attacker, defender, particles, rings, callouts, 0f, 0f)
        }

        val isLight = attacker.action == FighterAction.LIGHT_PUNCH && attacker.actionTimer in 0.05f..0.16f
        val isHeavy = attacker.action == FighterAction.HEAVY_KICK && attacker.actionTimer in 0.10f..0.26f
        if (!isLight && !isHeavy) return MeleeResult(attacker, defender, particles, rings, callouts, 0f, 0f)

        val dir = if (attacker.facingRight) 1f else -1f
        val reach = if (isHeavy) 138f else 110f
        val hitCenterX = attacker.x + dir * (reach * 0.58f)
        val hitHalfW = reach * 0.48f
        val hitBottomY = attacker.y + (if (isHeavy) 42f else 68f)
        val hitTopY = attacker.y + 132f

        val defLeft = defender.x - CombatConstants.HURTBOX_HALF_WIDTH
        val defRight = defender.x + CombatConstants.HURTBOX_HALF_WIDTH
        val defBottom = defender.y
        val defTop = defender.y + CombatConstants.HURTBOX_HEIGHT

        val overlapsX = (hitCenterX + hitHalfW >= defLeft) && (hitCenterX - hitHalfW <= defRight)
        val overlapsY = (hitTopY >= defBottom) && (hitBottomY <= defTop)
        if (!overlapsX || !overlapsY) return MeleeResult(attacker, defender, particles, rings, callouts, 0f, 0f)

        val isCounterHit = (defender.action == FighterAction.LIGHT_PUNCH || defender.action == FighterAction.HEAVY_KICK) &&
            !defender.hitRegistered && defender.actionTimer < 0.11f
        val counterMult = if (isCounterHit) 1.25f else 1f
        val rawDamage = (if (isHeavy) 14f else 7.5f) * attacker.profile.damageScale * damageMult * counterMult
        val isBlocking = defender.action == FighterAction.GUARD && defender.facingRight != attacker.facingRight
        val isParry = isBlocking && defender.guardFreshTimer in 0.02f..0.22f
        val defenseFactor = 1f / (defender.profile.defenseScale * defBonus)
        val finalDamage = when {
            isParry -> 0f
            isBlocking -> rawDamage * 0.20f * chipScale * defenseFactor
            else -> rawDamage * defenseFactor
        }

        val impactX = (attacker.x + defender.x) * 0.5f
        val impactY = defender.y + 92f
        val wallSlam = !isBlocking && isHeavy &&
            (defender.x <= CombatConstants.STAGE_MIN_X + 26f || defender.x >= CombatConstants.STAGE_MAX_X - 26f)

        val ringColor = when {
            isParry -> SolarGold
            isBlocking -> ElectricCyan
            isCounterHit -> SolarGold
            isHeavy -> CrimsonFlame
            else -> attacker.profile.auraColor
        }
        var newRings = rings + ImpactRing(
            x = impactX,
            y = impactY,
            maxRadius = if (isParry || isHeavy || isCounterHit) 84f else 54f,
            color = ringColor,
            life = if (isParry) 0.28f else 0.24f,
            maxLife = if (isParry) 0.28f else 0.24f
        )
        if (wallSlam) {
            newRings = newRings + ImpactRing(
                x = defender.x,
                y = impactY,
                maxRadius = 96f,
                color = SolarGold,
                life = 0.26f,
                maxLife = 0.26f
            )
        }

        val newParticles = particles + spawnBurstParticles(
            x = impactX,
            y = impactY,
            color = when {
                isParry -> SolarGold
                isBlocking -> ElectricCyan
                else -> attacker.profile.auraColor
            },
            count = if (isParry || isHeavy || wallSlam) 20 else 11
        )

        val newAttackerCombo = if (isBlocking) attacker.comboCount else attacker.comboCount + 1
        val newCallouts = when {
            isParry -> callouts + FloatingCallout(
                id = nextId++,
                text = strings.parryCallout,
                x = defender.x,
                y = defender.y + 178f,
                color = SolarGold,
                life = 0.85f,
                maxLife = 0.85f
            )
            isBlocking -> callouts + FloatingCallout(
                id = nextId++,
                text = strings.blockCallout,
                x = defender.x,
                y = defender.y + 160f,
                color = ElectricCyan,
                life = 0.55f,
                maxLife = 0.55f
            )
            isCounterHit -> callouts + FloatingCallout(
                id = nextId++,
                text = strings.counterCallout,
                x = defender.x,
                y = defender.y + 185f,
                color = SolarGold,
                life = 0.85f,
                maxLife = 0.85f
            )
            newAttackerCombo >= 2 -> callouts + FloatingCallout(
                id = nextId++,
                text = "${newAttackerCombo}× ${strings.comboLabel}!",
                x = defender.x,
                y = defender.y + 175f,
                color = if (newAttackerCombo >= 4) SolarGold else attacker.profile.auraColor,
                life = 0.85f,
                maxLife = 0.85f
            )
            else -> callouts
        }

        if (isParry) {
            onSound(SoundEffect.BLOCK)
            onHitImpact(true)
        } else if (isBlocking) {
            onSound(SoundEffect.BLOCK)
        } else {
            onSound(if (isHeavy || isCounterHit) SoundEffect.HIT_HEAVY else SoundEffect.HIT_LIGHT)
            onHitImpact(isHeavy || isCounterHit)
        }

        val hasArmor = !isBlocking && defender.superArmorTimer > 0f
        val knockback = (if (isHeavy) 295f else 155f) * (when {
            isParry -> 0.25f
            isBlocking -> 0.55f
            else -> 1f
        }) * knockbackMult * dir
        val updatedAttacker = attacker.copy(
            hitRegistered = true,
            energy = min(100f, attacker.energy + (if (isHeavy) 14f else 9f) * energyMult),
            comboCount = newAttackerCombo,
            comboTimer = if (isBlocking) attacker.comboTimer else 1.55f
        )
        val updatedDefender = defender.copy(
            hp = max(0f, defender.hp - finalDamage),
            energy = min(100f, defender.energy + (when {
                isParry -> 16f
                isBlocking -> 8f
                else -> 6f
            }) * energyMult),
            action = when {
                isBlocking -> FighterAction.GUARD
                hasArmor -> defender.action
                else -> FighterAction.HITSTUN
            },
            actionTimer = if (hasArmor) defender.actionTimer else 0f,
            actionDuration = when {
                isParry -> 0.06f
                isBlocking -> 0.14f
                hasArmor -> defender.actionDuration
                isHeavy -> 0.34f
                else -> 0.22f
            },
            guardFreshTimer = if (isBlocking) 1f else defender.guardFreshTimer,
            vx = if (hasArmor) defender.vx else knockback,
            vy = if (!isBlocking && !hasArmor && isHeavy && defender.y <= 2f) 190f else defender.vy,
            flashTimer = 0.12f
        )

        return MeleeResult(
            attacker = updatedAttacker,
            defender = updatedDefender,
            particles = newParticles,
            rings = newRings,
            callouts = newCallouts,
            hitStop = when {
                isParry -> 0.045f
                isBlocking -> 0.015f
                isHeavy || isCounterHit -> 0.055f
                else -> 0.02f
            },
            shake = when {
                isParry -> 6f
                isBlocking -> 3.5f
                wallSlam -> 16f
                isHeavy || isCounterHit -> 12f
                else -> 5f
            }
        )
    }

    private data class ProjStepResult(
        val projectiles: List<Projectile>,
        val player: FighterState,
        val cpu: FighterState,
        val particles: List<HitParticle>,
        val rings: List<ImpactRing>,
        val callouts: List<FloatingCallout>,
        val hitStop: Float,
        val shake: Float
    )

    private fun updateAndCollideProjectiles(
        projectiles: List<Projectile>,
        player: FighterState,
        cpu: FighterState,
        p1EnergyMult: Float,
        p2EnergyMult: Float,
        p1DefBonus: Float,
        p1ChipScale: Float,
        particles: List<HitParticle>,
        rings: List<ImpactRing>,
        callouts: List<FloatingCallout>,
        strings: LocalizedStrings,
        dt: Float,
        onSound: (SoundEffect) -> Unit,
        onHitImpact: (heavy: Boolean) -> Unit
    ): ProjStepResult {
        if (projectiles.isEmpty()) {
            return ProjStepResult(projectiles, player, cpu, particles, rings, callouts, 0f, 0f)
        }
        var p1 = player
        var p2 = cpu
        var parts = particles
        var nextRings = rings
        var calls = callouts
        var hitStop = 0f
        var shake = 0f
        val remaining = ArrayList<Projectile>(projectiles.size)

        for (proj in projectiles) {
            val nx = proj.x + proj.vx * dt
            val ny = proj.y + proj.vy * dt
            val nLife = proj.life - dt
            if (nLife <= 0f || nx < 0f || nx > 1000f || ny < 0f) continue

            if (Random.nextFloat() < 0.45f) {
                parts = parts + HitParticle(
                    x = nx,
                    y = ny + (Random.nextFloat() - 0.5f) * 14f,
                    vx = -proj.vx * 0.15f,
                    vy = (Random.nextFloat() - 0.5f) * 60f,
                    color = proj.coreColor,
                    size = 4f + Random.nextFloat() * 4f,
                    life = 0.18f,
                    maxLife = 0.18f
                )
            }

            val target = if (proj.ownerIsPlayer) p2 else p1
            val attacker = if (proj.ownerIsPlayer) p1 else p2

            // Dashing gives brief projectile dodge invulnerability frames
            val dashEvading = target.action == FighterAction.DASH && target.actionTimer < 0.14f
            val hitX = abs(nx - target.x) <= (CombatConstants.HURTBOX_HALF_WIDTH + proj.radius)
            val hitY = ny in target.y..(target.y + CombatConstants.HURTBOX_HEIGHT)

            if (hitX && hitY && !dashEvading && target.action != FighterAction.KNOCKDOWN && target.action != FighterAction.BOSS_TELEPORT) {
                val isBlocking = target.action == FighterAction.GUARD
                val isParry = isBlocking && target.guardFreshTimer in 0.02f..0.22f
                val defBonus = if (proj.ownerIsPlayer) 1f else p1DefBonus
                val chipScale = if (proj.ownerIsPlayer) 1f else p1ChipScale
                val targetEnergyMult = if (proj.ownerIsPlayer) p2EnergyMult else p1EnergyMult
                val defenseFactor = 1f / (target.profile.defenseScale * defBonus)
                val dmg = when {
                    isParry -> 0f
                    isBlocking -> proj.damage * defenseFactor * 0.25f * chipScale
                    else -> proj.damage * defenseFactor
                }
                val dir = if (proj.vx >= 0f) 1f else -1f

                if (isParry) {
                    onSound(SoundEffect.BLOCK)
                    onHitImpact(true)
                    calls = calls + FloatingCallout(nextId++, strings.parryCallout, target.x, target.y + 178f, SolarGold, 0.85f, 0.85f)
                } else if (isBlocking) {
                    onSound(SoundEffect.BLOCK)
                    calls = calls + FloatingCallout(nextId++, strings.specialBlockCallout, target.x, target.y + 165f, ElectricCyan, 0.7f, 0.7f)
                } else {
                    onSound(SoundEffect.HIT_HEAVY)
                    onHitImpact(true)
                    val combo = attacker.comboCount + 1
                    val updatedAttacker = attacker.copy(comboCount = combo, comboTimer = 1.6f)
                    if (proj.ownerIsPlayer) p1 = updatedAttacker else p2 = updatedAttacker
                    calls = calls + FloatingCallout(nextId++, strings.specialHitCallout, target.x, target.y + 175f, SolarGold, 0.9f, 0.9f)
                }

                nextRings = nextRings + ImpactRing(
                    x = nx,
                    y = ny,
                    maxRadius = if (isParry) 105f else 92f,
                    color = when {
                        isParry -> SolarGold
                        isBlocking -> ElectricCyan
                        else -> proj.coreColor
                    },
                    life = 0.28f,
                    maxLife = 0.28f
                )
                parts = parts + spawnBurstParticles(
                    nx,
                    ny,
                    when {
                        isParry -> SolarGold
                        isBlocking -> ElectricCyan
                        else -> proj.coreColor
                    },
                    22
                )
                val hasArmor = !isBlocking && target.superArmorTimer > 0f
                val updatedTarget = target.copy(
                    hp = max(0f, target.hp - dmg),
                    energy = min(100f, target.energy + (if (isParry) 18f else 10f) * targetEnergyMult),
                    action = when {
                        isBlocking -> FighterAction.GUARD
                        hasArmor -> target.action
                        else -> FighterAction.HITSTUN
                    },
                    actionTimer = if (hasArmor) target.actionTimer else 0f,
                    actionDuration = when {
                        isParry -> 0.06f
                        isBlocking -> 0.18f
                        hasArmor -> target.actionDuration
                        else -> 0.38f
                    },
                    guardFreshTimer = if (isBlocking) 1f else target.guardFreshTimer,
                    vx = if (hasArmor) target.vx else dir * (when {
                        isParry -> 60f
                        isBlocking -> 160f
                        else -> 360f
                    }),
                    vy = if (!isBlocking && !hasArmor && target.y <= 2f) 245f else target.vy,
                    flashTimer = 0.14f
                )
                if (proj.ownerIsPlayer) p2 = updatedTarget else p1 = updatedTarget
                hitStop = max(hitStop, if (isParry) 0.05f else 0.06f)
                shake = max(shake, if (isParry) 7f else 15f)
            } else {
                remaining.add(proj.copy(x = nx, y = ny, life = nLife))
            }
        }

        return ProjStepResult(remaining, p1, p2, parts, nextRings, calls, hitStop, shake)
    }

    private fun spawnBurstParticles(x: Float, y: Float, color: Color, count: Int): List<HitParticle> {
        return List(count) { i ->
            val angle = (i.toFloat() / count) * 6.283f + Random.nextFloat() * 0.4f
            val speed = 170f + Random.nextFloat() * 350f
            HitParticle(
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed + 80f,
                color = if (i % 3 == 0) Color.White else color,
                size = 5f + Random.nextFloat() * 8f,
                life = 0.26f + Random.nextFloat() * 0.20f,
                maxLife = 0.46f
            )
        }
    }

    private fun spawnDustParticles(x: Float, color: Color): List<HitParticle> {
        return List(6) { i ->
            val dir = if (i % 2 == 0) 1f else -1f
            HitParticle(
                x = x + dir * 12f,
                y = 4f,
                vx = dir * (60f + Random.nextFloat() * 90f),
                vy = 40f + Random.nextFloat() * 60f,
                color = color.copy(alpha = 0.7f),
                size = 4f + Random.nextFloat() * 5f,
                life = 0.24f,
                maxLife = 0.24f
            )
        }
    }

    private fun updateParticles(particles: List<HitParticle>, dt: Float): List<HitParticle> {
        if (particles.isEmpty()) return emptyList()
        val out = ArrayList<HitParticle>(particles.size)
        for (i in particles.indices) {
            val p = particles[i]
            val nl = p.life - dt
            if (nl > 0f) {
                out.add(
                    p.copy(
                        x = p.x + p.vx * dt,
                        y = p.y + p.vy * dt,
                        vy = p.vy - 700f * dt,
                        life = nl
                    )
                )
            }
        }
        return out
    }

    private fun updateRings(rings: List<ImpactRing>, dt: Float): List<ImpactRing> {
        if (rings.isEmpty()) return emptyList()
        val out = ArrayList<ImpactRing>(rings.size)
        for (i in rings.indices) {
            val r = rings[i]
            val nl = r.life - dt
            if (nl > 0f) out.add(r.copy(life = nl))
        }
        return out
    }

    private fun updateCallouts(callouts: List<FloatingCallout>, dt: Float): List<FloatingCallout> {
        if (callouts.isEmpty()) return emptyList()
        val out = ArrayList<FloatingCallout>(callouts.size)
        for (i in callouts.indices) {
            val c = callouts[i]
            val nl = c.life - dt
            if (nl > 0f) out.add(c.copy(y = c.y + 42f * dt, life = nl))
        }
        return out
    }

    private fun resolveRoundEnd(
        state: MatchState,
        timedOut: Boolean,
        strings: LocalizedStrings,
        onSound: (SoundEffect) -> Unit
    ): MatchState {
        val p1Pct = state.player.hp / state.player.maxHp
        val p2Pct = state.cpu.hp / state.cpu.maxHp

        var p1Wins = state.player.roundsWon
        var p2Wins = state.cpu.roundsWon
        var p1Action = state.player.action
        var p2Action = state.cpu.action
        val title: String
        val subtitle: String

        val p1Name = strings.fighterName(state.player.profile.id)
        val p2Name = if (state.cpu.bossType != BossType.NONE) {
            strings.bossName(state.cpu.bossType)
        } else {
            strings.fighterName(state.cpu.profile.id)
        }

        when {
            p1Pct > p2Pct -> {
                p1Wins++
                p1Action = FighterAction.VICTORY
                val isPerfect = p1Pct >= 0.99f
                title = if (isPerfect) strings.perfectTitle else strings.winnerFormat(p1Name)
                subtitle = if (timedOut) strings.timeUpSub else strings.greatRoundSub
                onSound(SoundEffect.VICTORY)
            }
            p2Pct > p1Pct -> {
                p2Wins++
                p2Action = FighterAction.VICTORY
                title = strings.winnerFormat(p2Name)
                subtitle = if (timedOut) strings.timeUpSub else strings.dontGiveUpSub
            }
            else -> {
                p1Wins++
                p2Wins++
                title = strings.drawTitle
                subtitle = strings.drawSub
            }
        }

        val updatedP1 = state.player.copy(roundsWon = p1Wins, action = p1Action, vx = 0f)
        val updatedP2 = state.cpu.copy(roundsWon = p2Wins, action = p2Action, vx = 0f)

        val matchFinished = p1Wins >= 2 || p2Wins >= 2
        return if (matchFinished) {
            val playerWon = p1Wins >= 2 && p1Wins >= p2Wins
            state.copy(
                player = updatedP1,
                cpu = updatedP2,
                allyStriker = null,
                phase = RoundPhase.MATCH_OVER,
                phaseTimer = 0f,
                bannerText = if (playerWon) strings.winBanner else strings.loseBanner,
                bannerSubtext = title,
                bossWarningText = "",
                playerWonMatch = playerWon
            )
        } else {
            state.copy(
                player = updatedP1,
                cpu = updatedP2,
                allyStriker = null,
                phase = RoundPhase.ROUND_OVER,
                phaseTimer = 1.7f,
                bannerText = title,
                bannerSubtext = subtitle,
                bossWarningText = ""
            )
        }
    }

    private fun startNextRound(
        state: MatchState,
        equippedPerk: CombatPerk?,
        strings: LocalizedStrings
    ): MatchState {
        clearAllState()
        val nextRound = state.roundNumber + 1
        val minPlayerEnergy = if (equippedPerk == CombatPerk.SURGE_BATTERY) 55f else 35f
        val cpuStartEnergy = if (state.activeModifier == StageModifier.OVERLORD_RAGE) 100f else max(35f, state.cpu.energy)
        return state.copy(
            roundNumber = nextRound,
            roundTimeRemaining = 60f,
            phase = RoundPhase.INTRO,
            phaseTimer = 1.3f,
            bannerText = "${strings.roundLabel} $nextRound",
            bannerSubtext = strings.readySub,
            bossWarningText = "",
            aiTacticLabel = "",
            allyStriker = null,
            allyCooldown = 4.5f,
            projectiles = emptyList(),
            particles = emptyList(),
            impactRings = emptyList(),
            player = state.player.copy(
                x = 270f,
                y = 0f,
                vx = 0f,
                vy = 0f,
                facingRight = true,
                hp = state.player.maxHp,
                trailHp = state.player.maxHp,
                energy = max(minPlayerEnergy, state.player.energy),
                action = FighterAction.IDLE,
                actionTimer = 0f,
                comboCount = 0
            ),
            cpu = state.cpu.copy(
                x = 730f,
                y = 0f,
                vx = 0f,
                vy = 0f,
                facingRight = false,
                hp = state.cpu.maxHp,
                trailHp = state.cpu.maxHp,
                energy = cpuStartEnergy,
                action = FighterAction.IDLE,
                actionTimer = 0f,
                comboCount = 0,
                bossPhase = 1,
                superArmorTimer = 0f,
                bossCooldown = 2.4f
            )
        )
    }

    fun advanceNextCampaignLevel(
        state: MatchState,
        equippedPerk: CombatPerk?,
        strings: LocalizedStrings
    ): MatchState {
        val currentNum = state.campaignLevel?.levelNumber ?: 1
        val nextLevel = LevelCatalog.byNumber((currentNum + 1).coerceAtMost(LevelCatalog.all.size))
        return createMatch(
            mode = GameMode.CAMPAIGN,
            playerProfile = state.player.profile,
            opponentProfile = null,
            arena = ArenaCatalog.byId(nextLevel.arenaId),
            difficulty = nextLevel.difficulty,
            campaignLevel = nextLevel,
            equippedPerk = equippedPerk,
            strings = strings
        ).copy(
            highestComboInMatch = state.highestComboInMatch,
            kosInMatch = state.kosInMatch
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
