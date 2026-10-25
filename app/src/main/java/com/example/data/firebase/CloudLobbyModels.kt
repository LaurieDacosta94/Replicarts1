package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MatchLobbyDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hostName: String = "",
    val lobbyTitle: String = "",
    val mapBiome: String = "Mediterranean",
    val startAge: String = "ANCIENT",
    val endAge: String = "INFORMATION",
    val maxPlayers: Long = 4L,
    val currentPlayers: Long = 1L,
    val activeModpackName: String = "Vanilla Imperial Standard",
    val status: String = "waiting",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(authenticatedUid: String): Map<String, Any> {
        return mapOf(
            "userId" to authenticatedUid,
            "hostName" to hostName.take(60).ifBlank { "Commander" },
            "lobbyTitle" to lobbyTitle.take(80).ifBlank { "Imperial Conquest" },
            "mapBiome" to mapBiome.take(40).ifBlank { "Mediterranean" },
            "startAge" to startAge.take(40).ifBlank { "ANCIENT" },
            "endAge" to endAge.take(40).ifBlank { "INFORMATION" },
            "maxPlayers" to maxPlayers.coerceIn(2L, 8L),
            "currentPlayers" to currentPlayers.coerceIn(1L, maxPlayers.coerceIn(2L, 8L)),
            "activeModpackName" to activeModpackName.take(80).ifBlank { "Vanilla Imperial Standard" },
            "status" to "waiting",
            "visibility" to if (visibility == "private") "private" else "public",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}

@IgnoreExtraProperties
data class CloudModpackDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val author: String = "",
    val version: String = "1.0.0",
    val description: String = "",
    val modCount: Long = 1L,
    val payloadJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(authenticatedUid: String): Map<String, Any> {
        return mapOf(
            "userId" to authenticatedUid,
            "name" to name.take(80).ifBlank { "Custom Modpack" },
            "author" to author.take(60).ifBlank { "Imperial Modder" },
            "version" to version.take(30).ifBlank { "1.0.0" },
            "description" to description.take(500).ifBlank { "Custom rule and hook overrides" },
            "modCount" to modCount.coerceIn(1L, 64L),
            "payloadJson" to payloadJson.take(50000).ifBlank { "{}" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}
