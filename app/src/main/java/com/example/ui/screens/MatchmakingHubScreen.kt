package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.firebase.GoogleAuthManager
import com.example.data.firebase.MatchLobbyDocument
import com.example.data.local.SavedSkirmishPresetEntity
import com.example.engine.model.CivilizationId
import com.example.engine.model.HistoricalAge
import com.example.ui.components.CollapsibleImperialContainer
import com.example.ui.theme.AgedBrassMuted
import com.example.ui.theme.BronzeBorderMuted
import com.example.ui.theme.ElevatedBronzeSurface
import com.example.ui.theme.IlluminatedIvory
import com.example.ui.theme.ImperialBurnishedGold
import com.example.ui.theme.ObsidianSlateBg
import com.example.ui.viewmodel.CloudUiState
import com.google.firebase.auth.FirebaseUser

@Composable
fun MatchmakingHubScreen(
    currentUser: FirebaseUser?,
    publicLobbiesState: CloudUiState<List<MatchLobbyDocument>>,
    skirmishPresets: List<SavedSkirmishPresetEntity>,
    onBackToBattlefield: () -> Unit,
    onLaunchOfflineSkirmish: (String, CivilizationId, Int, HistoricalAge, Boolean) -> Unit,
    onCreateOnlineLobby: (String, String, String, HistoricalAge, HistoricalAge, Int, String) -> Unit,
    onJoinOnlineLobby: (MatchLobbyDocument) -> Unit,
    onDeleteOnlineLobby: (String) -> Unit,
    onAuthChanged: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBackToBattlefield)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var offlineCollapsed by remember { mutableStateOf(false) }
    var onlineCollapsed by remember { mutableStateOf(false) }
    var presetsCollapsed by remember { mutableStateOf(false) }

    val biomes = listOf(
        "Mediterranean Basin",
        "Continental Heartland",
        "Arid Oil Mesa",
        "Archipelago Atolls",
        "Highland Passes",
        "Twin Rivers Delta"
    )
    var selectedBiome by remember { mutableStateOf(biomes.first()) }
    var selectedCiv by remember { mutableStateOf(CivilizationId.ROMANS) }
    var selectedAiCount by remember { mutableIntStateOf(3) }
    var selectedStartAge by remember { mutableStateOf(HistoricalAge.ANCIENT) }
    var selectedEndAge by remember { mutableStateOf(HistoricalAge.INFORMATION) }

    var lobbyTitleInput by remember { mutableStateOf("Imperial Conquest 4v4") }
    var maxPlayersInput by remember { mutableIntStateOf(4) }
    var isSigningIn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianSlateBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToBattlefield,
                    modifier = Modifier.testTag("matchmaking_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to 3D Battlefield",
                        tint = ImperialBurnishedGold
                    )
                }
                Column {
                    Text(
                        text = "Matchmaking Command Citadel",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ImperialBurnishedGold
                    )
                    Text(
                        text = "Offline AI Skirmish • Spectator AI Arena • Online Cloud Lobbies",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgedBrassMuted
                    )
                }
            }
            Button(
                onClick = onBackToBattlefield,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElevatedBronzeSurface,
                    contentColor = ImperialBurnishedGold
                ),
                modifier = Modifier.testTag("resume_active_battle_button")
            ) {
                Text("Resume Active Match", style = MaterialTheme.typography.labelMedium)
            }
        }

        // 1. Offline AI Skirmish & Spectator Match Configurator
        CollapsibleImperialContainer(
            title = "Offline Skirmish & AI Spectator Match Setup",
            subtitle = "Customize Nations, 3D Map Biome, Historical Epochs & AI Opponents",
            isCollapsed = offlineCollapsed,
            onToggleCollapse = { offlineCollapsed = !offlineCollapsed },
            testTagPrefix = "offline_matchmaking_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Your Civilization (8 Asymmetric Empires):",
                    style = MaterialTheme.typography.titleSmall,
                    color = IlluminatedIvory
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (civ in CivilizationId.entries) {
                        FilterChip(
                            selected = selectedCiv == civ,
                            onClick = { selectedCiv = civ },
                            label = {
                                Text(
                                    text = "${civ.displayName} (${civ.powerTitle})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("select_civ_${civ.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ImperialBurnishedGold,
                                selectedLabelColor = Color(0xFF1A1208)
                            )
                        )
                    }
                }

                // Selected Civ Bonus Card
                Surface(
                    color = Color(0xFF17120D),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(selectedCiv.primaryColorHex)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "${selectedCiv.displayName} — ${selectedCiv.uniqueUnitSummary}",
                            style = MaterialTheme.typography.titleSmall,
                            color = ImperialBurnishedGold
                        )
                        selectedCiv.nationalBonuses.forEach { bonus ->
                            Text(
                                text = "• $bonus",
                                style = MaterialTheme.typography.bodySmall,
                                color = IlluminatedIvory
                            )
                        }
                    }
                }

                Text(
                    text = "Select 3D Terrain Biome:",
                    style = MaterialTheme.typography.titleSmall,
                    color = IlluminatedIvory
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (biome in biomes) {
                        FilterChip(
                            selected = selectedBiome == biome,
                            onClick = { selectedBiome = biome },
                            label = { Text(biome, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("select_biome_${biome.take(6).lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Starting Epoch:",
                        style = MaterialTheme.typography.titleSmall,
                        color = IlluminatedIvory
                    )
                    for (age in HistoricalAge.entries) {
                        FilterChip(
                            selected = selectedStartAge == age,
                            onClick = { selectedStartAge = age },
                            label = { Text(age.shortTitle, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("select_start_age_${age.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onLaunchOfflineSkirmish(
                                selectedBiome,
                                selectedCiv,
                                selectedAiCount,
                                selectedStartAge,
                                false
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImperialBurnishedGold,
                            contentColor = Color(0xFF1A1208)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_offline_skirmish_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Launch 3D Skirmish",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onLaunchOfflineSkirmish(
                                selectedBiome,
                                selectedCiv,
                                selectedAiCount,
                                selectedStartAge,
                                true
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_spectator_match_button")
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Spectate AI vs AI", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. Online Cloud Matchmaking Lobbies (Firestore + Google Sign-In)
        CollapsibleImperialContainer(
            title = "Online Cloud Matchmaking Lobbies (Firebase Firestore)",
            subtitle = if (currentUser != null) {
                "Authenticated Commander: ${currentUser.displayName ?: currentUser.email ?: currentUser.uid}"
            } else {
                "Sign in with Google to host or join real-time cloud matchmaking rooms"
            },
            isCollapsed = onlineCollapsed,
            onToggleCollapse = { onlineCollapsed = !onlineCollapsed },
            testTagPrefix = "online_matchmaking_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentUser == null) {
                Surface(
                    color = Color(0xFF17120D),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BronzeBorderMuted),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = ImperialBurnishedGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Online Cloud Matchmaking & Modpack Sync",
                            style = MaterialTheme.typography.titleMedium,
                            color = IlluminatedIvory
                        )
                        Text(
                            text = "Authenticate with Google Sign-In to browse live multiplayer rooms, host custom modpack lobbies, and sync your modpacks to the cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgedBrassMuted
                        )
                        Button(
                            onClick = {
                                isSigningIn = true
                                GoogleAuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onAuthChanged()
                                        onShowMessage("Signed in with Google!")
                                    },
                                    onAuthError = { err ->
                                        isSigningIn = false
                                        onShowMessage(err)
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImperialBurnishedGold,
                                contentColor = Color(0xFF1A1208)
                            ),
                            modifier = Modifier.testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF1A1208)
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Signed in as ${currentUser.displayName ?: currentUser.email ?: "Commander"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ImperialBurnishedGold
                        )
                        OutlinedButton(
                            onClick = {
                                GoogleAuthManager.signOut(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {
                                        onAuthChanged()
                                        onShowMessage("Signed out of online session.")
                                    },
                                    scope = scope
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("google_sign_out_button")
                        ) {
                            Text("Sign Out", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Host New Online Lobby Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = lobbyTitleInput,
                            onValueChange = { lobbyTitleInput = it },
                            label = { Text("Lobby Title") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("online_lobby_title_input")
                        )
                        Button(
                            onClick = {
                                onCreateOnlineLobby(
                                    currentUser.displayName ?: "Imperator",
                                    lobbyTitleInput,
                                    selectedBiome,
                                    selectedStartAge,
                                    selectedEndAge,
                                    maxPlayersInput,
                                    "public"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImperialBurnishedGold,
                                contentColor = Color(0xFF1A1208)
                            ),
                            modifier = Modifier.testTag("host_online_lobby_button")
                        ) {
                            Text("Host Room", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    when (publicLobbiesState) {
                        is CloudUiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Querying public matchmaking lobbies...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is CloudUiState.Error -> {
                            Text(
                                text = publicLobbiesState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is CloudUiState.Success -> {
                            val lobbies = publicLobbiesState.data
                            if (lobbies.isEmpty()) {
                                Text(
                                    text = "No open public lobbies right now. Click 'Host Room' above to create the first room!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AgedBrassMuted
                                )
                            } else {
                                lobbies.forEach { lobby ->
                                    Surface(
                                        color = Color(0xFF17120D),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BronzeBorderMuted),
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
                                                    text = "${lobby.lobbyTitle} (${lobby.currentPlayers}/${lobby.maxPlayers})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = ImperialBurnishedGold
                                                )
                                                Text(
                                                    text = "Host: ${lobby.hostName} • Map: ${lobby.mapBiome} • ${lobby.startAge} -> ${lobby.endAge}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = IlluminatedIvory
                                                )
                                                Text(
                                                    text = "Modpack: ${lobby.activeModpackName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AgedBrassMuted
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { onJoinOnlineLobby(lobby) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("join_lobby_${lobby.id}")
                                                ) {
                                                    Text("Join & Launch", style = MaterialTheme.typography.labelSmall)
                                                }
                                                if (lobby.userId == currentUser.uid) {
                                                    OutlinedButton(
                                                        onClick = { onDeleteOnlineLobby(lobby.id) },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Close", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        CloudUiState.Unauthenticated -> {}
                    }
                }
            }
        }

        // 3. Saved Local Skirmish Templates
        CollapsibleImperialContainer(
            title = "Quick-Launch Historical Skirmish Presets (${skirmishPresets.size})",
            subtitle = "Pre-configured scenarios stored in local Room database",
            isCollapsed = presetsCollapsed,
            onToggleCollapse = { presetsCollapsed = !presetsCollapsed },
            testTagPrefix = "skirmish_presets_panel",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                skirmishPresets.forEach { preset ->
                    Surface(
                        color = Color(0xFF17120D),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BronzeBorderMuted),
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
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ImperialBurnishedGold
                                )
                                Text(
                                    text = "${preset.mapBiome} • ${preset.playerCivilization} • ${preset.startAge} -> ${preset.endAge}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AgedBrassMuted
                                )
                            }
                            Button(
                                onClick = {
                                    val civ = CivilizationId.entries.firstOrNull { it.name == preset.playerCivilization }
                                        ?: CivilizationId.ROMANS
                                    val age = HistoricalAge.entries.firstOrNull { it.name == preset.startAge }
                                        ?: HistoricalAge.ANCIENT
                                    onLaunchOfflineSkirmish(preset.mapBiome, civ, preset.aiCount, age, false)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("launch_preset_${preset.presetId}")
                            ) {
                                Text("Play Preset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
