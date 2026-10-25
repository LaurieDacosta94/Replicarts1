package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_mods")
data class LocalModEntity(
    @PrimaryKey val modId: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val loadOrder: Int,
    val isEnabled: Boolean,
    val hookRulesJson: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_modpacks")
data class LocalModpackEntity(
    @PrimaryKey val modpackId: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val isActive: Boolean,
    val includedModIdsCsv: String,
    val bundleJson: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "skirmish_presets")
data class SavedSkirmishPresetEntity(
    @PrimaryKey val presetId: String,
    val title: String,
    val mapBiome: String,
    val playerCivilization: String,
    val aiCount: Int,
    val aiDifficulty: String,
    val aiPersonality: String,
    val startAge: String,
    val endAge: String,
    val startingResourcesLevel: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
