package com.example.data.local

import com.example.engine.logging.EngineConsoleLogger
import com.example.engine.logging.LogCategory
import com.example.engine.logging.LogSeverity
import com.example.engine.modding.CoreRuleHooks
import com.example.engine.modding.ModDefinition
import com.example.engine.modding.ModHookRegistry
import com.example.engine.modding.ModpackBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class LocalModAndPresetRepository(private val dao: CrownDominionDao) {

    val allModsFlow: Flow<List<ModDefinition>> = dao.observeAllLocalMods().map { entities ->
        entities.map { entity ->
            val hooksObj = try {
                JSONObject(entity.hookRulesJson)
            } catch (e: Exception) {
                JSONObject()
            }
            ModDefinition(
                modId = entity.modId,
                name = entity.name,
                author = entity.author,
                version = entity.version,
                description = entity.description,
                loadOrder = entity.loadOrder,
                isEnabled = entity.isEnabled,
                hooks = CoreRuleHooks.fromJsonObject(hooksObj)
            )
        }
    }

    val allModpacksFlow: Flow<List<ModpackBundle>> = dao.observeAllLocalModpacks().map { entities ->
        entities.mapNotNull { entity ->
            ModpackBundle.fromJsonString(entity.bundleJson).getOrNull()?.copy(
                modpackId = entity.modpackId,
                name = entity.name,
                author = entity.author,
                version = entity.version,
                description = entity.description,
                isActive = entity.isActive
            )
        }
    }

    val skirmishPresetsFlow: Flow<List<SavedSkirmishPresetEntity>> = dao.observeSkirmishPresets()

    suspend fun seedDefaultsIfEmpty() {
        val existingMods = dao.observeAllLocalMods().first()
        if (existingMods.isEmpty()) {
            val entities = ModHookRegistry.defaultBuiltInMods.map { mod ->
                LocalModEntity(
                    modId = mod.modId,
                    name = mod.name,
                    author = mod.author,
                    version = mod.version,
                    description = mod.description,
                    loadOrder = mod.loadOrder,
                    isEnabled = mod.isEnabled,
                    hookRulesJson = mod.hooks.toJsonObject().toString()
                )
            }
            dao.upsertLocalMods(entities)
        }

        val existingPacks = dao.observeAllLocalModpacks().first()
        if (existingPacks.isEmpty()) {
            for (pack in ModHookRegistry.defaultBuiltInModpacks) {
                dao.upsertLocalModpack(
                    LocalModpackEntity(
                        modpackId = pack.modpackId,
                        name = pack.name,
                        author = pack.author,
                        version = pack.version,
                        description = pack.description,
                        isActive = pack.isActive,
                        includedModIdsCsv = pack.mods.joinToString(",") { it.modId },
                        bundleJson = pack.toPrettyJson()
                    )
                )
            }
        }

        val existingPresets = dao.observeSkirmishPresets().first()
        if (existingPresets.isEmpty()) {
            dao.upsertSkirmishPreset(
                SavedSkirmishPresetEntity(
                    presetId = "preset_roman_heartland",
                    title = "Mediterranean Imperial Clash (4 Nations)",
                    mapBiome = "Mediterranean Basin",
                    playerCivilization = "ROMANS",
                    aiCount = 3,
                    aiDifficulty = "Commander",
                    aiPersonality = "Balanced Expansionist",
                    startAge = "ANCIENT",
                    endAge = "INFORMATION",
                    startingResourcesLevel = "Standard Imperial"
                )
            )
            dao.upsertSkirmishPreset(
                SavedSkirmishPresetEntity(
                    presetId = "preset_modern_blitz",
                    title = "Industrial-to-Information Armor Blitz",
                    mapBiome = "Arid Oil Mesa",
                    playerCivilization = "AMERICANS",
                    aiCount = 3,
                    aiDifficulty = "Emperor",
                    aiPersonality = "Aggressive Blitzkrieg",
                    startAge = "INDUSTRIAL",
                    endAge = "INFORMATION",
                    startingResourcesLevel = "High War Chest"
                )
            )
        }
    }

    suspend fun saveOrUpdateMod(mod: ModDefinition) {
        dao.upsertLocalMod(
            LocalModEntity(
                modId = mod.modId,
                name = mod.name,
                author = mod.author,
                version = mod.version,
                description = mod.description,
                loadOrder = mod.loadOrder,
                isEnabled = mod.isEnabled,
                hookRulesJson = mod.hooks.toJsonObject().toString(),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        EngineConsoleLogger.log(
            category = LogCategory.HOOK,
            severity = LogSeverity.INFO,
            message = "Saved mod '${mod.name}' (v${mod.version}, enabled=${mod.isEnabled}, loadOrder=${mod.loadOrder})."
        )
    }

    suspend fun deleteMod(modId: String) {
        dao.deleteLocalMod(modId)
        EngineConsoleLogger.log(
            category = LogCategory.HOOK,
            severity = LogSeverity.INFO,
            message = "Deleted local mod '$modId'."
        )
    }

    suspend fun saveOrUpdateModpack(pack: ModpackBundle) {
        dao.upsertLocalModpack(
            LocalModpackEntity(
                modpackId = pack.modpackId,
                name = pack.name,
                author = pack.author,
                version = pack.version,
                description = pack.description,
                isActive = pack.isActive,
                includedModIdsCsv = pack.mods.joinToString(",") { it.modId },
                bundleJson = pack.toPrettyJson(),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        for (mod in pack.mods) {
            saveOrUpdateMod(mod.copy(isEnabled = pack.isActive))
        }
        EngineConsoleLogger.log(
            category = LogCategory.HOOK,
            severity = LogSeverity.INFO,
            message = "Saved Modpack '${pack.name}' (${pack.mods.size} mods, active=${pack.isActive})."
        )
    }

    suspend fun deleteModpack(modpackId: String) {
        dao.deleteLocalModpack(modpackId)
    }

    suspend fun saveSkirmishPreset(preset: SavedSkirmishPresetEntity) {
        dao.upsertSkirmishPreset(preset)
    }
}
