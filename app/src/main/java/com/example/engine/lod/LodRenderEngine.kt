package com.example.engine.lod

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class LodTier(
    val level: Int,
    val shortCode: String,
    val label: String,
    val polygonEstimate: Int
) {
    LOD0_HIGH_MESH(0, "LOD0", "High-Detail Articulated 3D Mesh", 480),
    LOD1_MEDIUM_MESH(1, "LOD1", "Simplified Tactical 3D Mesh", 140),
    LOD2_BILLBOARD_PROXY(2, "LOD2", "Shaded Octahedral Impostor", 24),
    LOD3_STRATEGIC_ICON(3, "LOD3", "Cartographic Strategic Emblem", 4)
}

enum class GraphicsPreset(val label: String) {
    ULTRA_IMPERIAL("Ultra Imperial"),
    BALANCED_TACTICAL("Balanced Tactical"),
    BATTERY_SAVER("Mobile Endurance"),
    CUSTOM("Custom Tuned")
}

data class GraphicsConfig(
    val preset: GraphicsPreset = GraphicsPreset.BALANCED_TACTICAL,
    val lod0MaxDistance: Float = 55f,
    val lod1MaxDistance: Float = 115f,
    val lod2MaxDistance: Float = 195f,
    val terrainSubdivision: Int = 28,
    val enableDynamicShadows: Boolean = true,
    val enableWaterReflections: Boolean = true,
    val enableBorderAuraGlow: Boolean = true,
    val maxParticlesActive: Int = 600,
    val resolutionScale: Float = 1.0f,
    val targetFps: Int = 60,
    val showLodWireframeBadges: Boolean = false
) {
    fun evaluateLodTier(distanceToCamera: Float): LodTier {
        return when {
            distanceToCamera <= lod0MaxDistance -> LodTier.LOD0_HIGH_MESH
            distanceToCamera <= lod1MaxDistance -> LodTier.LOD1_MEDIUM_MESH
            distanceToCamera <= lod2MaxDistance -> LodTier.LOD2_BILLBOARD_PROXY
            else -> LodTier.LOD3_STRATEGIC_ICON
        }
    }

    companion object {
        fun fromPreset(preset: GraphicsPreset): GraphicsConfig = when (preset) {
            GraphicsPreset.ULTRA_IMPERIAL -> GraphicsConfig(
                preset = preset,
                lod0MaxDistance = 85f,
                lod1MaxDistance = 160f,
                lod2MaxDistance = 260f,
                terrainSubdivision = 36,
                enableDynamicShadows = true,
                enableWaterReflections = true,
                enableBorderAuraGlow = true,
                maxParticlesActive = 1200,
                resolutionScale = 1.0f,
                targetFps = 60
            )
            GraphicsPreset.BALANCED_TACTICAL -> GraphicsConfig(
                preset = preset,
                lod0MaxDistance = 55f,
                lod1MaxDistance = 115f,
                lod2MaxDistance = 195f,
                terrainSubdivision = 28,
                enableDynamicShadows = true,
                enableWaterReflections = true,
                enableBorderAuraGlow = true,
                maxParticlesActive = 600,
                resolutionScale = 1.0f,
                targetFps = 60
            )
            GraphicsPreset.BATTERY_SAVER -> GraphicsConfig(
                preset = preset,
                lod0MaxDistance = 30f,
                lod1MaxDistance = 70f,
                lod2MaxDistance = 130f,
                terrainSubdivision = 18,
                enableDynamicShadows = false,
                enableWaterReflections = false,
                enableBorderAuraGlow = true,
                maxParticlesActive = 200,
                resolutionScale = 0.85f,
                targetFps = 30
            )
            GraphicsPreset.CUSTOM -> GraphicsConfig(preset = GraphicsPreset.CUSTOM)
        }
    }
}

data class Camera3DState(
    val targetX: Float = 120f,
    val targetZ: Float = 120f,
    val distance: Float = 78f,
    val yawDegrees: Float = 45f,
    val pitchDegrees: Float = 52f,
    val fovDegrees: Float = 50f
) {
    fun computeEyePosition(): Triple<Float, Float, Float> {
        val yawRad = Math.toRadians(yawDegrees.toDouble())
        val pitchRad = Math.toRadians(pitchDegrees.toDouble())
        val horizontalDist = (distance * cos(pitchRad)).toFloat()
        val eyeY = (distance * sin(pitchRad)).toFloat()
        val eyeX = targetX - (horizontalDist * sin(yawRad)).toFloat()
        val eyeZ = targetZ + (horizontalDist * cos(yawRad)).toFloat()
        return Triple(eyeX, eyeY, eyeZ)
    }

    fun distanceToPoint(worldX: Float, worldY: Float, worldZ: Float): Float {
        val (eyeX, eyeY, eyeZ) = computeEyePosition()
        val dx = worldX - eyeX
        val dy = worldY - eyeY
        val dz = worldZ - eyeZ
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

data class ProjectedPoint3D(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val scaleFactor: Float,
    val distanceToCamera: Float,
    val isVisible: Boolean
)

data class LodFrameTelemetry(
    val lod0Count: Int = 0,
    val lod1Count: Int = 0,
    val lod2Count: Int = 0,
    val lod3Count: Int = 0,
    val culledCount: Int = 0,
    val estimatedTrianglesRendered: Int = 0,
    val frameTimeMs: Float = 8.2f
)

object LodRenderEngine {

    @Volatile
    var latestFrameTelemetry: LodFrameTelemetry = LodFrameTelemetry()

    fun projectWorldToScreen(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        camera: Camera3DState,
        viewportWidth: Float,
        viewportHeight: Float
    ): ProjectedPoint3D {
        if (viewportWidth <= 1f || viewportHeight <= 1f) {
            return ProjectedPoint3D(0f, 0f, 0f, 1f, 100f, false)
        }
        val yawRad = Math.toRadians(camera.yawDegrees.toDouble())
        val pitchRad = Math.toRadians(camera.pitchDegrees.toDouble())
        val cosYaw = cos(yawRad).toFloat()
        val sinYaw = sin(yawRad).toFloat()
        val cosPitch = cos(pitchRad).toFloat()
        val sinPitch = sin(pitchRad).toFloat()

        val relX = worldX - camera.targetX
        val relZ = worldZ - camera.targetZ

        // Rotate in XZ plane around target by yaw
        val rotX = relX * cosYaw - relZ * sinYaw
        val rotZ = relX * sinYaw + relZ * cosYaw

        // Apply pitch and vertical elevation
        val camSpaceX = rotX
        val camSpaceY = rotZ * sinPitch - worldY * cosPitch
        val camSpaceZ = camera.distance + rotZ * cosPitch + worldY * sinPitch

        if (camSpaceZ <= 4f) {
            return ProjectedPoint3D(0f, 0f, camSpaceZ, 0f, camSpaceZ, false)
        }

        val focalLength = (viewportWidth.coerceAtMost(viewportHeight) * 0.92f)
        val perspectiveScale = focalLength / camSpaceZ

        val screenX = viewportWidth * 0.5f + camSpaceX * perspectiveScale
        val screenY = viewportHeight * 0.5f + camSpaceY * perspectiveScale

        val distanceToCamera = sqrt(camSpaceX * camSpaceX + camSpaceY * camSpaceY + camSpaceZ * camSpaceZ)
        val margin = 140f
        val visible = screenX >= -margin &&
            screenX <= viewportWidth + margin &&
            screenY >= -margin &&
            screenY <= viewportHeight + margin

        return ProjectedPoint3D(
            screenX = screenX,
            screenY = screenY,
            depth = camSpaceZ,
            scaleFactor = perspectiveScale,
            distanceToCamera = distanceToCamera,
            isVisible = visible
        )
    }

    fun screenToWorldGroundPlane(
        screenX: Float,
        screenY: Float,
        camera: Camera3DState,
        viewportWidth: Float,
        viewportHeight: Float
    ): Pair<Float, Float> {
        if (viewportWidth <= 1f || viewportHeight <= 1f) {
            return camera.targetX to camera.targetZ
        }
        val yawRad = Math.toRadians(camera.yawDegrees.toDouble())
        val pitchRad = Math.toRadians(camera.pitchDegrees.toDouble()).coerceAtLeast(0.25)
        val cosYaw = cos(yawRad).toFloat()
        val sinYaw = sin(yawRad).toFloat()
        val sinPitch = sin(pitchRad).toFloat()

        val focalLength = (viewportWidth.coerceAtMost(viewportHeight) * 0.92f)
        val normX = (screenX - viewportWidth * 0.5f) / focalLength
        val normY = (screenY - viewportHeight * 0.5f) / focalLength

        val rotX = normX * camera.distance
        val rotZ = (normY * camera.distance) / sinPitch

        val worldX = camera.targetX + (rotX * cosYaw + rotZ * sinYaw)
        val worldZ = camera.targetZ + (-rotX * sinYaw + rotZ * cosYaw)
        return worldX.coerceIn(8f, 232f) to worldZ.coerceIn(8f, 232f)
    }
}
