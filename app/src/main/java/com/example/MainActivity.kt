package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.CloudMatchmakingRepository
import com.example.data.firebase.GoogleAuthManager
import com.example.data.firebase.authStateFlow
import com.example.data.local.CrownDominionDatabase
import com.example.data.local.LocalModAndPresetRepository
import com.example.engine.logging.EngineConsoleLogger
import com.example.engine.logging.LogCategory
import com.example.engine.logging.LogSeverity
import com.example.engine.pool.ObjectPoolManager
import com.example.ui.components.CombinedBottomCommandDeck
import com.example.ui.components.CombinedLeftEmpireAndTechDock
import com.example.ui.components.CombinedRightTacticalAndEngineDrawer
import com.example.ui.components.ResourceMapTrackerBanner
import com.example.ui.components.SpectatorControlAndTelemetryOverlay
import com.example.ui.components.TopImperialResourceRibbon
import com.example.ui.screens.CompleteApiExportScreen
import com.example.ui.screens.MatchmakingHubScreen
import com.example.ui.screens.ModAndModpackStudioScreen
import com.example.ui.theme.CrownDominionTheme
import com.example.ui.theme.DeepTacticalParchment
import com.example.ui.theme.IlluminatedIvory
import com.example.ui.theme.ImperialBurnishedGold
import com.example.ui.viewmodel.CrownDominionViewModel
import com.example.ui.viewmodel.PrimaryAppScreen
import com.example.ui.viewport.Battlefield3DViewport
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        enableEdgeToEdge()
        setContent {
            CrownDominionTheme {
                CrownDominionRootApp()
            }
        }
    }
}

@Composable
fun CrownDominionRootApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val viewModel: CrownDominionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                if (FirebaseApp.getApps(app).isEmpty()) {
                    FirebaseApp.initializeApp(app)
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val roomDb = CrownDominionDatabase.getInstance(app)
                CrownDominionViewModel(
                    localRepository = LocalModAndPresetRepository(roomDb.dao()),
                    cloudRepository = CloudMatchmakingRepository(firestore)
                )
            }
        }
    )

    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(currentUser?.uid) {
        viewModel.onAuthStateChanged(currentUser?.uid)
    }

    LaunchedEffect(Unit) {
        try {
            val credentialManager = CredentialManager.create(context)
            GoogleAuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { viewModel.onAuthStateChanged(Firebase.auth.currentUser?.uid) },
                onUnauthenticated = { viewModel.onAuthStateChanged(null) },
                scope = scope
            )
        } catch (_: Exception) {
            viewModel.onAuthStateChanged(null)
        }
    }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val worldState by viewModel.worldState.collectAsStateWithLifecycle()
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val hudCollapseState by viewModel.hudCollapseState.collectAsStateWithLifecycle()
    val graphicsConfig by viewModel.graphicsConfig.collectAsStateWithLifecycle()
    val lodTelemetry by viewModel.lodTelemetry.collectAsStateWithLifecycle()
    val poolTelemetry by viewModel.objectPoolTelemetry.collectAsStateWithLifecycle()
    val citizenAuto by viewModel.citizenAutomation.collectAsStateWithLifecycle()
    val militaryAuto by viewModel.militaryAutomation.collectAsStateWithLifecycle()
    val spectatorState by viewModel.spectatorState.collectAsStateWithLifecycle()
    val resourceTracking by viewModel.resourceTrackingState.collectAsStateWithLifecycle()
    val isMarqueeBoxMode by viewModel.isMarqueeBoxSelectEnabled.collectAsStateWithLifecycle()
    val pendingBuildingId by viewModel.pendingPlacementBuildingId.collectAsStateWithLifecycle()

    val installedMods by viewModel.installedMods.collectAsStateWithLifecycle()
    val installedModpacks by viewModel.installedModpacks.collectAsStateWithLifecycle()
    val activeHooks by viewModel.activeCombinedHooks.collectAsStateWithLifecycle()
    val activeConflicts by viewModel.activeModConflicts.collectAsStateWithLifecycle()
    val skirmishPresets by viewModel.skirmishPresets.collectAsStateWithLifecycle()
    val publicCloudLobbies by viewModel.publicCloudLobbies.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()

    // Synchronize Android System Bars with Fullscreen Toggle
    LaunchedEffect(isFullscreen) {
        val window = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Auto-dismiss transient status banner after 3.5s
    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3500L)
            viewModel.clearBanner()
        }
    }

    val copyToClipboard: (String, String) -> Unit = remember(context) {
        { label, text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
            EngineConsoleLogger.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.INFO,
                message = "Copied '$label' (${text.length} chars) to system clipboard."
            )
            viewModel.postBanner("Copied $label to clipboard (${text.length} chars)!")
        }
    }

    val shareText: (String, String) -> Unit = remember(context) {
        { title, text ->
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    when (currentScreen) {
        PrimaryAppScreen.MATCHMAKING_HUB -> {
            MatchmakingHubScreen(
                currentUser = currentUser,
                publicLobbiesState = publicCloudLobbies,
                skirmishPresets = skirmishPresets,
                onBackToBattlefield = { viewModel.navigateToScreen(PrimaryAppScreen.BATTLEFIELD_3D) },
                onLaunchOfflineSkirmish = { biome, civ, aiCount, startAge, specOnly ->
                    viewModel.launchNewSkirmishMatch(biome, civ, aiCount, startAge, specOnly)
                },
                onCreateOnlineLobby = { host, title, biome, sAge, eAge, maxP, vis ->
                    viewModel.createOnlineLobby(host, title, biome, sAge, eAge, maxP, vis)
                },
                onJoinOnlineLobby = { lobby -> viewModel.joinOnlineLobby(lobby) },
                onDeleteOnlineLobby = { id -> viewModel.deleteOnlineLobby(id) },
                onAuthChanged = { viewModel.onAuthStateChanged(Firebase.auth.currentUser?.uid) },
                onShowMessage = { msg -> viewModel.postBanner(msg) }
            )
        }

        PrimaryAppScreen.MOD_AND_MODPACK_STUDIO -> {
            ModAndModpackStudioScreen(
                installedMods = installedMods,
                installedModpacks = installedModpacks,
                activeHooks = activeHooks,
                activeConflicts = activeConflicts,
                currentUser = currentUser,
                onBackToBattlefield = { viewModel.navigateToScreen(PrimaryAppScreen.BATTLEFIELD_3D) },
                onToggleModEnabled = { mod -> viewModel.toggleModEnabled(mod) },
                onSaveCustomMod = { mod -> viewModel.saveOrUpdateMod(mod) },
                onDeleteMod = { id -> viewModel.deleteMod(id) },
                onActivateModpack = { pack, active -> viewModel.activateModpack(pack, active) },
                onImportModOrModpackJson = { raw -> viewModel.importModOrModpackFromJson(raw) },
                onSyncModpackToCloud = { pack, author -> viewModel.syncModpackToCloud(pack, author) },
                onCopyTextToClipboard = copyToClipboard,
                onShareText = shareText
            )
        }

        PrimaryAppScreen.COMPLETE_API_EXPORT -> {
            val apiJson = remember(installedMods, graphicsConfig) {
                viewModel.generateCompleteApiJson()
            }
            val apiMarkdown = remember(installedMods, graphicsConfig) {
                viewModel.generateCompleteApiMarkdown()
            }
            val allLogsText = remember(consoleLogs) {
                EngineConsoleLogger.exportAllLogsText()
            }
            CompleteApiExportScreen(
                apiJsonSpec = apiJson,
                apiMarkdownGuide = apiMarkdown,
                consoleLogsText = allLogsText,
                onBackToBattlefield = { viewModel.navigateToScreen(PrimaryAppScreen.BATTLEFIELD_3D) },
                onCopyTextToClipboard = copyToClipboard,
                onShareText = shareText
            )
        }

        PrimaryAppScreen.BATTLEFIELD_3D -> {
            val resourceStocks = remember(worldState, spectatorState.observedNationId) {
                worldState.computeResourceStockStates(spectatorState.observedNationId)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Full-Bleed 3D RTS Viewport
                Battlefield3DViewport(
                    worldState = worldState,
                    cameraState = cameraState,
                    graphicsConfig = graphicsConfig,
                    resourceTracking = resourceTracking,
                    spectatorState = spectatorState,
                    isMarqueeBoxMode = isMarqueeBoxMode,
                    onPanCamera = { dx, dz -> viewModel.panCamera(dx, dz) },
                    onZoomAndOrbit = { z, yaw, pitch -> viewModel.zoomAndOrbitCamera(z, yaw, pitch) },
                    onWorldTap = { wx, wz, dbl -> viewModel.onWorldTap(wx, wz, dbl) },
                    onMarqueeSelect = { x1, z1, x2, z2 -> viewModel.onMarqueeBoxSelect(x1, z1, x2, z2) },
                    onReportLodTelemetry = { t -> viewModel.reportLodFrameTelemetry(t) },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Collapsible & Combined Imperial Parchment & Bronze HUD Overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Section: Resource & Epoch Ribbon + Active Resource Map-Tracking Banner + Spectator Overlay
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TopImperialResourceRibbon(
                            worldState = worldState,
                            resourceStocks = resourceStocks,
                            trackedResource = resourceTracking.trackedResource,
                            hudCollapseState = hudCollapseState,
                            isFullscreen = isFullscreen,
                            onStockResourceClick = { res -> viewModel.onStockResourceClicked(res) },
                            onToggleTopRibbonCollapse = { viewModel.toggleTopRibbonCollapse() },
                            onToggleCollapseAllHud = { viewModel.toggleCollapseAllHud() },
                            onToggleFullscreen = { viewModel.toggleFullscreen() },
                            onAdvanceAgeClick = { viewModel.advanceToNextHistoricalAge() },
                            onNavigateScreen = { scr -> viewModel.navigateToScreen(scr) }
                        )

                        // Interactive Map-Tracking Banner when any Stock Resource is clicked
                        if (resourceTracking.trackedResource != null) {
                            ResourceMapTrackerBanner(
                                resourceTracking = resourceTracking,
                                isCollapsed = hudCollapseState.resourceTrackerBannerCollapsed,
                                onToggleCollapse = { viewModel.toggleResourceTrackerBannerCollapse() },
                                onCycleNextSite = { viewModel.cycleNextTrackedResourceSite() },
                                onFocusSite = { wx, wz -> viewModel.focusCameraOnWorldPoint(wx, wz) },
                                onDismissTracking = { viewModel.clearResourceTracking() }
                            )
                        }

                        // Dedicated Spectator Mode Controls & Telemetry Graph
                        if (spectatorState.isSpectatorActive) {
                            SpectatorControlAndTelemetryOverlay(
                                worldState = worldState,
                                spectatorState = spectatorState,
                                isCollapsed = hudCollapseState.spectatorTelemetryCollapsed,
                                onToggleCollapse = { viewModel.toggleSpectatorTelemetryCollapse() },
                                onUpdateSpectator = { fn -> viewModel.updateSpectatorState(fn) }
                            )
                        }
                    }

                    // Middle Section: Left Empire/Library Dock & Right Tactical Radar/Automation/LOD/Logs Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CombinedLeftEmpireAndTechDock(
                            worldState = worldState,
                            hudCollapseState = hudCollapseState,
                            onToggleCollapse = { viewModel.toggleLeftDockCollapse() },
                            onSelectTab = { tab -> viewModel.selectLeftDockTab(tab) },
                            onResearchTrack = { track -> viewModel.researchLibraryTrack(track) },
                            onStartWonderPlacement = { wonderId -> viewModel.startBuildingPlacement(wonderId) }
                        )

                        CombinedRightTacticalAndEngineDrawer(
                            worldState = worldState,
                            cameraState = cameraState,
                            resourceTracking = resourceTracking,
                            hudCollapseState = hudCollapseState,
                            citizenAuto = citizenAuto,
                            militaryAuto = militaryAuto,
                            graphicsConfig = graphicsConfig,
                            lodTelemetry = lodTelemetry,
                            poolTelemetry = poolTelemetry,
                            consoleLogs = consoleLogs,
                            spectatorState = spectatorState,
                            onToggleCollapse = { viewModel.toggleRightDrawerCollapse() },
                            onSelectTab = { tab -> viewModel.selectRightDrawerTab(tab) },
                            onFocusWorldPoint = { wx, wz -> viewModel.focusCameraOnWorldPoint(wx, wz) },
                            onUpdateCitizenAuto = { fn -> viewModel.updateCitizenAutomation(fn) },
                            onUpdateMilitaryAuto = { fn -> viewModel.updateMilitaryAutomation(fn) },
                            onUpdateGraphicsConfig = { fn -> viewModel.updateGraphicsConfig(fn) },
                            onUpdateSpectator = { fn -> viewModel.updateSpectatorState(fn) },
                            onPreWarmPools = {
                                ObjectPoolManager.preWarmAllPools()
                                viewModel.postBanner("Pre-warmed all 3D Object Pools (Zero-GC mode)!")
                            },
                            onCopyConsoleLogs = {
                                val logsText = EngineConsoleLogger.exportAllLogsText()
                                copyToClipboard("Engine Console Logs", logsText)
                            }
                        )
                    }

                    // Bottom Section: Combined Selection Inspector & Production Command Deck
                    CombinedBottomCommandDeck(
                        worldState = worldState,
                        hudCollapseState = hudCollapseState,
                        isMarqueeBoxMode = isMarqueeBoxMode,
                        pendingPlacementBuildingId = pendingBuildingId,
                        onToggleCollapse = { viewModel.toggleBottomDeckCollapse() },
                        onSelectTab = { tab -> viewModel.selectBottomDeckTab(tab) },
                        onToggleMarqueeBox = { viewModel.toggleMarqueeBoxSelect() },
                        onSelectAllMilitary = { viewModel.selectAllMilitaryUnits() },
                        onSetUnitStance = { stance -> viewModel.setSelectedUnitsStance(stance) },
                        onStartBuildingPlacement = { id -> viewModel.startBuildingPlacement(id) },
                        onQueueUnit = { id -> viewModel.queueUnitProduction(id) }
                    )
                }

                // Floating Status Toast Banner
                AnimatedVisibility(
                    visible = statusBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 86.dp)
                ) {
                    Surface(
                        color = DeepTacticalParchment.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, ImperialBurnishedGold),
                        modifier = Modifier.testTag("status_toast_banner")
                    ) {
                        Text(
                            text = statusBanner ?: "",
                            color = IlluminatedIvory,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
