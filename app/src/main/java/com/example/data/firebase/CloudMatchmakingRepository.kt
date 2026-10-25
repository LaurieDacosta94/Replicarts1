package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CloudMatchmakingRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePublicLobbies(): Flow<List<MatchLobbyDocument>> {
        val path = COLLECTION_MATCH_LOBBIES
        return db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MatchLobbyDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeUserLobbies(userId: String): Flow<List<MatchLobbyDocument>> {
        val path = COLLECTION_MATCH_LOBBIES
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MatchLobbyDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createLobby(draft: MatchLobbyDocument): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_MATCH_LOBBIES)
            return Result.failure(e)
        }
        val lobbyId = draft.id.takeIf { it.isNotBlank() } ?: "lobby_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_MATCH_LOBBIES).document(lobbyId)
        return try {
            docRef.set(draft.toCreateMap(uid)).await()
            Result.success(lobbyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getLobbyById(lobbyId: String): Result<MatchLobbyDocument> {
        val docRef = db.collection(COLLECTION_MATCH_LOBBIES).document(lobbyId)
        return try {
            val snapshot = docRef.get().await()
            val lobby = snapshot.toObject(
                MatchLobbyDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Lobby $lobbyId not found")
            Result.success(lobby)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinPublicLobby(lobbyId: String, currentPlayers: Long, maxPlayers: Long): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_MATCH_LOBBIES).document(lobbyId)
        val nextCount = (currentPlayers + 1L).coerceAtMost(maxPlayers)
        return try {
            docRef.update(
                mapOf(
                    "currentPlayers" to nextCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateLobbyStatus(lobbyId: String, newStatus: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_MATCH_LOBBIES).document(lobbyId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteLobby(lobbyId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_MATCH_LOBBIES).document(lobbyId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserCloudModpacks(userId: String): Flow<List<CloudModpackDocument>> {
        val path = COLLECTION_CLOUD_MODPACKS
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudModpackDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun saveCloudModpack(draft: CloudModpackDocument): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_CLOUD_MODPACKS)
            return Result.failure(e)
        }
        val packId = draft.id.takeIf { it.isNotBlank() } ?: "modpack_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_CLOUD_MODPACKS).document(packId)
        return try {
            docRef.set(draft.toCreateMap(uid)).await()
            Result.success(packId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCloudModpackById(modpackId: String): Result<CloudModpackDocument> {
        val docRef = db.collection(COLLECTION_CLOUD_MODPACKS).document(modpackId)
        return try {
            val snapshot = docRef.get().await()
            val pack = snapshot.toObject(
                CloudModpackDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Cloud modpack $modpackId not found")
            Result.success(pack)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudModpack(modpackId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_CLOUD_MODPACKS).document(modpackId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_MATCH_LOBBIES = "match_lobbies"
        const val COLLECTION_CLOUD_MODPACKS = "cloud_modpacks"
    }
}
