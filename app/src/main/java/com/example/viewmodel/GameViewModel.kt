package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.PlayerStats
import com.example.data.PlayerStatsRepository
import com.example.engine.CombatEngine
import com.example.engine.PlayerInputState
import com.example.model.AiDifficulty
import com.example.model.ArenaCatalog
import com.example.model.ArenaStage
import com.example.model.BossType
import com.example.model.CampaignLevel
import com.example.model.CombatPerk
import com.example.model.FighterId
import com.example.model.FighterProfile
import com.example.model.FighterRoster
import com.example.model.GameMode
import com.example.model.LevelCatalog
import com.example.model.MatchState
import com.example.model.RoundPhase
import com.example.model.StageModifier
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.GameStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SelectionConfig(
    val mode: GameMode = GameMode.CAMPAIGN,
    val playerFighter: FighterProfile = FighterRoster.all[0],
    val opponentFighter: FighterProfile = FighterRoster.all[1],
    val arena: ArenaStage = ArenaCatalog.all[0],
    val difficulty: AiDifficulty = AiDifficulty.NORMAL,
    val selectedLevel: CampaignLevel = LevelCatalog.all.first()
)

data class CombatHudState(
    val mode: GameMode = GameMode.VERSUS,
    val difficulty: AiDifficulty = AiDifficulty.NORMAL,
    val arenaBackgroundRes: Int = 0,
    val playerName: String = "",
    val cpuName: String = "",
    val playerHp: Int = 100,
    val playerMaxHp: Int = 100,
    val playerRoundsWon: Int = 0,
    val playerCombo: Int = 0,
    val cpuHp: Int = 100,
    val cpuMaxHp: Int = 100,
    val cpuRoundsWon: Int = 0,
    val timeRemainingSec: Int = 60,
    val phase: RoundPhase = RoundPhase.INTRO,
    val bannerText: String = "",
    val bannerSubtext: String = "",
    val bossType: BossType = BossType.NONE,
    val bossPhase: Int = 1,
    val bossWarningText: String = "",
    val aiTacticLabel: String = "",
    val hasAllyActive: Boolean = false,
    val isPaused: Boolean = false,
    val playerWonMatch: Boolean? = null,
    val highestComboInMatch: Int = 0,
    val campaignLevelNumber: Int? = null,
    val activeModifier: StageModifier = StageModifier.STANDARD
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlayerStatsRepository(application.applicationContext)
    private val soundEngine = SoundEngine()
    private val combatEngine = CombatEngine()

    val playerStats: StateFlow<PlayerStats> = repository.statsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerStats()
    )

    private val _selection = MutableStateFlow(SelectionConfig())
    val selection: StateFlow<SelectionConfig> = _selection.asStateFlow()

    private val _hudState = MutableStateFlow<CombatHudState?>(null)
    val hudState: StateFlow<CombatHudState?> = _hudState.asStateFlow()

    private val _hasActiveMatch = MutableStateFlow(false)
    val hasActiveMatch: StateFlow<Boolean> = _hasActiveMatch.asStateFlow()

    private val _playerEnergyPct = MutableStateFlow(0)
    val playerEnergyPct: StateFlow<Int> = _playerEnergyPct.asStateFlow()

    private val _manualSaveToast = MutableStateFlow(false)
    val manualSaveToast: StateFlow<Boolean> = _manualSaveToast.asStateFlow()

    var renderMatchState by mutableStateOf<MatchState?>(null)
        private set

    private var currentInput = PlayerInputState()
    private var matchResultRecorded = false
    private var lastRoundSaved = 0
    private var autoSaveAccum = 0f
    private var initialPrefsLoaded = false
    private var userModifiedSelection = false

    private var cachedLang = AppLanguage.ENGLISH
    private var cachedStrings = GameStrings.english

    init {
        soundEngine.warmUp()
        viewModelScope.launch {
            playerStats.collect { stats ->
                soundEngine.isMuted = !stats.soundEnabled
                if (stats.language != cachedLang) {
                    cachedLang = stats.language
                    cachedStrings = GameStrings.forLanguage(stats.language)
                }
                if (!initialPrefsLoaded && stats.lastSavedEpochMs > 0L) {
                    initialPrefsLoaded = true
                    if (!userModifiedSelection) {
                        val fighter = FighterRoster.byId(stats.preferredFighter)
                        val opp = FighterRoster.all.first { it.id != fighter.id }
                        _selection.value = _selection.value.copy(
                            difficulty = stats.preferredDifficulty,
                            playerFighter = fighter,
                            opponentFighter = opp,
                            arena = ArenaCatalog.byId(stats.preferredArena),
                            selectedLevel = LevelCatalog.byNumber(stats.highestUnlockedLevel)
                        )
                    }
                }
            }
        }
    }

    private fun currentStrings() = cachedStrings

    override fun onCleared() {
        soundEngine.release()
        super.onCleared()
    }

    fun setGameMode(mode: GameMode) {
        userModifiedSelection = true
        _selection.value = _selection.value.copy(mode = mode)
    }

    fun selectCampaignLevel(level: CampaignLevel) {
        userModifiedSelection = true
        _selection.value = _selection.value.copy(
            mode = GameMode.CAMPAIGN,
            selectedLevel = level
        )
    }

    fun selectPlayerFighter(fighter: FighterProfile) {
        if (fighter.id !in playerStats.value.unlockedFighters) return
        userModifiedSelection = true
        val currentOpp = _selection.value.opponentFighter
        val newOpp = if (currentOpp.id == fighter.id) {
            FighterRoster.all.first { it.id != fighter.id }
        } else {
            currentOpp
        }
        _selection.value = _selection.value.copy(
            playerFighter = fighter,
            opponentFighter = newOpp
        )
        viewModelScope.launch {
            repository.setPreferredSelections(fighter.id, _selection.value.arena.id)
        }
    }

    fun selectOpponentFighter(fighter: FighterProfile) {
        userModifiedSelection = true
        _selection.value = _selection.value.copy(opponentFighter = fighter)
    }

    fun selectArena(arena: ArenaStage) {
        userModifiedSelection = true
        _selection.value = _selection.value.copy(arena = arena)
        viewModelScope.launch {
            repository.setPreferredSelections(_selection.value.playerFighter.id, arena.id)
        }
    }

    fun selectDifficulty(difficulty: AiDifficulty) {
        userModifiedSelection = true
        _selection.value = _selection.value.copy(difficulty = difficulty)
        viewModelScope.launch {
            repository.setPreferredDifficulty(difficulty)
        }
    }

    fun playNow() {
        if (resumeSavedMatch()) return
        val stats = playerStats.value
        val fighter = FighterRoster.byId(stats.preferredFighter)
        val level = LevelCatalog.byNumber(stats.highestUnlockedLevel)
        userModifiedSelection = true
        _selection.value = _selection.value.copy(
            mode = GameMode.CAMPAIGN,
            playerFighter = fighter,
            selectedLevel = level,
            difficulty = stats.preferredDifficulty
        )
        startMatch()
    }

    fun startMatch() {
        val cfg = _selection.value
        val strings = currentStrings()
        currentInput = PlayerInputState()
        matchResultRecorded = false
        lastRoundSaved = 1
        autoSaveAccum = 0f
        val initial = combatEngine.createMatch(
            mode = cfg.mode,
            playerProfile = cfg.playerFighter,
            opponentProfile = if (cfg.mode == GameMode.CAMPAIGN) null else cfg.opponentFighter,
            arena = cfg.arena,
            difficulty = cfg.difficulty,
            campaignLevel = if (cfg.mode == GameMode.CAMPAIGN) cfg.selectedLevel else null,
            equippedPerk = playerStats.value.equippedPerk,
            strings = strings
        )
        publishMatchState(initial)
        saveCheckpointNow()
    }

    fun resumeSavedMatch(): Boolean {
        val snap = playerStats.value.activeSnapshot ?: return false
        currentInput = PlayerInputState()
        matchResultRecorded = false
        lastRoundSaved = snap.roundNumber
        autoSaveAccum = 0f
        val restored = combatEngine.createMatchFromSnapshot(snap, currentStrings())
        publishMatchState(restored)
        return true
    }

    fun rematch() {
        val current = renderMatchState ?: return
        val strings = currentStrings()
        currentInput = PlayerInputState()
        matchResultRecorded = false
        lastRoundSaved = 1
        autoSaveAccum = 0f
        val restarted = combatEngine.createMatch(
            mode = current.mode,
            playerProfile = current.player.profile,
            opponentProfile = current.cpu.profile,
            arena = current.arena,
            difficulty = current.difficulty,
            campaignLevel = current.campaignLevel,
            equippedPerk = playerStats.value.equippedPerk,
            strings = strings
        )
        publishMatchState(restarted)
        saveCheckpointNow()
    }

    fun advanceNextLevel() {
        val current = renderMatchState ?: return
        currentInput = PlayerInputState()
        matchResultRecorded = false
        lastRoundSaved = 1
        autoSaveAccum = 0f
        val next = combatEngine.advanceNextCampaignLevel(
            state = current,
            equippedPerk = playerStats.value.equippedPerk,
            strings = currentStrings()
        )
        publishMatchState(next)
        saveCheckpointNow()
    }

    fun togglePause() {
        val current = renderMatchState ?: return
        val nextPaused = !current.isPaused
        publishMatchState(current.copy(isPaused = nextPaused))
        if (nextPaused) saveCheckpointNow()
    }

    fun setPaused(paused: Boolean) {
        val current = renderMatchState ?: return
        publishMatchState(current.copy(isPaused = paused))
        if (paused) saveCheckpointNow()
    }

    fun saveCheckpointNow() {
        val current = renderMatchState ?: return
        val snap = combatEngine.toSnapshot(current)
        viewModelScope.launch {
            repository.saveMatchSnapshot(snap)
        }
    }

    fun manualSaveProgress() {
        val snap = renderMatchState?.let { combatEngine.toSnapshot(it) }
        viewModelScope.launch {
            repository.triggerManualSave(snap)
            _manualSaveToast.value = true
            delay(2600)
            _manualSaveToast.value = false
        }
    }

    fun unlockFighter(fighterId: FighterId, cost: Int) {
        viewModelScope.launch {
            repository.unlockFighter(fighterId, cost)
        }
    }

    fun unlockOrEquipPerk(perk: CombatPerk) {
        viewModelScope.launch {
            val stats = playerStats.value
            if (perk in stats.unlockedPerks) {
                repository.toggleEquipPerk(perk)
            } else {
                repository.unlockAndEquipPerk(perk)
            }
        }
    }

    fun toggleSound() {
        val nextEnabled = !playerStats.value.soundEnabled
        soundEngine.isMuted = !nextEnabled
        viewModelScope.launch { repository.setSoundEnabled(nextEnabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticsEnabled(enabled) }
    }

    fun setLargeControls(enabled: Boolean) {
        viewModelScope.launch { repository.setLargeControls(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setMoveLeft(pressed: Boolean) {
        currentInput = currentInput.copy(moveLeft = pressed)
    }

    fun setMoveRight(pressed: Boolean) {
        currentInput = currentInput.copy(moveRight = pressed)
    }

    fun setGuard(pressed: Boolean) {
        currentInput = currentInput.copy(guardHeld = pressed)
    }

    fun triggerJump() {
        currentInput = currentInput.copy(jumpQueued = true)
    }

    fun triggerPunch() {
        currentInput = currentInput.copy(punchQueued = true)
    }

    fun triggerKick() {
        currentInput = currentInput.copy(kickQueued = true)
    }

    fun triggerSpecial() {
        currentInput = currentInput.copy(specialQueued = true)
    }

    fun triggerDash() {
        currentInput = currentInput.copy(dashQueued = true)
    }

    fun stepFrame(dt: Float, onHitImpact: (Boolean) -> Unit) {
        val current = renderMatchState ?: return
        val strings = currentStrings()
        val (nextState, nextInput) = combatEngine.step(
            state = current,
            dtRaw = dt,
            input = currentInput,
            equippedPerk = playerStats.value.equippedPerk,
            strings = strings,
            onSound = { effect -> soundEngine.play(effect) },
            onHitImpact = { heavy ->
                if (playerStats.value.hapticsEnabled) {
                    onHitImpact(heavy)
                }
            }
        )
        currentInput = nextInput
        publishMatchState(nextState)

        // Periodic + round-boundary continuous auto-save (10s interval keeps frame pacing silky)
        if (!nextState.isPaused && nextState.mode != GameMode.TRAINING && nextState.phase != RoundPhase.MATCH_OVER) {
            autoSaveAccum += dt
            if (nextState.roundNumber != lastRoundSaved || autoSaveAccum >= 10.0f) {
                lastRoundSaved = nextState.roundNumber
                autoSaveAccum = 0f
                saveCheckpointNow()
            }
        }

        if (nextState.phase == RoundPhase.MATCH_OVER && !matchResultRecorded && nextState.mode != GameMode.TRAINING) {
            matchResultRecorded = true
            val won = nextState.playerWonMatch == true
            val clearedLevel = if (won && nextState.mode == GameMode.CAMPAIGN) {
                nextState.campaignLevel?.levelNumber
            } else null
            val wasBoss = nextState.cpu.bossType != BossType.NONE
            val perfectRound = won && (nextState.player.hp >= nextState.player.maxHp * 0.99f)
            viewModelScope.launch {
                repository.recordMatchResult(
                    won = won,
                    kosEarned = nextState.kosInMatch,
                    maxCombo = nextState.highestComboInMatch,
                    perfectRoundWon = perfectRound,
                    difficulty = nextState.difficulty,
                    clearedLevelNumber = clearedLevel,
                    wasBossBattle = wasBoss
                )
            }
        }
    }

    private fun publishMatchState(state: MatchState) {
        renderMatchState = state
        if (!_hasActiveMatch.value) _hasActiveMatch.value = true
        val energyInt = state.player.energy.roundToInt().coerceIn(0, 100)
        if (_playerEnergyPct.value != energyInt) _playerEnergyPct.value = energyInt
        val strings = currentStrings()
        val cpuDisplayName = if (state.cpu.bossType != BossType.NONE) {
            strings.bossName(state.cpu.bossType)
        } else {
            strings.fighterName(state.cpu.profile.id)
        }
        val hud = CombatHudState(
            mode = state.mode,
            difficulty = state.difficulty,
            arenaBackgroundRes = state.arena.backgroundRes,
            playerName = strings.fighterName(state.player.profile.id),
            cpuName = cpuDisplayName,
            playerHp = state.player.hp.roundToInt().coerceAtLeast(0),
            playerMaxHp = state.player.maxHp.roundToInt(),
            playerRoundsWon = state.player.roundsWon,
            playerCombo = state.player.comboCount,
            cpuHp = state.cpu.hp.roundToInt().coerceAtLeast(0),
            cpuMaxHp = state.cpu.maxHp.roundToInt(),
            cpuRoundsWon = state.cpu.roundsWon,
            timeRemainingSec = state.roundTimeRemaining.toInt().coerceAtLeast(0),
            phase = state.phase,
            bannerText = state.bannerText,
            bannerSubtext = state.bannerSubtext,
            bossType = state.cpu.bossType,
            bossPhase = state.cpu.bossPhase,
            bossWarningText = state.bossWarningText,
            aiTacticLabel = state.aiTacticLabel,
            hasAllyActive = state.allyStriker?.active == true,
            isPaused = state.isPaused,
            playerWonMatch = state.playerWonMatch,
            highestComboInMatch = state.highestComboInMatch,
            campaignLevelNumber = state.campaignLevel?.levelNumber,
            activeModifier = state.activeModifier
        )
        if (_hudState.value != hud) {
            _hudState.value = hud
        }
    }
}
