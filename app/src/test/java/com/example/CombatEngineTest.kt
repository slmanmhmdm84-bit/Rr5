package com.example

import com.example.engine.AdaptiveAiBrain
import com.example.engine.CombatEngine
import com.example.engine.PlayerInputState
import com.example.model.AiDifficulty
import com.example.model.ArenaCatalog
import com.example.model.BossType
import com.example.model.CombatPerk
import com.example.model.FighterAction
import com.example.model.FighterRoster
import com.example.model.GameMode
import com.example.model.LevelCatalog
import com.example.model.RoundPhase
import com.example.model.StageModifier
import com.example.ui.i18n.GameStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatEngineTest {

    @Test
    fun punchDealsDamageAndBuildsEnergy() {
        val engine = CombatEngine()
        var state = engine.createMatch(
            mode = GameMode.TRAINING,
            playerProfile = FighterRoster.all[0],
            opponentProfile = FighterRoster.all[1],
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.EASY
        ).copy(
            phase = RoundPhase.FIGHTING,
            phaseTimer = 0f
        )
        state = state.copy(
            player = state.player.copy(x = 450f, energy = 20f),
            cpu = state.cpu.copy(x = 520f)
        )

        var input = PlayerInputState(punchQueued = true)
        repeat(12) {
            val res = engine.step(state, 0.016f, input, onSound = {}, onHitImpact = {})
            state = res.first
            input = res.second
        }

        assertTrue("CPU should take damage from close punch", state.cpu.hp < 100f)
        assertTrue("Player should gain energy on hit", state.player.energy > 20f)
        assertTrue("Impact ring should spawn on melee hit", state.impactRings.isNotEmpty() || state.particles.isNotEmpty())
    }

    @Test
    fun specialSpawnsProjectileWhenEnergySufficient() {
        val engine = CombatEngine()
        var state = engine.createMatch(
            mode = GameMode.VERSUS,
            playerProfile = FighterRoster.all[0],
            opponentProfile = FighterRoster.all[1],
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.NORMAL
        ).copy(
            phase = RoundPhase.FIGHTING,
            phaseTimer = 0f
        )
        state = state.copy(
            player = state.player.copy(energy = 60f)
        )

        var input = PlayerInputState(specialQueued = true)
        repeat(15) {
            val res = engine.step(state, 0.016f, input, onSound = {}, onHitImpact = {})
            state = res.first
            input = res.second
        }

        assertTrue("Special attack should spend 50 energy", state.player.energy < 20f)
        assertTrue("Special attack should spawn projectile", state.projectiles.isNotEmpty())
    }

    @Test
    fun blockingReducesIncomingDamageAndPreventsHitstun() {
        val engine = CombatEngine()
        var state = engine.createMatch(
            mode = GameMode.VERSUS,
            playerProfile = FighterRoster.all[0],
            opponentProfile = FighterRoster.all[1],
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.NORMAL
        ).copy(
            phase = RoundPhase.FIGHTING,
            phaseTimer = 0f
        )
        state = state.copy(
            player = state.player.copy(x = 450f, facingRight = true),
            cpu = state.cpu.copy(
                x = 515f,
                facingRight = false,
                action = FighterAction.LIGHT_PUNCH,
                actionTimer = 0.08f,
                actionDuration = 0.26f,
                hitRegistered = false
            )
        )
        val initialPlayerHp = state.player.hp

        val res = engine.step(
            state,
            0.016f,
            PlayerInputState(guardHeld = true),
            onSound = {},
            onHitImpact = {}
        )
        state = res.first

        val damageTaken = initialPlayerHp - state.player.hp
        assertTrue("Blocked punch should deal minimal chip damage ($damageTaken)", damageTaken in 0.5f..3.0f)
        assertEquals(FighterAction.GUARD, state.player.action)
    }

    @Test
    fun perfectParryNegatesDamageAndAwardsBonusEnergy() {
        val engine = CombatEngine()
        var state = engine.createMatch(
            mode = GameMode.VERSUS,
            playerProfile = FighterRoster.all[0],
            opponentProfile = FighterRoster.all[1],
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.NORMAL
        ).copy(
            phase = RoundPhase.FIGHTING,
            phaseTimer = 0f
        )
        state = state.copy(
            player = state.player.copy(
                x = 450f,
                facingRight = true,
                energy = 20f,
                action = FighterAction.GUARD,
                guardFreshTimer = 0.06f
            ),
            cpu = state.cpu.copy(
                x = 515f,
                facingRight = false,
                action = FighterAction.HEAVY_KICK,
                actionTimer = 0.12f,
                actionDuration = 0.35f,
                hitRegistered = false
            )
        )
        val initialPlayerHp = state.player.hp

        val res = engine.step(
            state,
            0.016f,
            PlayerInputState(guardHeld = true),
            onSound = {},
            onHitImpact = {}
        )
        state = res.first

        assertEquals(initialPlayerHp, state.player.hp, 0.01f)
        assertTrue("Parry should award bonus energy", state.player.energy >= 35f)
        assertTrue("Parry callout should spawn", state.callouts.any { it.text == GameStrings.english.parryCallout })
    }

    @Test
    fun zeroHpEndsRoundAndAwardsPoint() {
        val engine = CombatEngine()
        var state = engine.createMatch(
            mode = GameMode.VERSUS,
            playerProfile = FighterRoster.all[0],
            opponentProfile = FighterRoster.all[1],
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.EASY
        ).copy(
            phase = RoundPhase.FIGHTING,
            phaseTimer = 0f
        )
        state = state.copy(
            player = state.player.copy(x = 450f, facingRight = true),
            cpu = state.cpu.copy(x = 515f, facingRight = false, hp = 1f, trailHp = 1f)
        )

        var input = PlayerInputState(kickQueued = true)
        repeat(15) {
            val res = engine.step(state, 0.016f, input, onSound = {}, onHitImpact = {})
            state = res.first
            input = res.second
        }
        assertEquals(0f, state.cpu.hp, 0.01f)
        assertEquals(RoundPhase.KO_SLOWMO, state.phase)

        repeat(90) {
            val res = engine.step(state, 0.02f, PlayerInputState(), onSound = {}, onHitImpact = {})
            state = res.first
        }
        assertEquals(1, state.player.roundsWon)
        assertTrue(state.phase == RoundPhase.ROUND_OVER || state.phase == RoundPhase.INTRO)
    }

    @Test
    fun campaignLevelsConfigureBossesModifiersAndPerks() {
        val engine = CombatEngine()

        val level3 = LevelCatalog.byNumber(3)
        val match3 = engine.createMatch(
            mode = GameMode.CAMPAIGN,
            playerProfile = FighterRoster.all[0],
            opponentProfile = null,
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.EASY,
            campaignLevel = level3,
            equippedPerk = CombatPerk.SURGE_BATTERY
        )
        assertEquals(BossType.IRON_COLOSSUS, match3.cpu.bossType)
        assertEquals(StageModifier.ARMORED_BOSS, match3.activeModifier)
        assertEquals(135f, match3.cpu.maxHp, 0.01f)
        assertEquals(55f, match3.player.energy, 0.01f)

        val level8 = LevelCatalog.byNumber(8)
        val match8 = engine.createMatch(
            mode = GameMode.CAMPAIGN,
            playerProfile = FighterRoster.all[0],
            opponentProfile = null,
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.EASY,
            campaignLevel = level8
        )
        assertEquals(BossType.OMEGA_SHOGUN, match8.cpu.bossType)
        assertEquals(AiDifficulty.EXTREME, match8.difficulty)
        assertEquals(StageModifier.OVERLORD_RAGE, match8.activeModifier)
        assertEquals(165f, match8.cpu.maxHp, 0.01f)
        assertEquals(100f, match8.cpu.energy, 0.01f)
    }

    @Test
    fun bossEntersPhaseTwoBelowHalfHp() {
        val engine = CombatEngine()
        var state = engine.createMatch(
            mode = GameMode.CAMPAIGN,
            playerProfile = FighterRoster.all[0],
            opponentProfile = null,
            arena = ArenaCatalog.all[2],
            difficulty = AiDifficulty.NORMAL,
            campaignLevel = LevelCatalog.byNumber(3)
        ).copy(
            phase = RoundPhase.FIGHTING,
            phaseTimer = 0f
        )
        state = state.copy(
            cpu = state.cpu.copy(hp = state.cpu.maxHp * 0.45f)
        )

        val res = engine.step(state, 0.016f, PlayerInputState(), onSound = {}, onHitImpact = {})
        assertEquals(2, res.first.cpu.bossPhase)
    }

    @Test
    fun adaptiveAiLearnsPlayerHabitsAndEngages() {
        val brain = AdaptiveAiBrain()
        val engine = CombatEngine()
        val state = engine.createMatch(
            mode = GameMode.VERSUS,
            playerProfile = FighterRoster.all[0],
            opponentProfile = FighterRoster.all[1],
            arena = ArenaCatalog.all[0],
            difficulty = AiDifficulty.EXTREME
        )
        val jumpingPlayer = state.player.copy(action = FighterAction.JUMP)
        brain.observePlayer(jumpingPlayer, 150f, 0.016f)
        assertTrue("Adaptive AI should register jump habit", brain.jumpHabit > 0.2f)

        val guardPlayer = state.player.copy(action = FighterAction.GUARD)
        repeat(20) {
            brain.observePlayer(guardPlayer, 120f, 0.05f)
        }
        assertTrue("Adaptive AI should detect turtling guard habit", brain.guardHabit > 0.5f)
    }

    @Test
    fun matchSnapshotSavesAndRestoresExactRoundAndHp() {
        val engine = CombatEngine()
        val original = engine.createMatch(
            mode = GameMode.CAMPAIGN,
            playerProfile = FighterRoster.all[0],
            opponentProfile = null,
            arena = ArenaCatalog.all[1],
            difficulty = AiDifficulty.HARD,
            campaignLevel = LevelCatalog.byNumber(5)
        ).copy(
            roundNumber = 2,
            roundTimeRemaining = 38.5f,
            phase = RoundPhase.FIGHTING
        ).let { m ->
            m.copy(
                player = m.player.copy(hp = 72f, energy = 80f, roundsWon = 1),
                cpu = m.cpu.copy(hp = 44f, roundsWon = 1)
            )
        }

        val snapshot = engine.toSnapshot(original)
        assertNotNull(snapshot)

        val restored = engine.createMatchFromSnapshot(snapshot!!, GameStrings.english)
        assertEquals(2, restored.roundNumber)
        assertEquals(38.5f, restored.roundTimeRemaining, 0.01f)
        assertEquals(72f, restored.player.hp, 0.01f)
        assertEquals(80f, restored.player.energy, 0.01f)
        assertEquals(1, restored.player.roundsWon)
        assertEquals(44f, restored.cpu.hp, 0.01f)
        assertEquals(1, restored.cpu.roundsWon)
        assertEquals(5, restored.campaignLevel?.levelNumber)
    }
}
