package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AchievementId
import com.example.model.AiDifficulty
import com.example.model.ArenaId
import com.example.model.CombatPerk
import com.example.model.FighterId
import com.example.model.FighterRoster
import com.example.model.GameMode
import com.example.model.LevelCatalog
import com.example.model.MissionId
import com.example.model.SavedMatchSnapshot
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "arena_champions_save_v2")

data class PlayerStats(
    val totalWins: Int = 0,
    val totalKos: Int = 0,
    val bestCombo: Int = 0,
    val honorPoints: Int = 0,
    val levelsCleared: Int = 0,
    val highestUnlockedLevel: Int = 1,
    val clearedStages: Set<Int> = emptySet(),
    val unlockedFighters: Set<FighterId> = setOf(FighterId.SAQR, FighterId.SAIF),
    val unlockedPerks: Set<CombatPerk> = emptySet(),
    val equippedPerk: CombatPerk? = null,
    val unlockedAchievements: Set<AchievementId> = emptySet(),
    val missionProgress: Map<MissionId, Int> = emptyMap(),
    val completedMissions: Set<MissionId> = emptySet(),
    val preferredDifficulty: AiDifficulty = AiDifficulty.NORMAL,
    val preferredFighter: FighterId = FighterId.SAQR,
    val preferredArena: ArenaId = ArenaId.NEON_DOJO,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val largeControls: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val lastSavedEpochMs: Long = 0L,
    val integrityVerified: Boolean = true,
    val recoveredFromBackup: Boolean = false,
    val activeSnapshot: SavedMatchSnapshot? = null
)

class PlayerStatsRepository(private val context: Context) {
    private object Keys {
        val SAVE_PAYLOAD = stringPreferencesKey("save_payload_json")
        val SAVE_CHECKSUM = stringPreferencesKey("save_payload_sha256")
        val BACKUP_PAYLOAD = stringPreferencesKey("backup_payload_json")
        val BACKUP_CHECKSUM = stringPreferencesKey("backup_payload_sha256")
    }

    val statsFlow: Flow<PlayerStats> = context.dataStore.data.map { prefs ->
        decodeVerified(prefs)
    }

    private fun decodeVerified(prefs: Preferences): PlayerStats {
        val primaryJson = prefs[Keys.SAVE_PAYLOAD]
        val primaryHash = prefs[Keys.SAVE_CHECKSUM]
        if (!primaryJson.isNullOrBlank() && !primaryHash.isNullOrBlank() && computeSha256(primaryJson) == primaryHash) {
            parseJson(primaryJson)?.let { return it.copy(integrityVerified = true, recoveredFromBackup = false) }
        }

        val backupJson = prefs[Keys.BACKUP_PAYLOAD]
        val backupHash = prefs[Keys.BACKUP_CHECKSUM]
        if (!backupJson.isNullOrBlank() && !backupHash.isNullOrBlank() && computeSha256(backupJson) == backupHash) {
            parseJson(backupJson)?.let { return it.copy(integrityVerified = true, recoveredFromBackup = true) }
        }

        return PlayerStats()
    }

    private suspend fun updateAndCommit(transform: (PlayerStats) -> PlayerStats): PlayerStats {
        var result = PlayerStats()
        context.dataStore.edit { prefs ->
            val current = decodeVerified(prefs)
            val prevPrimary = prefs[Keys.SAVE_PAYLOAD]
            val prevHash = prefs[Keys.SAVE_CHECKSUM]
            if (!prevPrimary.isNullOrBlank() && !prevHash.isNullOrBlank() && computeSha256(prevPrimary) == prevHash) {
                prefs[Keys.BACKUP_PAYLOAD] = prevPrimary
                prefs[Keys.BACKUP_CHECKSUM] = prevHash
            }
            val updated = transform(current).copy(
                lastSavedEpochMs = System.currentTimeMillis(),
                integrityVerified = true,
                recoveredFromBackup = false
            )
            val encoded = serializeJson(updated)
            val hash = computeSha256(encoded)
            prefs[Keys.SAVE_PAYLOAD] = encoded
            prefs[Keys.SAVE_CHECKSUM] = hash
            result = updated
        }
        return result
    }

    suspend fun recordMatchResult(
        won: Boolean,
        kosEarned: Int,
        maxCombo: Int,
        perfectRoundWon: Boolean,
        difficulty: AiDifficulty,
        clearedLevelNumber: Int?,
        wasBossBattle: Boolean
    ) {
        updateAndCommit { current ->
            var wins = current.totalWins + if (won) 1 else 0
            val kos = current.totalKos + kosEarned.coerceAtLeast(0)
            val bestCombo = maxOf(current.bestCombo, maxCombo)
            var honor = current.honorPoints + if (won) 45 else 15
            honor += kosEarned * 10

            val clearedStages = current.clearedStages.toMutableSet()
            var highestUnlocked = current.highestUnlockedLevel
            var unlockedFighters = current.unlockedFighters.toMutableSet()

            if (won && clearedLevelNumber != null) {
                val firstClear = clearedStages.add(clearedLevelNumber)
                val level = LevelCatalog.byNumber(clearedLevelNumber)
                honor += if (firstClear) level.rewardHonor else (level.rewardHonor / 3)
                val nextStage = (clearedLevelNumber + 1).coerceAtMost(LevelCatalog.all.size)
                if (nextStage > highestUnlocked) {
                    highestUnlocked = nextStage
                }
                if (clearedLevelNumber == 3) unlockedFighters.add(FighterId.JABBAR)
                if (clearedLevelNumber == 6) unlockedFighters.add(FighterId.LAYLA)
            }

            // Update Mission Progress
            val progressMap = current.missionProgress.toMutableMap()
            val completedMissions = current.completedMissions.toMutableSet()
            fun bumpMission(id: MissionId, delta: Int) {
                if (delta <= 0 || id in completedMissions) return
                val next = ((progressMap[id] ?: 0) + delta).coerceAtMost(id.target)
                progressMap[id] = next
                if (next >= id.target && completedMissions.add(id)) {
                    honor += id.rewardHonor
                }
            }

            if (won) bumpMission(MissionId.WIN_MATCHES, 1)
            if (maxCombo >= 3) bumpMission(MissionId.LAND_COMBOS, 1)
            if (won && wasBossBattle) bumpMission(MissionId.DEFEAT_BOSSES, 1)
            if (won && (difficulty == AiDifficulty.HARD || difficulty == AiDifficulty.EXTREME)) {
                bumpMission(MissionId.WIN_EXTREME_OR_HARD, 1)
            }

            // Check Achievements
            val achievements = current.unlockedAchievements.toMutableSet()
            fun unlockAch(id: AchievementId, condition: Boolean) {
                if (condition && achievements.add(id)) {
                    honor += id.rewardHonor
                }
            }
            unlockAch(AchievementId.FIRST_VICTORY, won)
            unlockAch(AchievementId.COMBO_SAVANT, maxCombo >= 5)
            unlockAch(AchievementId.PERFECT_ROUND, perfectRoundWon)
            unlockAch(AchievementId.COLOSSUS_SLAYER, won && clearedLevelNumber == 3)
            unlockAch(AchievementId.PHANTOM_BREAKER, won && clearedLevelNumber == 6)
            unlockAch(AchievementId.APEX_CHAMPION, won && clearedLevelNumber == 8)
            unlockAch(AchievementId.EXTREME_SURVIVOR, won && difficulty == AiDifficulty.EXTREME)
            unlockAch(AchievementId.ARSENAL_COLLECTOR, unlockedFighters.size >= FighterRoster.all.size)

            current.copy(
                totalWins = wins,
                totalKos = kos,
                bestCombo = bestCombo,
                honorPoints = honor,
                levelsCleared = clearedStages.size,
                highestUnlockedLevel = highestUnlocked,
                clearedStages = clearedStages,
                unlockedFighters = unlockedFighters,
                unlockedAchievements = achievements,
                missionProgress = progressMap,
                completedMissions = completedMissions,
                activeSnapshot = null
            )
        }
    }

    suspend fun saveMatchSnapshot(snapshot: SavedMatchSnapshot?) {
        updateAndCommit { current ->
            current.copy(activeSnapshot = snapshot)
        }
    }

    suspend fun triggerManualSave(snapshot: SavedMatchSnapshot?): PlayerStats {
        return updateAndCommit { current ->
            if (snapshot != null) {
                current.copy(activeSnapshot = snapshot)
            } else {
                current
            }
        }
    }

    suspend fun unlockFighter(fighterId: FighterId, cost: Int): Boolean {
        var success = false
        updateAndCommit { current ->
            if (fighterId in current.unlockedFighters || current.honorPoints < cost) {
                current
            } else {
                success = true
                val nextFighters = current.unlockedFighters + fighterId
                val achievements = current.unlockedAchievements.toMutableSet()
                var bonusHonor = 0
                if (nextFighters.size >= FighterRoster.all.size && achievements.add(AchievementId.ARSENAL_COLLECTOR)) {
                    bonusHonor = AchievementId.ARSENAL_COLLECTOR.rewardHonor
                }
                current.copy(
                    honorPoints = current.honorPoints - cost + bonusHonor,
                    unlockedFighters = nextFighters,
                    unlockedAchievements = achievements
                )
            }
        }
        return success
    }

    suspend fun unlockAndEquipPerk(perk: CombatPerk): Boolean {
        var success = false
        updateAndCommit { current ->
            if (perk in current.unlockedPerks) {
                success = true
                current.copy(equippedPerk = perk)
            } else if (current.honorPoints >= perk.unlockCost) {
                success = true
                current.copy(
                    honorPoints = current.honorPoints - perk.unlockCost,
                    unlockedPerks = current.unlockedPerks + perk,
                    equippedPerk = perk
                )
            } else {
                current
            }
        }
        return success
    }

    suspend fun toggleEquipPerk(perk: CombatPerk) {
        updateAndCommit { current ->
            if (perk !in current.unlockedPerks) current
            else current.copy(equippedPerk = if (current.equippedPerk == perk) null else perk)
        }
    }

    suspend fun setPreferredDifficulty(difficulty: AiDifficulty) {
        updateAndCommit { it.copy(preferredDifficulty = difficulty) }
    }

    suspend fun setPreferredSelections(fighterId: FighterId, arenaId: ArenaId) {
        updateAndCommit { it.copy(preferredFighter = fighterId, preferredArena = arenaId) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        updateAndCommit { it.copy(soundEnabled = enabled) }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        updateAndCommit { it.copy(hapticsEnabled = enabled) }
    }

    suspend fun setLargeControls(enabled: Boolean) {
        updateAndCommit { it.copy(largeControls = enabled) }
    }

    suspend fun setLanguage(language: AppLanguage) {
        updateAndCommit { it.copy(language = language) }
    }

    internal fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("AC_SAVE_SALT_V2:$input").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    internal fun serializeJson(s: PlayerStats): String {
        val root = JSONObject().apply {
            put("totalWins", s.totalWins)
            put("totalKos", s.totalKos)
            put("bestCombo", s.bestCombo)
            put("honorPoints", s.honorPoints)
            put("levelsCleared", s.levelsCleared)
            put("highestUnlockedLevel", s.highestUnlockedLevel)
            put("clearedStages", JSONArray(s.clearedStages.sorted()))
            put("unlockedFighters", JSONArray(s.unlockedFighters.map { it.name }))
            put("unlockedPerks", JSONArray(s.unlockedPerks.map { it.name }))
            put("equippedPerk", s.equippedPerk?.name ?: "")
            put("unlockedAchievements", JSONArray(s.unlockedAchievements.map { it.name }))
            val mp = JSONObject()
            s.missionProgress.forEach { (k, v) -> mp.put(k.name, v) }
            put("missionProgress", mp)
            put("completedMissions", JSONArray(s.completedMissions.map { it.name }))
            put("preferredDifficulty", s.preferredDifficulty.name)
            put("preferredFighter", s.preferredFighter.name)
            put("preferredArena", s.preferredArena.name)
            put("soundEnabled", s.soundEnabled)
            put("hapticsEnabled", s.hapticsEnabled)
            put("largeControls", s.largeControls)
            put("language", s.language.code)
            put("lastSavedEpochMs", s.lastSavedEpochMs)

            s.activeSnapshot?.let { snap ->
                val snapJson = JSONObject().apply {
                    put("mode", snap.mode.name)
                    put("difficulty", snap.difficulty.name)
                    put("playerId", snap.playerId.name)
                    put("opponentId", snap.opponentId.name)
                    put("arenaId", snap.arenaId.name)
                    put("campaignLevelNumber", snap.campaignLevelNumber ?: -1)
                    put("roundNumber", snap.roundNumber)
                    put("roundTimeRemaining", snap.roundTimeRemaining.toDouble())
                    put("playerHp", snap.playerHp.toDouble())
                    put("playerEnergy", snap.playerEnergy.toDouble())
                    put("playerRoundsWon", snap.playerRoundsWon)
                    put("cpuHp", snap.cpuHp.toDouble())
                    put("cpuEnergy", snap.cpuEnergy.toDouble())
                    put("cpuRoundsWon", snap.cpuRoundsWon)
                    put("bossPhase", snap.bossPhase)
                    put("highestComboInMatch", snap.highestComboInMatch)
                    put("kosInMatch", snap.kosInMatch)
                    put("savedAtEpochMs", snap.savedAtEpochMs)
                }
                put("activeSnapshot", snapJson)
            }
        }
        return root.toString()
    }

    internal fun parseJson(raw: String): PlayerStats? {
        return try {
            val obj = JSONObject(raw)
            val clearedSet = mutableSetOf<Int>()
            obj.optJSONArray("clearedStages")?.let { arr ->
                for (i in 0 until arr.length()) clearedSet.add(arr.getInt(i))
            }

            val fighterSet = mutableSetOf(FighterId.SAQR, FighterId.SAIF)
            obj.optJSONArray("unlockedFighters")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { FighterId.valueOf(arr.getString(i)) }.getOrNull()?.let { fighterSet.add(it) }
                }
            }

            val perkSet = mutableSetOf<CombatPerk>()
            obj.optJSONArray("unlockedPerks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { CombatPerk.valueOf(arr.getString(i)) }.getOrNull()?.let { perkSet.add(it) }
                }
            }
            val equippedPerk = obj.optString("equippedPerk").takeIf { it.isNotBlank() }
                ?.let { runCatching { CombatPerk.valueOf(it) }.getOrNull() }
                ?.takeIf { it in perkSet }

            val achSet = mutableSetOf<AchievementId>()
            obj.optJSONArray("unlockedAchievements")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { AchievementId.valueOf(arr.getString(i)) }.getOrNull()?.let { achSet.add(it) }
                }
            }

            val mpMap = mutableMapOf<MissionId, Int>()
            obj.optJSONObject("missionProgress")?.let { mp ->
                MissionId.entries.forEach { m ->
                    if (mp.has(m.name)) mpMap[m] = mp.optInt(m.name, 0)
                }
            }

            val completedMissions = mutableSetOf<MissionId>()
            obj.optJSONArray("completedMissions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { MissionId.valueOf(arr.getString(i)) }.getOrNull()?.let { completedMissions.add(it) }
                }
            }

            val diff = runCatching { AiDifficulty.valueOf(obj.optString("preferredDifficulty")) }
                .getOrDefault(AiDifficulty.NORMAL)
            val prefFighter = runCatching { FighterId.valueOf(obj.optString("preferredFighter")) }
                .getOrDefault(FighterId.SAQR)
                .let { if (it in fighterSet) it else FighterId.SAQR }
            val prefArena = runCatching { ArenaId.valueOf(obj.optString("preferredArena")) }
                .getOrDefault(ArenaId.NEON_DOJO)
            val langCode = obj.optString("language", AppLanguage.ENGLISH.code)
            val lang = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.ENGLISH

            val snapshot = obj.optJSONObject("activeSnapshot")?.let { s ->
                val lvlNum = s.optInt("campaignLevelNumber", -1).takeIf { it > 0 }
                SavedMatchSnapshot(
                    mode = GameMode.valueOf(s.getString("mode")),
                    difficulty = AiDifficulty.valueOf(s.getString("difficulty")),
                    playerId = FighterId.valueOf(s.getString("playerId")),
                    opponentId = FighterId.valueOf(s.getString("opponentId")),
                    arenaId = ArenaId.valueOf(s.getString("arenaId")),
                    campaignLevelNumber = lvlNum,
                    roundNumber = s.optInt("roundNumber", 1).coerceIn(1, 3),
                    roundTimeRemaining = s.optDouble("roundTimeRemaining", 60.0).toFloat().coerceIn(5f, 60f),
                    playerHp = s.optDouble("playerHp", 100.0).toFloat().coerceIn(1f, 150f),
                    playerEnergy = s.optDouble("playerEnergy", 35.0).toFloat().coerceIn(0f, 100f),
                    playerRoundsWon = s.optInt("playerRoundsWon", 0).coerceIn(0, 1),
                    cpuHp = s.optDouble("cpuHp", 100.0).toFloat().coerceIn(1f, 250f),
                    cpuEnergy = s.optDouble("cpuEnergy", 35.0).toFloat().coerceIn(0f, 100f),
                    cpuRoundsWon = s.optInt("cpuRoundsWon", 0).coerceIn(0, 1),
                    bossPhase = s.optInt("bossPhase", 1).coerceIn(1, 2),
                    highestComboInMatch = s.optInt("highestComboInMatch", 0),
                    kosInMatch = s.optInt("kosInMatch", 0),
                    savedAtEpochMs = s.optLong("savedAtEpochMs", 0L)
                )
            }

            val highestLvl = obj.optInt("highestUnlockedLevel", 1).coerceIn(1, LevelCatalog.all.size)
            PlayerStats(
                totalWins = obj.optInt("totalWins", 0),
                totalKos = obj.optInt("totalKos", 0),
                bestCombo = obj.optInt("bestCombo", 0),
                honorPoints = obj.optInt("honorPoints", 0),
                levelsCleared = clearedSet.size.coerceAtLeast(obj.optInt("levelsCleared", 0)),
                highestUnlockedLevel = highestLvl,
                clearedStages = clearedSet,
                unlockedFighters = fighterSet,
                unlockedPerks = perkSet,
                equippedPerk = equippedPerk,
                unlockedAchievements = achSet,
                missionProgress = mpMap,
                completedMissions = completedMissions,
                preferredDifficulty = diff,
                preferredFighter = prefFighter,
                preferredArena = prefArena,
                soundEnabled = obj.optBoolean("soundEnabled", true),
                hapticsEnabled = obj.optBoolean("hapticsEnabled", true),
                largeControls = obj.optBoolean("largeControls", false),
                language = lang,
                lastSavedEpochMs = obj.optLong("lastSavedEpochMs", 0L),
                activeSnapshot = snapshot
            )
        } catch (_: Throwable) {
            null
        }
    }
}
