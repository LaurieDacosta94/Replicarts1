package com.example.engine.modding

import com.example.engine.lod.GraphicsConfig
import com.example.engine.model.CivilizationId
import com.example.engine.model.HistoricalAge
import com.example.engine.model.LibraryTrack
import com.example.engine.model.ResourceType
import com.example.engine.model.RtsCatalogRegistry
import com.example.engine.pool.ObjectPoolManager
import org.json.JSONArray
import org.json.JSONObject

object ApiExportGenerator {

    fun generateCompleteApiSpecificationJson(
        activeMods: List<ModDefinition>,
        graphicsConfig: GraphicsConfig
    ): String {
        val root = JSONObject()
        root.put("engineName", "Crown & Dominion: Rise of Ages 3D Engine")
        root.put("apiVersion", "2.4.0-RTS-ANDROID")
        root.put("architecture", "Deterministic 20Hz ECS Simulation + 60Hz 4-Tier LOD 3D Viewport + Pre-Allocated Object Pools")

        // 1. Deep Hook Endpoints
        val hooksArray = JSONArray()
        for (ep in DeepHookEndpoint.entries) {
            hooksArray.put(
                JSONObject().apply {
                    put("hookId", ep.hookId)
                    put("domain", ep.domain)
                    put("signature", ep.signature)
                    put("description", ep.description)
                }
            )
        }
        root.put("deepHookEndpoints", hooksArray)

        // 2. Resource Types & Gathering Mechanics
        val resourcesArray = JSONArray()
        for (res in ResourceType.entries) {
            resourcesArray.put(
                JSONObject().apply {
                    put("code", res.code)
                    put("displayName", res.displayName)
                    put("unlockedAgeIndex", res.unlockedAgeIndex)
                    put("gatherBuildingLabel", res.gatherBuildingLabel)
                    put("description", res.description)
                }
            )
        }
        root.put("resources", resourcesArray)

        // 3. Historical Ages (I - VIII)
        val agesArray = JSONArray()
        for (age in HistoricalAge.entries) {
            agesArray.put(
                JSONObject().apply {
                    put("index", age.index)
                    put("id", age.name)
                    put("title", age.title)
                    put("yearSpan", age.yearSpan)
                    put("baseFoodCost", age.foodCost)
                    put("baseKnowledgeCost", age.knowledgeCost)
                    put("requiredLibraryTotalLevels", age.requiredLibraryTotalLevels)
                    put("borderRadiusBonus", age.borderRadiusBonus.toDouble())
                }
            )
        }
        root.put("historicalAges", agesArray)

        // 4. Library Research Tracks
        val tracksArray = JSONArray()
        for (track in LibraryTrack.entries) {
            tracksArray.put(
                JSONObject().apply {
                    put("id", track.name)
                    put("title", track.title)
                    put("badgeCode", track.badgeCode)
                    put("effectSummary", track.effectSummary)
                }
            )
        }
        root.put("libraryTracks", tracksArray)

        // 5. Civilizations
        val civsArray = JSONArray()
        for (civ in CivilizationId.entries) {
            civsArray.put(
                JSONObject().apply {
                    put("id", civ.name)
                    put("displayName", civ.displayName)
                    put("powerTitle", civ.powerTitle)
                    put("uniqueUnitSummary", civ.uniqueUnitSummary)
                    put("nationalBonuses", JSONArray(civ.nationalBonuses))
                }
            )
        }
        root.put("civilizations", civsArray)

        // 6. Complete Unit Registry
        val unitsArray = JSONArray()
        for (u in RtsCatalogRegistry.allUnits) {
            val effective = ModHookRegistry.evaluateUnitArchetype(u)
            unitsArray.put(
                JSONObject().apply {
                    put("id", u.id)
                    put("name", u.name)
                    put("category", u.category.name)
                    put("minAge", u.minAge.name)
                    put("baseMaxHp", u.maxHp.toDouble())
                    put("effectiveMaxHp", effective.maxHp.toDouble())
                    put("baseAttackDamage", u.attackDamage.toDouble())
                    put("effectiveAttackDamage", effective.attackDamage.toDouble())
                    put("attackRange", effective.attackRange.toDouble())
                    put("moveSpeed", effective.moveSpeed.toDouble())
                    put("costs", JSONObject().apply {
                        put("food", effective.foodCost)
                        put("timber", effective.timberCost)
                        put("wealth", effective.wealthCost)
                        put("metal", effective.metalCost)
                        put("oil", effective.oilCost)
                    })
                    put("meshScale", effective.meshScale.toDouble())
                }
            )
        }
        root.put("unitArchetypes", unitsArray)

        // 7. Complete Building & Wonder Registry
        val buildingsArray = JSONArray()
        for (b in RtsCatalogRegistry.allBuildings) {
            val effective = ModHookRegistry.evaluateBuildingArchetype(b)
            buildingsArray.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("category", b.category.name)
                    put("minAge", b.minAge.name)
                    put("effectiveMaxHp", effective.maxHp.toDouble())
                    put("effectiveBorderPushRadius", effective.borderPushRadius.toDouble())
                    put("gatherResourceType", b.gatherResourceType?.name ?: JSONObject.NULL)
                    put("maxGatherers", b.maxGatherers)
                    put("wonderBonusSummary", b.wonderBonusSummary ?: JSONObject.NULL)
                }
            )
        }
        root.put("buildingAndWonderArchetypes", buildingsArray)

        // 8. LOD & Object Pool Telemetry
        val poolArray = JSONArray()
        for (snap in ObjectPoolManager.collectSnapshots()) {
            poolArray.put(
                JSONObject().apply {
                    put("poolName", snap.poolName)
                    put("capacity", snap.capacity)
                    put("activeCount", snap.activeCount)
                    put("peakActive", snap.peakActive)
                    put("totalAcquisitions", snap.totalAcquisitions)
                    put("reusedAllocationsSaved", snap.reusedAllocationsSaved)
                }
            )
        }
        root.put(
            "renderingAndPooling",
            JSONObject().apply {
                put("lod0MaxDistance", graphicsConfig.lod0MaxDistance.toDouble())
                put("lod1MaxDistance", graphicsConfig.lod1MaxDistance.toDouble())
                put("lod2MaxDistance", graphicsConfig.lod2MaxDistance.toDouble())
                put("terrainSubdivision", graphicsConfig.terrainSubdivision)
                put("objectPools", poolArray)
            }
        )

        // 9. Active Mods & Combined Hook State
        val activeModsArr = JSONArray()
        activeMods.forEach { activeModsArr.put(it.toJsonObject()) }
        root.put("installedMods", activeModsArr)
        root.put("activeMergedHooks", ModHookRegistry.activeCombinedHooks.value.toJsonObject())

        return root.toString(2)
    }

    fun generateMarkdownDeveloperGuide(
        activeMods: List<ModDefinition>,
        graphicsConfig: GraphicsConfig
    ): String = buildString {
        appendLine("# Crown & Dominion: 3D RTS Modding & Engine Hook API Specification")
        appendLine("Version: `2.4.0-RTS-ANDROID` | Active Mods: `${activeMods.count { it.isEnabled }} / ${activeMods.size}`")
        appendLine()
        appendLine("## 1. Deep Engine Hook Interceptors")
        for (ep in DeepHookEndpoint.entries) {
            appendLine("- **`${ep.hookId}`** (${ep.domain})")
            appendLine("  - Signature: `${ep.signature}`")
            appendLine("  - Description: ${ep.description}")
        }
        appendLine()
        appendLine("## 2. 3D Level of Detail (LOD) & Object Pool Architecture")
        appendLine("- **LOD0 (High-Detail Articulated 3D Mesh)**: `0m .. ${graphicsConfig.lod0MaxDistance.toInt()}m`")
        appendLine("- **LOD1 (Simplified Tactical 3D Mesh)**: `${graphicsConfig.lod0MaxDistance.toInt()}m .. ${graphicsConfig.lod1MaxDistance.toInt()}m`")
        appendLine("- **LOD2 (Shaded Octahedral Billboard Proxy)**: `${graphicsConfig.lod1MaxDistance.toInt()}m .. ${graphicsConfig.lod2MaxDistance.toInt()}m`")
        appendLine("- **LOD3 (Strategic Cartographic Emblem)**: `> ${graphicsConfig.lod2MaxDistance.toInt()}m`")
        appendLine()
        appendLine("## 3. Unit Archetype IDs (${RtsCatalogRegistry.allUnits.size} Total)")
        for (u in RtsCatalogRegistry.allUnits) {
            appendLine("- `${u.id}`: **${u.name}** (${u.minAge.shortTitle}) — HP `${u.maxHp.toInt()}`, ATK `${u.attackDamage.toInt()}`, RNG `${u.attackRange.toInt()}`")
        }
        appendLine()
        appendLine("## 4. Building & Wonder Archetype IDs (${RtsCatalogRegistry.allBuildings.size} Total)")
        for (b in RtsCatalogRegistry.allBuildings) {
            appendLine("- `${b.id}`: **${b.name}** (${b.minAge.shortTitle}) — HP `${b.maxHp.toInt()}`, Border Push `+${b.borderPushRadius.toInt()}m`")
        }
    }
}
