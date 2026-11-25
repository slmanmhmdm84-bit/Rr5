package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PlayerStats
import com.example.data.PlayerStatsRepository
import com.example.model.AchievementId
import com.example.model.AiDifficulty
import com.example.model.CombatPerk
import com.example.model.FighterId
import com.example.model.MissionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Arena Champions", appName)
    }

    @Test
    fun `save repository serializes and verifies integrity hash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PlayerStatsRepository(context)
        val stats = PlayerStats(
            totalWins = 7,
            totalKos = 12,
            bestCombo = 6,
            honorPoints = 420,
            levelsCleared = 4,
            highestUnlockedLevel = 5,
            clearedStages = setOf(1, 2, 3, 4),
            unlockedFighters = setOf(FighterId.SAQR, FighterId.SAIF, FighterId.JABBAR),
            unlockedPerks = setOf(CombatPerk.SURGE_BATTERY),
            equippedPerk = CombatPerk.SURGE_BATTERY,
            unlockedAchievements = setOf(AchievementId.FIRST_VICTORY, AchievementId.COLOSSUS_SLAYER),
            missionProgress = mapOf(MissionId.WIN_MATCHES to 3),
            completedMissions = setOf(MissionId.WIN_MATCHES),
            preferredDifficulty = AiDifficulty.EXTREME
        )

        val json = repo.serializeJson(stats)
        val hash = repo.computeSha256(json)
        val tamperedHash = repo.computeSha256("$json ")
        assertNotEquals(hash, tamperedHash)

        val parsed = repo.parseJson(json)
        assertNotNull(parsed)
        assertEquals(7, parsed!!.totalWins)
        assertEquals(420, parsed.honorPoints)
        assertEquals(5, parsed.highestUnlockedLevel)
        assertTrue(FighterId.JABBAR in parsed.unlockedFighters)
        assertEquals(CombatPerk.SURGE_BATTERY, parsed.equippedPerk)
        assertEquals(AiDifficulty.EXTREME, parsed.preferredDifficulty)
        assertTrue(AchievementId.COLOSSUS_SLAYER in parsed.unlockedAchievements)
    }
}
