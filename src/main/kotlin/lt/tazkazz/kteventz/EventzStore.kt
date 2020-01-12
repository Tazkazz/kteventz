package lt.tazkazz.kteventz

import com.github.msemys.esjc.*
import com.github.msemys.esjc.system.SystemConsumerStrategy
import java.util.*
import java.util.concurrent.CompletionException

/**
 * Eventz EventStore interface
 * @param host EventStore host name
 * @param port EventStore port
 * @param username EventStore user
 * @param password EventStore password
 */
class EventzStore(
    host: String,
    port: Int,
    username: String,
    password: String
) {
    companion object {
        private const val SUBSCRIPTION_STREAM_PREFIX = "\$ce-"
    }

    private val eventStore = EventStoreBuilder.newBuilder()
        .maxReconnections(-1)
        .persistentSubscriptionAutoAck(false)
        .singleNodeAddress(host, port)
        .userCredentials(username, password)
        .build()

    init {
        eventStore.connect()
    }

    /**
     * Persistently subscribe Tzubscriber to the EventStore
     * @param entityType Tzentity entity type
     * @param groupId Persistent subscription group ID
     * @param tzubscriber Tzubscriber to subscribe
     */
    fun subscribe(entityType: String, groupId: String, tzubscriber: Tzubscriber) {
        val streamId = SUBSCRIPTION_STREAM_PREFIX + entityType
        val settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(true)
            .startFromBeginning()
            .namedConsumerStrategy(SystemConsumerStrategy.PINNED)
            .minCheckPointCount(1)
            .build()

        try {
            val result = eventStore
                .createPersistentSubscription(streamId, groupId, settings)
                .join()
            if (result.status == PersistentSubscriptionCreateStatus.Failure) {
                throw RuntimeException("Failed to subscribe to '$streamId' as '$groupId'")
            }
        } catch (e: CompletionException) {
            if (e.cause?.message?.contains("already exists") == true) {
                eventStore
                    .updatePersistentSubscription(streamId, groupId, settings).join()
            } else {
                throw RuntimeException("Failed to subscribe to '$streamId' as '$groupId'", e)
            }
        }

        eventStore.subscribeToPersistent(streamId, groupId, tzubscriber.listener).join()
    }

    /**
     * Write events to the EventStore stream
     * @param entityType Tzentity entity type
     * @param entityId Tzentity entity ID
     * @param events Tzevent events
     * @param expectedVersion Expected version of the stream
     * @return Next expected version of the stream
     */
    fun writeEvents(entityType: String, entityId: UUID, events: List<Tzevent>, expectedVersion: Long): Long {
        val streamId = "$entityType-$entityId"
        val eventData = events.map { it.serializeEvent(entityType, entityId) }
        val result = eventStore.tryAppendToStream(streamId, expectedVersion, eventData).join()
        if (result.status != WriteStatus.Success) {
            throw RuntimeException("Failed to write events to stream '$streamId': ${result.status}")
        }
        return result.nextExpectedVersion
    }

    /**
     * Read events from the EventStore stream
     * @param entityType Tzentity entity type
     * @param entityId Tzentity entity ID
     * @return Tzevent events with version
     */
    fun readEvents(entityType: String, entityId: UUID): TzeventsWithVersion {
        val streamId = "$entityType-$entityId"
        val slice = eventStore.readStreamEventsForward(streamId, 0, 4096, true).join()
        if (slice.status != SliceReadStatus.Success) {
            throw RuntimeException("Failed to read events from stream '$streamId': ${slice.status}")
        }
        val events = slice.events.mapNotNull(ResolvedEvent::deserializeEvent)
        return TzeventsWithVersion(events, slice.lastEventNumber)
    }
}