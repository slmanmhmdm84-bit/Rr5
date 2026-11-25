package com.example.ui.i18n

import androidx.compose.runtime.compositionLocalOf
import com.example.model.AchievementId
import com.example.model.AiDifficulty
import com.example.model.ArenaId
import com.example.model.BossType
import com.example.model.CombatPerk
import com.example.model.FighterId
import com.example.model.GameMode
import com.example.model.MissionId
import com.example.model.StageModifier

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية")
}

data class LocalizedStrings(
    val isRtl: Boolean,
    val appTitle: String,
    val appSubtitle: String,
    val badgeArcade: String,
    val statWins: String,
    val statKos: String,
    val statBestCombo: String,
    val statLevelsCleared: String,
    val honorLabel: String,
    val modeCampaign: String,
    val modeCampaignDesc: String,
    val modeVersus: String,
    val modeVersusDesc: String,
    val modeTraining: String,
    val modeTrainingDesc: String,
    val btnResumeMatch: String,
    val resumeMatchDescFormat: (String, Int) -> String,
    val btnPlayNow: String,
    val playNowDescFormat: (Int, String) -> String,
    val btnProgression: String,
    val btnMoveList: String,
    val btnSettings: String,
    val settingsTitle: String,
    val settingLanguage: String,
    val settingDifficulty: String,
    val settingSound: String,
    val settingHaptics: String,
    val settingLargeControls: String,
    val btnSaveNow: String,
    val savedVerifiedMsg: String,
    val saveLastFormat: (String) -> String,
    val saveChecksumValid: String,
    val saveRestoredBackup: String,
    val btnClose: String,
    val btnBack: String,
    val levelsTitle: String,
    val levelsSubtitle: String,
    val levelLabel: String,
    val bossBadge: String,
    val allyBadge: String,
    val levelLocked: String,
    val levelCleared: String,
    val levelNextBadge: String,
    val selectTitle: String,
    val selectYourFighter: String,
    val selectOpponent: String,
    val selectDifficulty: String,
    val selectArena: String,
    val btnStartFight: String,
    val statSpeed: String,
    val statPower: String,
    val statDefense: String,
    val specialPrefix: String,
    val lockedFighterBadge: String,
    val unlockWithHonorFormat: (Int) -> String,
    val unlockHintStageFormat: (Int) -> String,
    val roundLabel: String,
    val readySub: String,
    val fightBanner: String,
    val koBanner: String,
    val koSub: String,
    val timeUpSub: String,
    val perfectTitle: String,
    val greatRoundSub: String,
    val dontGiveUpSub: String,
    val drawTitle: String,
    val drawSub: String,
    val winBanner: String,
    val loseBanner: String,
    val youWinTitle: String,
    val youLoseTitle: String,
    val campaignChampionTitle: String,
    val campaignChampionSub: String,
    val trainingBanner: String,
    val trainingSub: String,
    val comboLabel: String,
    val blockCallout: String,
    val specialBlockCallout: String,
    val specialHitCallout: String,
    val counterCallout: String,
    val guardBreakCallout: String,
    val parryCallout: String,
    val bossPhase2Banner: String,
    val bossWarnSlam: String,
    val bossWarnTeleport: String,
    val bossWarnBarrage: String,
    val bossWarnSlash: String,
    val allyIncomingWarn: String,
    val bossPhaseLabel: (Int) -> String,
    val aiTacticAntiAir: String,
    val aiTacticCounterZone: String,
    val aiTacticGuardBreak: String,
    val aiTacticWhiffPunish: String,
    val aiTacticFlankPinch: String,
    val ctrlPunch: String,
    val ctrlKick: String,
    val ctrlSpecial: String,
    val ctrlGuard: String,
    val ctrlJump: String,
    val ctrlDash: String,
    val pauseTitle: String,
    val btnResume: String,
    val btnSaveCheckpoint: String,
    val btnRematch: String,
    val btnNextLevel: String,
    val btnChangeFighter: String,
    val btnMainMenu: String,
    val movesTitle: String,
    val movesBasicsTitle: String,
    val movesBasics: List<Pair<String, String>>,
    val progressionTitle: String,
    val tabMissions: String,
    val tabAchievements: String,
    val tabArmory: String,
    val missionCompleteBadge: String,
    val rewardHonorFormat: (Int) -> String,
    val unlockedBadge: String,
    val equippedBadge: String,
    val btnEquip: String,
    val btnUnlock: String,
    val armoryFightersHeader: String,
    val armoryPerksHeader: String,
    val savePanelTitle: String,
    val cdPause: String,
    val cdSound: String,
    val cdSettings: String,
    val cdMoveLeft: String,
    val cdMoveRight: String,
    val cdHeroBanner: String,
    val cdArenaBg: String,
    val bestComboLabel: String,
    val winnerFormat: (String) -> String,
    val difficultyLabel: (AiDifficulty) -> String,
    val difficultyDesc: (AiDifficulty) -> String,
    val modeLabel: (GameMode) -> String,
    val fighterName: (FighterId) -> String,
    val fighterTitle: (FighterId) -> String,
    val fighterBio: (FighterId) -> String,
    val fighterSpecialName: (FighterId) -> String,
    val fighterSpecialDesc: (FighterId) -> String,
    val arenaName: (ArenaId) -> String,
    val arenaSubtitle: (ArenaId) -> String,
    val levelTitle: (Int) -> String,
    val bossName: (BossType) -> String,
    val bossTitle: (BossType) -> String,
    val modifierName: (StageModifier) -> String,
    val modifierDesc: (StageModifier) -> String,
    val perkName: (CombatPerk) -> String,
    val perkDesc: (CombatPerk) -> String,
    val achievementTitle: (AchievementId) -> String,
    val achievementDesc: (AchievementId) -> String,
    val missionTitle: (MissionId) -> String,
    val missionDesc: (MissionId) -> String
)

object GameStrings {
    val english = LocalizedStrings(
        isRtl = false,
        appTitle = "Arena Champions",
        appSubtitle = "Legends Clash — Adaptive 2D Fighter",
        badgeArcade = "ARCADE 2D FIGHTER",
        statWins = "Wins",
        statKos = "K.O.s",
        statBestCombo = "Combo",
        statLevelsCleared = "Stages",
        honorLabel = "Honor",
        modeCampaign = "Challenge Campaign",
        modeCampaignDesc = "Battle through 8 stages, coordinated ambushes & 3 unique Bosses",
        modeVersus = "Quick Versus",
        modeVersusDesc = "Choose any unlocked fighter, arena, and 4 AI difficulty tiers",
        modeTraining = "Training Dojo",
        modeTrainingDesc = "Practice combos, dashes, parries, and specials with auto-heal",
        btnResumeMatch = "Continue Saved Fight",
        resumeMatchDescFormat = { opp, round -> "Resume vs $opp • Round $round (Auto-Saved)" },
        btnPlayNow = "PLAY NOW",
        playNowDescFormat = { stage, opp -> "Jump straight into Stage $stage vs $opp" },
        btnProgression = "Missions, Armory & Save",
        btnMoveList = "Move Guide",
        btnSettings = "Settings",
        settingsTitle = "Game & Save Settings",
        settingLanguage = "Language",
        settingDifficulty = "Default AI Difficulty",
        settingSound = "Sound Effects",
        settingHaptics = "Hit Vibration",
        settingLargeControls = "Large Touch Controls",
        btnSaveNow = "Save Progress Now",
        savedVerifiedMsg = "Progress saved & integrity verified!",
        saveLastFormat = { t -> "Auto-Save Active • Last saved: $t" },
        saveChecksumValid = "SHA-256 Save Integrity Verified",
        saveRestoredBackup = "Recovered cleanly from verified backup",
        btnClose = "Done",
        btnBack = "Back",
        levelsTitle = "Challenge Stages",
        levelsSubtitle = "Defeat rivals & unique bosses to unlock fighters and honor",
        levelLabel = "Stage",
        bossBadge = "BOSS",
        allyBadge = "2v1 TEAM",
        levelLocked = "Locked",
        levelCleared = "Cleared",
        levelNextBadge = "NEXT",
        selectTitle = "Battle Setup",
        selectYourFighter = "Choose Your Fighter",
        selectOpponent = "Choose Opponent",
        selectDifficulty = "AI Difficulty Mode",
        selectArena = "Select Arena",
        btnStartFight = "FIGHT NOW!",
        statSpeed = "Speed",
        statPower = "Power",
        statDefense = "Defense",
        specialPrefix = "Special Move",
        lockedFighterBadge = "LOCKED",
        unlockWithHonorFormat = { cost -> "Unlock ($cost Honor)" },
        unlockHintStageFormat = { st -> "Clear Stage $st or unlock in Armory" },
        roundLabel = "ROUND",
        readySub = "GET READY!",
        fightBanner = "FIGHT!",
        koBanner = "K.O.!",
        koSub = "KNOCKOUT",
        timeUpSub = "TIME UP",
        perfectTitle = "PERFECT!",
        greatRoundSub = "Dominant Round",
        dontGiveUpSub = "Stay Sharp!",
        drawTitle = "DRAW!",
        drawSub = "Double Point",
        winBanner = "VICTORY!",
        loseBanner = "DEFEAT",
        youWinTitle = "YOU WIN!",
        youLoseTitle = "YOU LOSE",
        campaignChampionTitle = "APEX CHAMPION!",
        campaignChampionSub = "You defeated all 8 stages, team ambushes, and the Omega Shogun!",
        trainingBanner = "TRAINING",
        trainingSub = "Practice freely!",
        comboLabel = "HIT COMBO",
        blockCallout = "BLOCK!",
        specialBlockCallout = "GUARDED!",
        specialHitCallout = "CRITICAL!",
        counterCallout = "COUNTER!",
        guardBreakCallout = "GUARD BREAK!",
        parryCallout = "PARRY!",
        bossPhase2Banner = "PHASE II ENRAGED!",
        bossWarnSlam = "! SEISMIC SLAM !",
        bossWarnTeleport = "! SHADOW BLINK !",
        bossWarnBarrage = "! PLASMA BARRAGE !",
        bossWarnSlash = "! OVERDRIVE SLASH !",
        allyIncomingWarn = "! ALLY ASSIST !",
        bossPhaseLabel = { p -> if (p >= 2) "PHASE II • ENRAGED" else "PHASE I • BOSS" },
        aiTacticAntiAir = "AI Adapting: Anti-Air",
        aiTacticCounterZone = "AI Adapting: Anti-Zoning",
        aiTacticGuardBreak = "AI Adapting: Guard Crush",
        aiTacticWhiffPunish = "AI Adapting: Whiff Punish",
        aiTacticFlankPinch = "Coordinated Pincer!",
        ctrlPunch = "PUNCH",
        ctrlKick = "KICK",
        ctrlSpecial = "SPECIAL",
        ctrlGuard = "GUARD",
        ctrlJump = "JUMP",
        ctrlDash = "DASH",
        pauseTitle = "PAUSED",
        btnResume = "Resume Fight",
        btnSaveCheckpoint = "Manual Save Checkpoint",
        btnRematch = "Rematch",
        btnNextLevel = "Next Stage",
        btnChangeFighter = "Change Fighter",
        btnMainMenu = "Main Menu",
        movesTitle = "Combat & Move Guide",
        movesBasicsTitle = "Core Mechanics & Boss Tactics",
        movesBasics = listOf(
            "Light Punch" to "Fast startup jab to interrupt opponents and start combos.",
            "Heavy Kick" to "Longer reach and heavy knockback with high damage.",
            "Combo Chain" to "Tap Punch -> Kick -> Special right as each strike lands for maximum combo damage.",
            "Guard & Timing" to "Hold Guard to block 80% of damage, or raise Guard right before impact for a zero-damage Perfect Parry!",
            "Dash Dodge" to "Dash cleanly through zoning fireballs or reposition out of corners.",
            "Boss Phases" to "Bosses enter Enraged Phase II below 50% HP with faster abilities and ally assists."
        ),
        progressionTitle = "Progression & Armory",
        tabMissions = "Missions",
        tabAchievements = "Awards",
        tabArmory = "Armory",
        missionCompleteBadge = "COMPLETED",
        rewardHonorFormat = { h -> "+$h Honor" },
        unlockedBadge = "UNLOCKED",
        equippedBadge = "EQUIPPED",
        btnEquip = "Equip",
        btnUnlock = "Unlock",
        armoryFightersHeader = "Unlockable Fighters",
        armoryPerksHeader = "Combat Abilities (Equip 1 Active)",
        savePanelTitle = "Save System & Backup",
        cdPause = "Pause",
        cdSound = "Toggle Sound",
        cdSettings = "Settings",
        cdMoveLeft = "Move Left",
        cdMoveRight = "Move Right",
        cdHeroBanner = "Arena Champions Hero Art",
        cdArenaBg = "Arena Stage Background",
        bestComboLabel = "Max Combo",
        winnerFormat = { name -> "$name Wins!" },
        difficultyLabel = { diff ->
            when (diff) {
                AiDifficulty.EASY -> "Easy"
                AiDifficulty.NORMAL -> "Normal"
                AiDifficulty.HARD -> "Hard"
                AiDifficulty.EXTREME -> "Extreme"
            }
        },
        difficultyDesc = { diff ->
            when (diff) {
                AiDifficulty.EASY -> "Slower reactions and simple attacks for newcomers."
                AiDifficulty.NORMAL -> "Balanced spacing, blocking, and 2-hit combos."
                AiDifficulty.HARD -> "Aggressive pressure, anti-airs, and tactical whiff punishes."
                AiDifficulty.EXTREME -> "Adaptive AI reads your habits, dodges, and executes full combos."
            }
        },
        modeLabel = { mode ->
            when (mode) {
                GameMode.CAMPAIGN -> "Challenge Campaign"
                GameMode.VERSUS -> "Quick Versus"
                GameMode.TRAINING -> "Training"
            }
        },
        fighterName = { id ->
            when (id) {
                FighterId.SAQR -> "Saqr"
                FighterId.SAIF -> "Saif"
                FighterId.JABBAR -> "Jabbar"
                FighterId.LAYLA -> "Layla"
            }
        },
        fighterTitle = { id ->
            when (id) {
                FighterId.SAQR -> "Crimson Striker"
                FighterId.SAIF -> "Cyber Ronin"
                FighterId.JABBAR -> "Iron Titan"
                FighterId.LAYLA -> "Shadow Empress"
            }
        },
        fighterBio = { id ->
            when (id) {
                FighterId.SAQR -> "Balanced martial artist combining rapid strikes with blazing fire waves."
                FighterId.SAIF -> "Lightning-fast cyber ninja wielding plasma slashes and agile footwork."
                FighterId.JABBAR -> "Armored powerhouse built for crushing blows and unmatched durability."
                FighterId.LAYLA -> "Elusive mystic assassin who overwhelms foes with twin shadow blades."
            }
        },
        fighterSpecialName = { id ->
            when (id) {
                FighterId.SAQR -> "Flame Surge"
                FighterId.SAIF -> "Volt Slash"
                FighterId.JABBAR -> "Earthquake Slam"
                FighterId.LAYLA -> "Shadow Crescents"
            }
        },
        fighterSpecialDesc = { id ->
            when (id) {
                FighterId.SAQR -> "Fires a blazing fireball wave across the arena."
                FighterId.SAIF -> "Unleashes a high-velocity electric plasma bolt."
                FighterId.JABBAR -> "Sends a heavy ground shockwave that launches opponents."
                FighterId.LAYLA -> "Throws twin piercing dark-energy crescent blades."
            }
        },
        arenaName = { id ->
            when (id) {
                ArenaId.NEON_DOJO -> "Neon Rooftop"
                ArenaId.DESERT_CITADEL -> "Desert Citadel"
                ArenaId.VOLCANIC_FORGE -> "Volcanic Coliseum"
            }
        },
        arenaSubtitle = { id ->
            when (id) {
                ArenaId.NEON_DOJO -> "Cyberpunk skyline bathed in electric rain"
                ArenaId.DESERT_CITADEL -> "Ancient sandstone fortress at golden dusk"
                ArenaId.VOLCANIC_FORGE -> "Obsidian arena surrounded by molten magma"
            }
        },
        levelTitle = { num ->
            when (num) {
                1 -> "Rooftop Initiation"
                2 -> "Sands of Energy"
                3 -> "Colossus Gate"
                4 -> "Hyper Shadow Duel"
                5 -> "Sandstorm Ambush"
                6 -> "Mirror Throne"
                7 -> "Molten Gauntlet"
                else -> "Omega Protocol"
            }
        },
        bossName = { boss ->
            when (boss) {
                BossType.NONE -> ""
                BossType.IRON_COLOSSUS -> "Iron Colossus Jabbar"
                BossType.PHANTOM_EMPRESS -> "Phantom Empress Layla"
                BossType.OMEGA_SHOGUN -> "Omega Shogun Saif"
            }
        },
        bossTitle = { boss ->
            when (boss) {
                BossType.NONE -> ""
                BossType.IRON_COLOSSUS -> "Seismic Leap & Hyper Armor"
                BossType.PHANTOM_EMPRESS -> "Shadow Blink & Mirror Clone"
                BossType.OMEGA_SHOGUN -> "Plasma Barrage & Overdrive"
            }
        },
        modifierName = { mod ->
            when (mod) {
                StageModifier.STANDARD -> "Standard Duel"
                StageModifier.FAST_ENERGY -> "Energy Overcharge"
                StageModifier.ARMORED_BOSS -> "Armored Boss"
                StageModifier.HYPER_SPEED -> "Hyper Reflex"
                StageModifier.CRITICAL_STRIKE -> "Sudden Impact"
                StageModifier.COORDINATED_AMBUSH -> "Coordinated Ambush"
                StageModifier.OVERLORD_RAGE -> "Omega Overdrive"
            }
        },
        modifierDesc = { mod ->
            when (mod) {
                StageModifier.STANDARD -> "Classic 3-round rules. Sharpen your fundamentals."
                StageModifier.FAST_ENERGY -> "Super Energy charges 2x faster for non-stop specials."
                StageModifier.ARMORED_BOSS -> "Boss gains Seismic Slam, Hyper Armor, and +35% HP."
                StageModifier.HYPER_SPEED -> "Movement and strike speed boosted by 22%."
                StageModifier.CRITICAL_STRIKE -> "Every clean hit deals +35% critical damage."
                StageModifier.COORDINATED_AMBUSH -> "Rival coordinates flank strikes with an allied striker."
                StageModifier.OVERLORD_RAGE -> "Final Boss has +65% HP, drone support & energy regen!"
            }
        },
        perkName = { perk ->
            when (perk) {
                CombatPerk.SURGE_BATTERY -> "Surge Battery"
                CombatPerk.IRON_BASTION -> "Iron Bastion"
                CombatPerk.COMBO_CATALYST -> "Combo Catalyst"
                CombatPerk.PHANTOM_REFLEX -> "Phantom Reflex"
            }
        },
        perkDesc = { perk ->
            when (perk) {
                CombatPerk.SURGE_BATTERY -> "Start every round with 55 Super Energy and +20% meter gain."
                CombatPerk.IRON_BASTION -> "Increases max defense by 12% and cuts chip damage in half."
                CombatPerk.COMBO_CATALYST -> "Deals +15% bonus damage on combos of 2 hits or more."
                CombatPerk.PHANTOM_REFLEX -> "Reduces dash cooldown by 30% and boosts move speed by 8%."
            }
        },
        achievementTitle = { ach ->
            when (ach) {
                AchievementId.FIRST_VICTORY -> "First Blood"
                AchievementId.COMBO_SAVANT -> "Combo Savant"
                AchievementId.PERFECT_ROUND -> "Untouchable"
                AchievementId.COLOSSUS_SLAYER -> "Colossus Slayer"
                AchievementId.PHANTOM_BREAKER -> "Mirror Breaker"
                AchievementId.APEX_CHAMPION -> "Apex Champion"
                AchievementId.EXTREME_SURVIVOR -> "Extreme Conqueror"
                AchievementId.ARSENAL_COLLECTOR -> "Full Roster"
            }
        },
        achievementDesc = { ach ->
            when (ach) {
                AchievementId.FIRST_VICTORY -> "Win your very first match."
                AchievementId.COMBO_SAVANT -> "Land a 5-hit combo or higher in a real match."
                AchievementId.PERFECT_ROUND -> "Win a round without taking a single point of damage."
                AchievementId.COLOSSUS_SLAYER -> "Defeat Stage 3 Boss: Iron Colossus Jabbar."
                AchievementId.PHANTOM_BREAKER -> "Defeat Stage 6 Boss: Phantom Empress Layla."
                AchievementId.APEX_CHAMPION -> "Defeat Stage 8 Final Boss: Omega Shogun Saif."
                AchievementId.EXTREME_SURVIVOR -> "Win any match on Extreme AI difficulty."
                AchievementId.ARSENAL_COLLECTOR -> "Unlock all 4 playable fighters."
            }
        },
        missionTitle = { m ->
            when (m) {
                MissionId.WIN_MATCHES -> "Arena Victor"
                MissionId.LAND_COMBOS -> "Combo Technician"
                MissionId.DEFEAT_BOSSES -> "Boss Hunter"
                MissionId.WIN_EXTREME_OR_HARD -> "Elite Challenger"
            }
        },
        missionDesc = { m ->
            when (m) {
                MissionId.WIN_MATCHES -> "Win 3 matches in Campaign or Versus."
                MissionId.LAND_COMBOS -> "Achieve a 3+ hit combo in 5 matches."
                MissionId.DEFEAT_BOSSES -> "Defeat 2 campaign boss encounters."
                MissionId.WIN_EXTREME_OR_HARD -> "Win 2 matches on Hard or Extreme difficulty."
            }
        }
    )

    val arabic = LocalizedStrings(
        isRtl = true,
        appTitle = "أبطال الحلبة",
        appSubtitle = "صدام الأساطير — قتال ذكي متطور",
        badgeArcade = "ARCADE 2D FIGHTER",
        statWins = "الانتصارات",
        statKos = "الضربات القاضية",
        statBestCombo = "أعلى كومبو",
        statLevelsCleared = "المراحل",
        honorLabel = "الشرف",
        modeCampaign = "مراحل الحملة والزعماء",
        modeCampaignDesc = "8 مراحل متدرجة مع 3 زعماء فريدين وهجمات منسقة",
        modeVersus = "قتال سريع",
        modeVersusDesc = "اختر المقاتل والساحة وأحد مستويات الصعوبة الأربعة",
        modeTraining = "ساحة التدريب",
        modeTrainingDesc = "تدرّب على الكومبو والاندفاع والصد مع شفاء تلقائي",
        btnResumeMatch = "متابعة القتال المحفوظ",
        resumeMatchDescFormat = { opp, round -> "استكمال ضد $opp • الجولة $round (حفظ تلقائي)" },
        btnPlayNow = "العب الآن",
        playNowDescFormat = { stage, opp -> "ابدأ فوراً في المرحلة $stage ضد $opp" },
        btnProgression = "المهام والإنجازات والترسانة",
        btnMoveList = "دليل القتال",
        btnSettings = "الإعدادات",
        settingsTitle = "إعدادات اللعبة والحفظ",
        settingLanguage = "اللغة",
        settingDifficulty = "صعوبة الذكاء الافتراضية",
        settingSound = "المؤثرات الصوتية",
        settingHaptics = "اهتزاز الضربات",
        settingLargeControls = "أزرار تحكم كبيرة",
        btnSaveNow = "حفظ التقدم يدوياً",
        savedVerifiedMsg = "تم حفظ التقدم والتحقق من سلامته!",
        saveLastFormat = { t -> "الحفظ التلقائي مفعّل • آخر حفظ: $t" },
        saveChecksumValid = "تم التحقق من سلامة البيانات (SHA-256)",
        saveRestoredBackup = "تمت استعادة النسخة الاحتياطية بأمان",
        btnClose = "تم",
        btnBack = "رجوع",
        levelsTitle = "مراحل التحدي",
        levelsSubtitle = "اهزم الخصوم والزعماء لفتح المقاتلين ونقاط الشرف",
        levelLabel = "المرحلة",
        bossBadge = "زعيم",
        allyBadge = "تعاون 2 ضد 1",
        levelLocked = "مقفلة",
        levelCleared = "مكتملة",
        levelNextBadge = "التالية",
        selectTitle = "تجهيز المعركة",
        selectYourFighter = "اختر مقاتلك",
        selectOpponent = "اختر الخصم",
        selectDifficulty = "مستوى صعوبة الذكاء الاصطناعي",
        selectArena = "اختر الساحة",
        btnStartFight = "ابدأ القتال!",
        statSpeed = "السرعة",
        statPower = "القوة",
        statDefense = "الدفاع",
        specialPrefix = "المهارة الخاصة",
        lockedFighterBadge = "مقفل",
        unlockWithHonorFormat = { cost -> "فتح ($cost شرف)" },
        unlockHintStageFormat = { st -> "اهزم المرحلة $st أو افتحه من الترسانة" },
        roundLabel = "الجولة",
        readySub = "استعد للمواجهة!",
        fightBanner = "قاتل!",
        koBanner = "ضربة قاضية!",
        koSub = "K.O.",
        timeUpSub = "انتهى الوقت",
        perfectTitle = "فوز ساحق!",
        greatRoundSub = "جولة رائعة",
        dontGiveUpSub = "لا تستسلم!",
        drawTitle = "تعادل!",
        drawSub = "نقطة لكلا المقاتلين",
        winBanner = "انتصار!",
        loseBanner = "هزيمة",
        youWinTitle = "لقد فزت!",
        youLoseTitle = "لقد خسرت",
        campaignChampionTitle = "البطل الأسطوري!",
        campaignChampionSub = "أتممت جميع المراحل الثماني وهزمت شوغون أوميغا!",
        trainingBanner = "ساحة التدريب",
        trainingSub = "تدرّب بحرية!",
        comboLabel = "كومبو",
        blockCallout = "صد!",
        specialBlockCallout = "صد خاص!",
        specialHitCallout = "ضربة قاتلة!",
        counterCallout = "مرتدة!",
        guardBreakCallout = "كسر الدفاع!",
        parryCallout = "صد مثالي!",
        bossPhase2Banner = "المرحلة الثانية — غضب الزعيم!",
        bossWarnSlam = "! سحق زلزالي !",
        bossWarnTeleport = "! انتقال ظلي !",
        bossWarnBarrage = "! وابل البلازما !",
        bossWarnSlash = "! اندفاع أوميغا !",
        allyIncomingWarn = "! هجوم حليف !",
        bossPhaseLabel = { p -> if (p >= 2) "الطور 2 • غضب" else "الطور 1 • زعيم" },
        aiTacticAntiAir = "تكيّف الذكاء: مضاد للقفز",
        aiTacticCounterZone = "تكيّف الذكاء: اختراق المقذوفات",
        aiTacticGuardBreak = "تكيّف الذكاء: كسر الدفاع",
        aiTacticWhiffPunish = "تكيّف الذكاء: معاقبة الأخطاء",
        aiTacticFlankPinch = "كماشة منسّقة!",
        ctrlPunch = "لكمة",
        ctrlKick = "ركلة",
        ctrlSpecial = "خاصة",
        ctrlGuard = "صد",
        ctrlJump = "قفز",
        ctrlDash = "اندفاع",
        pauseTitle = "إيقاف مؤقت",
        btnResume = "متابعة",
        btnSaveCheckpoint = "حفظ يدوي الآن",
        btnRematch = "إعادة القتال",
        btnNextLevel = "المرحلة التالية",
        btnChangeFighter = "تغيير المقاتل",
        btnMainMenu = "القائمة الرئيسية",
        movesTitle = "دليل القتال والحركات",
        movesBasicsTitle = "الأساسيات وتكتيكات الزعماء",
        movesBasics = listOf(
            "لكمة سريعة" to "ضربة خاطفة لبداية الهجوم وفتح الثغرات.",
            "ركلة قوية" to "مدى أطول وضرر مضاعف تدفع الخصم للخلف.",
            "سلسلة الكومبو" to "اضغط لكمة ثم ركلة ثم خاصة فور الإصابة لربط هجوم متواصل.",
            "الصد والتوقيت" to "استمر بالضغط لصد 80% من الضرر، أو ارفع الصد لحظة الضربة لتنفيذ صد مثالي بدون ضرر!",
            "المراوغة بالاندفاع" to "استخدم الاندفاع لتجاوز المقذوفات أو الخروج من الزاوية.",
            "أطوار الزعماء" to "يدخل الزعماء الطور الغاضب تحت 50% صحة مع قدرات خاصة ومساعدة حلفاء."
        ),
        progressionTitle = "التقدم والترسانة",
        tabMissions = "المهام",
        tabAchievements = "الإنجازات",
        tabArmory = "الترسانة",
        missionCompleteBadge = "مكتملة",
        rewardHonorFormat = { h -> "+$h شرف" },
        unlockedBadge = "مفتوح",
        equippedBadge = "مجهّز",
        btnEquip = "تجهيز",
        btnUnlock = "فتح",
        armoryFightersHeader = "المقاتلون القابلون للفتح",
        armoryPerksHeader = "القدرات القتالية (اختر واحدة)",
        savePanelTitle = "نظام الحفظ والنسخ الاحتياطي",
        cdPause = "إيقاف مؤقت",
        cdSound = "تبديل الصوت",
        cdSettings = "الإعدادات",
        cdMoveLeft = "تحرك لليسار",
        cdMoveRight = "تحرك لليمين",
        cdHeroBanner = "صورة المواجهة القتالية",
        cdArenaBg = "خلفية ساحة القتال",
        bestComboLabel = "أعلى كومبو",
        winnerFormat = { name -> "فاز $name!" },
        difficultyLabel = { diff ->
            when (diff) {
                AiDifficulty.EASY -> "سهل"
                AiDifficulty.NORMAL -> "متوسط"
                AiDifficulty.HARD -> "صعب"
                AiDifficulty.EXTREME -> "أسطوري"
            }
        },
        difficultyDesc = { diff ->
            when (diff) {
                AiDifficulty.EASY -> "ردود فعل بطيئة وهجمات بسيطة للمبتدئين."
                AiDifficulty.NORMAL -> "قتال متوازن مع صد وتنوع متوسط."
                AiDifficulty.HARD -> "ضغط هجومي سريع ومضاد للقفز ومعاقبة الأخطاء."
                AiDifficulty.EXTREME -> "ذكاء متكيف يحلل أسلوبك ويتفادى ويطبق كومبو كامل."
            }
        },
        modeLabel = { mode ->
            when (mode) {
                GameMode.CAMPAIGN -> "الحملة"
                GameMode.VERSUS -> "قتال سريع"
                GameMode.TRAINING -> "التدريب"
            }
        },
        fighterName = { id ->
            when (id) {
                FighterId.SAQR -> "صقر"
                FighterId.SAIF -> "سيف"
                FighterId.JABBAR -> "جبار"
                FighterId.LAYLA -> "ليلى"
            }
        },
        fighterTitle = { id ->
            when (id) {
                FighterId.SAQR -> "مقاتل اللهب"
                FighterId.SAIF -> "رونين السايبر"
                FighterId.JABBAR -> "العملاق الفولاذي"
                FighterId.LAYLA -> "إمبراطورة الظل"
            }
        },
        fighterBio = { id ->
            when (id) {
                FighterId.SAQR -> "خبير فنون قتالية متوازن يمزج بين السرعة والضربات النارية."
                FighterId.SAIF -> "مقاتل خفيف وسريع يستخدم دروعاً تقنية ونصل برق فائق."
                FighterId.JABBAR -> "محارب مدرّع بقبضات ثقيلة يمتلك أعلى قدرة تحمل وضربات ساحقة."
                FighterId.LAYLA -> "مقاتلة غامضة تعتمد على الخفة والمراوغة ونصال الظل البنفسجية."
            }
        },
        fighterSpecialName = { id ->
            when (id) {
                FighterId.SAQR -> "كرة اللهب"
                FighterId.SAIF -> "برق النصل"
                FighterId.JABBAR -> "زلزال الأرض"
                FighterId.LAYLA -> "هلال الظلام"
            }
        },
        fighterSpecialDesc = { id ->
            when (id) {
                FighterId.SAQR -> "يطلق موجة نارية متفجرة تعبر الساحة بسرعة عالية."
                FighterId.SAIF -> "شعاع كهربائي خاطف يخترق المسافات ويصيب الخصم فوراً."
                FighterId.JABBAR -> "موجة صدمة أرضية ثقيلة تطلق الخصم في الهواء."
                FighterId.LAYLA -> "تُطلق نصلين طاقيين مزدوجين بتأثير حاد ومدى واسع."
            }
        },
        arenaName = { id ->
            when (id) {
                ArenaId.NEON_DOJO -> "معبد النيون"
                ArenaId.DESERT_CITADEL -> "قلعة الصحراء"
                ArenaId.VOLCANIC_FORGE -> "حلبة البركان"
            }
        },
        arenaSubtitle = { id ->
            when (id) {
                ArenaId.NEON_DOJO -> "سطح مدينة مستقبلية تحت أضواء الليزر"
                ArenaId.DESERT_CITADEL -> "ساحة حجرية عريقة عند الغروب"
                ArenaId.VOLCANIC_FORGE -> "مدرج بركاني محاط بأنهار الحمم"
            }
        },
        levelTitle = { num ->
            when (num) {
                1 -> "اختبار السطح"
                2 -> "رمال الطاقة"
                3 -> "بوابة العملاق"
                4 -> "مبارزة الظل السريعة"
                5 -> "كمين العاصفة"
                6 -> "عرش المرايا"
                7 -> "طريق الحمم"
                else -> "بروتوكول أوميغا"
            }
        },
        bossName = { boss ->
            when (boss) {
                BossType.NONE -> ""
                BossType.IRON_COLOSSUS -> "العملاق الفولاذي جبار"
                BossType.PHANTOM_EMPRESS -> "إمبراطورة الوهم ليلى"
                BossType.OMEGA_SHOGUN -> "شوغون أوميغا سيف"
            }
        },
        bossTitle = { boss ->
            when (boss) {
                BossType.NONE -> ""
                BossType.IRON_COLOSSUS -> "قفزة زلزالية ودرع فائق"
                BossType.PHANTOM_EMPRESS -> "انتقال ظلي ونسخة مرافقة"
                BossType.OMEGA_SHOGUN -> "وابل البلازما والاندفاع"
            }
        },
        modifierName = { mod ->
            when (mod) {
                StageModifier.STANDARD -> "مواجهة قياسية"
                StageModifier.FAST_ENERGY -> "شحن طاقة مضاعف"
                StageModifier.ARMORED_BOSS -> "زعيم مدرّع"
                StageModifier.HYPER_SPEED -> "سرعة فائقة"
                StageModifier.CRITICAL_STRIKE -> "ضربات قاتلة"
                StageModifier.COORDINATED_AMBUSH -> "كمين منسّق"
                StageModifier.OVERLORD_RAGE -> "غضب أوميغا"
            }
        },
        modifierDesc = { mod ->
            when (mod) {
                StageModifier.STANDARD -> "قواعد 3 جولات قياسية لإتقان الأساسيات."
                StageModifier.FAST_ENERGY -> "تمتلئ الطاقة الخاصة بسرعة مضاعفة لكلا المقاتلين."
                StageModifier.ARMORED_BOSS -> "يمتلك الزعيم قفزة زلزالية ودرعاً قوياً و+35% صحة."
                StageModifier.HYPER_SPEED -> "تزداد سرعة الحركة والضربات بنسبة 22%."
                StageModifier.CRITICAL_STRIKE -> "تُحدث كل ضربة ضرراً إضافياً بنسبة 35%."
                StageModifier.COORDINATED_AMBUSH -> "ينسّق الخصم هجمات كماشة مع مقاتل مساعد."
                StageModifier.OVERLORD_RAGE -> "الزعيم الأخير بصحة +65% ودعم درون وطاقة متجددة!"
            }
        },
        perkName = { perk ->
            when (perk) {
                CombatPerk.SURGE_BATTERY -> "بطارية الطاقة"
                CombatPerk.IRON_BASTION -> "الحصن الحديدي"
                CombatPerk.COMBO_CATALYST -> "محفّز الكومبو"
                CombatPerk.PHANTOM_REFLEX -> "انعكاس الشبح"
            }
        },
        perkDesc = { perk ->
            when (perk) {
                CombatPerk.SURGE_BATTERY -> "ابدأ كل جولة بـ 55 طاقة خاصة وزيادة +20% للشحن."
                CombatPerk.IRON_BASTION -> "يزيد الدفاع بنسبة 12% ويخفض ضرر الصد للنصف."
                CombatPerk.COMBO_CATALYST -> "ضرر إضافي +15% عند تنفيذ كومبو من ضربتين أو أكثر."
                CombatPerk.PHANTOM_REFLEX -> "يقلل انتظار الاندفاع 30% ويزيد سرعة الحركة 8%."
            }
        },
        achievementTitle = { ach ->
            when (ach) {
                AchievementId.FIRST_VICTORY -> "أول انتصار"
                AchievementId.COMBO_SAVANT -> "أستاذ الكومبو"
                AchievementId.PERFECT_ROUND -> "لا يُمس"
                AchievementId.COLOSSUS_SLAYER -> "قاهر العملاق"
                AchievementId.PHANTOM_BREAKER -> "كاسر المرايا"
                AchievementId.APEX_CHAMPION -> "بطل القمة"
                AchievementId.EXTREME_SURVIVOR -> "قاهر الصعوبة الأسطورية"
                AchievementId.ARSENAL_COLLECTOR -> "الترسانة الكاملة"
            }
        },
        achievementDesc = { ach ->
            when (ach) {
                AchievementId.FIRST_VICTORY -> "اربح أول مباراة لك."
                AchievementId.COMBO_SAVANT -> "نفّذ كومبو من 5 ضربات أو أكثر في مباراة حقيقية."
                AchievementId.PERFECT_ROUND -> "افز بجولة دون تلقي أي ضرر."
                AchievementId.COLOSSUS_SLAYER -> "اهزم زعيم المرحلة 3: العملاق جبار."
                AchievementId.PHANTOM_BREAKER -> "اهزم زعيم المرحلة 6: إمبراطورة الوهم ليلى."
                AchievementId.APEX_CHAMPION -> "اهزم الزعيم الأخير في المرحلة 8: شوغون أوميغا."
                AchievementId.EXTREME_SURVIVOR -> "اربح مباراة على مستوى الصعوبة الأسطوري."
                AchievementId.ARSENAL_COLLECTOR -> "افتح جميع المقاتلين الأربعة."
            }
        },
        missionTitle = { m ->
            when (m) {
                MissionId.WIN_MATCHES -> "بطل الحلبة"
                MissionId.LAND_COMBOS -> "خبير السلاسل"
                MissionId.DEFEAT_BOSSES -> "صائد الزعماء"
                MissionId.WIN_EXTREME_OR_HARD -> "تحدي النخبة"
            }
        },
        missionDesc = { m ->
            when (m) {
                MissionId.WIN_MATCHES -> "اربح 3 مباريات في الحملة أو القتال السريع."
                MissionId.LAND_COMBOS -> "حقّق كومبو من 3 ضربات في 5 مباريات."
                MissionId.DEFEAT_BOSSES -> "اهزم زعيمين في مراحل الحملة."
                MissionId.WIN_EXTREME_OR_HARD -> "اربح مباراتين على المستوى الصعب أو الأسطوري."
            }
        }
    )

    fun forLanguage(lang: AppLanguage): LocalizedStrings = when (lang) {
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> arabic
    }
}

val LocalStrings = compositionLocalOf { GameStrings.english }
