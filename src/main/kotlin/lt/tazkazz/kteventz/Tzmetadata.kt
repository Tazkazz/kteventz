package lt.tazkazz.kteventz

import java.util.*

/**
 * Tzevent metadata
 */
data class Tzmetadata(
    val eventClass: String,
    val entityType: String,
    val entityId: UUID
)