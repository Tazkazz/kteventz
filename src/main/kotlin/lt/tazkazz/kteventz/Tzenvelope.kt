package lt.tazkazz.kteventz

/**
 * Tzevent envelope with entity type and ID
 * @param <T> Tzevent event
 */
data class Tzenvelope<T : Tzevent>(
    val entityType: String,
    val entityId: String,
    val event: T
)