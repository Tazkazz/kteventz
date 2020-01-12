package lt.tazkazz.kteventz

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.msemys.esjc.EventData

/**
 * Tzevent event interface
 */
interface Tzevent {
    /**
     * Serialize Tzevent to EventData
     * @param event Tzevent event
     * @param entityType Tzentity entity type
     * @param entityId Tzentity entity ID
     * @return EventData event
     */
    fun serializeEvent(entityType: String, entityId: String): EventData = try {
        val metadata = Tzmetadata(javaClass.canonicalName, entityType, entityId)
        EventData.newBuilder()
            .type(javaClass.simpleName)
            .jsonData(OBJECT_MAPPER.writeValueAsBytes(this))
            .jsonMetadata(OBJECT_MAPPER.writeValueAsBytes(metadata))
            .build()
    } catch (e: JsonProcessingException) {
        throw RuntimeException(e)
    }
}