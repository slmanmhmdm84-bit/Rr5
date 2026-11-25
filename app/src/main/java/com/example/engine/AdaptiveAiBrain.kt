package com.example.engine

import com.example.model.AiDifficulty
import com.example.model.BossType
import com.example.model.FighterAction
import com.example.model.FighterState
import com.example.model.FighterStyle
import com.example.model.GameMode
import com.example.model.Projectile
import com.example.ui.i18n.LocalizedStrings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Tracks the player's real-time combat habits and shifts AI tactical weights
 * so opponents avoid repetitive loops and actively counter predictable tactics.
 */
class AdaptiveAiBrain {
    private val idleInput = PlayerInputState()
    private var decisionTimer = 0.12f
    private var desiredLeft = false
    private var desiredRight = false
    private var desiredGuard = false
    private var guardTimer = 0f
    private var comboStep = 0
    private var cornerEscapeCd = 0f
    private var evadeCooldown = 0f

    // Exponentially weighted player habit trackers (0f..1f)
    var jumpHabit = 0.1f
        private set
    var projectileHabit = 0.1f
        private set
    var guardHabit = 0.15f
        private set
    var rushHabit = 0.25f
        private set

    private var lastObservedAction = FighterAction.IDLE
    var activeTacticHint: String = ""
        private set

    fun reset() {
        decisionTimer = 0.12f
        desiredLeft = false
        desiredRight = false
        desiredGuard = false
        guardTimer = 0f
        comboStep = 0
        cornerEscapeCd = 0f
        evadeCooldown = 0f
        jumpHabit = 0.1f
        projectileHabit = 0.1f
        guardHabit = 0.15f
        rushHabit = 0.25f
        lastObservedAction = FighterAction.IDLE
        activeTacticHint = ""
    }

    fun observePlayer(player: FighterState, dist: Float, dt: Float) {
        val decay = max(0f, 1f - dt * 0.14f)
        jumpHabit *= decay
        projectileHabit *= decay
        guardHabit *= decay
        rushHabit *= decay

        if (player.action != lastObservedAction) {
            lastObservedAction = player.action
            when (player.action) {
                FighterAction.JUMP -> jumpHabit = min(1f, jumpHabit + 0.28f)
                FighterAction.SPECIAL_ATTACK -> projectileHabit = min(1f, projectileHabit + 0.34f)
                FighterAction.GUARD -> guardHabit = min(1f, guardHabit + 0.26f)
                FighterAction.DASH, FighterAction.LIGHT_PUNCH -> {
                    if (dist < 220f) rushHabit = min(1f, rushHabit + 0.20f)
                }
                else -> {}
            }
        }
        if (player.action == FighterAction.GUARD) {
            guardHabit = min(1f, guardHabit + dt * 0.22f)
        }
    }

    data class AiDecision(
        val input: PlayerInputState,
        val bossActionQueued: FighterAction? = null,
        val shouldSummonAlly: Boolean = false,
        val tacticLabel: String = ""
    )

    fun decide(
        ai: FighterState,
        player: FighterState,
        difficulty: AiDifficulty,
        aggression: Float,
        mode: GameMode,
        allyReady: Boolean,
        dt: Float,
        projectiles: List<Projectile>,
        strings: LocalizedStrings
    ): AiDecision {
        if (mode == GameMode.TRAINING) {
            activeTacticHint = ""
            return AiDecision(idleInput)
        }
        if (ai.action == FighterAction.HITSTUN ||
            ai.action == FighterAction.KNOCKDOWN ||
            ai.action == FighterAction.VICTORY ||
            ai.action == FighterAction.BOSS_SLAM ||
            ai.action == FighterAction.BOSS_TELEPORT ||
            ai.action == FighterAction.BOSS_BARRAGE ||
            ai.action == FighterAction.BOSS_SLASH
        ) {
            comboStep = 0
            return AiDecision(idleInput, tacticLabel = activeTacticHint)
        }

        val dist = abs(player.x - ai.x)
        observePlayer(player, dist, dt)

        decisionTimer -= dt
        guardTimer = max(0f, guardTimer - dt)
        cornerEscapeCd = max(0f, cornerEscapeCd - dt)
        evadeCooldown = max(0f, evadeCooldown - dt)

        val adaptationFactor = when (difficulty) {
            AiDifficulty.EASY -> 0.05f
            AiDifficulty.NORMAL -> 0.35f
            AiDifficulty.HARD -> 0.72f
            AiDifficulty.EXTREME -> 1.0f
        }

        val playerThreatening = (player.action == FighterAction.LIGHT_PUNCH ||
            player.action == FighterAction.HEAVY_KICK ||
            player.action == FighterAction.SPECIAL_ATTACK) && !player.hitRegistered

        val playerRecoveringWhiff = (player.action == FighterAction.HEAVY_KICK ||
            player.action == FighterAction.SPECIAL_ATTACK) &&
            !player.hitRegistered && player.actionTimer > player.actionDuration * 0.52f

        var incomingProj: Projectile? = null
        if (decisionTimer <= 0f && projectiles.isNotEmpty()) {
            for (i in projectiles.indices) {
                val p = projectiles[i]
                if (p.ownerIsPlayer && abs(p.x - ai.x) < 250f &&
                    ((p.vx > 0 && p.x < ai.x) || (p.vx < 0 && p.x > ai.x))
                ) {
                    incomingProj = p
                    break
                }
            }
        }
        val inCorner = ai.x < CombatConstants.STAGE_MIN_X + 82f || ai.x > CombatConstants.STAGE_MAX_X - 82f

        var queuePunch = false
        var queueKick = false
        var queueSpecial = false
        var queueJump = false
        var queueDash = false
        var queuedBossAction: FighterAction? = null
        var summonAlly = false

        // 1. Real-time Hit-Confirm Combo Chains
        if (ai.action == FighterAction.LIGHT_PUNCH && ai.hitRegistered && ai.actionTimer >= 0.07f && comboStep == 1) {
            val comboChance = when (difficulty) {
                AiDifficulty.EASY -> 0.14f
                AiDifficulty.NORMAL -> 0.58f
                AiDifficulty.HARD -> 0.86f
                AiDifficulty.EXTREME -> 0.96f
            }
            if (Random.nextFloat() < comboChance) {
                queueKick = true
                comboStep = 2
            } else {
                comboStep = 0
            }
        } else if (ai.action == FighterAction.HEAVY_KICK && ai.hitRegistered && ai.actionTimer >= 0.09f && comboStep == 2 && ai.energy >= CombatConstants.SPECIAL_COST) {
            val finisherChance = when (difficulty) {
                AiDifficulty.EASY -> 0.08f
                AiDifficulty.NORMAL -> 0.44f
                AiDifficulty.HARD -> 0.78f
                AiDifficulty.EXTREME -> 0.94f
            }
            if (Random.nextFloat() < finisherChance) {
                queueSpecial = true
            }
            comboStep = 0
        }

        // 2. Coordinated Ally Pincer Check (works even between attack decisions)
        if (allyReady && difficulty != AiDifficulty.EASY) {
            val allyTrigger = (player.action == FighterAction.GUARD && dist < 210f) ||
                (guardHabit > 0.45f) ||
                (ai.hp < ai.maxHp * 0.45f && dist > 180f) ||
                (ai.bossPhase >= 2 && Random.nextFloat() < 0.03f)
            if (allyTrigger) {
                summonAlly = true
                if (difficulty == AiDifficulty.HARD || difficulty == AiDifficulty.EXTREME) {
                    activeTacticHint = strings.aiTacticFlankPinch
                }
            }
        }

        // 3. Tactical Decision Cadence
        if (decisionTimer <= 0f) {
            val baseInterval = when (difficulty) {
                AiDifficulty.EASY -> 0.36f
                AiDifficulty.NORMAL -> 0.19f
                AiDifficulty.HARD -> 0.095f
                AiDifficulty.EXTREME -> 0.052f
            }
            decisionTimer = baseInterval + Random.nextFloat() * (if (difficulty == AiDifficulty.EXTREME) 0.025f else 0.07f)

            val guardSkill = when (difficulty) {
                AiDifficulty.EASY -> 0.18f
                AiDifficulty.NORMAL -> 0.48f
                AiDifficulty.HARD -> 0.76f
                AiDifficulty.EXTREME -> 0.90f
            }
            val dodgeSkill = when (difficulty) {
                AiDifficulty.EASY -> 0.10f
                AiDifficulty.NORMAL -> 0.34f
                AiDifficulty.HARD -> 0.64f
                AiDifficulty.EXTREME -> 0.85f
            }

            // Update Adaptive Hint for Hard/Extreme
            if (difficulty == AiDifficulty.HARD || difficulty == AiDifficulty.EXTREME) {
                activeTacticHint = when {
                    summonAlly -> strings.aiTacticFlankPinch
                    jumpHabit > 0.48f -> strings.aiTacticAntiAir
                    projectileHabit > 0.45f -> strings.aiTacticCounterZone
                    guardHabit > 0.50f -> strings.aiTacticGuardBreak
                    rushHabit > 0.52f -> strings.aiTacticWhiffPunish
                    else -> ""
                }
            } else {
                activeTacticHint = ""
            }

            // Priority A: Boss Signature Abilities when ready
            if (ai.bossType != BossType.NONE && ai.bossCooldown <= 0f && ai.y <= 2f) {
                val bossRoll = Random.nextFloat()
                val bossTriggerRate = (if (ai.bossPhase >= 2) 0.72f else 0.52f) * aggression
                if (bossRoll < bossTriggerRate) {
                    queuedBossAction = when (ai.bossType) {
                        BossType.IRON_COLOSSUS -> {
                            if (dist in 95f..420f) FighterAction.BOSS_SLAM else null
                        }
                        BossType.PHANTOM_EMPRESS -> {
                            if (dist > 140f || guardHabit > 0.4f) FighterAction.BOSS_TELEPORT else null
                        }
                        BossType.OMEGA_SHOGUN -> {
                            when {
                                dist > 230f -> FighterAction.BOSS_BARRAGE
                                dist in 100f..260f -> FighterAction.BOSS_SLASH
                                else -> FighterAction.BOSS_BARRAGE
                            }
                        }
                        BossType.NONE -> null
                    }
                    if (queuedBossAction != null) {
                        desiredLeft = false
                        desiredRight = false
                        desiredGuard = false
                        return AiDecision(
                            input = PlayerInputState(),
                            bossActionQueued = queuedBossAction,
                            shouldSummonAlly = summonAlly,
                            tacticLabel = activeTacticHint
                        )
                    }
                }
            }

            // Priority B: Incoming Projectile Evasion / Counter-Approach
            if (incomingProj != null) {
                val counterProj = dodgeSkill + projectileHabit * 0.25f * adaptationFactor
                if (ai.y <= 2f && evadeCooldown <= 0f && Random.nextFloat() < counterProj) {
                    if (ai.dashCooldown <= 0f && dist < 340f && Random.nextFloat() < 0.52f) {
                        val towardPlayer = player.x > ai.x
                        desiredRight = towardPlayer
                        desiredLeft = !towardPlayer
                        queueDash = true
                    } else {
                        queueJump = true
                        desiredRight = player.x > ai.x
                        desiredLeft = player.x < ai.x
                    }
                    desiredGuard = false
                    evadeCooldown = 0.65f
                } else if (Random.nextFloat() < guardSkill) {
                    desiredGuard = true
                    guardTimer = 0.25f
                    desiredLeft = false
                    desiredRight = false
                }
            }
            // Priority C: Threatening Close Attack -> Smart Block or Backdash Evade
            else if (playerThreatening && dist < 170f) {
                val evasiveBackdash = ai.y <= 2f && evadeCooldown <= 0f && ai.dashCooldown <= 0f &&
                    Random.nextFloat() < (dodgeSkill * 0.45f + rushHabit * 0.25f * adaptationFactor)
                if (evasiveBackdash) {
                    val awayRight = player.x < ai.x
                    desiredRight = awayRight
                    desiredLeft = !awayRight
                    queueDash = true
                    desiredGuard = false
                    evadeCooldown = 0.75f
                } else if (Random.nextFloat() < guardSkill) {
                    desiredGuard = true
                    guardTimer = if (difficulty == AiDifficulty.EXTREME) 0.19f else 0.24f
                    desiredLeft = false
                    desiredRight = false
                }
            } else if (guardTimer <= 0f) {
                desiredGuard = false
            }

            if (!desiredGuard && !queueJump && !queueDash) {
                // Priority D: Corner Escape (Jump cross-up or Dash out)
                if (inCorner && dist < 215f && cornerEscapeCd <= 0f && Random.nextFloat() < 0.72f * aggression) {
                    queueJump = true
                    val towardCenterRight = ai.x < 500f
                    desiredRight = towardCenterRight
                    desiredLeft = !towardCenterRight
                    cornerEscapeCd = 1.2f
                }
                // Priority E: Adaptive Anti-Air (boosted when player spams jumps)
                else if (player.y > 40f && dist < 195f &&
                    Random.nextFloat() < (0.65f + jumpHabit * 0.35f * adaptationFactor) * aggression
                ) {
                    queueKick = true
                }
                // Priority F: Whiff Punish player's recovery frames
                else if (playerRecoveringWhiff && dist < 215f &&
                    Random.nextFloat() < (0.78f + rushHabit * 0.20f * adaptationFactor) * aggression
                ) {
                    if (dist > 132f && ai.dashCooldown <= 0f) {
                        val towardPlayer = player.x > ai.x
                        desiredRight = towardPlayer
                        desiredLeft = !towardPlayer
                        queueDash = true
                    } else if (dist < 118f) {
                        queuePunch = true
                        comboStep = 1
                    } else {
                        queueKick = true
                    }
                }
                // Priority G: Adaptive Guard-Break Crossup if player turtles excessively
                else if (guardHabit > 0.45f && dist < 170f && difficulty != AiDifficulty.EASY &&
                    Random.nextFloat() < 0.60f * adaptationFactor
                ) {
                    if (ai.y <= 2f && dist < 125f && Random.nextFloat() < 0.55f) {
                        // Cross-up jump behind turtle guard
                        queueJump = true
                        desiredRight = player.x > ai.x
                        desiredLeft = player.x < ai.x
                    } else if (ai.energy >= CombatConstants.SPECIAL_COST) {
                        queueSpecial = true
                    } else {
                        queueKick = true
                    }
                }
                // Priority H: Optimal Range Positioning by Fighter Style
                else {
                    val optimalMin = when (ai.profile.style) {
                        FighterStyle.HEAVY_TITAN -> 75f
                        FighterStyle.MARTIAL_ARTIST -> 95f
                        FighterStyle.CYBER_RONIN -> 110f
                        FighterStyle.SHADOW_ASSASSIN -> 115f
                    }
                    val towardRight = player.x > ai.x
                    when {
                        dist > 280f -> {
                            if (ai.energy >= CombatConstants.SPECIAL_COST &&
                                Random.nextFloat() < 0.42f * aggression &&
                                projectileHabit < 0.7f
                            ) {
                                queueSpecial = true
                                desiredLeft = false
                                desiredRight = false
                            } else if (ai.dashCooldown <= 0f &&
                                Random.nextFloat() < (0.35f + projectileHabit * 0.30f * adaptationFactor) * aggression
                            ) {
                                desiredRight = towardRight
                                desiredLeft = !towardRight
                                queueDash = true
                            } else {
                                desiredRight = towardRight
                                desiredLeft = !towardRight
                            }
                        }
                        dist in optimalMin..280f -> {
                            val roll = Random.nextFloat()
                            when {
                                roll < 0.34f * aggression && ai.energy >= CombatConstants.SPECIAL_COST -> {
                                    queueSpecial = true
                                    desiredLeft = false
                                    desiredRight = false
                                }
                                roll < 0.65f * aggression -> {
                                    queueKick = true
                                }
                                roll < 0.87f -> {
                                    desiredRight = towardRight
                                    desiredLeft = !towardRight
                                }
                                else -> {
                                    // Footsies shimmy step back to bait whiff
                                    desiredRight = !towardRight
                                    desiredLeft = towardRight
                                }
                            }
                        }
                        else -> {
                            desiredLeft = false
                            desiredRight = false
                            val roll = Random.nextFloat()
                            when {
                                roll < 0.60f * aggression -> {
                                    queuePunch = true
                                    comboStep = 1
                                }
                                roll < 0.86f * aggression -> {
                                    queueKick = true
                                }
                                else -> {
                                    // Mixup backstep or cross-up dash
                                    val backRight = player.x < ai.x
                                    desiredRight = backRight
                                    desiredLeft = !backRight
                                    if ((difficulty == AiDifficulty.HARD || difficulty == AiDifficulty.EXTREME) &&
                                        ai.dashCooldown <= 0f && Random.nextFloat() < 0.45f
                                    ) {
                                        queueDash = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return AiDecision(
            input = PlayerInputState(
                moveLeft = desiredLeft,
                moveRight = desiredRight,
                guardHeld = desiredGuard,
                jumpQueued = queueJump,
                punchQueued = queuePunch,
                kickQueued = queueKick,
                specialQueued = queueSpecial,
                dashQueued = queueDash
            ),
            bossActionQueued = queuedBossAction,
            shouldSummonAlly = summonAlly,
            tacticLabel = activeTacticHint
        )
    }
}
