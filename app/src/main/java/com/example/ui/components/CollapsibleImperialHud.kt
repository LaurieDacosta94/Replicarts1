package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.lod.Camera3DState
import com.example.engine.lod.GraphicsConfig
import com.example.engine.lod.GraphicsPreset
import com.example.engine.lod.LodFrameTelemetry
import com.example.engine.logging.ConsoleLogEntry
import com.example.engine.modding.ModHookRegistry
import com.example.engine.model.BottomDeckTab
import com.example.engine.model.CitizenAutomationConfig
import com.example.engine.model.CitizenPriorityMode
import com.example.engine.model.FogOfWarMode
import com.example.engine.model.HudCollapseState
import com.example.engine.model.LeftDockTab
import com.example.engine.model.LibraryTrack
import com.example.engine.model.MilitaryAutomationConfig
import com.example.engine.model.MilitaryStance
import com.example.engine.model.ResourceStockState
import com.example.engine.model.ResourceTrackingState
import com.example.engine.model.ResourceType
import com.example.engine.model.RightDrawerTab
import com.example.engine.model.RtsCatalogRegistry
import com.example.engine.model.SpectatorModeState
import com.example.engine.pool.ObjectPoolManager
import com.example.engine.pool.PoolStatsSnapshot
import com.example.engine.simulation.WorldSimulationSnapshot
import com.example.ui.theme.AgedBrassMuted
import com.example.ui.theme.BronzeBorderMuted
import com.example.ui.theme.DeepTacticalParchment
import com.example.ui.theme.ElevatedBronzeSurface
import com.example.ui.theme.IlluminatedIvory
import com.example.ui.theme.ImperialBurnishedGold
import com.example.ui.viewmodel.PrimaryAppScreen

@Composable
fun CollapsibleImperialContainer(
    title: String,
    subtitle: String? = null,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier,
    headerActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(1.dp, BronzeBorderMuted, RoundedCornerShape(10.dp))
            .testTag("${testTagPrefix}_container"),
        shape = RoundedCornerShape(10.dp),
        color = DeepTacticalParchment.copy(alpha = 0.92f),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = ImperialBurnishedGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null && !isCollapsed) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = AgedBrassMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    headerActions()
                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("${testTagPrefix}_collapse_toggle")
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isCollapsed) "Expand $title" else "Collapse $title",
                            tint = ImperialBurnishedGold
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically(animationSpec = spring(stiffness = 400f)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = 400f)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun TopImperialResourceRibbon(
    worldState: WorldSimulationSnapshot,
    resourceStocks: List<ResourceStockState>,
    trackedResource: ResourceType?,
    hudCollapseState: HudCollapseState,
    isFullscreen: Boolean,
    onStockResourceClick: (ResourceType) -> Unit,
    onToggleTopRibbonCollapse: () -> Unit,
    onToggleCollapseAllHud: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onAdvanceAgeClick: () -> Unit,
    onNavigateScreen: (PrimaryAppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val humanNation = worldState.humanNation()
    CollapsibleImperialContainer(
        title = "${humanNation.civilization.displayName} • ${humanNation.currentAge.title}",
        subtitle = "Pop ${humanNation.populationCurrent}/${humanNation.populationCap} • Tap any Stock Resource to Map-Track Gather Sites",
        isCollapsed = hudCollapseState.topResourceRibbonCollapsed,
        onToggleCollapse = onToggleTopRibbonCollapse,
        testTagPrefix = "top_resource_ribbon",
        modifier = modifier.fillMaxWidth(),
        headerActions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Navigation pills for Matchmaking, Mod Studio, API Export, Spectator
                TopActionChip(
                    label = "Matchmaking",
                    testTag = "nav_matchmaking_button",
                    onClick = { onNavigateScreen(PrimaryAppScreen.MATCHMAKING_HUB) }
                )
                TopActionChip(
                    label = "Mods",
                    testTag = "nav_mod_manager_button",
                    onClick = { onNavigateScreen(PrimaryAppScreen.MOD_AND_MODPACK_STUDIO) }
                )
                TopActionChip(
                    label = "API Export",
                    testTag = "nav_api_export_button",
                    onClick = { onNavigateScreen(PrimaryAppScreen.COMPLETE_API_EXPORT) }
                )
                TopActionChip(
                    label = if (hudCollapseState.areAllMainPanelsCollapsed) "Show HUD" else "Clean HUD",
                    testTag = "toggle_all_hud_button",
                    onClick = onToggleCollapseAllHud
                )
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = ImperialBurnishedGold
                    )
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (stock in resourceStocks) {
                val isTracked = trackedResource == stock.type
                val resColor = Color(stock.type.colorHex)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStockResourceClick(stock.type) }
                        .border(
                            width = if (isTracked) 2.dp else 1.dp,
                            color = if (isTracked) ImperialBurnishedGold else BronzeBorderMuted,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .testTag("stock_resource_${stock.type.code.lowercase()}"),
                    color = if (isTracked) ElevatedBronzeSurface else Color(0xFF17130E),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(resColor, CircleShape)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stock.type.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = IlluminatedIvory
                                )
                                Text(
                                    text = "${stock.currentAmount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = resColor
                                )
                            }
                            Text(
                                text = "+${stock.ratePerMinute}/m (Cap ${stock.commerceCap}) • ${stock.assignedGatherers}/${stock.maxGathererSlots}W",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgedBrassMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Track ${stock.type.displayName} on Map",
                            tint = if (isTracked) ImperialBurnishedGold else AgedBrassMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Advance Age Button
            val nextAge = humanNation.currentAge.nextAge
            if (nextAge != null) {
                val (foodCost, knowCost) = ModHookRegistry.evaluateAgeCosts(nextAge)
                Button(
                    onClick = onAdvanceAgeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImperialBurnishedGold,
                        contentColor = Color(0xFF1A1208)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("advance_age_button")
                ) {
                    Text(
                        text = "Advance -> ${nextAge.shortTitle} (${foodCost}F/${knowCost}K)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TopActionChip(
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .border(1.dp, BronzeBorderMuted, RoundedCornerShape(6.dp))
            .testTag(testTag),
        color = ElevatedBronzeSurface
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IlluminatedIvory,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ResourceMapTrackerBanner(
    resourceTracking: ResourceTrackingState,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onCycleNextSite: () -> Unit,
    onFocusSite: (Float, Float) -> Unit,
    onDismissTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tracked = resourceTracking.trackedResource ?: return
    val resColor = Color(tracked.colorHex)

    CollapsibleImperialContainer(
        title = "Map-Tracking: ${tracked.displayName} Gathering Locations (${resourceTracking.activeSites.size})",
        subtitle = "3D Golden Beacons & Radar Pings Active • Tap site to center 3D camera",
        isCollapsed = isCollapsed,
        onToggleCollapse = onToggleCollapse,
        testTagPrefix = "resource_tracker_banner",
        modifier = modifier.widthIn(max = 520.dp),
        headerActions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopActionChip(
                    label = "Next Site (${resourceTracking.focusedSiteIndex + 1}/${resourceTracking.activeSites.size.coerceAtLeast(1)})",
                    testTag = "cycle_tracked_resource_site_button",
                    onClick = onCycleNextSite
                )
                IconButton(
                    onClick = onDismissTracking,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("close_resource_tracker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Resource Map Tracker",
                        tint = AgedBrassMuted
                    )
                }
            }
        }
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(resourceTracking.activeSites) { site ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFocusSite(site.worldX, site.worldZ) }
                        .border(1.dp, resColor, RoundedCornerShape(8.dp))
                        .testTag("tracked_site_card_${site.siteId}"),
                    color = Color(0xFF18130E)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = site.siteName,
                            style = MaterialTheme.typography.titleSmall,
                            color = resColor
                        )
                        Text(
                            text = "Coords (${site.worldX.toInt()}, ${site.worldZ.toInt()}) • Workers: ${site.assignedCitizens}/${site.maxCitizens} • +${site.yieldPerMinute}/m",
                            style = MaterialTheme.typography.labelSmall,
                            color = IlluminatedIvory
                        )
                        if (site.rareResourceBonusName != null) {
                            Text(
                                text = "Rare: ${site.rareResourceBonusName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ImperialBurnishedGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CombinedLeftEmpireAndTechDock(
    worldState: WorldSimulationSnapshot,
    hudCollapseState: HudCollapseState,
    onToggleCollapse: () -> Unit,
    onSelectTab: (LeftDockTab) -> Unit,
    onResearchTrack: (LibraryTrack) -> Unit,
    onStartWonderPlacement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val humanNation = worldState.humanNation()

    CollapsibleImperialContainer(
        title = "Empire, Library & Wonders",
        subtitle = "Combined Research, Territorial Sovereignty & Diplomacy",
        isCollapsed = hudCollapseState.leftEmpireDockCollapsed,
        onToggleCollapse = onToggleCollapse,
        testTagPrefix = "left_empire_dock",
        modifier = modifier.widthIn(max = 310.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (tab in LeftDockTab.entries) {
                FilterChip(
                    selected = hudCollapseState.activeLeftTab == tab,
                    onClick = { onSelectTab(tab) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("left_dock_tab_${tab.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ImperialBurnishedGold,
                        selectedLabelColor = Color(0xFF1A1208)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (hudCollapseState.activeLeftTab) {
            LeftDockTab.LIBRARY_TECH -> {
                Column(
                    modifier = Modifier
                        .heightIn(max = 230.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (track in LibraryTrack.entries) {
                        val lvl = humanNation.libraryLevels[track] ?: 1
                        val trackColor = Color(track.colorHex)
                        Surface(
                            color = Color(0xFF17120D),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BronzeBorderMuted),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${track.title} (Lv.$lvl/8)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = trackColor
                                    )
                                    Button(
                                        onClick = { onResearchTrack(track) },
                                        enabled = lvl < 8,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("research_track_${track.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = if (lvl < 8) "Research Lv.${lvl + 1}" else "MAX",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Text(
                                    text = track.effectSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AgedBrassMuted
                                )
                            }
                        }
                    }
                }
            }

            LeftDockTab.WONDERS_EMPIRE -> {
                Column(
                    modifier = Modifier
                        .heightIn(max = 230.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${humanNation.civilization.powerTitle}: ${humanNation.civilization.uniqueUnitSummary}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ImperialBurnishedGold
                    )
                    humanNation.civilization.nationalBonuses.forEach { bonus ->
                        Text(
                            text = "• $bonus",
                            style = MaterialTheme.typography.bodySmall,
                            color = IlluminatedIvory
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val wonders = RtsCatalogRegistry.allBuildings.filter { it.id.startsWith("wonder_") }
                    wonders.forEach { wonder ->
                        Surface(
                            color = Color(0xFF17120D),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BronzeBorderMuted),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = wonder.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ImperialBurnishedGold
                                )
                                Text(
                                    text = wonder.wonderBonusSummary ?: wonder.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AgedBrassMuted
                                )
                                OutlinedButton(
                                    onClick = { onStartWonderPlacement(wonder.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .height(28.dp)
                                        .testTag("place_wonder_${wonder.id}")
                                ) {
                                    Text(
                                        text = "Construct (${wonder.timberCost}T/${wonder.wealthCost}W/${wonder.knowledgeCost}K)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LeftDockTab.DIPLOMACY -> {
                Column(
                    modifier = Modifier
                        .heightIn(max = 230.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    worldState.nations.forEach { nation ->
                        Surface(
                            color = Color(0xFF17120D),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(nation.civilization.primaryColorHex)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${nation.commanderName} (${nation.currentAge.shortTitle})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IlluminatedIvory
                                    )
                                    Text(
                                        text = "Territory: ${nation.territoryPercent}% • Mil: ${nation.militaryScore} • Econ: ${nation.economyScore}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AgedBrassMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CombinedBottomCommandDeck(
    worldState: WorldSimulationSnapshot,
    hudCollapseState: HudCollapseState,
    isMarqueeBoxMode: Boolean,
    pendingPlacementBuildingId: String?,
    onToggleCollapse: () -> Unit,
    onSelectTab: (BottomDeckTab) -> Unit,
    onToggleMarqueeBox: () -> Unit,
    onSelectAllMilitary: () -> Unit,
    onSetUnitStance: (MilitaryStance) -> Unit,
    onStartBuildingPlacement: (String?) -> Unit,
    onQueueUnit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedUnits = worldState.units.filter { it.isSelected }
    val selectedBuildings = worldState.buildings.filter { it.isSelected }

    CollapsibleImperialContainer(
        title = "Combined Tactical Inspector & Production Command Deck",
        subtitle = if (pendingPlacementBuildingId != null) {
            "PLACEMENT ACTIVE: Tap 3D terrain to place structure"
        } else {
            "Selected: ${selectedUnits.size} Units, ${selectedBuildings.size} Structures"
        },
        isCollapsed = hudCollapseState.bottomCommandDeckCollapsed,
        onToggleCollapse = onToggleCollapse,
        testTagPrefix = "bottom_command_deck",
        modifier = modifier.fillMaxWidth(),
        headerActions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TopActionChip(
                    label = if (isMarqueeBoxMode) "Box Select: ON" else "Box Select: OFF",
                    testTag = "marquee_box_select_toggle",
                    onClick = onToggleMarqueeBox
                )
                TopActionChip(
                    label = "All Army",
                    testTag = "select_all_military_button",
                    onClick = onSelectAllMilitary
                )
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (tab in BottomDeckTab.entries) {
                FilterChip(
                    selected = hudCollapseState.activeBottomTab == tab,
                    onClick = { onSelectTab(tab) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("bottom_deck_tab_${tab.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ImperialBurnishedGold,
                        selectedLabelColor = Color(0xFF1A1208)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (hudCollapseState.activeBottomTab) {
            BottomDeckTab.TRAIN_FORCES -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RtsCatalogRegistry.allUnits) { rawUnit ->
                        val unit = ModHookRegistry.evaluateUnitArchetype(rawUnit)
                        Surface(
                            modifier = Modifier
                                .width(172.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQueueUnit(unit.id) }
                                .border(1.dp, BronzeBorderMuted, RoundedCornerShape(8.dp))
                                .testTag("recruit_unit_${unit.id}"),
                            color = Color(0xFF17120D)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = unit.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ImperialBurnishedGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${unit.minAge.shortTitle} • HP ${unit.maxHp.toInt()} • ATK ${unit.attackDamage.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IlluminatedIvory
                                )
                                Text(
                                    text = "Cost: ${unit.foodCost}F ${unit.timberCost}T ${unit.wealthCost}W ${unit.metalCost}M",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AgedBrassMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            BottomDeckTab.BUILD_STRUCTURES -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RtsCatalogRegistry.allBuildings.filter { !it.id.startsWith("wonder_") }) { rawBld ->
                        val bld = ModHookRegistry.evaluateBuildingArchetype(rawBld)
                        val isPending = pendingPlacementBuildingId == bld.id
                        Surface(
                            modifier = Modifier
                                .width(178.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onStartBuildingPlacement(if (isPending) null else bld.id)
                                }
                                .border(
                                    width = if (isPending) 2.dp else 1.dp,
                                    color = if (isPending) ImperialBurnishedGold else BronzeBorderMuted,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("construct_building_${bld.id}"),
                            color = if (isPending) ElevatedBronzeSurface else Color(0xFF17120D)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = bld.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ImperialBurnishedGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (bld.borderPushRadius > 0f) {
                                        "Border +${bld.borderPushRadius.toInt()}m • HP ${bld.maxHp.toInt()}"
                                    } else {
                                        "HP ${bld.maxHp.toInt()} • Max Workers ${bld.maxGatherers}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IlluminatedIvory
                                )
                                Text(
                                    text = "Cost: ${bld.timberCost}T ${bld.wealthCost}W ${bld.metalCost}M",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AgedBrassMuted
                                )
                            }
                        }
                    }
                }
            }

            BottomDeckTab.INSPECTOR_ORDERS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (stance in MilitaryStance.entries) {
                        OutlinedButton(
                            onClick = { onSetUnitStance(stance) },
                            modifier = Modifier.testTag("stance_button_${stance.name.lowercase()}")
                        ) {
                            Text(
                                text = "${stance.shortCode}: ${stance.label}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CombinedRightTacticalAndEngineDrawer(
    worldState: WorldSimulationSnapshot,
    cameraState: Camera3DState,
    resourceTracking: ResourceTrackingState,
    hudCollapseState: HudCollapseState,
    citizenAuto: CitizenAutomationConfig,
    militaryAuto: MilitaryAutomationConfig,
    graphicsConfig: GraphicsConfig,
    lodTelemetry: LodFrameTelemetry,
    poolTelemetry: List<PoolStatsSnapshot>,
    consoleLogs: List<ConsoleLogEntry>,
    spectatorState: SpectatorModeState,
    onToggleCollapse: () -> Unit,
    onSelectTab: (RightDrawerTab) -> Unit,
    onFocusWorldPoint: (Float, Float) -> Unit,
    onUpdateCitizenAuto: ((CitizenAutomationConfig) -> CitizenAutomationConfig) -> Unit,
    onUpdateMilitaryAuto: ((MilitaryAutomationConfig) -> MilitaryAutomationConfig) -> Unit,
    onUpdateGraphicsConfig: ((GraphicsConfig) -> GraphicsConfig) -> Unit,
    onUpdateSpectator: ((SpectatorModeState) -> SpectatorModeState) -> Unit,
    onPreWarmPools: () -> Unit,
    onCopyConsoleLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleImperialContainer(
        title = "Radar, Automation, LOD & Logs",
        subtitle = "LOD0:${lodTelemetry.lod0Count} LOD1:${lodTelemetry.lod1Count} LOD2:${lodTelemetry.lod2Count} LOD3:${lodTelemetry.lod3Count}",
        isCollapsed = hudCollapseState.rightTacticalDrawerCollapsed,
        onToggleCollapse = onToggleCollapse,
        testTagPrefix = "right_tactical_drawer",
        modifier = modifier.widthIn(max = 325.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (tab in RightDrawerTab.entries) {
                FilterChip(
                    selected = hudCollapseState.activeRightTab == tab,
                    onClick = { onSelectTab(tab) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("right_drawer_tab_${tab.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ImperialBurnishedGold,
                        selectedLabelColor = Color(0xFF1A1208)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (hudCollapseState.activeRightTab) {
            RightDrawerTab.RADAR_MAP -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10150E))
                            .border(1.dp, ImperialBurnishedGold, RoundedCornerShape(8.dp))
                            .testTag("tactical_radar_minimap")
                            .pointerInput(worldState.worldSize) {
                                detectTapGestures { offset ->
                                    val wx = (offset.x / size.width.coerceAtLeast(1)) * worldState.worldSize
                                    val wz = (offset.y / size.height.coerceAtLeast(1)) * worldState.worldSize
                                    onFocusWorldPoint(wx, wz)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val scaleX = size.width / worldState.worldSize
                            val scaleZ = size.height / worldState.worldSize

                            // Draw National Border Circles on Radar
                            for (bld in worldState.buildings) {
                                val arch = RtsCatalogRegistry.findBuilding(bld.archetypeId)
                                if (arch.borderPushRadius > 0f) {
                                    val nation = worldState.nations.firstOrNull { it.nationId == bld.nationId }
                                    val col = Color(nation?.civilization?.primaryColorHex ?: 0xFFD4AF37)
                                    drawCircle(
                                        color = col.copy(alpha = 0.25f),
                                        radius = arch.borderPushRadius * scaleX,
                                        center = Offset(bld.worldX * scaleX, bld.worldZ * scaleZ)
                                    )
                                }
                            }

                            // Draw Units on Radar
                            for (u in worldState.units) {
                                val nation = worldState.nations.firstOrNull { it.nationId == u.nationId }
                                val col = Color(nation?.civilization?.primaryColorHex ?: 0xFFFFFFFF)
                                drawCircle(
                                    color = col,
                                    radius = 2.5f,
                                    center = Offset(u.worldX * scaleX, u.worldZ * scaleZ)
                                )
                            }

                            // Draw Highlighted Resource Tracking Pings on Radar
                            val tracked = resourceTracking.trackedResource
                            if (tracked != null) {
                                val pingColor = Color(tracked.colorHex)
                                for (site in resourceTracking.activeSites) {
                                    drawCircle(
                                        color = pingColor,
                                        radius = 7f + resourceTracking.highlightPulsePhase * 8f,
                                        center = Offset(site.worldX * scaleX, site.worldZ * scaleZ),
                                        style = Stroke(width = 2.5f)
                                    )
                                }
                            }

                            // Camera Viewport Reticle
                            drawCircle(
                                color = Color(0xFFFBF6EE),
                                radius = 10f,
                                center = Offset(cameraState.targetX * scaleX, cameraState.targetZ * scaleZ),
                                style = Stroke(width = 1.8f)
                            )
                        }
                    }

                    // Quick Spectator Mode Switch inside Radar Drawer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = ImperialBurnishedGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Spectator Mode & Telemetry",
                                style = MaterialTheme.typography.labelMedium,
                                color = IlluminatedIvory
                            )
                        }
                        Switch(
                            checked = spectatorState.isSpectatorActive,
                            onCheckedChange = { active ->
                                onUpdateSpectator {
                                    it.copy(
                                        isSpectatorActive = active,
                                        fogOfWarMode = if (active) FogOfWarMode.REVEAL_ALL_SPECTATOR else FogOfWarMode.PLAYER_VISION_ONLY,
                                        showTelemetryOverlay = active
                                    )
                                }
                            },
                            modifier = Modifier.testTag("spectator_mode_switch")
                        )
                    }
                }
            }

            RightDrawerTab.AUTOMATION -> {
                Column(
                    modifier = Modifier
                        .heightIn(max = 225.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Citizen Management Automation",
                        style = MaterialTheme.typography.titleSmall,
                        color = ImperialBurnishedGold
                    )
                    AutomationToggleRow(
                        label = "Master Citizen Automation",
                        checked = citizenAuto.enabled,
                        testTag = "citizen_auto_master_switch",
                        onCheckedChange = { v -> onUpdateCitizenAuto { it.copy(enabled = v) } }
                    )
                    AutomationToggleRow(
                        label = "Auto-Assign Idle Citizens",
                        checked = citizenAuto.autoAssignIdleCitizens,
                        testTag = "citizen_auto_assign_switch",
                        onCheckedChange = { v -> onUpdateCitizenAuto { it.copy(autoAssignIdleCitizens = v) } }
                    )
                    AutomationToggleRow(
                        label = "Auto-Repair Damaged Structures",
                        checked = citizenAuto.autoRepairDamagedBuildings,
                        testTag = "citizen_auto_repair_switch",
                        onCheckedChange = { v -> onUpdateCitizenAuto { it.copy(autoRepairDamagedBuildings = v) } }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (mode in CitizenPriorityMode.entries) {
                            FilterChip(
                                selected = citizenAuto.priorityMode == mode,
                                onClick = { onUpdateCitizenAuto { it.copy(priorityMode = mode) } },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Military Command Automation",
                        style = MaterialTheme.typography.titleSmall,
                        color = ImperialBurnishedGold
                    )
                    AutomationToggleRow(
                        label = "Master Military Automation",
                        checked = militaryAuto.enabled,
                        testTag = "military_auto_master_switch",
                        onCheckedChange = { v -> onUpdateMilitaryAuto { it.copy(enabled = v) } }
                    )
                    AutomationToggleRow(
                        label = "Auto-Recruit Balanced Army",
                        checked = militaryAuto.autoRecruitBalancedArmy,
                        testTag = "military_auto_recruit_switch",
                        onCheckedChange = { v -> onUpdateMilitaryAuto { it.copy(autoRecruitBalancedArmy = v) } }
                    )
                    AutomationToggleRow(
                        label = "Auto-Scout Unexplored Fog",
                        checked = militaryAuto.autoScoutUnexploredTerrain,
                        testTag = "military_auto_scout_switch",
                        onCheckedChange = { v -> onUpdateMilitaryAuto { it.copy(autoScoutUnexploredTerrain = v) } }
                    )
                    AutomationToggleRow(
                        label = "Auto-Retreat Wounded Units",
                        checked = militaryAuto.autoRetreatWoundedUnits,
                        testTag = "military_auto_retreat_switch",
                        onCheckedChange = { v -> onUpdateMilitaryAuto { it.copy(autoRetreatWoundedUnits = v) } }
                    )
                }
            }

            RightDrawerTab.LOD_POOL_GFX -> {
                Column(
                    modifier = Modifier
                        .heightIn(max = 235.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (preset in listOf(GraphicsPreset.ULTRA_IMPERIAL, GraphicsPreset.BALANCED_TACTICAL, GraphicsPreset.BATTERY_SAVER)) {
                            FilterChip(
                                selected = graphicsConfig.preset == preset,
                                onClick = {
                                    onUpdateGraphicsConfig { GraphicsConfig.fromPreset(preset) }
                                },
                                label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("gfx_preset_${preset.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = "LOD0 High-Detail Mesh Distance: ${graphicsConfig.lod0MaxDistance.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = IlluminatedIvory
                    )
                    Slider(
                        value = graphicsConfig.lod0MaxDistance,
                        onValueChange = { d ->
                            onUpdateGraphicsConfig { it.copy(preset = GraphicsPreset.CUSTOM, lod0MaxDistance = d) }
                        },
                        valueRange = 20f..120f,
                        modifier = Modifier.testTag("slider_lod0_distance")
                    )

                    Text(
                        text = "LOD1 Medium Mesh Distance: ${graphicsConfig.lod1MaxDistance.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = IlluminatedIvory
                    )
                    Slider(
                        value = graphicsConfig.lod1MaxDistance,
                        onValueChange = { d ->
                            onUpdateGraphicsConfig { it.copy(preset = GraphicsPreset.CUSTOM, lod1MaxDistance = d) }
                        },
                        valueRange = 50f..200f,
                        modifier = Modifier.testTag("slider_lod1_distance")
                    )

                    Text(
                        text = "LOD2 Impostor Distance: ${graphicsConfig.lod2MaxDistance.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = IlluminatedIvory
                    )
                    Slider(
                        value = graphicsConfig.lod2MaxDistance,
                        onValueChange = { d ->
                            onUpdateGraphicsConfig { it.copy(preset = GraphicsPreset.CUSTOM, lod2MaxDistance = d) }
                        },
                        valueRange = 100f..290f,
                        modifier = Modifier.testTag("slider_lod2_distance")
                    )

                    AutomationToggleRow(
                        label = "Show 3D LOD Tier Badges on Units",
                        checked = graphicsConfig.showLodWireframeBadges,
                        testTag = "lod_badges_toggle",
                        onCheckedChange = { v -> onUpdateGraphicsConfig { it.copy(showLodWireframeBadges = v) } }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Object Pools (Zero-GC)",
                            style = MaterialTheme.typography.titleSmall,
                            color = ImperialBurnishedGold
                        )
                        OutlinedButton(
                            onClick = onPreWarmPools,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("prewarm_pools_button")
                        ) {
                            Text("Pre-Warm Pools", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    for (pool in poolTelemetry) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${pool.poolName}: ${pool.activeCount}/${pool.capacity} Active (Saved ${pool.reusedAllocationsSaved} GC Allocs)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgedBrassMuted
                            )
                            LinearProgressIndicator(
                                progress = { pool.utilizationFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = ImperialBurnishedGold
                            )
                        }
                    }
                }
            }

            RightDrawerTab.CONSOLE_LOGS -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Engine Logs (${consoleLogs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = ImperialBurnishedGold
                        )
                        Button(
                            onClick = onCopyConsoleLogs,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImperialBurnishedGold,
                                contentColor = Color(0xFF1A1208)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("copy_console_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Console Logs", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .background(Color(0xFF110E0A), RoundedCornerShape(6.dp))
                            .padding(6.dp)
                            .testTag("console_logs_list"),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(consoleLogs.takeLast(60).reversed()) { entry ->
                            Text(
                                text = entry.toFormattedLine(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (entry.severity.name) {
                                    "ERROR" -> Color(0xFFFC8181)
                                    "WARN" -> Color(0xFFF6E05E)
                                    else -> Color(0xFFCBD5E0)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = IlluminatedIvory,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun SpectatorControlAndTelemetryOverlay(
    worldState: WorldSimulationSnapshot,
    spectatorState: SpectatorModeState,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onUpdateSpectator: ((SpectatorModeState) -> SpectatorModeState) -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleImperialContainer(
        title = "Spectator Theatre & Live Empire Telemetry",
        subtitle = "POV: ${worldState.nations.getOrNull(spectatorState.observedNationId)?.commanderName ?: "Global"} • Speed ${spectatorState.playbackSpeedMultiplier}x",
        isCollapsed = isCollapsed,
        onToggleCollapse = onToggleCollapse,
        testTagPrefix = "spectator_telemetry_overlay",
        modifier = modifier.widthIn(max = 500.dp),
        headerActions = {
            IconButton(
                onClick = {
                    onUpdateSpectator { it.copy(isPaused = !it.isPaused) }
                },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("spectator_pause_resume_button")
            ) {
                Icon(
                    imageVector = if (spectatorState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause/Resume Simulation",
                    tint = ImperialBurnishedGold
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Playback Speed & Fog of War Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (spd in listOf(0.5f, 1.0f, 2.0f, 4.0f)) {
                    FilterChip(
                        selected = spectatorState.playbackSpeedMultiplier == spd,
                        onClick = { onUpdateSpectator { it.copy(playbackSpeedMultiplier = spd) } },
                        label = { Text("${spd}x", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("spectator_speed_${spd}x")
                    )
                }
                for (fog in FogOfWarMode.entries) {
                    FilterChip(
                        selected = spectatorState.fogOfWarMode == fog,
                        onClick = { onUpdateSpectator { it.copy(fogOfWarMode = fog) } },
                        label = { Text(fog.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Nation Perspective Switcher & Comparative Bars
            for (nation in worldState.nations) {
                val isObserved = spectatorState.observedNationId == nation.nationId
                val nationColor = Color(nation.civilization.primaryColorHex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isObserved) ElevatedBronzeSurface else Color(0xFF16120D))
                        .clickable {
                            onUpdateSpectator { it.copy(observedNationId = nation.nationId) }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(nationColor, CircleShape)
                        )
                        Text(
                            text = "${nation.commanderName} (${nation.currentAge.shortTitle})",
                            style = MaterialTheme.typography.labelMedium,
                            color = IlluminatedIvory
                        )
                    }
                    Text(
                        text = "Econ ${nation.economyScore}/m • Mil ${nation.militaryScore}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ImperialBurnishedGold
                    )
                }
            }
        }
    }
}
