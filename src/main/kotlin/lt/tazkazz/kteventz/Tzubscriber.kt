package lt.tazkazz.kteventz

import com.github.msemys.esjc.*
import java.io.IOException
import java.util.*

/**
 * Tzubscriber persistent subscriber for the EventStore
 */
abstract class Tzubscriber(
    private val eventzStore: EventzStore,
    private val entityType: String,
    private val groupId: String
) {
    companion object {
        private val RECOVERABLE_SUBSCRIPTION_DROP_REASONS = setOf(
            SubscriptionDropReason.ConnectionClosed,
            SubscriptionDropReason.ServerError,
            SubscriptionDropReason.SubscribingError,
            SubscriptionDropReason.CatchUpError
        )
    }

    val listener = object : PersistentSubscriptionListener {
        override fun onEvent(subscription: PersistentSubscription, event: RetryableResolvedEvent) {
            this@Tzubscriber.onEvent(subscription, event)
        }

        override fun onClose(
            subscription: PersistentSubscription,
            reason: SubscriptionDropReason,
            exception: Exception?
        ) {
            this@Tzubscriber.onClose(reason, exception)
        }
    }

    init {
        subscribe()
    }

    /**
     * Subscribe to the EventStore
     */
    private fun subscribe() {
        eventzStore.subscribe(entityType, groupId, this)
    }

    private fun onEvent(subscription: PersistentSubscription, eventMessage: RetryableResolvedEvent) {
        val event = eventMessage.event
        if (event != null && event.eventStreamId.startsWith(entityType)) {
            val metadata = getEventMetadata(event)
            val eventClass = getEventClass(metadata.eventClass)
            eventClass?.let { handleEvent(event, it, metadata.entityId) }
        }
        subscription.acknowledge(eventMessage)
    }

    private fun onClose(reason: SubscriptionDropReason, exception: Exception?) {
        if (exception is EventStoreException || reason in RECOVERABLE_SUBSCRIPTION_DROP_REASONS) {
            return subscribe()
        }
        throw RuntimeException("Connection to the event store was closed", exception)
    }

    /**
     * Log event on handle
     * @param envelope Tzenvelope envelope
     */
    abstract fun logHandlingEvent(envelope: Tzenvelope<out Tzevent>)

    /**
     * Handle event
     * @param event RecordedEvent event
     * @param eventClass Tzevent event class
     */
    private fun handleEvent(event: RecordedEvent, eventClass: Class<out Tzevent>, entityId: UUID) {
        try {
            val tzevent = OBJECT_MAPPER.readValue(event.data, eventClass)
            val tzenvelope = Tzenvelope(entityType, entityId, tzevent)
            logHandlingEvent(tzenvelope)
            javaClass.declaredMethods
                .filter { it.isMethodForThisEvent(eventClass) }
                .forEach { invokeMethod(this, it, tzenvelope) }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}