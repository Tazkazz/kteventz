package lt.tazkazz.kteventz

/**
 * Tzevent events with version
 */
data class TzeventsWithVersion(
    val events: List<Tzevent>,
    val version: Long
)