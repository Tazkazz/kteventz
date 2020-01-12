package lt.tazkazz.kteventz

import java.util.*

/**
 * Tzevent envelope with entity type and ID
 * @param <T> Tzevent event
 */
data class Tzenvelope<T : Tzevent>(
    val entityType: String,
    val entityId: UUID,
    val event: T
)