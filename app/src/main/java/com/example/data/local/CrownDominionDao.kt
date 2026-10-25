package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CrownDominionDao {

    @Query("SELECT * FROM local_mods ORDER BY loadOrder ASC, updatedAtMillis DESC")
    fun observeAllLocalMods(): Flow<List<LocalModEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalMod(mod: LocalModEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalMods(mods: List<LocalModEntity>)

    @Query("DELETE FROM local_mods WHERE modId = :modId")
    suspend fun deleteLocalMod(modId: String)

    @Query("SELECT * FROM local_modpacks ORDER BY updatedAtMillis DESC")
    fun observeAllLocalModpacks(): Flow<List<LocalModpackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalModpack(modpack: LocalModpackEntity)

    @Query("DELETE FROM local_modpacks WHERE modpackId = :modpackId")
    suspend fun deleteLocalModpack(modpackId: String)

    @Query("SELECT * FROM skirmish_presets ORDER BY createdAtMillis DESC")
    fun observeSkirmishPresets(): Flow<List<SavedSkirmishPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkirmishPreset(preset: SavedSkirmishPresetEntity)

    @Query("DELETE FROM skirmish_presets WHERE presetId = :presetId")
    suspend fun deleteSkirmishPreset(presetId: String)
}
