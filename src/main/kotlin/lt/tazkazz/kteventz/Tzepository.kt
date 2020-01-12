package lt.tazkazz.kteventz

import com.github.msemys.esjc.ExpectedVersion
import java.util.*

/**
 * Tzentity repository for operations with entities
 * @param <T> Tzentity entity
 * @param <C> Tzcommand base entity command
 */
class Tzepository<T : Tzentity<T, C>, C : Tzcommand>(
    private val entityClass: Class<T>,
    private val entityType: String,
    private val eventzStore: EventzStore
) {
    /**
     * Save a new entity to the EventStore
     * @param command Tzcommand command
     * @return Tzentity with ID and new version
     */
    fun save(command: C) = processCommand(newEntityWithIdAndVersion(), command)

    /**
     * Update an existing entity in the EventStore
     * @param entityId Tzentity entity ID
     * @param command Tzcommand command
     * @return Tzentity with ID and new version
     */
    fun update(entityId: UUID, command: C) = processCommand(loadEntity(entityId), command)

    /**
     * Process Tzcommand command and write Tzevent events
     * @param entityWithIdAndVersion Tzentity entity with ID and version
     * @param command Tzcommand command
     * @return Updated Tzentity entity with ID and version
     */
    private fun processCommand(
        entityWithIdAndVersion: TzentityWithIdAndVersion<T>,
        command: C
    ): TzentityWithIdAndVersion<T> {
        val entity = entityWithIdAndVersion.entity
        val id = entityWithIdAndVersion.id
        val events = entity.processCommand(command)
        val newVersion = writeEvents(events, id, entityWithIdAndVersion.version)
        events.forEach { entity.applyEvent(it) }
        return TzentityWithIdAndVersion(id, newVersion, entity)
    }

    /**
     * Write Tzevent events to the EventStore for Tzentity
     * @param events Tzevent events
     * @param entityId Tzentity entity ID
     * @param expectedVersion Expected version of the entity
     * @return Next expected version of the stream
     */
    private fun writeEvents(events: List<Tzevent>, entityId: UUID, expectedVersion: Long) =
        eventzStore.writeEvents(entityType, entityId, events, expectedVersion)

    /**
     * Create a new Tzentity entity
     * @return Tzentity entity
     */
    private fun newEntity(): T = try {
        entityClass.getConstructor().newInstance()
    } catch (e: InstantiationException) {
        throw RuntimeException(e)
    } catch (e: IllegalAccessException) {
        throw RuntimeException(e)
    }

    /**
     * Create a new Tzentity entity with ID and version
     * @return Tzentity entity with ID and version
     */
    private fun newEntityWithIdAndVersion() =
        TzentityWithIdAndVersion(UUID.randomUUID(), ExpectedVersion.NO_STREAM, newEntity())

    /**
     * Load the most recent Tzentity entity
     * @param entityId Tzentity entity ID
     * @return Tzentity entity with ID and version
     */
    private fun loadEntity(entityId: UUID): TzentityWithIdAndVersion<T> {
        val entity = newEntity()
        val eventsWithVersion = eventzStore.readEvents(entityType, entityId)
        eventsWithVersion.events.forEach { entity.applyEvent(it) }
        return TzentityWithIdAndVersion(entityId, eventsWithVersion.version, entity)
    }
}