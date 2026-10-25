package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CloudMatchmakingRepository
import com.example.data.firebase.CloudModpackDocument
import com.example.data.firebase.MatchLobbyDocument
import com.example.data.local.LocalModAndPresetRepository
import com.example.data.local.SavedSkirmishPresetEntity
import com.example.engine.lod.Camera3DState
import com.example.engine.lod.GraphicsConfig
import com.example.engine.lod.GraphicsPreset
import com.example.engine.lod.LodFrameTelemetry
import com.example.engine.logging.ConsoleLogEntry
import com.example.engine.logging.EngineConsoleLogger
import com.example.engine.logging.LogCategory
import com.example.engine.logging.LogSeverity
import com.example.engine.modding.ApiExportGenerator
import com.example.engine.modding.CoreRuleHooks
import com.example.engine.modding.ModDefinition
import com.example.engine.modding.ModHookRegistry
import com.example.engine.modding.ModpackBundle
import com.example.engine.model.BottomDeckTab
import com.example.engine.model.CitizenAutomationConfig
import com.example.engine.model.CitizenPriorityMode
import com.example.engine.model.CivilizationId
import com.example.engine.model.FogOfWarMode
import com.example.engine.model.HistoricalAge
import com.example.engine.model.HudCollapseState
import com.example.engine.model.LeftDockTab
import com.example.engine.model.LibraryTrack
import com.example.engine.model.MilitaryAutomationConfig
import com.example.engine.model.MilitaryStance
import com.example.engine.model.ResourceTrackingState
import com.example.engine.model.ResourceType
import com.example.engine.model.RightDrawerTab
import com.example.engine.model.RtsCatalogRegistry
import com.example.engine.model.SpectatorModeState
import com.example.engine.pool.ObjectPoolManager
import com.example.engine.pool.PoolStatsSnapshot
import com.example.engine.simulation.RtsSimulationEngine
import com.example.engine.simulation.SimBuildingInstance
import com.example.engine.simulation.WorldSimulationSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class PrimaryAppScreen(val title: String) {
    BATTLEFIELD_3D("3D Battlefield"),
    MATCHMAKING_HUB("Matchmaking Command"),
    MOD_AND_MODPACK_STUDIO("Mod & Modpack Studio"),
    COMPLETE_API_EXPORT("Complete API Export")
}

sealed interface CloudUiState<out T> {
    data object Unauthenticated : CloudUiState<Nothing>
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

class CrownDominionViewModel(
    private val localRepository: LocalModAndPresetRepository,
    private val cloudRepository: CloudMatchmakingRepository
) : ViewModel() {

    private val simEngine = RtsSimulationEngine()
    private var simulationLoopJob: Job? = null
    private var cloudPublicLobbiesJob: Job? = null
    private var cloudMyModpacksJob: Job? = null

    private val _currentScreen = MutableStateFlow(PrimaryAppScreen.BATTLEFIELD_3D)
    val currentScreen: StateFlow<PrimaryAppScreen> = _currentScreen.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // 3D Camera & Touch Selection Mode
    private val _cameraState = MutableStateFlow(
        Camera3DState(
            targetX = 56f,
            targetZ = 184f,
            distance = 72f,
            yawDegrees = 38f,
            pitchDegrees = 52f
        )
    )
    val cameraState: StateFlow<Camera3DState> = _cameraState.asStateFlow()

    private val _isMarqueeBoxSelectEnabled = MutableStateFlow(false)
    val isMarqueeBoxSelectEnabled: StateFlow<Boolean> = _isMarqueeBoxSelectEnabled.asStateFlow()

    private val _pendingPlacementBuildingId = MutableStateFlow<String?>(null)
    val pendingPlacementBuildingId: StateFlow<String?> = _pendingPlacementBuildingId.asStateFlow()

    // Collapsible & Combined HUD State
    private val _hudCollapseState = MutableStateFlow(HudCollapseState())
    val hudCollapseState: StateFlow<HudCollapseState> = _hudCollapseState.asStateFlow()

    // Graphics & Adjustable LOD Distances
    private val _graphicsConfig = MutableStateFlow(GraphicsConfig.fromPreset(GraphicsPreset.BALANCED_TACTICAL))
    val graphicsConfig: StateFlow<GraphicsConfig> = _graphicsConfig.asStateFlow()

    private val _lodTelemetry = MutableStateFlow(LodFrameTelemetry())
    val lodTelemetry: StateFlow<LodFrameTelemetry> = _lodTelemetry.asStateFlow()

    // Citizen & Military Automation Toggles
    private val _citizenAutomation = MutableStateFlow(CitizenAutomationConfig())
    val citizenAutomation: StateFlow<CitizenAutomationConfig> = _citizenAutomation.asStateFlow()

    private val _militaryAutomation = MutableStateFlow(MilitaryAutomationConfig())
    val militaryAutomation: StateFlow<MilitaryAutomationConfig> = _militaryAutomation.asStateFlow()

    // Spectator Mode State
    private val _spectatorState = MutableStateFlow(SpectatorModeState())
    val spectatorState: StateFlow<SpectatorModeState> = _spectatorState.asStateFlow()

    // Stock Resource Map-Tracking State
    private val _resourceTrackingState = MutableStateFlow(ResourceTrackingState())
    val resourceTrackingState: StateFlow<ResourceTrackingState> = _resourceTrackingState.asStateFlow()

    // World Simulation State
    private val _worldState = MutableStateFlow(
        simEngine.createInitialWorld(
            mapBiome = "Mediterranean Basin",
            playerCiv = CivilizationId.ROMANS,
            aiCount = 3,
            startAge = HistoricalAge.ANCIENT
        )
    )
    val worldState: StateFlow<WorldSimulationSnapshot> = _worldState.asStateFlow()

    // Local Room Flows
    val installedMods: StateFlow<List<ModDefinition>> = localRepository.allModsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ModHookRegistry.defaultBuiltInMods
        )

    val installedModpacks: StateFlow<List<ModpackBundle>> = localRepository.allModpacksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ModHookRegistry.defaultBuiltInModpacks
        )

    val skirmishPresets: StateFlow<List<SavedSkirmishPresetEntity>> = localRepository.skirmishPresetsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Online Cloud Matchmaking & Cloud Modpacks (Auth-Gated)
    private val _publicCloudLobbies = MutableStateFlow<CloudUiState<List<MatchLobbyDocument>>>(CloudUiState.Unauthenticated)
    val publicCloudLobbies: StateFlow<CloudUiState<List<MatchLobbyDocument>>> = _publicCloudLobbies.asStateFlow()

    private val _userCloudModpacks = MutableStateFlow<CloudUiState<List<CloudModpackDocument>>>(CloudUiState.Unauthenticated)
    val userCloudModpacks: StateFlow<CloudUiState<List<CloudModpackDocument>>> = _userCloudModpacks.asStateFlow()

    // Live Engine Console Logs & Object Pool Telemetry
    val consoleLogs: StateFlow<List<ConsoleLogEntry>> = EngineConsoleLogger.logs
    val objectPoolTelemetry: StateFlow<List<PoolStatsSnapshot>> = ObjectPoolManager.telemetry
    val activeCombinedHooks: StateFlow<CoreRuleHooks> = ModHookRegistry.activeCombinedHooks
    val activeModConflicts: StateFlow<List<String>> = ModHookRegistry.activeModConflicts

    init {
        viewModelScope.launch {
            localRepository.seedDefaultsIfEmpty()
        }
        viewModelScope.launch {
            installedMods.collect { mods ->
                ModHookRegistry.recomputeCombinedHooks(mods)
            }
        }
        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        simulationLoopJob?.cancel()
        simulationLoopJob = viewModelScope.launch {
            var tickCounter = 0
            while (isActive) {
                delay(50L) // 20Hz simulation tick
                tickCounter++
                if (tickCounter % 10 == 0) {
                    _lodTelemetry.value = com.example.engine.lod.LodRenderEngine.latestFrameTelemetry
                }
                val spec = _spectatorState.value
                if (!spec.isPaused) {
                    val effectiveDt = 0.05f * spec.playbackSpeedMultiplier
                    _worldState.value = simEngine.stepSimulation(
                        current = _worldState.value,
                        dtSeconds = effectiveDt,
                        citizenAuto = _citizenAutomation.value,
                        militaryAuto = _militaryAutomation.value,
                        aiControlsPlayer = spec.aiControlsHumanNation || spec.isSpectatorActive
                    )
                    // Keep resource tracker pulse animated and sites refreshed
                    val tracking = _resourceTrackingState.value
                    if (tracking.trackedResource != null) {
                        val refreshedSites = _worldState.value.getActiveGatheringSitesForResource(
                            resourceType = tracking.trackedResource,
                            nationId = spec.observedNationId
                        )
                        _resourceTrackingState.value = tracking.copy(
                            activeSites = refreshedSites,
                            highlightPulsePhase = (tracking.highlightPulsePhase + 0.08f) % 1.0f
                        )
                    }
                    if (spec.freeCameraOrbitAutoRotate) {
                        _cameraState.value = _cameraState.value.copy(
                            yawDegrees = (_cameraState.value.yawDegrees + 0.35f) % 360f
                        )
                    }
                }
            }
        }
    }

    // Auth-gated Firestore listener management
    fun onAuthStateChanged(authenticatedUserId: String?) {
        cloudPublicLobbiesJob?.cancel()
        cloudMyModpacksJob?.cancel()

        if (authenticatedUserId == null) {
            _publicCloudLobbies.value = CloudUiState.Unauthenticated
            _userCloudModpacks.value = CloudUiState.Unauthenticated
            return
        }

        _publicCloudLobbies.value = CloudUiState.Loading
        cloudPublicLobbiesJob = viewModelScope.launch {
            cloudRepository.observePublicLobbies()
                .map<List<MatchLobbyDocument>, CloudUiState<List<MatchLobbyDocument>>> {
                    CloudUiState.Success(it)
                }
                .catch { error ->
                    Log.w("CrownVM", "Error observing public lobbies", error)
                    emit(CloudUiState.Error(error.localizedMessage ?: "Failed to load online lobbies"))
                }
                .collect { state ->
                    _publicCloudLobbies.value = state
                }
        }

        _userCloudModpacks.value = CloudUiState.Loading
        cloudMyModpacksJob = viewModelScope.launch {
            cloudRepository.observeUserCloudModpacks(authenticatedUserId)
                .map<List<CloudModpackDocument>, CloudUiState<List<CloudModpackDocument>>> {
                    CloudUiState.Success(it)
                }
                .catch { error ->
                    Log.w("CrownVM", "Error observing cloud modpacks", error)
                    emit(CloudUiState.Error(error.localizedMessage ?: "Failed to load cloud modpacks"))
                }
                .collect { state ->
                    _userCloudModpacks.value = state
                }
        }
    }

    // Screen Navigation & Fullscreen
    fun navigateToScreen(screen: PrimaryAppScreen) {
        _currentScreen.value = screen
    }

    fun toggleFullscreen() {
        val next = !_isFullscreen.value
        _isFullscreen.value = next
        EngineConsoleLogger.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            message = "Fullscreen Immersive Mode ${if (next) "ENABLED" else "DISABLED"}."
        )
        postBanner("Fullscreen mode ${if (next) "enabled" else "disabled"}.")
    }

    fun postBanner(message: String) {
        _statusBannerMessage.value = message
    }

    fun clearBanner() {
        _statusBannerMessage.value = null
    }

    // Collapsible & Combined HUD Controls
    fun toggleTopRibbonCollapse() {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            topResourceRibbonCollapsed = !_hudCollapseState.value.topResourceRibbonCollapsed
        )
    }

    fun toggleLeftDockCollapse() {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            leftEmpireDockCollapsed = !_hudCollapseState.value.leftEmpireDockCollapsed
        )
    }

    fun toggleBottomDeckCollapse() {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            bottomCommandDeckCollapsed = !_hudCollapseState.value.bottomCommandDeckCollapsed
        )
    }

    fun toggleRightDrawerCollapse() {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            rightTacticalDrawerCollapsed = !_hudCollapseState.value.rightTacticalDrawerCollapsed
        )
    }

    fun toggleResourceTrackerBannerCollapse() {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            resourceTrackerBannerCollapsed = !_hudCollapseState.value.resourceTrackerBannerCollapsed
        )
    }

    fun toggleSpectatorTelemetryCollapse() {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            spectatorTelemetryCollapsed = !_hudCollapseState.value.spectatorTelemetryCollapsed
        )
    }

    fun toggleCollapseAllHud() {
        val current = _hudCollapseState.value
        _hudCollapseState.value = if (current.areAllMainPanelsCollapsed) {
            current.expandDefaultTactical()
        } else {
            current.collapseAll()
        }
    }

    fun selectLeftDockTab(tab: LeftDockTab) {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            leftEmpireDockCollapsed = false,
            activeLeftTab = tab
        )
    }

    fun selectBottomDeckTab(tab: BottomDeckTab) {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            bottomCommandDeckCollapsed = false,
            activeBottomTab = tab
        )
    }

    fun selectRightDrawerTab(tab: RightDrawerTab) {
        _hudCollapseState.value = _hudCollapseState.value.copy(
            rightTacticalDrawerCollapsed = false,
            activeRightTab = tab
        )
    }

    // Stock Resource Click -> Interactive Map-Tracking Feature
    fun onStockResourceClicked(resourceType: ResourceType) {
        val nationId = _spectatorState.value.observedNationId
        val sites = _worldState.value.getActiveGatheringSitesForResource(resourceType, nationId)
        val currentTracking = _resourceTrackingState.value

        val nextFocusIndex = if (currentTracking.trackedResource == resourceType && sites.isNotEmpty()) {
            (currentTracking.focusedSiteIndex + 1) % sites.size
        } else {
            0
        }

        _resourceTrackingState.value = ResourceTrackingState(
            trackedResource = resourceType,
            activeSites = sites,
            focusedSiteIndex = nextFocusIndex,
            highlightPulsePhase = 0f
        )
        _hudCollapseState.value = _hudCollapseState.value.copy(
            resourceTrackerBannerCollapsed = false
        )

        val focusedSite = sites.getOrNull(nextFocusIndex)
        if (focusedSite != null) {
            focusCameraOnWorldPoint(focusedSite.worldX, focusedSite.worldZ)
            EngineConsoleLogger.log(
                category = LogCategory.RESOURCE,
                severity = LogSeverity.INFO,
                message = "Tracking ${resourceType.displayName} site [${nextFocusIndex + 1}/${sites.size}]: ${focusedSite.siteName} at (${focusedSite.worldX.toInt()}, ${focusedSite.worldZ.toInt()}) yielding +${focusedSite.yieldPerMinute}/m."
            )
            postBanner(
                "Tracking ${resourceType.displayName} (${nextFocusIndex + 1}/${sites.size}): ${focusedSite.siteName} (+${focusedSite.yieldPerMinute}/m)"
            )
        } else {
            postBanner("No active ${resourceType.displayName} gathering sites yet.")
        }
    }

    fun cycleNextTrackedResourceSite() {
        val current = _resourceTrackingState.value
        val res = current.trackedResource ?: return
        onStockResourceClicked(res)
    }

    fun clearResourceTracking() {
        _resourceTrackingState.value = ResourceTrackingState(trackedResource = null)
    }

    // 3D Camera & Selection Controls
    fun panCamera(deltaWorldX: Float, deltaWorldZ: Float) {
        val cam = _cameraState.value
        _cameraState.value = cam.copy(
            targetX = (cam.targetX + deltaWorldX).coerceIn(12f, 228f),
            targetZ = (cam.targetZ + deltaWorldZ).coerceIn(12f, 228f)
        )
    }

    fun zoomAndOrbitCamera(zoomFactor: Float, deltaYawDeg: Float, deltaPitchDeg: Float = 0f) {
        val cam = _cameraState.value
        _cameraState.value = cam.copy(
            distance = (cam.distance / zoomFactor.coerceIn(0.75f, 1.35f)).coerceIn(24f, 235f),
            yawDegrees = (cam.yawDegrees + deltaYawDeg) % 360f,
            pitchDegrees = (cam.pitchDegrees + deltaPitchDeg).coerceIn(28f, 78f)
        )
    }

    fun focusCameraOnWorldPoint(worldX: Float, worldZ: Float) {
        _cameraState.value = _cameraState.value.copy(
            targetX = worldX.coerceIn(12f, 228f),
            targetZ = worldZ.coerceIn(12f, 228f)
        )
    }

    fun toggleMarqueeBoxSelect() {
        _isMarqueeBoxSelectEnabled.value = !_isMarqueeBoxSelectEnabled.value
    }

    fun startBuildingPlacement(buildingArchetypeId: String?) {
        _pendingPlacementBuildingId.value = buildingArchetypeId
        if (buildingArchetypeId != null) {
            val arch = RtsCatalogRegistry.findBuilding(buildingArchetypeId)
            postBanner("Tap on the 3D terrain inside your national borders to construct ${arch.name}.")
        }
    }

    fun onWorldTap(worldX: Float, worldZ: Float, isDoubleTap: Boolean = false) {
        val snap = _worldState.value
        val activeNationId = _spectatorState.value.observedNationId

        // 1. If a building placement is pending, construct it at tapped coordinates
        val pendingBldId = _pendingPlacementBuildingId.value
        if (pendingBldId != null) {
            constructBuildingAt(pendingBldId, worldX, worldZ)
            _pendingPlacementBuildingId.value = null
            return
        }

        // 2. Check if user tapped a unit
        val tappedUnit = snap.units.minByOrNull { hypot(it.worldX - worldX, it.worldZ - worldZ) }
            ?.takeIf { hypot(it.worldX - worldX, it.worldZ - worldZ) <= 8.5f }

        if (tappedUnit != null && tappedUnit.nationId == activeNationId) {
            val updatedUnits = snap.units.map { u ->
                val selectMatch = if (isDoubleTap) {
                    u.nationId == activeNationId && u.archetypeId == tappedUnit.archetypeId
                } else {
                    u.entityId == tappedUnit.entityId
                }
                u.copy(isSelected = selectMatch)
            }
            val updatedBuildings = snap.buildings.map { it.copy(isSelected = false) }
            _worldState.value = snap.copy(units = updatedUnits, buildings = updatedBuildings)
            return
        }

        // 3. Check if user tapped a building
        val tappedBuilding = snap.buildings.minByOrNull { hypot(it.worldX - worldX, it.worldZ - worldZ) }
            ?.takeIf { hypot(it.worldX - worldX, it.worldZ - worldZ) <= 10.5f }

        if (tappedBuilding != null && tappedBuilding.nationId == activeNationId) {
            val updatedBuildings = snap.buildings.map { b ->
                b.copy(isSelected = b.buildingId == tappedBuilding.buildingId)
            }
            val updatedUnits = snap.units.map { it.copy(isSelected = false) }
            _worldState.value = snap.copy(units = updatedUnits, buildings = updatedBuildings)
            return
        }

        // 4. Otherwise, if player has selected units, issue move/attack order to tapped world point
        val selectedUnits = snap.units.filter { it.isSelected && it.nationId == activeNationId }
        if (selectedUnits.isNotEmpty()) {
            val updatedUnits = snap.units.mapIndexed { idx, u ->
                if (u.isSelected && u.nationId == activeNationId) {
                    val spreadAngle = (idx % 8) * 0.785f
                    val spreadRadius = if (selectedUnits.size > 1) 3.2f else 0f
                    u.copy(
                        targetX = (worldX + cos(spreadAngle) * spreadRadius).coerceIn(10f, 230f),
                        targetZ = (worldZ + sin(spreadAngle) * spreadRadius).coerceIn(10f, 230f)
                    )
                } else {
                    u
                }
            }
            _worldState.value = snap.copy(units = updatedUnits)
            EngineConsoleLogger.log(
                category = LogCategory.SIMULATION,
                severity = LogSeverity.DEBUG,
                message = "Issued move/engagement order for ${selectedUnits.size} unit(s) to (${worldX.toInt()}, ${worldZ.toInt()})."
            )
        }
    }

    fun onMarqueeBoxSelect(minWorldX: Float, minWorldZ: Float, maxWorldX: Float, maxWorldZ: Float) {
        val snap = _worldState.value
        val activeNationId = _spectatorState.value.observedNationId
        val updatedUnits = snap.units.map { u ->
            val inside = u.nationId == activeNationId &&
                u.worldX in minWorldX..maxWorldX &&
                u.worldZ in minWorldZ..maxWorldZ
            u.copy(isSelected = inside)
        }
        val count = updatedUnits.count { it.isSelected }
        _worldState.value = snap.copy(
            units = updatedUnits,
            buildings = snap.buildings.map { it.copy(isSelected = false) }
        )
        if (count > 0) {
            postBanner("Selected $count units in tactical marquee box.")
        }
    }

    fun selectAllMilitaryUnits() {
        val snap = _worldState.value
        val activeNationId = _spectatorState.value.observedNationId
        val updatedUnits = snap.units.map { u ->
            val arch = RtsCatalogRegistry.findUnit(u.archetypeId)
            u.copy(isSelected = u.nationId == activeNationId && arch.category.name != "CITIZEN")
        }
        _worldState.value = snap.copy(units = updatedUnits)
        val count = updatedUnits.count { it.isSelected }
        postBanner("Selected all $count military combat units.")
    }

    fun setSelectedUnitsStance(stance: MilitaryStance) {
        val snap = _worldState.value
        val updatedUnits = snap.units.map { u ->
            if (u.isSelected) u.copy(stance = stance) else u
        }
        _worldState.value = snap.copy(units = updatedUnits)
        postBanner("Updated unit formation stance to ${stance.label}.")
    }

    // Economy, Construction, Recruitment, Library Research & Age Advancement
    fun constructBuildingAt(buildingArchetypeId: String, worldX: Float, worldZ: Float) {
        val snap = _worldState.value
        val nation = snap.humanNation()
        val arch = ModHookRegistry.evaluateBuildingArchetype(
            RtsCatalogRegistry.findBuilding(buildingArchetypeId)
        )

        val currentTimber = nation.stocks[ResourceType.TIMBER] ?: 0
        val currentWealth = nation.stocks[ResourceType.WEALTH] ?: 0
        val currentMetal = nation.stocks[ResourceType.METAL] ?: 0
        val currentKnowledge = nation.stocks[ResourceType.KNOWLEDGE] ?: 0

        if (currentTimber < arch.timberCost ||
            currentWealth < arch.wealthCost ||
            currentMetal < arch.metalCost ||
            currentKnowledge < arch.knowledgeCost
        ) {
            postBanner("Insufficient resources to construct ${arch.name}!")
            return
        }

        val updatedStocks = nation.stocks.toMutableMap().apply {
            put(ResourceType.TIMBER, (currentTimber - arch.timberCost).coerceAtLeast(0))
            put(ResourceType.WEALTH, (currentWealth - arch.wealthCost).coerceAtLeast(0))
            put(ResourceType.METAL, (currentMetal - arch.metalCost).coerceAtLeast(0))
            put(ResourceType.KNOWLEDGE, (currentKnowledge - arch.knowledgeCost).coerceAtLeast(0))
        }

        val initialWorkerIds = if (arch.gatherResourceType != null) {
            listOf(simEngine.nextId(), simEngine.nextId())
        } else {
            emptyList()
        }

        val newBld = SimBuildingInstance(
            buildingId = simEngine.nextId(),
            nationId = nation.nationId,
            cityName = if (arch.id == "bld_city") "Nova Colonia ${snap.buildings.count { it.archetypeId == "bld_city" } + 1}" else "Roma Invicta",
            archetypeId = arch.id,
            worldX = worldX.coerceIn(14f, 226f),
            worldY = 0f,
            worldZ = worldZ.coerceIn(14f, 226f),
            currentHp = arch.maxHp,
            maxHp = arch.maxHp,
            isConstructed = true,
            buildProgressFraction = 1.0f,
            assignedGathererIds = initialWorkerIds,
            rareResourceBonusName = if (arch.gatherResourceType != null) "Imperial Survey Bonus (+15%)" else null
        )

        val updatedNations = snap.nations.map {
            if (it.nationId == nation.nationId) it.copy(stocks = updatedStocks) else it
        }
        _worldState.value = snap.copy(
            nations = updatedNations,
            buildings = snap.buildings + newBld
        )
        EngineConsoleLogger.log(
            category = LogCategory.SIMULATION,
            severity = LogSeverity.INFO,
            message = "Constructed ${arch.name} at (${worldX.toInt()}, ${worldZ.toInt()})."
        )
        postBanner("Constructed ${arch.name}!")
    }

    fun queueUnitProduction(unitArchetypeId: String) {
        val snap = _worldState.value
        val nation = snap.humanNation()
        val arch = ModHookRegistry.evaluateUnitArchetype(RtsCatalogRegistry.findUnit(unitArchetypeId))

        if (nation.populationCurrent >= nation.populationCap) {
            postBanner("Population Cap (${nation.populationCap}) reached! Research Military Library or build Cities.")
            return
        }

        val food = nation.stocks[ResourceType.FOOD] ?: 0
        val timber = nation.stocks[ResourceType.TIMBER] ?: 0
        val wealth = nation.stocks[ResourceType.WEALTH] ?: 0
        val metal = nation.stocks[ResourceType.METAL] ?: 0
        val oil = nation.stocks[ResourceType.OIL] ?: 0

        if (food < arch.foodCost || timber < arch.timberCost || wealth < arch.wealthCost || metal < arch.metalCost || oil < arch.oilCost) {
            postBanner("Insufficient resources to recruit ${arch.name}.")
            return
        }

        val updatedStocks = nation.stocks.toMutableMap().apply {
            put(ResourceType.FOOD, (food - arch.foodCost).coerceAtLeast(0))
            put(ResourceType.TIMBER, (timber - arch.timberCost).coerceAtLeast(0))
            put(ResourceType.WEALTH, (wealth - arch.wealthCost).coerceAtLeast(0))
            put(ResourceType.METAL, (metal - arch.metalCost).coerceAtLeast(0))
            put(ResourceType.OIL, (oil - arch.oilCost).coerceAtLeast(0))
        }

        // Find a friendly building to queue or spawn immediately near Capital
        val bldIdx = snap.buildings.indexOfFirst {
            it.nationId == nation.nationId && it.isConstructed && it.productionQueueUnitId == null
        }

        val updatedBuildings = snap.buildings.toMutableList()
        if (bldIdx >= 0) {
            updatedBuildings[bldIdx] = updatedBuildings[bldIdx].copy(
                productionQueueUnitId = arch.id,
                productionProgressFraction = 0.25f
            )
        }

        val updatedNations = snap.nations.map {
            if (it.nationId == nation.nationId) it.copy(stocks = updatedStocks) else it
        }
        _worldState.value = snap.copy(nations = updatedNations, buildings = updatedBuildings)
        EngineConsoleLogger.log(
            category = LogCategory.SIMULATION,
            severity = LogSeverity.INFO,
            message = "Queued recruitment of ${arch.name}."
        )
        postBanner("Recruiting ${arch.name}...")
    }

    fun researchLibraryTrack(track: LibraryTrack) {
        val snap = _worldState.value
        val nation = snap.humanNation()
        val currentLvl = nation.libraryLevels[track] ?: 1
        if (currentLvl >= 8) {
            postBanner("${track.title} is already at Maximum Level 8!")
            return
        }

        val scienceLvl = nation.libraryLevels[LibraryTrack.SCIENCE] ?: 1
        val scienceDiscount = (1.0f - (scienceLvl - 1) * 0.06f).coerceIn(0.5f, 1.0f)
        val hookMult = ModHookRegistry.activeCombinedHooks.value.libraryResearchCostMultiplier
        val knowledgeCost = (currentLvl * 95 * scienceDiscount * hookMult).toInt().coerceAtLeast(25)
        val wealthCost = (currentLvl * 55 * scienceDiscount * hookMult).toInt().coerceAtLeast(15)

        val currentKnowledge = nation.stocks[ResourceType.KNOWLEDGE] ?: 0
        val currentWealth = nation.stocks[ResourceType.WEALTH] ?: 0
        if (currentKnowledge < knowledgeCost || currentWealth < wealthCost) {
            postBanner("Need $knowledgeCost Knowledge & $wealthCost Wealth for ${track.title} Lv.${currentLvl + 1}.")
            return
        }

        val nextStocks = nation.stocks.toMutableMap().apply {
            put(ResourceType.KNOWLEDGE, currentKnowledge - knowledgeCost)
            put(ResourceType.WEALTH, currentWealth - wealthCost)
        }
        val nextLevels = nation.libraryLevels.toMutableMap().apply {
            put(track, currentLvl + 1)
        }

        val updatedNations = snap.nations.map {
            if (it.nationId == nation.nationId) {
                it.copy(stocks = nextStocks, libraryLevels = nextLevels)
            } else {
                it
            }
        }
        _worldState.value = snap.copy(nations = updatedNations)
        EngineConsoleLogger.log(
            category = LogCategory.SIMULATION,
            severity = LogSeverity.INFO,
            message = "Researched ${track.title} Level ${currentLvl + 1}/8."
        )
        postBanner("Researched ${track.title} Level ${currentLvl + 1}!")
    }

    fun advanceToNextHistoricalAge() {
        val snap = _worldState.value
        val nation = snap.humanNation()
        val nextAge = nation.currentAge.nextAge ?: run {
            postBanner("Your Empire has already reached the Information Age!")
            return
        }

        val totalLibLevels = nation.libraryLevels.values.sum()
        if (totalLibLevels < nextAge.requiredLibraryTotalLevels) {
            postBanner(
                "Requires ${nextAge.requiredLibraryTotalLevels} total Library levels (currently $totalLibLevels) to advance to ${nextAge.shortTitle}."
            )
            return
        }

        val (foodCost, knowledgeCost) = ModHookRegistry.evaluateAgeCosts(nextAge)
        val curFood = nation.stocks[ResourceType.FOOD] ?: 0
        val curKnow = nation.stocks[ResourceType.KNOWLEDGE] ?: 0

        if (curFood < foodCost || curKnow < knowledgeCost) {
            postBanner("Need $foodCost Food & $knowledgeCost Knowledge to advance to ${nextAge.title}.")
            return
        }

        val nextStocks = nation.stocks.toMutableMap().apply {
            put(ResourceType.FOOD, curFood - foodCost)
            put(ResourceType.KNOWLEDGE, curKnow - knowledgeCost)
        }

        val updatedNations = snap.nations.map {
            if (it.nationId == nation.nationId) {
                it.copy(currentAge = nextAge, stocks = nextStocks)
            } else {
                it
            }
        }
        _worldState.value = snap.copy(nations = updatedNations)
        EngineConsoleLogger.log(
            category = LogCategory.SIMULATION,
            severity = LogSeverity.INFO,
            message = "EMPIRE ADVANCED TO ${nextAge.title.uppercase()} (${nextAge.yearSpan})!"
        )
        postBanner("Advanced to ${nextAge.title}!")
    }

    // Citizen & Military Automation Mutators
    fun updateCitizenAutomation(transform: (CitizenAutomationConfig) -> CitizenAutomationConfig) {
        val updated = transform(_citizenAutomation.value)
        _citizenAutomation.value = updated
        EngineConsoleLogger.log(
            category = LogCategory.AUTOMATION,
            severity = LogSeverity.INFO,
            message = "Citizen Automation updated: enabled=${updated.enabled}, priority=${updated.priorityMode.label}."
        )
    }

    fun updateMilitaryAutomation(transform: (MilitaryAutomationConfig) -> MilitaryAutomationConfig) {
        val updated = transform(_militaryAutomation.value)
        _militaryAutomation.value = updated
        EngineConsoleLogger.log(
            category = LogCategory.AUTOMATION,
            severity = LogSeverity.INFO,
            message = "Military Automation updated: enabled=${updated.enabled}, stance=${updated.defaultStance.label}, autoRecruit=${updated.autoRecruitBalancedArmy}."
        )
    }

    // Spectator Mode Mutators
    fun updateSpectatorState(transform: (SpectatorModeState) -> SpectatorModeState) {
        val updated = transform(_spectatorState.value)
        _spectatorState.value = updated
        EngineConsoleLogger.log(
            category = LogCategory.SIMULATION,
            severity = LogSeverity.INFO,
            message = "Spectator Mode updated: active=${updated.isSpectatorActive}, fog=${updated.fogOfWarMode.label}, speed=${updated.playbackSpeedMultiplier}x."
        )
    }

    // Graphics & LOD Configuration Mutators
    fun updateGraphicsConfig(transform: (GraphicsConfig) -> GraphicsConfig) {
        val updated = transform(_graphicsConfig.value)
        // Enforce monotonic LOD distance thresholds: LOD0 < LOD1 < LOD2
        val safeLod0 = updated.lod0MaxDistance.coerceIn(15f, 130f)
        val safeLod1 = updated.lod1MaxDistance.coerceIn(safeLod0 + 15f, 210f)
        val safeLod2 = updated.lod2MaxDistance.coerceIn(safeLod1 + 20f, 300f)
        _graphicsConfig.value = updated.copy(
            lod0MaxDistance = safeLod0,
            lod1MaxDistance = safeLod1,
            lod2MaxDistance = safeLod2
        )
        EngineConsoleLogger.log(
            category = LogCategory.LOD,
            severity = LogSeverity.INFO,
            message = "Updated Graphics & LOD thresholds: LOD0<=${safeLod0.toInt()}m, LOD1<=${safeLod1.toInt()}m, LOD2<=${safeLod2.toInt()}m."
        )
    }

    fun reportLodFrameTelemetry(telemetry: LodFrameTelemetry) {
        _lodTelemetry.value = telemetry
    }

    // Matchmaking (Offline Skirmish & Online Firestore Lobbies)
    fun launchNewSkirmishMatch(
        mapBiome: String,
        playerCiv: CivilizationId,
        aiCount: Int,
        startAge: HistoricalAge,
        spectatorOnly: Boolean = false
    ) {
        _worldState.value = simEngine.createInitialWorld(
            mapBiome = mapBiome,
            playerCiv = playerCiv,
            aiCount = aiCount,
            startAge = startAge
        )
        _spectatorState.value = _spectatorState.value.copy(
            isSpectatorActive = spectatorOnly,
            fogOfWarMode = if (spectatorOnly) FogOfWarMode.REVEAL_ALL_SPECTATOR else FogOfWarMode.PLAYER_VISION_ONLY,
            aiControlsHumanNation = spectatorOnly
        )
        _currentScreen.value = PrimaryAppScreen.BATTLEFIELD_3D
        postBanner("Launched $mapBiome skirmish as ${playerCiv.displayName} (${startAge.shortTitle})!")
    }

    fun createOnlineLobby(
        hostName: String,
        lobbyTitle: String,
        mapBiome: String,
        startAge: HistoricalAge,
        endAge: HistoricalAge,
        maxPlayers: Int,
        visibility: String
    ) {
        viewModelScope.launch {
            val activePack = installedModpacks.value.firstOrNull { it.isActive }?.name
                ?: "Vanilla Imperial Standard"
            val draft = MatchLobbyDocument(
                hostName = hostName,
                lobbyTitle = lobbyTitle,
                mapBiome = mapBiome,
                startAge = startAge.name,
                endAge = endAge.name,
                maxPlayers = maxPlayers.toLong(),
                currentPlayers = 1L,
                activeModpackName = activePack,
                status = "waiting",
                visibility = visibility
            )
            val result = cloudRepository.createLobby(draft)
            result.onSuccess { lobbyId ->
                EngineConsoleLogger.log(
                    category = LogCategory.NETWORK,
                    severity = LogSeverity.INFO,
                    message = "Hosted online matchmaking lobby '$lobbyTitle' ($lobbyId)."
                )
                postBanner("Hosted online lobby: $lobbyTitle")
            }.onFailure { err ->
                postBanner("Failed to host lobby: ${err.localizedMessage ?: "Check sign-in status"}")
            }
        }
    }

    fun joinOnlineLobby(lobby: MatchLobbyDocument) {
        viewModelScope.launch {
            val result = cloudRepository.joinPublicLobby(
                lobbyId = lobby.id,
                currentPlayers = lobby.currentPlayers,
                maxPlayers = lobby.maxPlayers
            )
            result.onSuccess {
                val startAge = HistoricalAge.entries.firstOrNull { it.name == lobby.startAge } ?: HistoricalAge.ANCIENT
                launchNewSkirmishMatch(
                    mapBiome = lobby.mapBiome,
                    playerCiv = CivilizationId.ROMANS,
                    aiCount = (lobby.maxPlayers.toInt() - 1).coerceIn(1, 3),
                    startAge = startAge,
                    spectatorOnly = false
                )
            }.onFailure { err ->
                postBanner("Could not join lobby: ${err.localizedMessage}")
            }
        }
    }

    fun deleteOnlineLobby(lobbyId: String) {
        viewModelScope.launch {
            cloudRepository.deleteLobby(lobbyId)
        }
    }

    // Mod & Modpack Studio Operations
    fun saveOrUpdateMod(mod: ModDefinition) {
        viewModelScope.launch {
            localRepository.saveOrUpdateMod(mod)
            postBanner("Saved mod '${mod.name}' and applied hooks to live engine.")
        }
    }

    fun toggleModEnabled(mod: ModDefinition) {
        saveOrUpdateMod(mod.copy(isEnabled = !mod.isEnabled))
    }

    fun deleteMod(modId: String) {
        viewModelScope.launch {
            localRepository.deleteMod(modId)
            postBanner("Deleted mod.")
        }
    }

    fun activateModpack(pack: ModpackBundle, active: Boolean) {
        viewModelScope.launch {
            localRepository.saveOrUpdateModpack(pack.copy(isActive = active))
            postBanner("${if (active) "Activated" else "Deactivated"} Modpack '${pack.name}'.")
        }
    }

    fun importModOrModpackFromJson(rawJson: String): Result<ModpackBundle> {
        val parseResult = ModpackBundle.fromJsonString(rawJson)
        parseResult.onSuccess { bundle ->
            viewModelScope.launch {
                localRepository.saveOrUpdateModpack(bundle.copy(isActive = true))
                postBanner("Imported '${bundle.name}' with ${bundle.mods.size} mod(s)!")
            }
        }.onFailure { err ->
            EngineConsoleLogger.log(
                category = LogCategory.HOOK,
                severity = LogSeverity.ERROR,
                message = "Modpack JSON import failed: ${err.localizedMessage ?: err.message}"
            )
            postBanner("Invalid Mod/Modpack JSON: ${err.localizedMessage}")
        }
        return parseResult
    }

    fun syncModpackToCloud(pack: ModpackBundle, authorName: String) {
        viewModelScope.launch {
            val doc = CloudModpackDocument(
                name = pack.name,
                author = authorName.ifBlank { pack.author },
                version = pack.version,
                description = pack.description,
                modCount = pack.mods.size.toLong(),
                payloadJson = pack.toPrettyJson()
            )
            val result = cloudRepository.saveCloudModpack(doc)
            result.onSuccess {
                postBanner("Synced Modpack '${pack.name}' to Cloud Firestore!")
            }.onFailure { err ->
                postBanner("Cloud sync failed: ${err.localizedMessage}")
            }
        }
    }

    fun generateCompleteApiJson(): String =
        ApiExportGenerator.generateCompleteApiSpecificationJson(
            activeMods = installedMods.value,
            graphicsConfig = _graphicsConfig.value
        )

    fun generateCompleteApiMarkdown(): String =
        ApiExportGenerator.generateMarkdownDeveloperGuide(
            activeMods = installedMods.value,
            graphicsConfig = _graphicsConfig.value
        )
}
