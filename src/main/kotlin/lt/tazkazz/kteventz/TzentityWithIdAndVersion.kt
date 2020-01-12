package lt.tazkazz.kteventz

import java.util.*

/**
 * Tzentity with it's ID and version
 * @param <T> Tzentity entity
 */
data class TzentityWithIdAndVersion<T : Tzentity<T, *>>(
    val id: UUID,
    val version: Long,
    val entity: T
)