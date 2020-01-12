package lt.tazkazz.kteventz

/**
 * Tzentity with it's ID and version
 * @param <T> Tzentity entity
 */
data class TzentityWithIdAndVersion<T : Tzentity<T, *>>(
    val id: String,
    val version: Long,
    val entity: T
)