package com.example.engine.pool

import com.example.engine.logging.EngineConsoleLogger
import com.example.engine.logging.LogCategory
import com.example.engine.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PoolableEntity {
    var poolIndex: Int
    var isActiveInScene: Boolean
    fun resetForPool()
}

data class PoolStatsSnapshot(
    val poolName: String,
    val capacity: Int,
    val activeCount: Int,
    val freeCount: Int,
    val peakActive: Int,
    val totalAcquisitions: Long,
    val totalReleases: Long,
    val reusedAllocationsSaved: Long
) {
    val utilizationFraction: Float
        get() = if (capacity > 0) (activeCount.toFloat() / capacity.toFloat()).coerceIn(0f, 1f) else 0f
}

class DeterministicObjectPool<T : PoolableEntity>(
    val poolName: String,
    initialCapacity: Int,
    private val maxCapacity: Int,
    private val factory: (Int) -> T
) {
    private val storage = ArrayList<T>(maxCapacity)
    private val freeIndices = IntArray(maxCapacity)
    private var freeTop = 0

    var activeCount = 0
        private set
    var peakActive = 0
        private set
    var totalAcquisitions = 0L
        private set
    var totalReleases = 0L
        private set
    var reusedAllocationsSaved = 0L
        private set

    init {
        expandCapacity(initialCapacity.coerceAtMost(maxCapacity))
    }

    @Synchronized
    private fun expandCapacity(targetCount: Int) {
        val currentSize = storage.size
        val toCreate = (targetCount - currentSize).coerceAtLeast(0)
        for (i in 0 until toCreate) {
            val idx = storage.size
            val instance = factory(idx)
            instance.poolIndex = idx
            instance.isActiveInScene = false
            storage.add(instance)
            freeIndices[freeTop++] = idx
        }
    }

    @Synchronized
    fun acquire(): T {
        if (freeTop == 0) {
            if (storage.size < maxCapacity) {
                val nextSize = (storage.size * 3 / 2 + 16).coerceAtMost(maxCapacity)
                expandCapacity(nextSize)
            } else {
                // Recycle oldest slot gracefully so no crash or GC spike occurs under extreme load
                val fallback = storage[0]
                fallback.resetForPool()
                fallback.isActiveInScene = true
                totalAcquisitions++
                reusedAllocationsSaved++
                return fallback
            }
        }
        val slotIndex = freeIndices[--freeTop]
        val item = storage[slotIndex]
        item.resetForPool()
        item.isActiveInScene = true
        activeCount++
        if (activeCount > peakActive) {
            peakActive = activeCount
        }
        totalAcquisitions++
        if (totalAcquisitions > storage.size) {
            reusedAllocationsSaved++
        }
        return item
    }

    @Synchronized
    fun release(item: T) {
        if (!item.isActiveInScene) return
        item.isActiveInScene = false
        item.resetForPool()
        if (freeTop < freeIndices.size) {
            freeIndices[freeTop++] = item.poolIndex
        }
        activeCount = (activeCount - 1).coerceAtLeast(0)
        totalReleases++
    }

    @Synchronized
    fun releaseAll() {
        freeTop = 0
        for (i in storage.indices.reversed()) {
            val item = storage[i]
            item.isActiveInScene = false
            item.resetForPool()
            freeIndices[freeTop++] = i
        }
        activeCount = 0
    }

    @Synchronized
    fun forEachActive(action: (T) -> Unit) {
        for (i in 0 until storage.size) {
            val item = storage[i]
            if (item.isActiveInScene) {
                action(item)
            }
        }
    }

    @Synchronized
    fun activeList(): List<T> {
        val result = ArrayList<T>(activeCount)
        for (i in 0 until storage.size) {
            val item = storage[i]
            if (item.isActiveInScene) {
                result.add(item)
            }
        }
        return result
    }

    @Synchronized
    fun preWarm(targetCapacity: Int) {
        val clamped = targetCapacity.coerceIn(storage.size, maxCapacity)
        if (clamped > storage.size) {
            expandCapacity(clamped)
        }
    }

    @Synchronized
    fun snapshot(): PoolStatsSnapshot = PoolStatsSnapshot(
        poolName = poolName,
        capacity = storage.size,
        activeCount = activeCount,
        freeCount = freeTop,
        peakActive = peakActive,
        totalAcquisitions = totalAcquisitions,
        totalReleases = totalReleases,
        reusedAllocationsSaved = reusedAllocationsSaved
    )
}

class PooledProjectileEntity(override var poolIndex: Int = 0) : PoolableEntity {
    override var isActiveInScene: Boolean = false
    var sourceNationId: Int = 0
    var startX: Float = 0f
    var startZ: Float = 0f
    var currentX: Float = 0f
    var currentY: Float = 0f
    var currentZ: Float = 0f
    var targetX: Float = 0f
    var targetZ: Float = 0f
    var targetEntityId: Int = -1
    var speed: Float = 18f
    var arcHeight: Float = 3.5f
    var progress: Float = 0f
    var damage: Float = 12f
    var splashRadius: Float = 0f
    var projectileKind: ProjectileKind = ProjectileKind.ARROW
    var colorArgb: Long = 0xFFFFD54F

    override fun resetForPool() {
        sourceNationId = 0
        startX = 0f
        startZ = 0f
        currentX = 0f
        currentY = 0f
        currentZ = 0f
        targetX = 0f
        targetZ = 0f
        targetEntityId = -1
        speed = 18f
        arcHeight = 3.5f
        progress = 0f
        damage = 12f
        splashRadius = 0f
        projectileKind = ProjectileKind.ARROW
        colorArgb = 0xFFFFD54F
    }
}

enum class ProjectileKind {
    ARROW,
    JAVELIN,
    BOULDER,
    CANNONBALL,
    MUSKET_TRACER,
    TANK_SHELL,
    ARTILLERY_ROCKET,
    LASER_PULSE,
    ICBM_WARHEAD
}

class PooledParticleEntity(override var poolIndex: Int = 0) : PoolableEntity {
    override var isActiveInScene: Boolean = false
    var worldX: Float = 0f
    var worldY: Float = 0f
    var worldZ: Float = 0f
    var velX: Float = 0f
    var velY: Float = 0f
    var velZ: Float = 0f
    var radius: Float = 1f
    var lifeRemaining: Float = 0f
    var maxLife: Float = 1f
    var colorArgb: Long = 0xFFFFAB40

    override fun resetForPool() {
        worldX = 0f
        worldY = 0f
        worldZ = 0f
        velX = 0f
        velY = 0f
        velZ = 0f
        radius = 1f
        lifeRemaining = 0f
        maxLife = 1f
        colorArgb = 0xFFFFAB40
    }
}

class PooledCombatTextEntity(override var poolIndex: Int = 0) : PoolableEntity {
    override var isActiveInScene: Boolean = false
    var worldX: Float = 0f
    var worldY: Float = 0f
    var worldZ: Float = 0f
    var text: String = ""
    var colorArgb: Long = 0xFFFFF59D
    var lifeRemaining: Float = 0f
    var maxLife: Float = 1.4f

    override fun resetForPool() {
        worldX = 0f
        worldY = 0f
        worldZ = 0f
        text = ""
        colorArgb = 0xFFFFF59D
        lifeRemaining = 0f
        maxLife = 1.4f
    }
}

object ObjectPoolManager {
    val projectilePool = DeterministicObjectPool(
        poolName = "3D Projectiles & Ordnance",
        initialCapacity = 256,
        maxCapacity = 1024
    ) { idx -> PooledProjectileEntity(idx) }

    val particlePool = DeterministicObjectPool(
        poolName = "Combat & Gather Particles",
        initialCapacity = 512,
        maxCapacity = 2048
    ) { idx -> PooledParticleEntity(idx) }

    val combatTextPool = DeterministicObjectPool(
        poolName = "Floating Yield & Combat Text",
        initialCapacity = 128,
        maxCapacity = 512
    ) { idx -> PooledCombatTextEntity(idx) }

    private val _telemetry = MutableStateFlow(collectSnapshots())
    val telemetry: StateFlow<List<PoolStatsSnapshot>> = _telemetry.asStateFlow()

    fun refreshTelemetry() {
        _telemetry.value = collectSnapshots()
    }

    fun collectSnapshots(): List<PoolStatsSnapshot> = listOf(
        projectilePool.snapshot(),
        particlePool.snapshot(),
        combatTextPool.snapshot()
    )

    fun preWarmAllPools() {
        projectilePool.preWarm(512)
        particlePool.preWarm(1024)
        combatTextPool.preWarm(256)
        refreshTelemetry()
        EngineConsoleLogger.log(
            category = LogCategory.POOL,
            severity = LogSeverity.INFO,
            message = "ObjectPoolManager expanded & pre-warmed all pools (Zero-GC mode active)."
        )
    }

    fun reclaimAllPools() {
        projectilePool.releaseAll()
        particlePool.releaseAll()
        combatTextPool.releaseAll()
        refreshTelemetry()
        EngineConsoleLogger.log(
            category = LogCategory.POOL,
            severity = LogSeverity.INFO,
            message = "ObjectPoolManager reclaimed all active pooled instances back to free lists."
        )
    }
}
