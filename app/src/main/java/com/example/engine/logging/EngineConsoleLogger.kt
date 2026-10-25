package com.example.engine.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogCategory(val tag: String, val label: String) {
    SIMULATION("SIM", "Simulation"),
    LOD("LOD", "3D LOD Engine"),
    POOL("POOL", "Object Pool"),
    HOOK("HOOK", "Mod Hooks"),
    AUTOMATION("AUTO", "Automation AI"),
    RESOURCE("RES", "Resource Tracker"),
    NETWORK("NET", "Matchmaking"),
    SYSTEM("SYS", "System Core")
}

enum class LogSeverity(val label: String) {
    INFO("INFO"),
    DEBUG("DEBUG"),
    WARN("WARN"),
    ERROR("ERROR")
}

data class ConsoleLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val formattedTime: String,
    val category: LogCategory,
    val severity: LogSeverity,
    val message: String,
    val details: String = ""
) {
    fun toFormattedLine(): String {
        val detailSuffix = if (details.isNotBlank()) " | $details" else ""
        return "[$formattedTime] [${severity.label}] [${category.tag}] $message$detailSuffix"
    }
}

object EngineConsoleLogger {
    private const val MAX_LOG_ENTRIES = 600
    private var nextId = 1L
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val logs: StateFlow<List<ConsoleLogEntry>> = _logs.asStateFlow()

    init {
        log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            message = "Crown & Dominion 3D Engine initialized (OpenGL/Canvas 3D + 4-Tier LOD + Pre-Allocated Object Pools)."
        )
        log(
            category = LogCategory.POOL,
            severity = LogSeverity.INFO,
            message = "Pre-warmed UnitPool(512), ProjectilePool(1024), ParticlePool(2048), FloatingTextPool(256)."
        )
        log(
            category = LogCategory.HOOK,
            severity = LogSeverity.INFO,
            message = "ModHookRegistry online: 28 deep core mechanics & asset interceptors active."
        )
    }

    @Synchronized
    fun log(
        category: LogCategory,
        severity: LogSeverity = LogSeverity.INFO,
        message: String,
        details: String = ""
    ) {
        val now = System.currentTimeMillis()
        val entry = ConsoleLogEntry(
            id = nextId++,
            timestampMillis = now,
            formattedTime = synchronized(timeFormat) { timeFormat.format(Date(now)) },
            category = category,
            severity = severity,
            message = message,
            details = details
        )
        val updated = (_logs.value + entry).takeLast(MAX_LOG_ENTRIES)
        _logs.value = updated
    }

    @Synchronized
    fun clear() {
        _logs.value = emptyList()
        log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            message = "Console log buffer cleared by commander."
        )
    }

    fun exportAllLogsText(
        categoryFilter: LogCategory? = null,
        severityFilter: LogSeverity? = null
    ): String {
        val filtered = _logs.value.filter { entry ->
            (categoryFilter == null || entry.category == categoryFilter) &&
                (severityFilter == null || entry.severity == severityFilter)
        }
        val header = buildString {
            appendLine("=== CROWN & DOMINION: 3D RTS ENGINE CONSOLE LOGS ===")
            appendLine("Exported Entries: ${filtered.size} / ${_logs.value.size}")
            appendLine("Filter Category: ${categoryFilter?.label ?: "ALL"} | Severity: ${severityFilter?.label ?: "ALL"}")
            appendLine("====================================================")
        }
        return header + filtered.joinToString("\n") { it.toFormattedLine() }
    }
}
