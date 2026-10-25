package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ImperialBronzeColorScheme = darkColorScheme(
    primary = ImperialBurnishedGold,
    onPrimary = Color(0xFF1A1208),
    primaryContainer = Color(0xFF3E2D16),
    onPrimaryContainer = ImperialGoldBright,
    secondary = CorinthianBronze,
    onSecondary = IlluminatedIvory,
    secondaryContainer = Color(0xFF332519),
    onSecondaryContainer = WarmParchmentVellum,
    tertiary = KnowledgeAzure,
    onTertiary = IlluminatedIvory,
    background = ObsidianSlateBg,
    onBackground = IlluminatedIvory,
    surface = DeepTacticalParchment,
    onSurface = IlluminatedIvory,
    surfaceVariant = ElevatedBronzeSurface,
    onSurfaceVariant = AgedBrassMuted,
    outline = BronzeBorderMuted,
    error = ImperialCrimson,
    onError = IlluminatedIvory
)

@Composable
fun CrownDominionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ImperialBronzeColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CrownDominionTheme(content = content)
}
