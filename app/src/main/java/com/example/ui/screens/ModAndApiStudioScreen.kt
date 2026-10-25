package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.modding.CoreRuleHooks
import com.example.engine.modding.DeepHookEndpoint
import com.example.engine.modding.ModDefinition
import com.example.engine.modding.ModpackBundle
import com.example.ui.components.CollapsibleImperialContainer
import com.example.ui.theme.AgedBrassMuted
import com.example.ui.theme.BronzeBorderMuted
import com.example.ui.theme.IlluminatedIvory
import com.example.ui.theme.ImperialBurnishedGold
import com.example.ui.theme.ObsidianSlateBg
import com.google.firebase.auth.FirebaseUser

@Composable
fun ModAndModpackStudioScreen(
    installedMods: List<ModDefinition>,
    installedModpacks: List<ModpackBundle>,
    activeHooks: CoreRuleHooks,
    activeConflicts: List<String>,
    currentUser: FirebaseUser?,
    onBackToBattlefield: () -> Unit,
    onToggleModEnabled: (ModDefinition) -> Unit,
    onSaveCustomMod: (ModDefinition) -> Unit,
    onDeleteMod: (String) -> Unit,
    onActivateModpack: (ModpackBundle, Boolean) -> Unit,
    onImportModOrModpackJson: (String) -> Unit,
    onSyncModpackToCloud: (ModpackBundle, String) -> Unit,
    onCopyTextToClipboard: (String, String) -> Unit,
    onShareText: (String, String) -> Unit
) {
    BackHandler(onBack = onBackToBattlefield)

    var modpacksCollapsed by remember { mutableStateOf(false) }
    var modsListCollapsed by remember { mutableStateOf(false) }
    var visualEditorCollapsed by remember { mutableStateOf(false) }
    var importExportCollapsed by remember { mutableStateOf(false) }

    // Visual Rule & Hook Editor State
    var customModName by remember { mutableStateOf("Imperial Balance Overhaul") }
    var customModAuthor by remember { mutableStateOf(currentUser?.displayName ?: "Commander") }
    var gatherMult by remember { mutableFloatStateOf(activeHooks.globalGatherRateMultiplier) }
    var commerceCapMult by remember { mutableFloatStateOf(activeHooks.commerceCapMultiplier) }
    var borderRadiusMult by remember { mutableFloatStateOf(activeHooks.nationalBorderRadiusMultiplier) }
    var attritionMult by remember { mutableFloatStateOf(activeHooks.enemyAttritionDamageMultiplier) }
    var unitHpMult by remember { mutableFloatStateOf(activeHooks.unitHpMultiplier) }
    var unitAtkMult by remember { mutableFloatStateOf(activeHooks.unitAttackMultiplier) }
    var unitSpeedMult by remember { mutableFloatStateOf(activeHooks.unitMoveSpeedMultiplier) }
    var researchCostMult by remember { mutableFloatStateOf(activeHooks.libraryResearchCostMultiplier) }
    var meshScaleMult by remember { mutableFloatStateOf(activeHooks.customUnitMeshScale) }
    var popCapBonus by remember { mutableIntStateOf(activeHooks.populationCapFlatBonus) }

    var jsonImportExportBuffer by remember {
        mutableStateOf(installedModpacks.firstOrNull()?.toPrettyJson() ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianSlateBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToBattlefield,
                    modifier = Modifier.testTag("mod_studio_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to 3D Battlefield",
                        tint = ImperialBurnishedGold
                    )
                }
                Column {
                    Text(
                        text = "Mod & Modpack Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ImperialBurnishedGold
                    )
                    Text(
                        text = "Deep Core Mechanic Hooks • Visual Rule Editor • Modpack Import/Export",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgedBrassMuted
                    )
                }
            }
        }

        // Conflict Detector Banner if any hook overlaps exist
        if (activeConflicts.isNotEmpty()) {
            Surface(
                color = Color(0xFF3B1D1D),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFC8181)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Hook Load-Order Resolution Notice (${activeConflicts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFFC8181)
                    )
                    activeConflicts.forEach { msg ->
                        Text(text = "• $msg", style = MaterialTheme.typography.labelSmall, color = IlluminatedIvory)
                    }
                }
            }
        }

        // 1. Modpacks Manager (Multi-mod bundles with 1-click export/import/cloud sync)
        CollapsibleImperialContainer(
            title = "Installed Modpacks (${installedModpacks.size})",
            subtitle = "Multi-Mod Bundles with Load-Order & JSON Export",
            isCollapsed = modpacksCollapsed,
            onToggleCollapse = { modpacksCollapsed = !modpacksCollapsed },
            testTagPrefix = "modpacks_manager_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (pack in installedModpacks) {
                    Surface(
                        color = Color(0xFF17120D),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (pack.isActive) ImperialBurnishedGold else BronzeBorderMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${pack.name} (v${pack.version})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ImperialBurnishedGold
                                    )
                                    Text(
                                        text = "Author: ${pack.author} • Includes ${pack.mods.size} Mods",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IlluminatedIvory
                                    )
                                    Text(
                                        text = pack.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AgedBrassMuted
                                    )
                                }
                                Switch(
                                    checked = pack.isActive,
                                    onCheckedChange = { active -> onActivateModpack(pack, active) },
                                    modifier = Modifier.testTag("toggle_modpack_${pack.modpackId}")
                                )
                            }
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val json = pack.toPrettyJson()
                                        jsonImportExportBuffer = json
                                        onCopyTextToClipboard("Modpack JSON", json)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("export_modpack_${pack.modpackId}")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export JSON", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onShareText("${pack.name} Modpack", pack.toPrettyJson())
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share Pack", style = MaterialTheme.typography.labelSmall)
                                }
                                if (currentUser != null) {
                                    OutlinedButton(
                                        onClick = {
                                            onSyncModpackToCloud(pack, currentUser.displayName ?: "Commander")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cloud Sync", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Individual Mods & Load Order
        CollapsibleImperialContainer(
            title = "Individual Mods & Load Order (${installedMods.size})",
            subtitle = "Toggle individual rule & asset mods or export standalone JSON",
            isCollapsed = modsListCollapsed,
            onToggleCollapse = { modsListCollapsed = !modsListCollapsed },
            testTagPrefix = "individual_mods_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (mod in installedMods) {
                    Surface(
                        color = Color(0xFF17120D),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (mod.isEnabled) ImperialBurnishedGold else BronzeBorderMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "[#${mod.loadOrder}] ${mod.name} (v${mod.version})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ImperialBurnishedGold
                                )
                                Text(
                                    text = mod.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AgedBrassMuted
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val json = mod.toPrettyJson()
                                        jsonImportExportBuffer = json
                                        onCopyTextToClipboard("Mod JSON", json)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Export", style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(
                                    checked = mod.isEnabled,
                                    onCheckedChange = { onToggleModEnabled(mod) },
                                    modifier = Modifier.testTag("toggle_mod_${mod.modId}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Visual Deep Hook & Core Mechanic Editor
        CollapsibleImperialContainer(
            title = "In-App Visual Deep Hook & Rule Editor",
            subtitle = "Modify Gather Rates, Borders, Attrition, Combat Stats, Tech Costs & 3D Mesh Scale",
            isCollapsed = visualEditorCollapsed,
            onToggleCollapse = { visualEditorCollapsed = !visualEditorCollapsed },
            testTagPrefix = "visual_hook_editor_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customModName,
                        onValueChange = { customModName = it },
                        label = { Text("Mod Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_mod_name_input")
                    )
                    OutlinedTextField(
                        value = customModAuthor,
                        onValueChange = { customModAuthor = it },
                        label = { Text("Author") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HookSliderRow(
                    label = "Hook [onCalculateGatherRate] Global Yield Multiplier: ${"%.2f".format(gatherMult)}x",
                    value = gatherMult,
                    range = 0.5f..4.0f,
                    testTag = "hook_slider_gather_rate",
                    onValueChange = { gatherMult = it }
                )
                HookSliderRow(
                    label = "Hook [onEvaluateCommerceCap] Commerce Cap Multiplier: ${"%.2f".format(commerceCapMult)}x",
                    value = commerceCapMult,
                    range = 0.5f..4.0f,
                    testTag = "hook_slider_commerce_cap",
                    onValueChange = { commerceCapMult = it }
                )
                HookSliderRow(
                    label = "Hook [onBorderRadiusCalculate] National Border Push: ${"%.2f".format(borderRadiusMult)}x",
                    value = borderRadiusMult,
                    range = 0.5f..2.5f,
                    testTag = "hook_slider_border_radius",
                    onValueChange = { borderRadiusMult = it }
                )
                HookSliderRow(
                    label = "Hook [onAttritionTickDamage] Hostile Border Attrition: ${"%.2f".format(attritionMult)}x",
                    value = attritionMult,
                    range = 0.0f..5.0f,
                    testTag = "hook_slider_attrition",
                    onValueChange = { attritionMult = it }
                )
                HookSliderRow(
                    label = "Hook [onUnitSpawnStatPatch] Unit Max HP Multiplier: ${"%.2f".format(unitHpMult)}x",
                    value = unitHpMult,
                    range = 0.5f..3.0f,
                    testTag = "hook_slider_unit_hp",
                    onValueChange = { unitHpMult = it }
                )
                HookSliderRow(
                    label = "Hook [onCombatDamageResolve] Unit Attack Damage: ${"%.2f".format(unitAtkMult)}x",
                    value = unitAtkMult,
                    range = 0.5f..3.0f,
                    testTag = "hook_slider_unit_attack",
                    onValueChange = { unitAtkMult = it }
                )
                HookSliderRow(
                    label = "Hook [onTechAndAgeCostEvaluate] Library & Age Cost Scale: ${"%.2f".format(researchCostMult)}x",
                    value = researchCostMult,
                    range = 0.2f..2.0f,
                    testTag = "hook_slider_tech_cost",
                    onValueChange = { researchCostMult = it }
                )
                HookSliderRow(
                    label = "Hook [onAssetMeshAndShaderOverride] 3D Unit Mesh Scale: ${"%.2f".format(meshScaleMult)}x",
                    value = meshScaleMult,
                    range = 0.6f..2.2f,
                    testTag = "hook_slider_mesh_scale",
                    onValueChange = { meshScaleMult = it }
                )

                Button(
                    onClick = {
                        val newMod = ModDefinition(
                            modId = "mod_custom_${customModName.lowercase().replace(Regex("[^a-z0-9]"), "_")}",
                            name = customModName.ifBlank { "Custom Imperial Rule Mod" },
                            author = customModAuthor.ifBlank { "Commander" },
                            version = "1.0.0",
                            description = "Custom Visual Hook Overrides (Gather=${"%.1f".format(gatherMult)}x, Border=${"%.1f".format(borderRadiusMult)}x, Mesh=${"%.1f".format(meshScaleMult)}x).",
                            loadOrder = installedMods.size + 1,
                            isEnabled = true,
                            hooks = CoreRuleHooks(
                                globalGatherRateMultiplier = gatherMult,
                                commerceCapMultiplier = commerceCapMult,
                                nationalBorderRadiusMultiplier = borderRadiusMult,
                                enemyAttritionDamageMultiplier = attritionMult,
                                unitHpMultiplier = unitHpMult,
                                unitAttackMultiplier = unitAtkMult,
                                unitMoveSpeedMultiplier = unitSpeedMult,
                                libraryResearchCostMultiplier = researchCostMult,
                                ageAdvancementCostMultiplier = researchCostMult,
                                populationCapFlatBonus = popCapBonus,
                                customUnitMeshScale = meshScaleMult
                            )
                        )
                        onSaveCustomMod(newMod)
                        jsonImportExportBuffer = newMod.toPrettyJson()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImperialBurnishedGold,
                        contentColor = Color(0xFF1A1208)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_apply_custom_mod_button")
                ) {
                    Text(
                        text = "Compile & Apply Deep Hook Mod to Live 3D Engine",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. JSON Mod & Modpack Import / Export Console
        CollapsibleImperialContainer(
            title = "Mod & Modpack JSON Import / Export Console",
            subtitle = "Paste external Mod or Modpack JSON below to validate and import, or copy active bundle",
            isCollapsed = importExportCollapsed,
            onToggleCollapse = { importExportCollapsed = !importExportCollapsed },
            testTagPrefix = "json_import_export_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = jsonImportExportBuffer,
                    onValueChange = { jsonImportExportBuffer = it },
                    label = { Text("Mod / Modpack JSON Payload") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modpack_json_text_field")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onImportModOrModpackJson(jsonImportExportBuffer) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImperialBurnishedGold,
                            contentColor = Color(0xFF1A1208)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_modpack_json_button")
                    ) {
                        Text("Import JSON Bundle", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = {
                            onCopyTextToClipboard("Modpack JSON Bundle", jsonImportExportBuffer)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_modpack_json_button")
                    ) {
                        Text("Copy JSON to Clipboard", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun HookSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IlluminatedIvory
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun CompleteApiExportScreen(
    apiJsonSpec: String,
    apiMarkdownGuide: String,
    consoleLogsText: String,
    onBackToBattlefield: () -> Unit,
    onCopyTextToClipboard: (String, String) -> Unit,
    onShareText: (String, String) -> Unit
) {
    BackHandler(onBack = onBackToBattlefield)

    var endpointsCollapsed by remember { mutableStateOf(false) }
    var jsonSpecCollapsed by remember { mutableStateOf(false) }
    var markdownCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianSlateBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToBattlefield,
                    modifier = Modifier.testTag("api_export_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to 3D Battlefield",
                        tint = ImperialBurnishedGold
                    )
                }
                Column {
                    Text(
                        text = "Complete API Export & Engine SDK",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ImperialBurnishedGold
                    )
                    Text(
                        text = "Full Hook Signatures • Entity Registries • Live Runtime State Export",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgedBrassMuted
                    )
                }
            }
        }

        // Primary Export Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onCopyTextToClipboard("Crown & Dominion Complete API JSON", apiJsonSpec)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImperialBurnishedGold,
                    contentColor = Color(0xFF1A1208)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_complete_api_json_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Complete API JSON", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = {
                    onShareText("Crown & Dominion Complete API Specification", apiJsonSpec)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("share_complete_api_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Complete API", style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onCopyTextToClipboard("Crown & Dominion Markdown SDK Guide", apiMarkdownGuide)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("copy_api_markdown_button")
            ) {
                Text("Copy Markdown SDK Guide", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
                onClick = {
                    onCopyTextToClipboard("Engine Console Logs", consoleLogsText)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("api_screen_copy_console_logs_button")
            ) {
                Text("Copy Console Logs", style = MaterialTheme.typography.labelSmall)
            }
        }

        // 1. Deep Hook Endpoints Reference
        CollapsibleImperialContainer(
            title = "Engine Hook Interceptor Signatures (${DeepHookEndpoint.entries.size})",
            subtitle = "Every core mechanic, combat formula, border rule, and 3D asset hook",
            isCollapsed = endpointsCollapsed,
            onToggleCollapse = { endpointsCollapsed = !endpointsCollapsed },
            testTagPrefix = "api_endpoints_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (ep in DeepHookEndpoint.entries) {
                    Surface(
                        color = Color(0xFF17120D),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BronzeBorderMuted),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${ep.hookId}  [${ep.domain}]",
                                style = MaterialTheme.typography.labelLarge,
                                color = ImperialBurnishedGold
                            )
                            Text(
                                text = ep.signature,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF90CDF4)
                            )
                            Text(
                                text = ep.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = AgedBrassMuted
                            )
                        }
                    }
                }
            }
        }

        // 2. Live JSON API Specification Preview
        CollapsibleImperialContainer(
            title = "Complete JSON API Specification Payload (${apiJsonSpec.length} bytes)",
            subtitle = "Includes all 8 Ages, 8 Nations, 22 Units, 19 Buildings/Wonders, LOD & Pool Schema",
            isCollapsed = jsonSpecCollapsed,
            onToggleCollapse = { jsonSpecCollapsed = !jsonSpecCollapsed },
            testTagPrefix = "api_json_preview_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = Color(0xFF100D0A),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = apiJsonSpec.take(4500) + "\n... [Full ${apiJsonSpec.length} chars exported via Copy / Share]",
                    style = MaterialTheme.typography.labelSmall,
                    color = IlluminatedIvory,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // 3. Markdown Developer Guide Preview
        CollapsibleImperialContainer(
            title = "Markdown Modder & Engine SDK Documentation",
            subtitle = "Human-readable reference for modders and tool authors",
            isCollapsed = markdownCollapsed,
            onToggleCollapse = { markdownCollapsed = !markdownCollapsed },
            testTagPrefix = "api_markdown_preview_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = Color(0xFF100D0A),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = apiMarkdownGuide,
                    style = MaterialTheme.typography.labelSmall,
                    color = IlluminatedIvory,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
