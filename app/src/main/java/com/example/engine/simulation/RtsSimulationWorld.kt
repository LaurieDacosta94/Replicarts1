package com.example.engine.simulation

import com.example.engine.logging.EngineConsoleLogger
import com.example.engine.logging.LogCategory
import com.example.engine.logging.LogSeverity
import com.example.engine.modding.ModHookRegistry
import com.example.engine.model.ActiveGatheringSite
import com.example.engine.model.BuildingArchetype
import com.example.engine.model.CitizenAutomationConfig
import com.example.engine.model.CitizenPriorityMode
import com.example.engine.model.CivilizationId
import com.example.engine.model.HistoricalAge
import com.example.engine.model.LibraryTrack
import com.example.engine.model.MilitaryAutomationConfig
import com.example.engine.model.MilitaryStance
import com.example.engine.model.NationTelemetryPoint
import com.example.engine.model.ResourceStockState
import com.example.engine.model.ResourceType
import com.example.engine.model.RtsCatalogRegistry
import com.example.engine.model.UnitArchetype
import com.example.engine.model.UnitCategory
import com.example.engine.pool.ObjectPoolManager
import com.example.engine.pool.ProjectileKind
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class WorldResourceNode(
    val nodeId: Int,
    val resourceType: ResourceType,
    val label: String,
    val rareBonusLabel: String?,
    val worldX: Float,
    val worldZ: Float,
    val elevationY: Float = 0f,
    val richnessMultiplier: Float = 1.0f
)

data class SimUnitInstance(
    val entityId: Int,
    val nationId: Int,
    val archetypeId: String,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val targetX: Float,
    val targetZ: Float,
    val headingRad: Float,
    val currentHp: Float,
    val maxHp: Float,
    val stance: MilitaryStance = MilitaryStance.DEFENSIVE,
    val assignedBuildingId: Int? = null,
    val assignedResourceType: ResourceType? = null,
    val targetEnemyId: Int? = null,
    val attackCooldown: Float = 0f,
    val isSelected: Boolean = false,
    val isUnderAttrition: Boolean = false
)

data class SimBuildingInstance(
    val buildingId: Int,
    val nationId: Int,
    val cityName: String,
    val archetypeId: String,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val currentHp: Float,
    val maxHp: Float,
    val isConstructed: Boolean = true,
    val buildProgressFraction: Float = 1.0f,
    val assignedGathererIds: List<Int> = emptyList(),
    val rareResourceBonusName: String? = null,
    val productionQueueUnitId: String? = null,
    val productionProgressFraction: Float = 0f,
    val isSelected: Boolean = false
)

data class NationRuntimeState(
    val nationId: Int,
    val commanderName: String,
    val isHumanPlayer: Boolean,
    val civilization: CivilizationId,
    val currentAge: HistoricalAge,
    val libraryLevels: Map<LibraryTrack, Int>,
    val stocks: Map<ResourceType, Int>,
    val ratesPerMin: Map<ResourceType, Int>,
    val commerceCaps: Map<ResourceType, Int>,
    val populationCurrent: Int,
    val populationCap: Int,
    val territoryPercent: Int,
    val militaryScore: Int,
    val economyScore: Int
)

data class WorldSimulationSnapshot(
    val mapBiome: String = "Mediterranean Basin",
    val worldSize: Float = 240f,
    val elapsedSeconds: Int = 0,
    val nations: List<NationRuntimeState> = emptyList(),
    val units: List<SimUnitInstance> = emptyList(),
    val buildings: List<SimBuildingInstance> = emptyList(),
    val resourceNodes: List<WorldResourceNode> = emptyList(),
    val telemetryHistory: List<NationTelemetryPoint> = emptyList(),
    val activeBattleAlertText: String? = null
) {
    fun humanNation(): NationRuntimeState =
        nations.firstOrNull { it.isHumanPlayer } ?: nations.first()

    fun computeResourceStockStates(nationId: Int = 0): List<ResourceStockState> {
        val nation = nations.firstOrNull { it.nationId == nationId } ?: return emptyList()
        val nationBuildings = buildings.filter { it.nationId == nationId && it.isConstructed }

        return ResourceType.entries.map { resType ->
            val matchingBuildings = nationBuildings.filter { bld ->
                val arch = RtsCatalogRegistry.findBuilding(bld.archetypeId)
                arch.gatherResourceType == resType
            }
            val assignedWorkers = matchingBuildings.sumOf { it.assignedGathererIds.size }
            val maxSlots = matchingBuildings.sumOf { bld ->
                RtsCatalogRegistry.findBuilding(bld.archetypeId).maxGatherers
            }
            ResourceStockState(
                type = resType,
                currentAmount = nation.stocks[resType] ?: 0,
                ratePerMinute = nation.ratesPerMin[resType] ?: 0,
                commerceCap = nation.commerceCaps[resType] ?: 300,
                assignedGatherers = assignedWorkers,
                maxGathererSlots = maxSlots,
                activeSiteCount = matchingBuildings.size
            )
        }
    }

    fun getActiveGatheringSitesForResource(
        resourceType: ResourceType,
        nationId: Int = 0
    ): List<ActiveGatheringSite> {
        val nationBuildings = buildings.filter { it.nationId == nationId && it.isConstructed }
        val nation = nations.firstOrNull { it.nationId == nationId }
        val result = mutableListOf<ActiveGatheringSite>()

        for (bld in nationBuildings) {
            val arch = RtsCatalogRegistry.findBuilding(bld.archetypeId)
            if (arch.gatherResourceType == resourceType) {
                val workerCount = bld.assignedGathererIds.size.coerceAtLeast(1)
                val baseYield = workerCount * arch.baseGatherRatePerWorker
                val effectiveYield = ModHookRegistry.evaluateGatherRate(resourceType, baseYield)
                val enemyNearby = units.any { u ->
                    u.nationId != nationId && hypot(u.worldX - bld.worldX, u.worldZ - bld.worldZ) < 24f
                }
                result.add(
                    ActiveGatheringSite(
                        siteId = bld.buildingId,
                        resourceType = resourceType,
                        siteName = "${bld.cityName} ${arch.name}",
                        buildingName = arch.name,
                        worldX = bld.worldX,
                        worldZ = bld.worldZ,
                        assignedCitizens = workerCount,
                        maxCitizens = arch.maxGatherers.coerceAtLeast(1),
                        yieldPerMinute = effectiveYield,
                        rareResourceBonusName = bld.rareResourceBonusName,
                        isUnderAttack = enemyNearby
                    )
                )
            }
        }

        // Also include unexploited or nearby matching resource deposits if no camp is built yet
        if (result.isEmpty() && nation != null) {
            val matchingNodes = resourceNodes.filter { it.resourceType == resourceType }.take(3)
            for (node in matchingNodes) {
                result.add(
                    ActiveGatheringSite(
                        siteId = 10000 + node.nodeId,
                        resourceType = resourceType,
                        siteName = "${node.label} (Unclaimed Deposit)",
                        buildingName = resourceType.gatherBuildingLabel,
                        worldX = node.worldX,
                        worldZ = node.worldZ,
                        assignedCitizens = 0,
                        maxCitizens = 6,
                        yieldPerMinute = 0,
                        rareResourceBonusName = node.rareBonusLabel,
                        isUnderAttack = false
                    )
                )
            }
        }
        return result
    }
}

class RtsSimulationEngine {
    private var nextEntityId = 100
    private var tickAccumulatorSeconds = 0f
    private var resourceAccumulatorSeconds = 0f
    private var automationAccumulatorSeconds = 0f
    private var aiDecisionAccumulatorSeconds = 0f
    private var telemetryAccumulatorSeconds = 0f

    fun createInitialWorld(
        mapBiome: String = "Mediterranean Basin",
        playerCiv: CivilizationId = CivilizationId.ROMANS,
        aiCount: Int = 3,
        startAge: HistoricalAge = HistoricalAge.ANCIENT
    ): WorldSimulationSnapshot {
        nextEntityId = 100
        ObjectPoolManager.reclaimAllPools()

        val startMult = ModHookRegistry.activeCombinedHooks.value.startingResourcesMultiplier
        val baseStock = (520 * startMult).toInt()

        val allCivs = (listOf(playerCiv) + CivilizationId.entries.filter { it != playerCiv })
            .take((1 + aiCount).coerceIn(2, 4))

        val spawnCorners = listOf(
            Triple(56f, 184f, "Roma Invicta"),
            Triple(184f, 56f, "Athenai Citadel"),
            Triple(56f, 56f, "Memphis Delta"),
            Triple(184f, 184f, "Karakorum Steppe")
        )

        val resourceNodes = mutableListOf<WorldResourceNode>()
        var nodeSeq = 1

        val units = mutableListOf<SimUnitInstance>()
        val buildings = mutableListOf<SimBuildingInstance>()
        val nations = mutableListOf<NationRuntimeState>()

        for ((index, civ) in allCivs.withIndex()) {
            val (cx, cz, capitalName) = spawnCorners[index]
            val isHuman = index == 0

            // Resource deposits around each nation's capital
            resourceNodes.add(
                WorldResourceNode(
                    nodeId = nodeSeq++,
                    resourceType = ResourceType.TIMBER,
                    label = "$capitalName Cedar Forest",
                    rareBonusLabel = "Amber Resin (+15% Timber)",
                    worldX = (cx - 18f).coerceIn(16f, 224f),
                    worldZ = (cz - 14f).coerceIn(16f, 224f)
                )
            )
            resourceNodes.add(
                WorldResourceNode(
                    nodeId = nodeSeq++,
                    resourceType = ResourceType.METAL,
                    label = "$capitalName Iron Ridge",
                    rareBonusLabel = "Titanium Vein (+20% Metal & Wealth)",
                    worldX = (cx + 20f).coerceIn(16f, 224f),
                    worldZ = (cz - 16f).coerceIn(16f, 224f),
                    elevationY = 2.4f
                )
            )
            resourceNodes.add(
                WorldResourceNode(
                    nodeId = nodeSeq++,
                    resourceType = ResourceType.OIL,
                    label = "$capitalName Petroleum Basin",
                    rareBonusLabel = "High-Grade Crude (+25% Oil)",
                    worldX = (cx + 22f).coerceIn(16f, 224f),
                    worldZ = (cz + 18f).coerceIn(16f, 224f)
                )
            )

            // Capital City
            val cityBldId = nextEntityId++
            buildings.add(
                SimBuildingInstance(
                    buildingId = cityBldId,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_city",
                    worldX = cx,
                    worldY = 0f,
                    worldZ = cz,
                    currentHp = 2400f,
                    maxHp = 2400f
                )
            )

            // Grand Library
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_library",
                    worldX = cx - 9f,
                    worldY = 0f,
                    worldZ = cz + 8f,
                    currentHp = 1200f,
                    maxHp = 1200f
                )
            )

            // 2 Irrigated Farms (Food)
            val farm1WorkerId = nextEntityId++
            val farm2WorkerId = nextEntityId++
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_farm",
                    worldX = cx - 10f,
                    worldY = 0f,
                    worldZ = cz - 6f,
                    currentHp = 550f,
                    maxHp = 550f,
                    assignedGathererIds = listOf(farm1WorkerId),
                    rareResourceBonusName = "Fertile Alluvium (+10 Food)"
                )
            )
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_farm",
                    worldX = cx - 4f,
                    worldY = 0f,
                    worldZ = cz - 11f,
                    currentHp = 550f,
                    maxHp = 550f,
                    assignedGathererIds = listOf(farm2WorkerId)
                )
            )

            // Woodcutter's Camp (Timber)
            val lumberWorker1 = nextEntityId++
            val lumberWorker2 = nextEntityId++
            val woodCampId = nextEntityId++
            buildings.add(
                SimBuildingInstance(
                    buildingId = woodCampId,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_woodcutter",
                    worldX = cx - 16f,
                    worldY = 0f,
                    worldZ = cz - 12f,
                    currentHp = 750f,
                    maxHp = 750f,
                    assignedGathererIds = listOf(lumberWorker1, lumberWorker2),
                    rareResourceBonusName = "Amber Resin (+15% Timber)"
                )
            )

            // Mountain Ore Mine (Metal)
            val mineWorker1 = nextEntityId++
            val mineWorker2 = nextEntityId++
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_mine",
                    worldX = cx + 18f,
                    worldY = 1.6f,
                    worldZ = cz - 14f,
                    currentHp = 900f,
                    maxHp = 900f,
                    assignedGathererIds = listOf(mineWorker1, mineWorker2),
                    rareResourceBonusName = "Titanium Vein (+20% Metal)"
                )
            )

            // Imperial Market (Wealth)
            val marketWorker1 = nextEntityId++
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_market",
                    worldX = cx + 10f,
                    worldY = 0f,
                    worldZ = cz + 7f,
                    currentHp = 950f,
                    maxHp = 950f,
                    assignedGathererIds = listOf(marketWorker1),
                    rareResourceBonusName = "Silk Road Caravan (+20 Wealth)"
                )
            )

            // Royal University (Knowledge)
            val scholar1 = nextEntityId++
            val scholar2 = nextEntityId++
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_university",
                    worldX = cx + 2f,
                    worldY = 0f,
                    worldZ = cz + 13f,
                    currentHp = 1050f,
                    maxHp = 1050f,
                    assignedGathererIds = listOf(scholar1, scholar2),
                    rareResourceBonusName = "Classical Scrolls (+15 Knowledge)"
                )
            )

            // Oil Well (active immediately if starting Industrial+ or pre-built derrick site)
            val oilWorker1 = nextEntityId++
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_oil_well",
                    worldX = cx + 20f,
                    worldY = 0f,
                    worldZ = cz + 16f,
                    currentHp = 1100f,
                    maxHp = 1100f,
                    assignedGathererIds = listOf(oilWorker1),
                    rareResourceBonusName = "High-Grade Crude (+25% Oil)"
                )
            )

            // Infantry Barracks & Fort
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_barracks",
                    worldX = cx + 11f,
                    worldY = 0f,
                    worldZ = cz - 3f,
                    currentHp = 1150f,
                    maxHp = 1150f
                )
            )
            buildings.add(
                SimBuildingInstance(
                    buildingId = nextEntityId++,
                    nationId = index,
                    cityName = capitalName,
                    archetypeId = "bld_fort",
                    worldX = cx + (if (index % 2 == 0) 14f else -14f),
                    worldY = 0.5f,
                    worldZ = cz + (if (index < 2) -14f else 14f),
                    currentHp = 2200f,
                    maxHp = 2200f
                )
            )

            // Spawn Citizen & Scholar visual entities at their sites
            val workerPlacements = listOf(
                Triple(farm1WorkerId, cx - 10f, cz - 5f) to ResourceType.FOOD,
                Triple(farm2WorkerId, cx - 4f, cz - 10f) to ResourceType.FOOD,
                Triple(lumberWorker1, cx - 15f, cz - 11f) to ResourceType.TIMBER,
                Triple(lumberWorker2, cx - 17f, cz - 13f) to ResourceType.TIMBER,
                Triple(mineWorker1, cx + 17f, cz - 13f) to ResourceType.METAL,
                Triple(mineWorker2, cx + 19f, cz - 15f) to ResourceType.METAL,
                Triple(marketWorker1, cx + 10f, cz + 8f) to ResourceType.WEALTH,
                Triple(scholar1, cx + 1f, cz + 12f) to ResourceType.KNOWLEDGE,
                Triple(scholar2, cx + 3f, cz + 14f) to ResourceType.KNOWLEDGE,
                Triple(oilWorker1, cx + 19f, cz + 15f) to ResourceType.OIL
            )
            for ((placement, resType) in workerPlacements) {
                val (uid, wx, wz) = placement
                units.add(
                    SimUnitInstance(
                        entityId = uid,
                        nationId = index,
                        archetypeId = if (resType == ResourceType.KNOWLEDGE) "unit_scholar" else "unit_citizen",
                        worldX = wx,
                        worldY = 0f,
                        worldZ = wz,
                        targetX = wx,
                        targetZ = wz,
                        headingRad = 0.6f,
                        currentHp = 85f,
                        maxHp = 85f,
                        assignedResourceType = resType
                    )
                )
            }

            // Spawn initial standing garrison depending on starting Age
            val starterInfantryId = when {
                startAge.index >= HistoricalAge.INFORMATION.index -> "unit_shock_infantry"
                startAge.index >= HistoricalAge.INDUSTRIAL.index -> "unit_bolt_rifleman"
                startAge.index >= HistoricalAge.GUNPOWDER.index -> "unit_arquebusier"
                startAge.index >= HistoricalAge.CLASSICAL.index -> "unit_legionary"
                else -> "unit_hoplite"
            }
            val starterRangedOrArmorId = when {
                startAge.index >= HistoricalAge.MODERN.index -> "unit_main_battle_tank"
                startAge.index >= HistoricalAge.INDUSTRIAL.index -> "unit_landship_tank"
                startAge.index >= HistoricalAge.MEDIEVAL.index -> "unit_paladin_knight"
                else -> "unit_slinger"
            }

            val infArch = ModHookRegistry.evaluateUnitArchetype(RtsCatalogRegistry.findUnit(starterInfantryId))
            val cavArch = ModHookRegistry.evaluateUnitArchetype(RtsCatalogRegistry.findUnit(starterRangedOrArmorId))

            for (g in 0 until 4) {
                val ux = cx + (g - 1.5f) * 3.8f
                val uz = cz + (if (index == 0) -16f else 16f)
                units.add(
                    SimUnitInstance(
                        entityId = nextEntityId++,
                        nationId = index,
                        archetypeId = starterInfantryId,
                        worldX = ux,
                        worldY = 0f,
                        worldZ = uz,
                        targetX = ux,
                        targetZ = uz,
                        headingRad = if (index == 0) -0.78f else 2.35f,
                        currentHp = infArch.maxHp,
                        maxHp = infArch.maxHp
                    )
                )
            }
            for (g in 0 until 2) {
                val ux = cx + (g - 0.5f) * 5.0f
                val uz = cz + (if (index == 0) -21f else 21f)
                units.add(
                    SimUnitInstance(
                        entityId = nextEntityId++,
                        nationId = index,
                        archetypeId = starterRangedOrArmorId,
                        worldX = ux,
                        worldY = 0f,
                        worldZ = uz,
                        targetX = ux,
                        targetZ = uz,
                        headingRad = if (index == 0) -0.78f else 2.35f,
                        currentHp = cavArch.maxHp,
                        maxHp = cavArch.maxHp
                    )
                )
            }

            // Supply Wagon to demonstrate anti-attrition logistics
            val wagonArch = ModHookRegistry.evaluateUnitArchetype(RtsCatalogRegistry.findUnit("unit_supply_wagon"))
            units.add(
                SimUnitInstance(
                    entityId = nextEntityId++,
                    nationId = index,
                    archetypeId = "unit_supply_wagon",
                    worldX = cx + 6f,
                    worldY = 0f,
                    worldZ = cz + (if (index == 0) -18f else 18f),
                    targetX = cx + 6f,
                    targetZ = cz + (if (index == 0) -18f else 18f),
                    headingRad = 0f,
                    currentHp = wagonArch.maxHp,
                    maxHp = wagonArch.maxHp
                )
            )

            val initialLibLevel = (startAge.index + 1).coerceIn(1, 8)
            nations.add(
                NationRuntimeState(
                    nationId = index,
                    commanderName = if (isHuman) "Imperator (${civ.displayName})" else "AI ${civ.displayName}",
                    isHumanPlayer = isHuman,
                    civilization = civ,
                    currentAge = startAge,
                    libraryLevels = mapOf(
                        LibraryTrack.MILITARY to initialLibLevel,
                        LibraryTrack.CIVIC to initialLibLevel,
                        LibraryTrack.COMMERCE to initialLibLevel,
                        LibraryTrack.SCIENCE to initialLibLevel
                    ),
                    stocks = mapOf(
                        ResourceType.FOOD to baseStock,
                        ResourceType.TIMBER to baseStock,
                        ResourceType.WEALTH to (baseStock * 0.85f).toInt(),
                        ResourceType.METAL to (baseStock * 0.75f).toInt(),
                        ResourceType.OIL to (baseStock * 0.55f).toInt(),
                        ResourceType.KNOWLEDGE to (baseStock * 0.65f).toInt()
                    ),
                    ratesPerMin = mapOf(
                        ResourceType.FOOD to 140,
                        ResourceType.TIMBER to 135,
                        ResourceType.WEALTH to 110,
                        ResourceType.METAL to 105,
                        ResourceType.OIL to 75,
                        ResourceType.KNOWLEDGE to 95
                    ),
                    commerceCaps = mapOf(
                        ResourceType.FOOD to 350,
                        ResourceType.TIMBER to 350,
                        ResourceType.WEALTH to 350,
                        ResourceType.METAL to 350,
                        ResourceType.OIL to 350,
                        ResourceType.KNOWLEDGE to 350
                    ),
                    populationCurrent = 17,
                    populationCap = 125 + ModHookRegistry.activeCombinedHooks.value.populationCapFlatBonus,
                    territoryPercent = 100 / allCivs.size,
                    militaryScore = 420,
                    economyScore = 680
                )
            )
        }

        // Central contested Rare Resource deposits in the heart of the map
        resourceNodes.add(
            WorldResourceNode(
                nodeId = nodeSeq++,
                resourceType = ResourceType.WEALTH,
                label = "Central Oasis Diamond Deposit",
                rareBonusLabel = "Imperial Diamonds (+45 Wealth & +10% Commerce Cap)",
                worldX = 120f,
                worldZ = 120f,
                elevationY = 1.2f,
                richnessMultiplier = 1.6f
            )
        )
        resourceNodes.add(
            WorldResourceNode(
                nodeId = nodeSeq++,
                resourceType = ResourceType.KNOWLEDGE,
                label = "Ancient Astronomical Obelisk",
                rareBonusLabel = "Star Tablets (+35 Knowledge/min)",
                worldX = 104f,
                worldZ = 134f,
                elevationY = 1.8f,
                richnessMultiplier = 1.5f
            )
        )

        EngineConsoleLogger.log(
            category = LogCategory.SIMULATION,
            severity = LogSeverity.INFO,
            message = "Generated 3D World '$mapBiome' with ${nations.size} nations, ${buildings.size} structures, ${units.size} units, and ${resourceNodes.size} resource nodes."
        )

        return WorldSimulationSnapshot(
            mapBiome = mapBiome,
            worldSize = 240f,
            elapsedSeconds = 0,
            nations = nations,
            units = units,
            buildings = buildings,
            resourceNodes = resourceNodes
        )
    }

    fun stepSimulation(
        current: WorldSimulationSnapshot,
        dtSeconds: Float,
        citizenAuto: CitizenAutomationConfig,
        militaryAuto: MilitaryAutomationConfig,
        aiControlsPlayer: Boolean
    ): WorldSimulationSnapshot {
        if (dtSeconds <= 0f) return current

        tickAccumulatorSeconds += dtSeconds
        resourceAccumulatorSeconds += dtSeconds
        automationAccumulatorSeconds += dtSeconds
        aiDecisionAccumulatorSeconds += dtSeconds
        telemetryAccumulatorSeconds += dtSeconds

        val updatedBuildings = current.buildings.toMutableList()
        val updatedUnits = current.units.toMutableList()
        val updatedNations = current.nations.toMutableList()
        var battleAlert: String? = current.activeBattleAlertText

        // 1. Update pooled projectiles & particles
        stepPooledProjectilesAndParticles(dtSeconds, updatedUnits, updatedBuildings)

        // 2. Update building construction & unit training queues
        for (i in updatedBuildings.indices) {
            val bld = updatedBuildings[i]
            if (!bld.isConstructed) {
                val nextProg = (bld.buildProgressFraction + dtSeconds * 0.14f).coerceAtMost(1.0f)
                updatedBuildings[i] = bld.copy(
                    buildProgressFraction = nextProg,
                    isConstructed = nextProg >= 1.0f
                )
            } else if (bld.productionQueueUnitId != null) {
                val unitArch = ModHookRegistry.evaluateUnitArchetype(
                    RtsCatalogRegistry.findUnit(bld.productionQueueUnitId)
                )
                val step = dtSeconds / unitArch.buildTimeSeconds.coerceAtLeast(2f)
                val nextProg = bld.productionProgressFraction + step
                if (nextProg >= 1.0f) {
                    val spawnAngle = Random.nextFloat() * 6.28f
                    val spawnDist = 7f + Random.nextFloat() * 4f
                    val sx = (bld.worldX + cos(spawnAngle) * spawnDist).coerceIn(10f, 230f)
                    val sz = (bld.worldZ + sin(spawnAngle) * spawnDist).coerceIn(10f, 230f)
                    val spawnedId = nextEntityId++
                    val resAssigned = if (unitArch.id == "unit_scholar") {
                        ResourceType.KNOWLEDGE
                    } else if (unitArch.id == "unit_citizen") {
                        ResourceType.FOOD
                    } else {
                        null
                    }
                    updatedUnits.add(
                        SimUnitInstance(
                            entityId = spawnedId,
                            nationId = bld.nationId,
                            archetypeId = unitArch.id,
                            worldX = sx,
                            worldY = 0f,
                            worldZ = sz,
                            targetX = sx,
                            targetZ = sz,
                            headingRad = spawnAngle,
                            currentHp = unitArch.maxHp,
                            maxHp = unitArch.maxHp,
                            stance = if (bld.nationId == 0) militaryAuto.defaultStance else MilitaryStance.AGGRESSIVE,
                            assignedResourceType = resAssigned
                        )
                    )
                    updatedBuildings[i] = bld.copy(
                        productionQueueUnitId = null,
                        productionProgressFraction = 0f
                    )
                } else {
                    updatedBuildings[i] = bld.copy(productionProgressFraction = nextProg)
                }
            }
        }

        // 3. Move units, evaluate National Border Supply Attrition, and resolve Combat
        val aliveUnits = ArrayList<SimUnitInstance>(updatedUnits.size)
        for (unit in updatedUnits) {
            if (unit.currentHp <= 0f) continue
            val arch = ModHookRegistry.evaluateUnitArchetype(RtsCatalogRegistry.findUnit(unit.archetypeId))

            // Check if unit is inside hostile National Border without a friendly Supply Wagon nearby
            val hostileBorderOwner = findHostileBorderOwnerAt(
                worldX = unit.worldX,
                worldZ = unit.worldZ,
                unitNationId = unit.nationId,
                buildings = updatedBuildings,
                nations = updatedNations
            )
            val hasSupplyProtection = arch.isSupplyWagon || updatedUnits.any { ally ->
                ally.nationId == unit.nationId &&
                    ally.archetypeId == "unit_supply_wagon" &&
                    hypot(ally.worldX - unit.worldX, ally.worldZ - unit.worldZ) <= 28f
            }
            val underAttrition = hostileBorderOwner != null && !hasSupplyProtection
            val attritionDmg = if (underAttrition) {
                ModHookRegistry.evaluateAttritionDamage(3.2f) * dtSeconds
            } else {
                0f
            }

            // Friendly territory healing for garrisoned/retreated units
            val inFriendlyTerritory = isInsideFriendlyBorder(
                worldX = unit.worldX,
                worldZ = unit.worldZ,
                unitNationId = unit.nationId,
                buildings = updatedBuildings,
                nations = updatedNations
            )
            val healAmount = if (inFriendlyTerritory && unit.currentHp < unit.maxHp) {
                2.2f * dtSeconds
            } else {
                0f
            }

            var nextX = unit.worldX
            var nextZ = unit.worldZ
            var nextTargetX = unit.targetX
            var nextTargetZ = unit.targetZ
            var nextHeading = unit.headingRad
            var nextCooldown = (unit.attackCooldown - dtSeconds).coerceAtLeast(0f)

            // Military Automation: Auto-retreat critically wounded units back to nearest friendly City
            if (arch.category != UnitCategory.CITIZEN &&
                ((unit.nationId == 0 && militaryAuto.enabled && militaryAuto.autoRetreatWoundedUnits) || unit.nationId != 0) &&
                unit.currentHp / unit.maxHp < militaryAuto.retreatHpThresholdFraction
            ) {
                val homeCity = updatedBuildings.firstOrNull {
                    it.nationId == unit.nationId && it.archetypeId == "bld_city"
                }
                if (homeCity != null) {
                    nextTargetX = homeCity.worldX
                    nextTargetZ = homeCity.worldZ
                }
            }

            // Find nearest enemy unit or building within engagement distance
            var nearestEnemyUnit: SimUnitInstance? = null
            var nearestUnitDist = Float.MAX_VALUE
            if (arch.attackDamage > 0f) {
                val searchRadius = when (unit.stance) {
                    MilitaryStance.AGGRESSIVE, MilitaryStance.RAID_ECONOMY -> (arch.attackRange + 36f)
                    MilitaryStance.DEFENSIVE -> (arch.attackRange + 16f)
                    MilitaryStance.STAND_GROUND -> arch.attackRange
                }
                for (other in updatedUnits) {
                    if (other.nationId != unit.nationId && other.currentHp > 0f) {
                        val d = hypot(other.worldX - unit.worldX, other.worldZ - unit.worldZ)
                        if (d < nearestUnitDist && d <= searchRadius) {
                            nearestUnitDist = d
                            nearestEnemyUnit = other
                        }
                    }
                }
            }

            if (nearestEnemyUnit != null) {
                val enemy = nearestEnemyUnit
                nextHeading = atan2(enemy.worldZ - unit.worldZ, enemy.worldX - unit.worldX)
                if (nearestUnitDist <= arch.attackRange.coerceAtLeast(4.5f)) {
                    if (nextCooldown <= 0f) {
                        nextCooldown = 1.25f
                        spawnPooledAttackProjectile(unit, arch, enemy.worldX, enemy.worldZ, enemy.entityId)
                        if (unit.nationId == 0 || enemy.nationId == 0) {
                            battleAlert = "Battle joined near (${unit.worldX.toInt()}, ${unit.worldZ.toInt()})!"
                        }
                    }
                } else if (unit.stance != MilitaryStance.STAND_GROUND) {
                    val moveStep = arch.moveSpeed * dtSeconds
                    nextX = (unit.worldX + cos(nextHeading) * moveStep).coerceIn(8f, 232f)
                    nextZ = (unit.worldZ + sin(nextHeading) * moveStep).coerceIn(8f, 232f)
                }
            } else {
                // Move toward target waypoint
                val dx = nextTargetX - unit.worldX
                val dz = nextTargetZ - unit.worldZ
                val dist = hypot(dx, dz)
                if (dist > 1.2f) {
                    nextHeading = atan2(dz, dx)
                    val step = (arch.moveSpeed * dtSeconds).coerceAtMost(dist)
                    nextX = (unit.worldX + cos(nextHeading) * step).coerceIn(8f, 232f)
                    nextZ = (unit.worldZ + sin(nextHeading) * step).coerceIn(8f, 232f)
                } else if (arch.category == UnitCategory.CITIZEN && unit.assignedResourceType != null) {
                    // Subtle harvest work animation orbit
                    nextHeading += dtSeconds * 1.4f
                }
            }

            val nextHp = (unit.currentHp - attritionDmg + healAmount).coerceIn(0f, unit.maxHp)
            if (nextHp > 0f) {
                aliveUnits.add(
                    unit.copy(
                        worldX = nextX,
                        worldZ = nextZ,
                        targetX = nextTargetX,
                        targetZ = nextTargetZ,
                        headingRad = nextHeading,
                        currentHp = nextHp,
                        attackCooldown = nextCooldown,
                        isUnderAttrition = underAttrition
                    )
                )
            }
        }

        // 4. Resource Stock Accumulation every 1.0s
        if (resourceAccumulatorSeconds >= 1.0f) {
            val elapsedSec = resourceAccumulatorSeconds
            resourceAccumulatorSeconds = 0f

            for (nIdx in updatedNations.indices) {
                val nation = updatedNations[nIdx]
                val nationBlds = updatedBuildings.filter { it.nationId == nation.nationId && it.isConstructed }
                val nationUnits = aliveUnits.filter { it.nationId == nation.nationId }

                val commerceLvl = nation.libraryLevels[LibraryTrack.COMMERCE] ?: 1
                val militaryLvl = nation.libraryLevels[LibraryTrack.MILITARY] ?: 1
                val hasColossus = nationBlds.any { it.archetypeId == "wonder_colossus" }
                val hasSupercollider = nationBlds.any { it.archetypeId == "wonder_supercollider" }

                val baseCap = 260 + commerceLvl * 95 + (if (hasColossus) 120 else 0) + (if (hasSupercollider) 350 else 0)
                val effectiveCap = ModHookRegistry.evaluateCommerceCap(baseCap)

                val newStocks = nation.stocks.toMutableMap()
                val newRates = mutableMapOf<ResourceType, Int>()
                val newCaps = mutableMapOf<ResourceType, Int>()

                for (resType in ResourceType.entries) {
                    val gatherBlds = nationBlds.filter {
                        RtsCatalogRegistry.findBuilding(it.archetypeId).gatherResourceType == resType
                    }
                    val workerCount = gatherBlds.sumOf { it.assignedGathererIds.size.coerceAtLeast(1) }
                    val rawRatePerMin = gatherBlds.sumOf { bld ->
                        val bArch = RtsCatalogRegistry.findBuilding(bld.archetypeId)
                        val w = bld.assignedGathererIds.size.coerceAtLeast(1)
                        w * bArch.baseGatherRatePerWorker + (if (bld.rareResourceBonusName != null) 18 else 0)
                    } + 25 // Base city tribute

                    val hookedRate = ModHookRegistry.evaluateGatherRate(resType, rawRatePerMin)
                        .coerceAtMost(effectiveCap)
                    val deltaStock = ((hookedRate / 60f) * elapsedSec).toInt().coerceAtLeast(1)

                    newRates[resType] = hookedRate
                    newCaps[resType] = effectiveCap
                    newStocks[resType] = ((newStocks[resType] ?: 0) + deltaStock).coerceAtMost(99999)
                }

                val cityCount = nationBlds.count { it.archetypeId == "bld_city" }.coerceAtLeast(1)
                val popCap = 75 + militaryLvl * 25 + cityCount * 25 +
                    (if (hasColossus) 50 else 0) +
                    ModHookRegistry.activeCombinedHooks.value.populationCapFlatBonus

                val milUnits = nationUnits.count {
                    RtsCatalogRegistry.findUnit(it.archetypeId).category != UnitCategory.CITIZEN
                }

                updatedNations[nIdx] = nation.copy(
                    stocks = newStocks,
                    ratesPerMin = newRates,
                    commerceCaps = newCaps,
                    populationCurrent = nationUnits.size,
                    populationCap = popCap,
                    militaryScore = milUnits * 45 + nationBlds.count { it.archetypeId == "bld_fort" } * 90,
                    economyScore = newRates.values.sum()
                )
            }
        }

        // 5. Citizen & Military Automation AI Tick (Every 3.0s)
        if (automationAccumulatorSeconds >= 3.0f) {
            automationAccumulatorSeconds = 0f
            runAutomationAndAiControllers(
                nations = updatedNations,
                buildings = updatedBuildings,
                units = aliveUnits,
                citizenAuto = citizenAuto,
                militaryAuto = militaryAuto,
                aiControlsPlayer = aiControlsPlayer
            )
        }

        // 6. Spectator Telemetry History Snapshot (Every 5.0s)
        val updatedTelemetry = current.telemetryHistory.toMutableList()
        if (telemetryAccumulatorSeconds >= 5.0f) {
            telemetryAccumulatorSeconds = 0f
            val nextSec = current.elapsedSeconds + 5
            for (n in updatedNations) {
                updatedTelemetry.add(
                    NationTelemetryPoint(
                        timestampSeconds = nextSec,
                        nationId = n.nationId,
                        totalEconomyRate = n.ratesPerMin.values.sum(),
                        militaryPowerScore = n.militaryScore,
                        territoryPercent = n.territoryPercent,
                        techScore = n.libraryLevels.values.sum() * 25 + n.currentAge.index * 100
                    )
                )
            }
            while (updatedTelemetry.size > 160) {
                updatedTelemetry.removeAt(0)
            }
        }

        ObjectPoolManager.refreshTelemetry()

        return current.copy(
            elapsedSeconds = current.elapsedSeconds + (if (tickAccumulatorSeconds >= 1f) {
                val addSec = tickAccumulatorSeconds.toInt()
                tickAccumulatorSeconds -= addSec
                addSec
            } else 0),
            nations = updatedNations,
            units = aliveUnits,
            buildings = updatedBuildings,
            telemetryHistory = updatedTelemetry,
            activeBattleAlertText = battleAlert
        )
    }

    private fun spawnPooledAttackProjectile(
        attacker: SimUnitInstance,
        arch: UnitArchetype,
        targetX: Float,
        targetZ: Float,
        targetEntityId: Int
    ) {
        val proj = ObjectPoolManager.projectilePool.acquire()
        proj.sourceNationId = attacker.nationId
        proj.startX = attacker.worldX
        proj.startZ = attacker.worldZ
        proj.currentX = attacker.worldX
        proj.currentY = 1.6f
        proj.currentZ = attacker.worldZ
        proj.targetX = targetX
        proj.targetZ = targetZ
        proj.targetEntityId = targetEntityId
        proj.damage = arch.attackDamage
        proj.progress = 0f

        when (arch.category) {
            UnitCategory.SIEGE_ARTILLERY -> {
                proj.projectileKind = ProjectileKind.CANNONBALL
                proj.speed = 22f
                proj.arcHeight = 7.5f
                proj.splashRadius = 5.5f
                proj.colorArgb = 0xFFFF7043
            }
            UnitCategory.ARMOR_MECHANIZED -> {
                proj.projectileKind = ProjectileKind.TANK_SHELL
                proj.speed = 38f
                proj.arcHeight = 1.2f
                proj.splashRadius = 3.5f
                proj.colorArgb = 0xFFFFEE58
            }
            UnitCategory.AIR_POWER -> {
                proj.projectileKind = ProjectileKind.ARTILLERY_ROCKET
                proj.speed = 42f
                proj.arcHeight = 4.0f
                proj.splashRadius = 7.0f
                proj.colorArgb = 0xFFEF5350
            }
            else -> {
                proj.projectileKind = ProjectileKind.ARROW
                proj.speed = 28f
                proj.arcHeight = 3.2f
                proj.splashRadius = 0f
                proj.colorArgb = 0xFFFFD54F
            }
        }
    }

    private fun stepPooledProjectilesAndParticles(
        dtSeconds: Float,
        units: MutableList<SimUnitInstance>,
        buildings: MutableList<SimBuildingInstance>
    ) {
        val activeProjectiles = ObjectPoolManager.projectilePool.activeList()
        for (proj in activeProjectiles) {
            val totalDist = hypot(proj.targetX - proj.startX, proj.targetZ - proj.startZ).coerceAtLeast(1f)
            val stepProgress = (proj.speed * dtSeconds) / totalDist
            proj.progress += stepProgress
            if (proj.progress >= 1.0f) {
                // Impact! Apply damage to target unit or nearby enemies
                for (uIdx in units.indices) {
                    val u = units[uIdx]
                    if (u.nationId != proj.sourceNationId && u.currentHp > 0f) {
                        val d = hypot(u.worldX - proj.targetX, u.worldZ - proj.targetZ)
                        if (u.entityId == proj.targetEntityId || d <= (proj.splashRadius.coerceAtLeast(2.8f))) {
                            val newHp = (u.currentHp - proj.damage).coerceAtLeast(0f)
                            units[uIdx] = u.copy(currentHp = newHp)
                            break
                        }
                    }
                }
                // Spawn pooled impact particles
                for (p in 0 until 3) {
                    val part = ObjectPoolManager.particlePool.acquire()
                    val angle = Random.nextFloat() * 6.28f
                    part.worldX = proj.targetX
                    part.worldY = 1.0f
                    part.worldZ = proj.targetZ
                    part.velX = cos(angle) * 4.5f
                    part.velY = 3.5f + Random.nextFloat() * 2.5f
                    part.velZ = sin(angle) * 4.5f
                    part.maxLife = 0.55f
                    part.lifeRemaining = 0.55f
                    part.colorArgb = proj.colorArgb
                }
                ObjectPoolManager.projectilePool.release(proj)
            } else {
                val t = proj.progress
                proj.currentX = proj.startX + (proj.targetX - proj.startX) * t
                proj.currentZ = proj.startZ + (proj.targetZ - proj.startZ) * t
                proj.currentY = 1.2f + 4f * proj.arcHeight * t * (1f - t)
            }
        }

        val activeParticles = ObjectPoolManager.particlePool.activeList()
        for (part in activeParticles) {
            part.lifeRemaining -= dtSeconds
            if (part.lifeRemaining <= 0f) {
                ObjectPoolManager.particlePool.release(part)
            } else {
                part.worldX += part.velX * dtSeconds
                part.worldY = (part.worldY + part.velY * dtSeconds).coerceAtLeast(0f)
                part.worldZ += part.velZ * dtSeconds
                part.velY -= 9.8f * dtSeconds
            }
        }

        val activeTexts = ObjectPoolManager.combatTextPool.activeList()
        for (txt in activeTexts) {
            txt.lifeRemaining -= dtSeconds
            if (txt.lifeRemaining <= 0f) {
                ObjectPoolManager.combatTextPool.release(txt)
            } else {
                txt.worldY += 2.4f * dtSeconds
            }
        }
    }

    private fun runAutomationAndAiControllers(
        nations: MutableList<NationRuntimeState>,
        buildings: MutableList<SimBuildingInstance>,
        units: MutableList<SimUnitInstance>,
        citizenAuto: CitizenAutomationConfig,
        militaryAuto: MilitaryAutomationConfig,
        aiControlsPlayer: Boolean
    ) {
        for (nIdx in nations.indices) {
            val nation = nations[nIdx]
            val isAutomatedPlayer = nation.isHumanPlayer && (citizenAuto.enabled || militaryAuto.enabled || aiControlsPlayer)
            val isAiNation = !nation.isHumanPlayer

            if (!isAutomatedPlayer && !isAiNation) continue

            // Citizen Automation: Auto-assign idle citizens or boost lowest stock resource
            if ((nation.isHumanPlayer && citizenAuto.enabled && citizenAuto.autoAssignIdleCitizens) || isAiNation) {
                val targetRes = when (citizenAuto.priorityMode) {
                    CitizenPriorityMode.LOWEST_STOCK_FIRST -> {
                        nation.stocks.minByOrNull { it.value }?.key ?: ResourceType.FOOD
                    }
                    CitizenPriorityMode.WAR_PRODUCTION -> ResourceType.METAL
                    CitizenPriorityMode.AGE_RUSH_KNOWLEDGE -> ResourceType.KNOWLEDGE
                    CitizenPriorityMode.BALANCED_ECONOMY -> ResourceType.TIMBER
                }
                val bldIdx = buildings.indexOfFirst { b ->
                    b.nationId == nation.nationId &&
                        RtsCatalogRegistry.findBuilding(b.archetypeId).gatherResourceType == targetRes &&
                        b.assignedGathererIds.size < RtsCatalogRegistry.findBuilding(b.archetypeId).maxGatherers
                }
                if (bldIdx >= 0) {
                    val bld = buildings[bldIdx]
                    val newWorkerId = nextEntityId++
                    buildings[bldIdx] = bld.copy(
                        assignedGathererIds = bld.assignedGathererIds + newWorkerId
                    )
                }
            }

            // Citizen Automation: Auto-repair damaged structures
            if ((nation.isHumanPlayer && citizenAuto.enabled && citizenAuto.autoRepairDamagedBuildings) || isAiNation) {
                for (bIdx in buildings.indices) {
                    val b = buildings[bIdx]
                    if (b.nationId == nation.nationId && b.currentHp < b.maxHp) {
                        buildings[bIdx] = b.copy(
                            currentHp = (b.currentHp + 110f).coerceAtMost(b.maxHp)
                        )
                    }
                }
            }

            // Military Automation / AI Recruitment
            if ((nation.isHumanPlayer && (militaryAuto.enabled && militaryAuto.autoRecruitBalancedArmy || aiControlsPlayer)) || isAiNation) {
                val barracksIdx = buildings.indexOfFirst {
                    it.nationId == nation.nationId &&
                        it.isConstructed &&
                        it.productionQueueUnitId == null &&
                        (it.archetypeId == "bld_barracks" || it.archetypeId == "bld_stable")
                }
                if (barracksIdx >= 0 && nation.populationCurrent < nation.populationCap) {
                    val recruitUnitId = when {
                        nation.currentAge.index >= HistoricalAge.MODERN.index -> "unit_main_battle_tank"
                        nation.currentAge.index >= HistoricalAge.INDUSTRIAL.index -> "unit_landship_tank"
                        nation.currentAge.index >= HistoricalAge.GUNPOWDER.index -> "unit_arquebusier"
                        nation.currentAge.index >= HistoricalAge.MEDIEVAL.index -> "unit_paladin_knight"
                        nation.currentAge.index >= HistoricalAge.CLASSICAL.index -> "unit_legionary"
                        else -> "unit_hoplite"
                    }
                    buildings[barracksIdx] = buildings[barracksIdx].copy(
                        productionQueueUnitId = recruitUnitId,
                        productionProgressFraction = 0.1f
                    )
                }
            }

            // Military Automation: Auto-Scout / AI Patrol toward map center
            if ((nation.isHumanPlayer && militaryAuto.enabled && militaryAuto.autoScoutUnexploredTerrain) || isAiNation) {
                val idleCombatUnitIdx = units.indexOfFirst { u ->
                    u.nationId == nation.nationId &&
                        RtsCatalogRegistry.findUnit(u.archetypeId).category != UnitCategory.CITIZEN &&
                        hypot(u.targetX - u.worldX, u.targetZ - u.worldZ) < 2f
                }
                if (idleCombatUnitIdx >= 0) {
                    val u = units[idleCombatUnitIdx]
                    val patrolX = (120f + Random.nextFloat() * 60f - 30f).coerceIn(24f, 216f)
                    val patrolZ = (120f + Random.nextFloat() * 60f - 30f).coerceIn(24f, 216f)
                    units[idleCombatUnitIdx] = u.copy(targetX = patrolX, targetZ = patrolZ)
                }
            }
        }
    }

    fun computeBorderRadiusForBuilding(
        building: SimBuildingInstance,
        nation: NationRuntimeState?
    ): Float {
        val arch = ModHookRegistry.evaluateBuildingArchetype(
            RtsCatalogRegistry.findBuilding(building.archetypeId)
        )
        if (arch.borderPushRadius <= 0f) return 0f
        val civicLevel = nation?.libraryLevels?.get(LibraryTrack.CIVIC) ?: 1
        val ageBonus = nation?.currentAge?.borderRadiusBonus ?: 0f
        val romanBonus = if (nation?.civilization == CivilizationId.ROMANS && building.archetypeId == "bld_fort") 1.35f else 1.0f
        return (arch.borderPushRadius + civicLevel * 4.5f + ageBonus * 0.45f) * romanBonus
    }

    private fun findHostileBorderOwnerAt(
        worldX: Float,
        worldZ: Float,
        unitNationId: Int,
        buildings: List<SimBuildingInstance>,
        nations: List<NationRuntimeState>
    ): Int? {
        for (bld in buildings) {
            if (bld.nationId == unitNationId || !bld.isConstructed) continue
            val ownerNation = nations.firstOrNull { it.nationId == bld.nationId }
            val radius = computeBorderRadiusForBuilding(bld, ownerNation)
            if (radius > 0f && hypot(worldX - bld.worldX, worldZ - bld.worldZ) <= radius) {
                return bld.nationId
            }
        }
        return null
    }

    private fun isInsideFriendlyBorder(
        worldX: Float,
        worldZ: Float,
        unitNationId: Int,
        buildings: List<SimBuildingInstance>,
        nations: List<NationRuntimeState>
    ): Boolean {
        val ownerNation = nations.firstOrNull { it.nationId == unitNationId }
        for (bld in buildings) {
            if (bld.nationId != unitNationId || !bld.isConstructed) continue
            val radius = computeBorderRadiusForBuilding(bld, ownerNation)
            if (radius > 0f && hypot(worldX - bld.worldX, worldZ - bld.worldZ) <= radius) {
                return true
            }
        }
        return false
    }

    fun nextId(): Int = nextEntityId++
}
