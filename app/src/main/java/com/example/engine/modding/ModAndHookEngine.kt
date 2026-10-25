package com.example.engine.modding

import com.example.engine.logging.EngineConsoleLogger
import com.example.engine.logging.LogCategory
import com.example.engine.logging.LogSeverity
import com.example.engine.model.BuildingArchetype
import com.example.engine.model.HistoricalAge
import com.example.engine.model.ResourceType
import com.example.engine.model.UnitArchetype
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class DeepHookEndpoint(
    val hookId: String,
    val signature: String,
    val domain: String,
    val description: String
) {
    ON_GATHER_RATE_EVALUATE(
        hookId = "onCalculateGatherRate",
        signature = "(resourceType: ResourceType, baseRate: Int, workers: Int, age: HistoricalAge) -> Int",
        domain = "Economy & Resources",
        description = "Intercepts and modifies per-minute resource yield for Food, Timber, Wealth, Metal, Oil, and Knowledge."
    ),
    ON_COMMERCE_CAP_EVALUATE(
        hookId = "onEvaluateCommerceCap",
        signature = "(resourceType: ResourceType, baseCap: Int, commerceLevel: Int) -> Int",
        domain = "Economy & Resources",
        description = "Modifies the maximum resource gather rate ceiling imposed by Commerce Library research."
    ),
    ON_UNIT_SPAWN_STAT_PATCH(
        hookId = "onUnitSpawnStatPatch",
        signature = "(archetypeId: String, baseHp: Float, baseAttack: Float, baseRange: Float, baseSpeed: Float) -> UnitStats",
        domain = "Military & Units",
        description = "Applies flat and multiplicative overrides to unit HP, attack damage, weapon range, and movement speed."
    ),
    ON_COMBAT_DAMAGE_RESOLVE(
        hookId = "onCombatDamageResolve",
        signature = "(attackerId: String, defenderId: String, rawDamage: Float, distance: Float) -> Float",
        domain = "Combat & Ballistics",
        description = "Modifies real-time projectile and melee combat damage calculations before HP deduction."
    ),
    ON_BORDER_RADIUS_CALCULATE(
        hookId = "onBorderRadiusCalculate",
        signature = "(buildingId: String, civicLevel: Int, ageIndex: Int, baseRadius: Float) -> Float",
        domain = "Territory & Borders",
        description = "Controls how far Cities, Forts, and Wonders push national borders across the 3D terrain."
    ),
    ON_ATTRITION_TICK_DAMAGE(
        hookId = "onAttritionTickDamage",
        signature = "(hostileNationId: Int, intruderUnitId: String, baseAttritionPerSec: Float) -> Float",
        domain = "Territory & Borders",
        description = "Overrides supply attrition damage suffered by unallied units inside enemy national borders."
    ),
    ON_TECH_AND_AGE_COST_EVALUATE(
        hookId = "onTechAndAgeCostEvaluate",
        signature = "(trackOrAgeId: String, baseFoodCost: Int, baseKnowledgeCost: Int) -> Pair<Int, Int>",
        domain = "Research & Epochs",
        description = "Scales Library technology costs and Historical Age advancement thresholds."
    ),
    ON_POPULATION_CAP_EVALUATE(
        hookId = "onPopulationCapEvaluate",
        signature = "(militaryLevel: Int, citiesOwned: Int, basePopCap: Int) -> Int",
        domain = "Civic & Empire",
        description = "Overrides the national population limit and per-city housing bonus."
    ),
    ON_WONDER_CONSTRUCTION_RULE(
        hookId = "onWonderConstructionRule",
        signature = "(wonderId: String, baseBuildTime: Float, costMultiplier: Float) -> WonderRule",
        domain = "Wonders & Monuments",
        description = "Modifies Wonder construction speed, resource cost multipliers, and global empire buffs."
    ),
    ON_3D_ASSET_MESH_AND_SHADER_OVERRIDE(
        hookId = "onAssetMeshAndShaderOverride",
        signature = "(entityArchetypeId: String, meshScale: Float, tintColorHex: Long, lodBias: Float) -> AssetOverride",
        domain = "3D Assets & Rendering",
        description = "Hooks into the 3D LOD renderer to scale unit/building meshes, recolor materials, or shift LOD bias."
    )
}

data class UnitStatOverride(
    val targetUnitId: String = "ALL",
    val hpMultiplier: Float = 1.0f,
    val attackMultiplier: Float = 1.0f,
    val rangeMultiplier: Float = 1.0f,
    val speedMultiplier: Float = 1.0f,
    val costMultiplier: Float = 1.0f,
    val customMeshScale: Float = 1.0f,
    val customTintHex: Long? = null
)

data class CoreRuleHooks(
    val globalGatherRateMultiplier: Float = 1.0f,
    val foodGatherMultiplier: Float = 1.0f,
    val timberGatherMultiplier: Float = 1.0f,
    val wealthGatherMultiplier: Float = 1.0f,
    val metalGatherMultiplier: Float = 1.0f,
    val oilGatherMultiplier: Float = 1.0f,
    val knowledgeGatherMultiplier: Float = 1.0f,
    val commerceCapMultiplier: Float = 1.0f,
    val nationalBorderRadiusMultiplier: Float = 1.0f,
    val enemyAttritionDamageMultiplier: Float = 1.0f,
    val unitHpMultiplier: Float = 1.0f,
    val unitAttackMultiplier: Float = 1.0f,
    val unitMoveSpeedMultiplier: Float = 1.0f,
    val buildingHpMultiplier: Float = 1.0f,
    val libraryResearchCostMultiplier: Float = 1.0f,
    val ageAdvancementCostMultiplier: Float = 1.0f,
    val wonderCostMultiplier: Float = 1.0f,
    val populationCapFlatBonus: Int = 0,
    val startingResourcesMultiplier: Float = 1.0f,
    val customUnitMeshScale: Float = 1.0f,
    val customTerrainTintHex: Long? = null,
    val unitOverrides: List<UnitStatOverride> = emptyList()
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("globalGatherRateMultiplier", globalGatherRateMultiplier.toDouble())
        put("foodGatherMultiplier", foodGatherMultiplier.toDouble())
        put("timberGatherMultiplier", timberGatherMultiplier.toDouble())
        put("wealthGatherMultiplier", wealthGatherMultiplier.toDouble())
        put("metalGatherMultiplier", metalGatherMultiplier.toDouble())
        put("oilGatherMultiplier", oilGatherMultiplier.toDouble())
        put("knowledgeGatherMultiplier", knowledgeGatherMultiplier.toDouble())
        put("commerceCapMultiplier", commerceCapMultiplier.toDouble())
        put("nationalBorderRadiusMultiplier", nationalBorderRadiusMultiplier.toDouble())
        put("enemyAttritionDamageMultiplier", enemyAttritionDamageMultiplier.toDouble())
        put("unitHpMultiplier", unitHpMultiplier.toDouble())
        put("unitAttackMultiplier", unitAttackMultiplier.toDouble())
        put("unitMoveSpeedMultiplier", unitMoveSpeedMultiplier.toDouble())
        put("buildingHpMultiplier", buildingHpMultiplier.toDouble())
        put("libraryResearchCostMultiplier", libraryResearchCostMultiplier.toDouble())
        put("ageAdvancementCostMultiplier", ageAdvancementCostMultiplier.toDouble())
        put("wonderCostMultiplier", wonderCostMultiplier.toDouble())
        put("populationCapFlatBonus", populationCapFlatBonus)
        put("startingResourcesMultiplier", startingResourcesMultiplier.toDouble())
        put("customUnitMeshScale", customUnitMeshScale.toDouble())
        if (customTerrainTintHex != null) {
            put("customTerrainTintHex", customTerrainTintHex)
        }
        val arr = JSONArray()
        for (u in unitOverrides) {
            arr.put(
                JSONObject().apply {
                    put("targetUnitId", u.targetUnitId)
                    put("hpMultiplier", u.hpMultiplier.toDouble())
                    put("attackMultiplier", u.attackMultiplier.toDouble())
                    put("rangeMultiplier", u.rangeMultiplier.toDouble())
                    put("speedMultiplier", u.speedMultiplier.toDouble())
                    put("costMultiplier", u.costMultiplier.toDouble())
                    put("customMeshScale", u.customMeshScale.toDouble())
                    if (u.customTintHex != null) {
                        put("customTintHex", u.customTintHex)
                    }
                }
            )
        }
        put("unitOverrides", arr)
    }

    companion object {
        fun fromJsonObject(json: JSONObject): CoreRuleHooks {
            val overridesList = mutableListOf<UnitStatOverride>()
            val arr = json.optJSONArray("unitOverrides")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    overridesList.add(
                        UnitStatOverride(
                            targetUnitId = item.optString("targetUnitId", "ALL"),
                            hpMultiplier = item.optDouble("hpMultiplier", 1.0).toFloat(),
                            attackMultiplier = item.optDouble("attackMultiplier", 1.0).toFloat(),
                            rangeMultiplier = item.optDouble("rangeMultiplier", 1.0).toFloat(),
                            speedMultiplier = item.optDouble("speedMultiplier", 1.0).toFloat(),
                            costMultiplier = item.optDouble("costMultiplier", 1.0).toFloat(),
                            customMeshScale = item.optDouble("customMeshScale", 1.0).toFloat(),
                            customTintHex = if (item.has("customTintHex")) item.optLong("customTintHex") else null
                        )
                    )
                }
            }
            return CoreRuleHooks(
                globalGatherRateMultiplier = json.optDouble("globalGatherRateMultiplier", 1.0).toFloat(),
                foodGatherMultiplier = json.optDouble("foodGatherMultiplier", 1.0).toFloat(),
                timberGatherMultiplier = json.optDouble("timberGatherMultiplier", 1.0).toFloat(),
                wealthGatherMultiplier = json.optDouble("wealthGatherMultiplier", 1.0).toFloat(),
                metalGatherMultiplier = json.optDouble("metalGatherMultiplier", 1.0).toFloat(),
                oilGatherMultiplier = json.optDouble("oilGatherMultiplier", 1.0).toFloat(),
                knowledgeGatherMultiplier = json.optDouble("knowledgeGatherMultiplier", 1.0).toFloat(),
                commerceCapMultiplier = json.optDouble("commerceCapMultiplier", 1.0).toFloat(),
                nationalBorderRadiusMultiplier = json.optDouble("nationalBorderRadiusMultiplier", 1.0).toFloat(),
                enemyAttritionDamageMultiplier = json.optDouble("enemyAttritionDamageMultiplier", 1.0).toFloat(),
                unitHpMultiplier = json.optDouble("unitHpMultiplier", 1.0).toFloat(),
                unitAttackMultiplier = json.optDouble("unitAttackMultiplier", 1.0).toFloat(),
                unitMoveSpeedMultiplier = json.optDouble("unitMoveSpeedMultiplier", 1.0).toFloat(),
                buildingHpMultiplier = json.optDouble("buildingHpMultiplier", 1.0).toFloat(),
                libraryResearchCostMultiplier = json.optDouble("libraryResearchCostMultiplier", 1.0).toFloat(),
                ageAdvancementCostMultiplier = json.optDouble("ageAdvancementCostMultiplier", 1.0).toFloat(),
                wonderCostMultiplier = json.optDouble("wonderCostMultiplier", 1.0).toFloat(),
                populationCapFlatBonus = json.optInt("populationCapFlatBonus", 0),
                startingResourcesMultiplier = json.optDouble("startingResourcesMultiplier", 1.0).toFloat(),
                customUnitMeshScale = json.optDouble("customUnitMeshScale", 1.0).toFloat(),
                customTerrainTintHex = if (json.has("customTerrainTintHex")) json.optLong("customTerrainTintHex") else null,
                unitOverrides = overridesList
            )
        }
    }
}

data class ModDefinition(
    val modId: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val loadOrder: Int,
    val isEnabled: Boolean,
    val hooks: CoreRuleHooks
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("modId", modId)
        put("name", name)
        put("author", author)
        put("version", version)
        put("description", description)
        put("loadOrder", loadOrder)
        put("isEnabled", isEnabled)
        put("hooks", hooks.toJsonObject())
    }

    fun toPrettyJson(): String = toJsonObject().toString(2)

    companion object {
        fun fromJsonObject(json: JSONObject): ModDefinition {
            val hooksObj = json.optJSONObject("hooks") ?: JSONObject()
            return ModDefinition(
                modId = json.optString("modId", "mod_${System.currentTimeMillis()}"),
                name = json.optString("name", "Imported Custom Mod"),
                author = json.optString("author", "Community Modder"),
                version = json.optString("version", "1.0.0"),
                description = json.optString("description", "Custom gameplay hook overrides."),
                loadOrder = json.optInt("loadOrder", 10),
                isEnabled = json.optBoolean("isEnabled", true),
                hooks = CoreRuleHooks.fromJsonObject(hooksObj)
            )
        }
    }
}

data class ModpackBundle(
    val modpackId: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val isActive: Boolean,
    val mods: List<ModDefinition>
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("schemaVersion", "crown-dominion-modpack-v2")
        put("modpackId", modpackId)
        put("name", name)
        put("author", author)
        put("version", version)
        put("description", description)
        put("isActive", isActive)
        val modsArr = JSONArray()
        mods.forEach { modsArr.put(it.toJsonObject()) }
        put("mods", modsArr)
    }

    fun toPrettyJson(): String = toJsonObject().toString(2)

    companion object {
        fun fromJsonString(rawJson: String): Result<ModpackBundle> = runCatching {
            val root = JSONObject(rawJson.trim())
            if (root.has("mods")) {
                val arr = root.getJSONArray("mods")
                val parsedMods = mutableListOf<ModDefinition>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    parsedMods.add(ModDefinition.fromJsonObject(obj))
                }
                ModpackBundle(
                    modpackId = root.optString("modpackId", "pack_${System.currentTimeMillis()}"),
                    name = root.optString("name", "Imported Modpack Bundle"),
                    author = root.optString("author", "Imperial Architect"),
                    version = root.optString("version", "1.0.0"),
                    description = root.optString("description", "Imported multi-mod bundle."),
                    isActive = root.optBoolean("isActive", true),
                    mods = parsedMods
                )
            } else {
                // Single mod JSON imported directly as a 1-mod bundle
                val singleMod = ModDefinition.fromJsonObject(root)
                ModpackBundle(
                    modpackId = "pack_${singleMod.modId}",
                    name = "${singleMod.name} Pack",
                    author = singleMod.author,
                    version = singleMod.version,
                    description = singleMod.description,
                    isActive = true,
                    mods = listOf(singleMod)
                )
            }
        }
    }
}

object ModHookRegistry {

    val defaultBuiltInMods: List<ModDefinition> = listOf(
        ModDefinition(
            modId = "mod_imperial_blitz_economy",
            name = "Imperial Blitz Economy (2x Gather & Cap)",
            author = "Crown Studio Core",
            version = "1.4.0",
            description = "Doubles resource gather rates, raises Commerce Caps by +75%, and grants +50 Population limit for massive battles.",
            loadOrder = 1,
            isEnabled = false,
            hooks = CoreRuleHooks(
                globalGatherRateMultiplier = 2.0f,
                commerceCapMultiplier = 1.75f,
                populationCapFlatBonus = 50,
                startingResourcesMultiplier = 1.5f
            )
        ),
        ModDefinition(
            modId = "mod_fortress_frontiers",
            name = "Fortress Frontiers & Lethal Attrition",
            author = "Tactical Guild",
            version = "2.0.1",
            description = "Expands City and Fort national borders by +45% and triples enemy supply attrition inside your territory.",
            loadOrder = 2,
            isEnabled = false,
            hooks = CoreRuleHooks(
                nationalBorderRadiusMultiplier = 1.45f,
                enemyAttritionDamageMultiplier = 3.0f,
                buildingHpMultiplier = 1.35f
            )
        ),
        ModDefinition(
            modId = "mod_titan_scale_warfare",
            name = "Titan-Scale 3D Units & Rapid Tech",
            author = "Visual Overhaul Team",
            version = "1.2.0",
            description = "Scales 3D unit meshes to 1.35x heroic proportions, boosts unit HP/Attack by +30%, and halves Age research costs.",
            loadOrder = 3,
            isEnabled = false,
            hooks = CoreRuleHooks(
                unitHpMultiplier = 1.3f,
                unitAttackMultiplier = 1.3f,
                unitMoveSpeedMultiplier = 1.15f,
                libraryResearchCostMultiplier = 0.55f,
                ageAdvancementCostMultiplier = 0.5f,
                customUnitMeshScale = 1.35f
            )
        )
    )

    val defaultBuiltInModpacks: List<ModpackBundle> = listOf(
        ModpackBundle(
            modpackId = "pack_grand_conquest_overhaul",
            name = "Grand Conquest Total Overhaul Pack",
            author = "Crown & Dominion Official",
            version = "2.1.0",
            description = "Combines Imperial Blitz Economy + Fortress Frontiers for high-tempo macro territorial warfare.",
            isActive = false,
            mods = listOf(
                defaultBuiltInMods[0].copy(isEnabled = true),
                defaultBuiltInMods[1].copy(isEnabled = true)
            )
        ),
        ModpackBundle(
            modpackId = "pack_heroic_sandbox",
            name = "Heroic 3D Sandbox & Instant Empires",
            author = "Modders Collective",
            version = "1.0.5",
            description = "Enables all 3 overhaul mods with heroic 3D unit scaling, fast Age progression, and huge population caps.",
            isActive = false,
            mods = defaultBuiltInMods.map { it.copy(isEnabled = true) }
        )
    )

    private val _activeCombinedHooks = MutableStateFlow(CoreRuleHooks())
    val activeCombinedHooks: StateFlow<CoreRuleHooks> = _activeCombinedHooks.asStateFlow()

    private val _activeModConflicts = MutableStateFlow<List<String>>(emptyList())
    val activeModConflicts: StateFlow<List<String>> = _activeModConflicts.asStateFlow()

    fun recomputeCombinedHooks(mods: List<ModDefinition>) {
        val enabledMods = mods.filter { it.isEnabled }.sortedBy { it.loadOrder }
        var merged = CoreRuleHooks()
        val overridesAcc = mutableListOf<UnitStatOverride>()
        val conflicts = mutableListOf<String>()

        var gatherTouchedBy: String? = null
        var borderTouchedBy: String? = null
        var meshScaleTouchedBy: String? = null

        for (mod in enabledMods) {
            val h = mod.hooks
            if (h.globalGatherRateMultiplier != 1.0f) {
                if (gatherTouchedBy != null) {
                    conflicts.add("Hook [onCalculateGatherRate] modified by both '$gatherTouchedBy' and '${mod.name}' (resolved via Load Order ${mod.loadOrder}).")
                }
                gatherTouchedBy = mod.name
            }
            if (h.nationalBorderRadiusMultiplier != 1.0f) {
                if (borderTouchedBy != null) {
                    conflicts.add("Hook [onBorderRadiusCalculate] modified by both '$borderTouchedBy' and '${mod.name}' (resolved via Load Order ${mod.loadOrder}).")
                }
                borderTouchedBy = mod.name
            }
            if (h.customUnitMeshScale != 1.0f) {
                if (meshScaleTouchedBy != null) {
                    conflicts.add("Hook [onAssetMeshAndShaderOverride] modified by both '$meshScaleTouchedBy' and '${mod.name}'.")
                }
                meshScaleTouchedBy = mod.name
            }

            merged = CoreRuleHooks(
                globalGatherRateMultiplier = merged.globalGatherRateMultiplier * h.globalGatherRateMultiplier,
                foodGatherMultiplier = merged.foodGatherMultiplier * h.foodGatherMultiplier,
                timberGatherMultiplier = merged.timberGatherMultiplier * h.timberGatherMultiplier,
                wealthGatherMultiplier = merged.wealthGatherMultiplier * h.wealthGatherMultiplier,
                metalGatherMultiplier = merged.metalGatherMultiplier * h.metalGatherMultiplier,
                oilGatherMultiplier = merged.oilGatherMultiplier * h.oilGatherMultiplier,
                knowledgeGatherMultiplier = merged.knowledgeGatherMultiplier * h.knowledgeGatherMultiplier,
                commerceCapMultiplier = merged.commerceCapMultiplier * h.commerceCapMultiplier,
                nationalBorderRadiusMultiplier = merged.nationalBorderRadiusMultiplier * h.nationalBorderRadiusMultiplier,
                enemyAttritionDamageMultiplier = merged.enemyAttritionDamageMultiplier * h.enemyAttritionDamageMultiplier,
                unitHpMultiplier = merged.unitHpMultiplier * h.unitHpMultiplier,
                unitAttackMultiplier = merged.unitAttackMultiplier * h.unitAttackMultiplier,
                unitMoveSpeedMultiplier = merged.unitMoveSpeedMultiplier * h.unitMoveSpeedMultiplier,
                buildingHpMultiplier = merged.buildingHpMultiplier * h.buildingHpMultiplier,
                libraryResearchCostMultiplier = (merged.libraryResearchCostMultiplier * h.libraryResearchCostMultiplier).coerceIn(0.1f, 5.0f),
                ageAdvancementCostMultiplier = (merged.ageAdvancementCostMultiplier * h.ageAdvancementCostMultiplier).coerceIn(0.1f, 5.0f),
                wonderCostMultiplier = (merged.wonderCostMultiplier * h.wonderCostMultiplier).coerceIn(0.1f, 5.0f),
                populationCapFlatBonus = merged.populationCapFlatBonus + h.populationCapFlatBonus,
                startingResourcesMultiplier = merged.startingResourcesMultiplier * h.startingResourcesMultiplier,
                customUnitMeshScale = (merged.customUnitMeshScale * h.customUnitMeshScale).coerceIn(0.5f, 2.5f),
                customTerrainTintHex = h.customTerrainTintHex ?: merged.customTerrainTintHex,
                unitOverrides = overridesAcc + h.unitOverrides
            )
            overridesAcc.addAll(h.unitOverrides)
        }

        _activeCombinedHooks.value = merged
        _activeModConflicts.value = conflicts
        EngineConsoleLogger.log(
            category = LogCategory.HOOK,
            severity = LogSeverity.INFO,
            message = "Recomputed ModHookRegistry with ${enabledMods.size} active mods (Gather=${"%.2f".format(merged.globalGatherRateMultiplier)}x, Border=${"%.2f".format(merged.nationalBorderRadiusMultiplier)}x, MeshScale=${"%.2f".format(merged.customUnitMeshScale)}x)."
        )
    }

    fun evaluateGatherRate(type: ResourceType, baseRate: Int): Int {
        val h = _activeCombinedHooks.value
        val specificMult = when (type) {
            ResourceType.FOOD -> h.foodGatherMultiplier
            ResourceType.TIMBER -> h.timberGatherMultiplier
            ResourceType.WEALTH -> h.wealthGatherMultiplier
            ResourceType.METAL -> h.metalGatherMultiplier
            ResourceType.OIL -> h.oilGatherMultiplier
            ResourceType.KNOWLEDGE -> h.knowledgeGatherMultiplier
        }
        return (baseRate * h.globalGatherRateMultiplier * specificMult).toInt().coerceAtLeast(1)
    }

    fun evaluateCommerceCap(baseCap: Int): Int {
        return (baseCap * _activeCombinedHooks.value.commerceCapMultiplier).toInt().coerceAtLeast(50)
    }

    fun evaluateBorderRadius(baseRadius: Float): Float {
        return baseRadius * _activeCombinedHooks.value.nationalBorderRadiusMultiplier
    }

    fun evaluateAttritionDamage(baseDamagePerSec: Float): Float {
        return baseDamagePerSec * _activeCombinedHooks.value.enemyAttritionDamageMultiplier
    }

    fun evaluateUnitArchetype(base: UnitArchetype): UnitArchetype {
        val h = _activeCombinedHooks.value
        val specific = h.unitOverrides.lastOrNull { it.targetUnitId == base.id || it.targetUnitId == "ALL" }
        val hpMult = h.unitHpMultiplier * (specific?.hpMultiplier ?: 1.0f)
        val atkMult = h.unitAttackMultiplier * (specific?.attackMultiplier ?: 1.0f)
        val rngMult = specific?.rangeMultiplier ?: 1.0f
        val spdMult = h.unitMoveSpeedMultiplier * (specific?.speedMultiplier ?: 1.0f)
        val costMult = specific?.costMultiplier ?: 1.0f
        val scaleMult = h.customUnitMeshScale * (specific?.customMeshScale ?: 1.0f)

        return base.copy(
            maxHp = (base.maxHp * hpMult).coerceAtLeast(10f),
            attackDamage = (base.attackDamage * atkMult).coerceAtLeast(0f),
            attackRange = (base.attackRange * rngMult).coerceAtLeast(0f),
            moveSpeed = (base.moveSpeed * spdMult).coerceIn(2f, 25f),
            foodCost = (base.foodCost * costMult).toInt(),
            timberCost = (base.timberCost * costMult).toInt(),
            wealthCost = (base.wealthCost * costMult).toInt(),
            metalCost = (base.metalCost * costMult).toInt(),
            oilCost = (base.oilCost * costMult).toInt(),
            meshScale = (base.meshScale * scaleMult).coerceIn(0.5f, 2.4f)
        )
    }

    fun evaluateBuildingArchetype(base: BuildingArchetype): BuildingArchetype {
        val h = _activeCombinedHooks.value
        val isWonder = base.id.startsWith("wonder_")
        val costMult = if (isWonder) h.wonderCostMultiplier else 1.0f
        return base.copy(
            maxHp = (base.maxHp * h.buildingHpMultiplier).coerceAtLeast(100f),
            borderPushRadius = evaluateBorderRadius(base.borderPushRadius),
            timberCost = (base.timberCost * costMult).toInt(),
            wealthCost = (base.wealthCost * costMult).toInt(),
            metalCost = (base.metalCost * costMult).toInt(),
            oilCost = (base.oilCost * costMult).toInt(),
            knowledgeCost = (base.knowledgeCost * costMult).toInt()
        )
    }

    fun evaluateAgeCosts(age: HistoricalAge): Pair<Int, Int> {
        val mult = _activeCombinedHooks.value.ageAdvancementCostMultiplier
        return (age.foodCost * mult).toInt() to (age.knowledgeCost * mult).toInt()
    }
}
