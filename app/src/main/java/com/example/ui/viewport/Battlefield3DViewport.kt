package com.example.ui.viewport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.engine.lod.Camera3DState
import com.example.engine.lod.GraphicsConfig
import com.example.engine.lod.LodFrameTelemetry
import com.example.engine.lod.LodRenderEngine
import com.example.engine.lod.LodTier
import com.example.engine.modding.ModHookRegistry
import com.example.engine.model.FogOfWarMode
import com.example.engine.model.ResourceTrackingState
import com.example.engine.model.ResourceType
import com.example.engine.model.RtsCatalogRegistry
import com.example.engine.model.SpectatorModeState
import com.example.engine.model.UnitCategory
import com.example.engine.pool.ObjectPoolManager
import com.example.engine.simulation.SimBuildingInstance
import com.example.engine.simulation.SimUnitInstance
import com.example.engine.simulation.WorldSimulationSnapshot
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun Battlefield3DViewport(
    worldState: WorldSimulationSnapshot,
    cameraState: Camera3DState,
    graphicsConfig: GraphicsConfig,
    resourceTracking: ResourceTrackingState,
    spectatorState: SpectatorModeState,
    isMarqueeBoxMode: Boolean,
    onPanCamera: (Float, Float) -> Unit,
    onZoomAndOrbit: (Float, Float, Float) -> Unit,
    onWorldTap: (Float, Float, Boolean) -> Unit,
    onMarqueeSelect: (Float, Float, Float, Float) -> Unit,
    onReportLodTelemetry: (LodFrameTelemetry) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var viewportWidth by remember { mutableStateOf(1080f) }
    var viewportHeight by remember { mutableStateOf(1920f) }

    var marqueeStartScreen by remember { mutableStateOf<Offset?>(null) }
    var marqueeEndScreen by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF10130E))
            .testTag("battlefield_3d_viewport")
            .pointerInput(isMarqueeBoxMode, cameraState) {
                if (isMarqueeBoxMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            marqueeStartScreen = offset
                            marqueeEndScreen = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            marqueeEndScreen = change.position
                        },
                        onDragEnd = {
                            val s = marqueeStartScreen
                            val e = marqueeEndScreen
                            if (s != null && e != null) {
                                val (wx1, wz1) = LodRenderEngine.screenToWorldGroundPlane(
                                    s.x, s.y, cameraState, viewportWidth, viewportHeight
                                )
                                val (wx2, wz2) = LodRenderEngine.screenToWorldGroundPlane(
                                    e.x, e.y, cameraState, viewportWidth, viewportHeight
                                )
                                onMarqueeSelect(
                                    min(wx1, wx2),
                                    min(wz1, wz2),
                                    max(wx1, wx2),
                                    max(wz1, wz2)
                                )
                            }
                            marqueeStartScreen = null
                            marqueeEndScreen = null
                        },
                        onDragCancel = {
                            marqueeStartScreen = null
                            marqueeEndScreen = null
                        }
                    )
                } else {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        if (zoom != 1.0f || kotlin.math.abs(rotation) > 0.2f) {
                            onZoomAndOrbit(zoom, rotation * 0.45f, 0f)
                        } else {
                            val yawRad = Math.toRadians(cameraState.yawDegrees.toDouble())
                            val cosYaw = cos(yawRad).toFloat()
                            val sinYaw = sin(yawRad).toFloat()
                            val panScale = (cameraState.distance / 650f).coerceIn(0.05f, 0.45f)
                            val dx = (-pan.x * cosYaw - pan.y * sinYaw) * panScale
                            val dz = (pan.x * sinYaw - pan.y * cosYaw) * panScale
                            onPanCamera(dx, dz)
                        }
                    }
                }
            }
            .pointerInput(cameraState) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val (wx, wz) = LodRenderEngine.screenToWorldGroundPlane(
                            tapOffset.x, tapOffset.y, cameraState, size.width.toFloat(), size.height.toFloat()
                        )
                        onWorldTap(wx, wz, false)
                    },
                    onDoubleTap = { tapOffset ->
                        val (wx, wz) = LodRenderEngine.screenToWorldGroundPlane(
                            tapOffset.x, tapOffset.y, cameraState, size.width.toFloat(), size.height.toFloat()
                        )
                        onWorldTap(wx, wz, true)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (viewportWidth != size.width && size.width > 1f) {
                viewportWidth = size.width
            }
            if (viewportHeight != size.height && size.height > 1f) {
                viewportHeight = size.height
            }

            // 1. Sky & Horizon Atmosphere
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141C24),
                        Color(0xFF1B261D),
                        Color(0xFF141B12)
                    )
                )
            )

            // 2. Render 3D Terrain Grid, River & Elevation Contours
            draw3DTerrainMesh(
                worldState = worldState,
                camera = cameraState,
                graphicsConfig = graphicsConfig
            )

            // 3. Render National Borders & Territorial Influence Rings
            drawNationalBorders3D(
                worldState = worldState,
                camera = cameraState,
                graphicsConfig = graphicsConfig
            )

            // 4. Render World Resource Deposits
            drawResourceDeposits3D(
                worldState = worldState,
                camera = cameraState,
                trackedResource = resourceTracking.trackedResource
            )

            // 5. Render 3D Buildings & Wonders (Depth-Sorted)
            val sortedBuildings = worldState.buildings.sortedByDescending { bld ->
                cameraState.distanceToPoint(bld.worldX, bld.worldY, bld.worldZ)
            }
            for (bld in sortedBuildings) {
                drawBuilding3D(
                    bld = bld,
                    worldState = worldState,
                    camera = cameraState,
                    graphicsConfig = graphicsConfig,
                    resourceTracking = resourceTracking,
                    textMeasurer = textMeasurer
                )
            }

            // 6. Render 3D Units with 4-Tier Level of Detail (LOD0, LOD1, LOD2, LOD3)
            var lod0 = 0
            var lod1 = 0
            var lod2 = 0
            var lod3 = 0
            var culled = 0
            var polySum = 0

            val sortedUnits = worldState.units.sortedByDescending { u ->
                cameraState.distanceToPoint(u.worldX, u.worldY, u.worldZ)
            }
            val activePovNation = spectatorState.observedNationId
            val revealAll = spectatorState.fogOfWarMode == FogOfWarMode.REVEAL_ALL_SPECTATOR || spectatorState.isSpectatorActive

            for (unit in sortedUnits) {
                val proj = LodRenderEngine.projectWorldToScreen(
                    unit.worldX, unit.worldY, unit.worldZ, cameraState, size.width, size.height
                )
                if (!proj.isVisible) {
                    culled++
                    continue
                }

                // Fog of war check for enemy units
                if (!revealAll && unit.nationId != activePovNation) {
                    val visibleToFriendly = worldState.units.any { f ->
                        f.nationId == activePovNation && hypot(f.worldX - unit.worldX, f.worldZ - unit.worldZ) <= 44f
                    } || worldState.buildings.any { b ->
                        b.nationId == activePovNation && hypot(b.worldX - unit.worldX, b.worldZ - unit.worldZ) <= 52f
                    }
                    if (!visibleToFriendly) {
                        culled++
                        continue
                    }
                }

                val tier = graphicsConfig.evaluateLodTier(proj.distanceToCamera)
                when (tier) {
                    LodTier.LOD0_HIGH_MESH -> lod0++
                    LodTier.LOD1_MEDIUM_MESH -> lod1++
                    LodTier.LOD2_BILLBOARD_PROXY -> lod2++
                    LodTier.LOD3_STRATEGIC_ICON -> lod3++
                }
                polySum += tier.polygonEstimate

                drawUnitWithLod3D(
                    unit = unit,
                    projX = proj.screenX,
                    projY = proj.screenY,
                    scaleFactor = proj.scaleFactor,
                    lodTier = tier,
                    worldState = worldState,
                    graphicsConfig = graphicsConfig,
                    textMeasurer = textMeasurer
                )
            }

            // 7. Render Pre-Allocated Pooled Projectiles & Particles
            drawPooledProjectilesAndParticles3D(cameraState)

            // 8. Render Active Stock Resource Map-Tracking Golden Beacons & Locator Rings
            if (resourceTracking.trackedResource != null) {
                drawResourceMapTrackingBeacons3D(
                    resourceTracking = resourceTracking,
                    camera = cameraState,
                    textMeasurer = textMeasurer
                )
            }

            // 9. Render Marquee Selection Box if active
            val mStart = marqueeStartScreen
            val mEnd = marqueeEndScreen
            if (mStart != null && mEnd != null) {
                val topLeft = Offset(min(mStart.x, mEnd.x), min(mStart.y, mEnd.y))
                val boxSize = Size(
                    kotlin.math.abs(mEnd.x - mStart.x),
                    kotlin.math.abs(mEnd.y - mStart.y)
                )
                drawRect(
                    color = Color(0x38D4AF37),
                    topLeft = topLeft,
                    size = boxSize
                )
                drawRect(
                    color = Color(0xFFD4AF37),
                    topLeft = topLeft,
                    size = boxSize,
                    style = Stroke(width = 2.5f)
                )
            }

            LodRenderEngine.latestFrameTelemetry = LodFrameTelemetry(
                lod0Count = lod0,
                lod1Count = lod1,
                lod2Count = lod2,
                lod3Count = lod3,
                culledCount = culled,
                estimatedTrianglesRendered = polySum + graphicsConfig.terrainSubdivision * graphicsConfig.terrainSubdivision * 2,
                frameTimeMs = (4.8f + polySum * 0.0012f).coerceIn(4.2f, 16.0f)
            )
        }
    }
}

private fun DrawScope.draw3DTerrainMesh(
    worldState: WorldSimulationSnapshot,
    camera: Camera3DState,
    graphicsConfig: GraphicsConfig
) {
    val subdiv = graphicsConfig.terrainSubdivision.coerceIn(14, 40)
    val step = worldState.worldSize / subdiv

    // Draw 3D ground grid lines
    val gridColor = Color(0x268C6D46)
    for (i in 0..subdiv) {
        val coord = i * step
        val pStart = LodRenderEngine.projectWorldToScreen(coord, 0f, 0f, camera, size.width, size.height)
        val pEnd = LodRenderEngine.projectWorldToScreen(coord, 0f, worldState.worldSize, camera, size.width, size.height)
        if (pStart.isVisible || pEnd.isVisible) {
            drawLine(
                color = gridColor,
                start = Offset(pStart.screenX, pStart.screenY),
                end = Offset(pEnd.screenX, pEnd.screenY),
                strokeWidth = 1f
            )
        }

        val qStart = LodRenderEngine.projectWorldToScreen(0f, 0f, coord, camera, size.width, size.height)
        val qEnd = LodRenderEngine.projectWorldToScreen(worldState.worldSize, 0f, coord, camera, size.width, size.height)
        if (qStart.isVisible || qEnd.isVisible) {
            drawLine(
                color = gridColor,
                start = Offset(qStart.screenX, qStart.screenY),
                end = Offset(qEnd.screenX, qEnd.screenY),
                strokeWidth = 1f
            )
        }
    }

    // Draw diagonal strategic river channel across the basin
    val riverPath = Path()
    var firstPoint = true
    for (t in 0..24) {
        val frac = t / 24f
        val rx = 18f + frac * 204f
        val rz = 120f + sin(frac * 6.28f) * 28f
        val proj = LodRenderEngine.projectWorldToScreen(rx, -0.4f, rz, camera, size.width, size.height)
        if (firstPoint) {
            riverPath.moveTo(proj.screenX, proj.screenY)
            firstPoint = false
        } else {
            riverPath.lineTo(proj.screenX, proj.screenY)
        }
    }
    drawPath(
        path = riverPath,
        color = if (graphicsConfig.enableWaterReflections) Color(0x662B6CB0) else Color(0x4422548A),
        style = Stroke(width = 18f)
    )
}

private fun DrawScope.drawNationalBorders3D(
    worldState: WorldSimulationSnapshot,
    camera: Camera3DState,
    graphicsConfig: GraphicsConfig
) {
    if (!graphicsConfig.enableBorderAuraGlow) return

    for (bld in worldState.buildings) {
        if (!bld.isConstructed) continue
        val arch = ModHookRegistry.evaluateBuildingArchetype(RtsCatalogRegistry.findBuilding(bld.archetypeId))
        if (arch.borderPushRadius <= 0f) continue

        val nation = worldState.nations.firstOrNull { it.nationId == bld.nationId }
        val borderColor = Color(nation?.civilization?.primaryColorHex ?: 0xFFD4AF37)
        val radius = arch.borderPushRadius

        val borderPath = Path()
        val segments = 28
        for (s in 0..segments) {
            val angle = (s.toFloat() / segments) * (Math.PI.toFloat() * 2f)
            val bx = (bld.worldX + cos(angle) * radius).coerceIn(4f, 236f)
            val bz = (bld.worldZ + sin(angle) * radius).coerceIn(4f, 236f)
            val proj = LodRenderEngine.projectWorldToScreen(bx, 0.2f, bz, camera, size.width, size.height)
            if (s == 0) {
                borderPath.moveTo(proj.screenX, proj.screenY)
            } else {
                borderPath.lineTo(proj.screenX, proj.screenY)
            }
        }
        drawPath(
            path = borderPath,
            color = borderColor.copy(alpha = 0.11f)
        )
        drawPath(
            path = borderPath,
            color = borderColor.copy(alpha = 0.72f),
            style = Stroke(
                width = 3.0f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
            )
        )
    }
}

private fun DrawScope.drawResourceDeposits3D(
    worldState: WorldSimulationSnapshot,
    camera: Camera3DState,
    trackedResource: ResourceType?
) {
    for (node in worldState.resourceNodes) {
        val proj = LodRenderEngine.projectWorldToScreen(
            node.worldX, node.elevationY, node.worldZ, camera, size.width, size.height
        )
        if (!proj.isVisible) continue

        val resColor = Color(node.resourceType.colorHex)
        val isTrackedMatch = trackedResource == node.resourceType
        val r = (9f * proj.scaleFactor).coerceIn(5f, 22f)

        if (isTrackedMatch) {
            drawCircle(
                color = resColor.copy(alpha = 0.35f),
                radius = r * 2.2f,
                center = Offset(proj.screenX, proj.screenY)
            )
        }
        // 3D Crystal / Deposit cluster
        drawCircle(
            color = resColor,
            radius = r,
            center = Offset(proj.screenX, proj.screenY)
        )
        drawCircle(
            color = Color(0xFFFBF6EE),
            radius = r * 0.4f,
            center = Offset(proj.screenX - r * 0.2f, proj.screenY - r * 0.2f)
        )
    }
}

private fun DrawScope.drawBuilding3D(
    bld: SimBuildingInstance,
    worldState: WorldSimulationSnapshot,
    camera: Camera3DState,
    graphicsConfig: GraphicsConfig,
    resourceTracking: ResourceTrackingState,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val proj = LodRenderEngine.projectWorldToScreen(
        bld.worldX, bld.worldY, bld.worldZ, camera, size.width, size.height
    )
    if (!proj.isVisible) return

    val nation = worldState.nations.firstOrNull { it.nationId == bld.nationId }
    val nationColor = Color(nation?.civilization?.primaryColorHex ?: 0xFFB82929)
    val arch = RtsCatalogRegistry.findBuilding(bld.archetypeId)

    val isCityOrWonder = arch.id == "bld_city" || arch.id.startsWith("wonder_")
    val baseW = ((if (isCityOrWonder) 26f else 17f) * proj.scaleFactor).coerceIn(10f, 64f)
    val baseH = ((if (isCityOrWonder) 22f else 14f) * proj.scaleFactor).coerceIn(8f, 54f)

    // Dynamic ground shadow
    if (graphicsConfig.enableDynamicShadows) {
        drawOval(
            color = Color(0x66000000),
            topLeft = Offset(proj.screenX - baseW * 0.65f, proj.screenY - baseH * 0.25f),
            size = Size(baseW * 1.3f, baseH * 0.65f)
        )
    }

    // Selection ring if selected
    if (bld.isSelected) {
        drawCircle(
            color = Color(0xFFF3CF55),
            radius = baseW * 0.85f,
            center = Offset(proj.screenX, proj.screenY),
            style = Stroke(width = 3f)
        )
    }

    // 3D Extruded Isometric Block (Front Wall, Side Wall, Roof)
    val roofY = proj.screenY - baseH
    drawRoundRect(
        color = Color(0xFF4A3B2C),
        topLeft = Offset(proj.screenX - baseW * 0.5f, roofY),
        size = Size(baseW, baseH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Nation-colored roof & banner trim
    drawRoundRect(
        color = nationColor,
        topLeft = Offset(proj.screenX - baseW * 0.46f, roofY - baseH * 0.18f),
        size = Size(baseW * 0.92f, baseH * 0.42f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Gold dome / spire for Cities and Wonders
    if (isCityOrWonder) {
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = baseW * 0.24f,
            center = Offset(proj.screenX, roofY - baseH * 0.22f)
        )
    }

    // HP Bar & Label when close enough
    if (proj.distanceToCamera < graphicsConfig.lod2MaxDistance) {
        val hpFrac = (bld.currentHp / bld.maxHp.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val barW = baseW.coerceAtLeast(28f)
        drawRect(
            color = Color(0xAA000000),
            topLeft = Offset(proj.screenX - barW * 0.5f, roofY - 12f),
            size = Size(barW, 4f)
        )
        drawRect(
            color = if (hpFrac > 0.45f) Color(0xFF48BB78) else Color(0xFFE53E3E),
            topLeft = Offset(proj.screenX - barW * 0.5f, roofY - 12f),
            size = Size(barW * hpFrac, 4f)
        )

        if (isCityOrWonder || bld.isSelected) {
            val labelText = if (arch.id == "bld_city") bld.cityName else arch.name
            val measured = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    color = Color(0xFFFBF6EE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(proj.screenX - measured.size.width * 0.5f, roofY - 26f)
            )
        }
    }
}

private fun DrawScope.drawUnitWithLod3D(
    unit: SimUnitInstance,
    projX: Float,
    projY: Float,
    scaleFactor: Float,
    lodTier: LodTier,
    worldState: WorldSimulationSnapshot,
    graphicsConfig: GraphicsConfig,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val nation = worldState.nations.firstOrNull { it.nationId == unit.nationId }
    val nationColor = Color(nation?.civilization?.primaryColorHex ?: 0xFFB82929)
    val accentColor = Color(nation?.civilization?.accentColorHex ?: 0xFFD4AF37)
    val arch = ModHookRegistry.evaluateUnitArchetype(RtsCatalogRegistry.findUnit(unit.archetypeId))

    val baseRadius = (6.8f * arch.meshScale * scaleFactor).coerceIn(3.5f, 26f)

    // Selection Ring
    if (unit.isSelected) {
        drawCircle(
            color = Color(0xFF48BB78),
            radius = baseRadius * 1.55f,
            center = Offset(projX, projY),
            style = Stroke(width = 2.4f)
        )
    }

    // Supply Attrition warning aura
    if (unit.isUnderAttrition) {
        drawCircle(
            color = Color(0xAAEF4444),
            radius = baseRadius * 1.8f,
            center = Offset(projX, projY),
            style = Stroke(width = 2f)
        )
    }

    when (lodTier) {
        LodTier.LOD0_HIGH_MESH -> {
            // High-Detail Articulated 3D Mesh representation: shadow + chassis/torso + turret/shield + directional barrel/spear + crest
            if (graphicsConfig.enableDynamicShadows) {
                drawOval(
                    color = Color(0x66000000),
                    topLeft = Offset(projX - baseRadius, projY - baseRadius * 0.35f),
                    size = Size(baseRadius * 2f, baseRadius * 0.9f)
                )
            }
            // 3D Base / Hull
            drawRoundRect(
                color = nationColor,
                topLeft = Offset(projX - baseRadius * 0.85f, projY - baseRadius * 1.3f),
                size = Size(baseRadius * 1.7f, baseRadius * 1.4f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Upper Turret / Helmet
            drawCircle(
                color = accentColor,
                radius = baseRadius * 0.55f,
                center = Offset(projX, projY - baseRadius * 0.85f)
            )
            // Directional Weapon / Lance / Cannon Barrel
            val dirX = cos(unit.headingRad) * baseRadius * 1.65f
            val dirY = sin(unit.headingRad) * baseRadius * 1.1f
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(projX, projY - baseRadius * 0.8f),
                end = Offset(projX + dirX, projY - baseRadius * 0.8f + dirY),
                strokeWidth = (baseRadius * 0.28f).coerceAtLeast(2f)
            )
        }

        LodTier.LOD1_MEDIUM_MESH -> {
            // Simplified Tactical 3D Mesh: body cylinder + directional indicator
            drawCircle(
                color = nationColor,
                radius = baseRadius * 0.85f,
                center = Offset(projX, projY - baseRadius * 0.5f)
            )
            drawCircle(
                color = accentColor,
                radius = baseRadius * 0.38f,
                center = Offset(projX, projY - baseRadius * 0.65f)
            )
        }

        LodTier.LOD2_BILLBOARD_PROXY -> {
            // Shaded Octahedral Impostor Diamond
            val diamond = Path().apply {
                moveTo(projX, projY - baseRadius * 1.1f)
                lineTo(projX + baseRadius * 0.8f, projY - baseRadius * 0.4f)
                lineTo(projX, projY + baseRadius * 0.3f)
                lineTo(projX - baseRadius * 0.8f, projY - baseRadius * 0.4f)
                close()
            }
            drawPath(diamond, color = nationColor)
        }

        LodTier.LOD3_STRATEGIC_ICON -> {
            // Strategic Cartographic Emblem dot
            drawCircle(
                color = nationColor,
                radius = baseRadius.coerceIn(3f, 6f),
                center = Offset(projX, projY)
            )
        }
    }

    // Optional LOD Debug Badge when enabled in Graphics Options
    if (graphicsConfig.showLodWireframeBadges && lodTier != LodTier.LOD3_STRATEGIC_ICON) {
        val badgeLayout = textMeasurer.measure(
            text = lodTier.shortCode,
            style = TextStyle(color = Color(0xFFF3CF55), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        )
        drawText(
            textLayoutResult = badgeLayout,
            topLeft = Offset(projX - badgeLayout.size.width * 0.5f, projY + 2f)
        )
    }
}

private fun DrawScope.drawPooledProjectilesAndParticles3D(camera: Camera3DState) {
    ObjectPoolManager.projectilePool.forEachActive { proj ->
        val p = LodRenderEngine.projectWorldToScreen(
            proj.currentX, proj.currentY, proj.currentZ, camera, size.width, size.height
        )
        if (p.isVisible) {
            drawCircle(
                color = Color(proj.colorArgb),
                radius = (3.8f * p.scaleFactor).coerceIn(2.5f, 9f),
                center = Offset(p.screenX, p.screenY)
            )
        }
    }

    ObjectPoolManager.particlePool.forEachActive { part ->
        val p = LodRenderEngine.projectWorldToScreen(
            part.worldX, part.worldY, part.worldZ, camera, size.width, size.height
        )
        if (p.isVisible) {
            val alpha = (part.lifeRemaining / part.maxLife.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
            drawCircle(
                color = Color(part.colorArgb).copy(alpha = alpha),
                radius = (3.2f * p.scaleFactor).coerceIn(1.5f, 7f),
                center = Offset(p.screenX, p.screenY)
            )
        }
    }
}

private fun DrawScope.drawResourceMapTrackingBeacons3D(
    resourceTracking: ResourceTrackingState,
    camera: Camera3DState,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val res = resourceTracking.trackedResource ?: return
    val beaconColor = Color(res.colorHex)
    val pulse = resourceTracking.highlightPulsePhase

    for ((idx, site) in resourceTracking.activeSites.withIndex()) {
        val groundProj = LodRenderEngine.projectWorldToScreen(
            site.worldX, 0f, site.worldZ, camera, size.width, size.height
        )
        val skyProj = LodRenderEngine.projectWorldToScreen(
            site.worldX, 34f, site.worldZ, camera, size.width, size.height
        )
        if (!groundProj.isVisible) continue

        val isFocused = idx == resourceTracking.focusedSiteIndex
        val ringRadius = (22f + pulse * 26f) * groundProj.scaleFactor.coerceIn(0.6f, 1.8f)

        // Pulsing ground radar ring
        drawCircle(
            color = beaconColor.copy(alpha = (1f - pulse) * 0.85f),
            radius = ringRadius,
            center = Offset(groundProj.screenX, groundProj.screenY),
            style = Stroke(width = if (isFocused) 4f else 2.5f)
        )

        // Vertical 3D Light Pillar
        val pillarWidth = if (isFocused) 14f else 8f
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    beaconColor.copy(alpha = 0.0f),
                    beaconColor.copy(alpha = 0.78f),
                    Color(0xFFFBF6EE)
                ),
                startY = skyProj.screenY,
                endY = groundProj.screenY
            ),
            start = Offset(skyProj.screenX, skyProj.screenY),
            end = Offset(groundProj.screenX, groundProj.screenY),
            strokeWidth = pillarWidth
        )

        // Floating Yield Tag above Pillar
        val tagText = "${res.shortLabel} +${site.yieldPerMinute}/m (${site.assignedCitizens}/${site.maxCitizens})"
        val measured = textMeasurer.measure(
            text = tagText,
            style = TextStyle(
                color = Color(0xFFFBF6EE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        val badgeW = measured.size.width + 14f
        val badgeH = measured.size.height + 6f
        val badgeTopLeft = Offset(
            groundProj.screenX - badgeW * 0.5f,
            skyProj.screenY - badgeH - 4f
        )
        drawRoundRect(
            color = Color(0xDD1A140E),
            topLeft = badgeTopLeft,
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = beaconColor,
            topLeft = badgeTopLeft,
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 1.5f)
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(badgeTopLeft.x + 7f, badgeTopLeft.y + 3f)
        )
    }
}
