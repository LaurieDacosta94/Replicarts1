package com.example.engine.model

enum class ResourceType(
    val code: String,
    val displayName: String,
    val shortLabel: String,
    val unlockedAgeIndex: Int,
    val gatherBuildingLabel: String,
    val colorHex: Long,
    val description: String
) {
    FOOD(
        code = "FOOD",
        displayName = "Food",
        shortLabel = "FOD",
        unlockedAgeIndex = 0,
        gatherBuildingLabel = "Farms & Granaries",
        colorHex = 0xFF48BB78,
        description = "Sustains Citizens, Infantry, Cavalry, and City growth."
    ),
    TIMBER(
        code = "TIMBER",
        displayName = "Timber",
        shortLabel = "TMB",
        unlockedAgeIndex = 0,
        gatherBuildingLabel = "Woodcutter Camps",
        colorHex = 0xFFC07A3E,
        description = "Constructs Buildings, Siege Engines, Ships, and Wonders."
    ),
    WEALTH(
        code = "WEALTH",
        displayName = "Wealth",
        shortLabel = "WLT",
        unlockedAgeIndex = 1,
        gatherBuildingLabel = "Markets & Caravans",
        colorHex = 0xFFECC94B,
        description = "Funds mercenary recruitment, espionage, mercenaries, and high-tier tech."
    ),
    METAL(
        code = "METAL",
        displayName = "Metal",
        shortLabel = "MTL",
        unlockedAgeIndex = 1,
        gatherBuildingLabel = "Mountain Mines & Smelters",
        colorHex = 0xFF90CDF4,
        description = "Forges heavy armor, fortifications, artillery, and industrial machinery."
    ),
    OIL(
        code = "OIL",
        displayName = "Oil",
        shortLabel = "OIL",
        unlockedAgeIndex = 5,
        gatherBuildingLabel = "Oil Wells & Refineries",
        colorHex = 0xFFB794F4,
        description = "Fuels Industrial-to-Information Age tanks, aircraft, and mechanized divisions."
    ),
    KNOWLEDGE(
        code = "KNOWLEDGE",
        displayName = "Knowledge",
        shortLabel = "KNW",
        unlockedAgeIndex = 1,
        gatherBuildingLabel = "Universities & Scholars",
        colorHex = 0xFF63B3ED,
        description = "Unlocks advanced Library tracks, Era advancements, and Global Wonders."
    )
}

data class ResourceStockState(
    val type: ResourceType,
    val currentAmount: Int,
    val ratePerMinute: Int,
    val commerceCap: Int,
    val assignedGatherers: Int,
    val maxGathererSlots: Int,
    val activeSiteCount: Int
)

data class ActiveGatheringSite(
    val siteId: Int,
    val resourceType: ResourceType,
    val siteName: String,
    val buildingName: String,
    val worldX: Float,
    val worldZ: Float,
    val assignedCitizens: Int,
    val maxCitizens: Int,
    val yieldPerMinute: Int,
    val rareResourceBonusName: String? = null,
    val isUnderAttack: Boolean = false
)

data class ResourceTrackingState(
    val trackedResource: ResourceType? = null,
    val activeSites: List<ActiveGatheringSite> = emptyList(),
    val focusedSiteIndex: Int = 0,
    val highlightPulsePhase: Float = 0f
) {
    val currentFocusedSite: ActiveGatheringSite?
        get() = if (activeSites.isNotEmpty()) {
            activeSites[focusedSiteIndex.coerceIn(0, activeSites.lastIndex)]
        } else {
            null
        }
}

enum class HistoricalAge(
    val index: Int,
    val title: String,
    val shortTitle: String,
    val yearSpan: String,
    val foodCost: Int,
    val knowledgeCost: Int,
    val requiredLibraryTotalLevels: Int,
    val borderRadiusBonus: Float,
    val summary: String
) {
    ANCIENT(
        index = 0,
        title = "Age I: Ancient Age",
        shortTitle = "Ancient",
        yearSpan = "3000 BCE – 500 BCE",
        foodCost = 0,
        knowledgeCost = 0,
        requiredLibraryTotalLevels = 0,
        borderRadiusBonus = 0f,
        summary = "Founding of first cities, foraging, woodcutting, Slingers and Hoplites."
    ),
    CLASSICAL(
        index = 1,
        title = "Age II: Classical Age",
        shortTitle = "Classical",
        yearSpan = "500 BCE – 500 CE",
        foodCost = 220,
        knowledgeCost = 0,
        requiredLibraryTotalLevels = 2,
        borderRadiusBonus = 5f,
        summary = "Unlocks Metal, Wealth, Knowledge, Universities, Markets, Catapults, and Classical Wonders."
    ),
    MEDIEVAL(
        index = 2,
        title = "Age III: Medieval Age",
        shortTitle = "Medieval",
        yearSpan = "500 CE – 1450 CE",
        foodCost = 420,
        knowledgeCost = 240,
        requiredLibraryTotalLevels = 5,
        borderRadiusBonus = 10f,
        summary = "Feudal castles, Heavy Knights, Pikemen, Trebuchets, and fortified national borders."
    ),
    GUNPOWDER(
        index = 3,
        title = "Age IV: Gunpowder Age",
        shortTitle = "Gunpowder",
        yearSpan = "1450 CE – 1700 CE",
        foodCost = 700,
        knowledgeCost = 480,
        requiredLibraryTotalLevels = 9,
        borderRadiusBonus = 16f,
        summary = "Arquebusiers, Bombard Cannons, Renaissance citadels, and global trade routes."
    ),
    ENLIGHTENMENT(
        index = 4,
        title = "Age V: Enlightenment Age",
        shortTitle = "Enlightenment",
        yearSpan = "1700 CE – 1850 CE",
        foodCost = 1050,
        knowledgeCost = 820,
        requiredLibraryTotalLevels = 13,
        borderRadiusBonus = 22f,
        summary = "Fusiliers, Field Cannon batteries, Imperial Cuirassiers, and Industrial precursors."
    ),
    INDUSTRIAL(
        index = 5,
        title = "Age VI: Industrial Age",
        shortTitle = "Industrial",
        yearSpan = "1850 CE – 1935 CE",
        foodCost = 1550,
        knowledgeCost = 1300,
        requiredLibraryTotalLevels = 18,
        borderRadiusBonus = 30f,
        summary = "Unlocks Oil resource, Oil Wells, Refineries, Early Tanks, Biplanes, and Dreadnoughts."
    ),
    MODERN(
        index = 6,
        title = "Age VII: Modern Age",
        shortTitle = "Modern",
        yearSpan = "1935 CE – 1985 CE",
        foodCost = 2200,
        knowledgeCost = 1950,
        requiredLibraryTotalLevels = 23,
        borderRadiusBonus = 38f,
        summary = "Main Battle Tanks, Jet Fighters, Rocket Artillery, Radar Arrays, and Missile Silos."
    ),
    INFORMATION(
        index = 7,
        title = "Age VIII: Information Age",
        shortTitle = "Information",
        yearSpan = "1985 CE – Future",
        foodCost = 3100,
        knowledgeCost = 2900,
        requiredLibraryTotalLevels = 28,
        borderRadiusBonus = 48f,
        summary = "Stealth Bombers, Advanced Armor, Global Satellite Reveal, and Instant Research."
    );

    val nextAge: HistoricalAge?
        get() = entries.getOrNull(index + 1)
}

enum class LibraryTrack(
    val title: String,
    val badgeCode: String,
    val colorHex: Long,
    val effectSummary: String
) {
    MILITARY(
        title = "Military Art",
        badgeCode = "MIL",
        colorHex = 0xFFE53E3E,
        effectSummary = "Increases Population Cap (+25/lvl) and unlocks advanced military unit upgrades."
    ),
    CIVIC(
        title = "Civic & Empire",
        badgeCode = "CIV",
        colorHex = 0xFFD69E2E,
        effectSummary = "Expands National Border Push (+6m/lvl), enemy attrition, and Max City Limit."
    ),
    COMMERCE(
        title = "Commerce & Trade",
        badgeCode = "COM",
        colorHex = 0xFF38A169,
        effectSummary = "Raises Resource Gather Rate Commerce Cap (+85/lvl) and Trade Caravan limit."
    ),
    SCIENCE(
        title = "Science & Lore",
        badgeCode = "SCI",
        colorHex = 0xFF3182CE,
        effectSummary = "Reduces all technology research costs (-8%/lvl) and increases Scholar output."
    )
}

enum class CivilizationId(
    val displayName: String,
    val powerTitle: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val uniqueUnitSummary: String,
    val nationalBonuses: List<String>
) {
    ROMANS(
        displayName = "Romans",
        powerTitle = "Power of Caesar",
        primaryColorHex = 0xFFB82929,
        accentColorHex = 0xFFD4AF37,
        uniqueUnitSummary = "Praetorian Legion -> Imperial Caesar Tank",
        nationalBonuses = listOf(
            "Forts expand national borders +35% further and cost 25% less",
            "Cities start with a free Barracks and generate +15 Wealth/min",
            "Heavy Infantry build 20% faster and deal +15% damage"
        )
    ),
    GREEKS(
        displayName = "Greeks",
        powerTitle = "Power of Philosophy",
        primaryColorHex = 0xFF2B6CB0,
        accentColorHex = 0xFFF7FAFC,
        uniqueUnitSummary = "Royal Companion Phalanx -> Strategos Armor",
        nationalBonuses = listOf(
            "Libraries and Universities cost 40% less Timber and build 50% faster",
            "Knowledge gather rate +25% from Classical Age onward",
            "Library technologies available one Era earlier"
        )
    ),
    EGYPTIANS(
        displayName = "Egyptians",
        powerTitle = "Power of the Nile",
        primaryColorHex = 0xFFD69E2E,
        accentColorHex = 0xFF319795,
        uniqueUnitSummary = "Nile War Chariot -> Mamluk Desert Armor",
        nationalBonuses = listOf(
            "Cities support 7 Farms instead of 5 and Farms cost 50% less Timber",
            "Wonders cost 25% less and can construct 2 Wonders per city",
            "+20% Food Commerce Cap across all Ages"
        )
    ),
    MONGOLS(
        displayName = "Mongols",
        powerTitle = "Power of the Horde",
        primaryColorHex = 0xFFC05621,
        accentColorHex = 0xFFF6AD55,
        uniqueUnitSummary = "Keshik Horse Archer -> Steppe Rocket Cavalry",
        nationalBonuses = listOf(
            "All Cavalry move 18% faster and cost 15% less Food",
            "Enemy attrition damage inside Mongol borders reduced by 50%",
            "Gain +150 Food & Wealth plunder upon capturing enemy buildings"
        )
    ),
    BRITISH(
        displayName = "British",
        powerTitle = "Power of the Empire",
        primaryColorHex = 0xFFC53030,
        accentColorHex = 0xFF4299E1,
        uniqueUnitSummary = "Yeoman Longbowman -> Highlander Rifleman -> Lancaster Bomber",
        nationalBonuses = listOf(
            "Commerce Cap +25% higher for all 6 resources",
            "Ranged foot units have +20% attack range and +12% fire rate",
            "Taxes & Market income generate +30% extra Wealth"
        )
    ),
    JAPANESE(
        displayName = "Japanese",
        powerTitle = "Power of Honor",
        primaryColorHex = 0xFF9B2C2C,
        accentColorHex = 0xFFFED7D7,
        uniqueUnitSummary = "Ashigaru & Samurai -> Zero Interceptor -> Type-90 MBT",
        nationalBonuses = listOf(
            "Barracks units cost 15% less per Era and deal +20% damage vs buildings",
            "Farms and Woodcutter Camps produce +15% Food and Timber",
            "Infantry heal slowly even outside friendly territory"
        )
    ),
    AZTECS(
        displayName = "Aztecs",
        powerTitle = "Power of Sacrifice",
        primaryColorHex = 0xFF2F855A,
        accentColorHex = 0xFFECC94B,
        uniqueUnitSummary = "Jaguar Sovereign Infantry -> Sun Eagle Strike Team",
        nationalBonuses = listOf(
            "Earn +25 Knowledge and +25 Wealth for every enemy unit defeated",
            "Enemy units suffer +45% stronger attrition inside Aztec borders",
            "Start with +1 free Military Library technology"
        )
    ),
    AMERICANS(
        displayName = "Americans",
        powerTitle = "Power of Innovation",
        primaryColorHex = 0xFF2C5282,
        accentColorHex = 0xFF63B3ED,
        uniqueUnitSummary = "Continental Marine -> M1A2 Abrams -> B-2 Spirit Stealth",
        nationalBonuses = listOf(
            "First Wonder constructed completes Instantly",
            "Each Citizen at a gathering site adds +2 Knowledge and +2 Wealth",
            "Modern & Information Age units cost 20% less Oil and Metal"
        )
    )
}

enum class UnitCategory(val label: String) {
    CITIZEN("Citizen & Logistics"),
    LIGHT_INFANTRY("Light & Ranged Infantry"),
    HEAVY_INFANTRY("Heavy & Anti-Cavalry Infantry"),
    CAVALRY("Cavalry & Mobile Strike"),
    SIEGE_ARTILLERY("Siege & Artillery"),
    ARMOR_MECHANIZED("Tanks & Mechanized"),
    AIR_POWER("Air Force & Strategic")
}

data class UnitArchetype(
    val id: String,
    val name: String,
    val category: UnitCategory,
    val minAge: HistoricalAge,
    val maxHp: Float,
    val attackDamage: Float,
    val attackRange: Float,
    val moveSpeed: Float,
    val buildTimeSeconds: Float,
    val foodCost: Int,
    val timberCost: Int,
    val wealthCost: Int,
    val metalCost: Int,
    val oilCost: Int,
    val isSupplyWagon: Boolean = false,
    val meshScale: Float = 1.0f,
    val description: String
)

enum class BuildingCategory(val label: String) {
    CIVIC("Civic & Borders"),
    ECONOMIC("Resource Gathering"),
    REFINERY("Economic Enhancers"),
    MILITARY("Military Production"),
    DEFENSE("Fortifications"),
    WONDER("Wonders of the World")
}

data class BuildingArchetype(
    val id: String,
    val name: String,
    val category: BuildingCategory,
    val minAge: HistoricalAge,
    val maxHp: Float,
    val timberCost: Int,
    val wealthCost: Int,
    val metalCost: Int,
    val oilCost: Int,
    val knowledgeCost: Int,
    val borderPushRadius: Float,
    val gatherResourceType: ResourceType? = null,
    val maxGatherers: Int = 0,
    val baseGatherRatePerWorker: Int = 0,
    val wonderBonusSummary: String? = null,
    val description: String
)

enum class MilitaryStance(val label: String, val shortCode: String, val description: String) {
    AGGRESSIVE("Aggressive Pursuit", "AGG", "Engage any enemy within sight and pursue retreating targets."),
    DEFENSIVE("Defensive Perimeter", "DEF", "Engage nearby threats and return to anchor position."),
    STAND_GROUND("Hold Formation", "HLD", "Hold exact position and fire only on enemies within weapon range."),
    RAID_ECONOMY("Prioritize Gatherers", "RAD", "Target enemy resource camps, trade caravans, and citizens first.")
}

enum class CitizenPriorityMode(val label: String) {
    BALANCED_ECONOMY("Balanced All Resources"),
    LOWEST_STOCK_FIRST("Lowest Stock Resource First"),
    WAR_PRODUCTION("Prioritize Metal, Oil & Food"),
    AGE_RUSH_KNOWLEDGE("Prioritize Knowledge & Food")
}

data class CitizenAutomationConfig(
    val enabled: Boolean = true,
    val autoQueueCitizensAtCities: Boolean = true,
    val autoAssignIdleCitizens: Boolean = true,
    val priorityMode: CitizenPriorityMode = CitizenPriorityMode.LOWEST_STOCK_FIRST,
    val autoConstructGatheringCamps: Boolean = true,
    val autoRepairDamagedBuildings: Boolean = true,
    val autoEstablishCaravans: Boolean = true
)

data class MilitaryAutomationConfig(
    val enabled: Boolean = false,
    val defaultStance: MilitaryStance = MilitaryStance.DEFENSIVE,
    val autoRecruitBalancedArmy: Boolean = false,
    val autoScoutUnexploredTerrain: Boolean = true,
    val autoRetreatWoundedUnits: Boolean = true,
    val retreatHpThresholdFraction: Float = 0.28f,
    val autoAttachSupplyWagons: Boolean = true
)

enum class FogOfWarMode(val label: String) {
    PLAYER_VISION_ONLY("Active Commander Vision"),
    REVEAL_ALL_SPECTATOR("Full Map Reveal (No Fog)"),
    BORDER_TERRITORY_ONLY("National Borders Only")
}

data class NationTelemetryPoint(
    val timestampSeconds: Int,
    val nationId: Int,
    val totalEconomyRate: Int,
    val militaryPowerScore: Int,
    val territoryPercent: Int,
    val techScore: Int
)

data class SpectatorModeState(
    val isSpectatorActive: Boolean = false,
    val fogOfWarMode: FogOfWarMode = FogOfWarMode.PLAYER_VISION_ONLY,
    val observedNationId: Int = 0,
    val playbackSpeedMultiplier: Float = 1.0f,
    val isPaused: Boolean = false,
    val freeCameraOrbitAutoRotate: Boolean = false,
    val aiControlsHumanNation: Boolean = false,
    val showTelemetryOverlay: Boolean = false
)

enum class LeftDockTab(val label: String) {
    LIBRARY_TECH("Library & Ages"),
    WONDERS_EMPIRE("Wonders & Borders"),
    DIPLOMACY("Diplomacy")
}

enum class BottomDeckTab(val label: String) {
    INSPECTOR_ORDERS("Unit Orders"),
    BUILD_STRUCTURES("Construct"),
    TRAIN_FORCES("Recruit Army")
}

enum class RightDrawerTab(val label: String) {
    RADAR_MAP("3D Radar"),
    AUTOMATION("Automation"),
    LOD_POOL_GFX("LOD & Pools"),
    CONSOLE_LOGS("Console Logs")
}

data class HudCollapseState(
    val topResourceRibbonCollapsed: Boolean = false,
    val leftEmpireDockCollapsed: Boolean = true,
    val bottomCommandDeckCollapsed: Boolean = false,
    val rightTacticalDrawerCollapsed: Boolean = false,
    val resourceTrackerBannerCollapsed: Boolean = false,
    val spectatorTelemetryCollapsed: Boolean = false,
    val activeLeftTab: LeftDockTab = LeftDockTab.LIBRARY_TECH,
    val activeBottomTab: BottomDeckTab = BottomDeckTab.TRAIN_FORCES,
    val activeRightTab: RightDrawerTab = RightDrawerTab.RADAR_MAP
) {
    val areAllMainPanelsCollapsed: Boolean
        get() = topResourceRibbonCollapsed &&
            leftEmpireDockCollapsed &&
            bottomCommandDeckCollapsed &&
            rightTacticalDrawerCollapsed

    fun collapseAll(): HudCollapseState = copy(
        topResourceRibbonCollapsed = true,
        leftEmpireDockCollapsed = true,
        bottomCommandDeckCollapsed = true,
        rightTacticalDrawerCollapsed = true,
        resourceTrackerBannerCollapsed = true,
        spectatorTelemetryCollapsed = true
    )

    fun expandDefaultTactical(): HudCollapseState = copy(
        topResourceRibbonCollapsed = false,
        leftEmpireDockCollapsed = false,
        bottomCommandDeckCollapsed = false,
        rightTacticalDrawerCollapsed = false,
        resourceTrackerBannerCollapsed = false
    )
}
