package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudMatchmakingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createLobby_andObservePublicLobbies_succeedsForAuthenticatedUsers() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudMatchmakingRepository(firestore)

        val draft = MatchLobbyDocument(
            hostName = "Emperor Trajan",
            lobbyTitle = "Continental Domination 4v4",
            mapBiome = "Continental",
            startAge = "ANCIENT",
            endAge = "INFORMATION",
            maxPlayers = 6L,
            currentPlayers = 1L,
            activeModpackName = "Vanilla Imperial Standard",
            visibility = "public"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createLobby(draft) }
        assertTrue(createResult.isSuccess)
        val lobbyId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicLobbies().first { list -> list.any { it.id == lobbyId } }
        }
        assertTrue(emitted.any { it.id == lobbyId && it.lobbyTitle == "Continental Domination 4v4" })
    }

    @Test
    fun saveCloudModpack_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudMatchmakingRepository(firestore)

        val packDraft = CloudModpackDocument(
            name = "Alice Imperial Balance Pack",
            author = "Commander Alice",
            version = "2.1.0",
            description = "Increases border radius and citizen speed",
            modCount = 2L,
            payloadJson = "{\"mods\":[\"fast_citizens\"]}"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveCloudModpack(packDraft) }
        assertTrue(createResult.isSuccess)
        val modpackId = createResult.getOrThrow()

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudMatchmakingRepository(firestore)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getCloudModpackById(modpackId) }
        assertTrue(bobReadResult.isFailure)
        val fsEx = bobReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observePublicLobbies_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CloudMatchmakingRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublicLobbies().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_rts@test.com"
        const val BOB_EMAIL = "bob_rts@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
