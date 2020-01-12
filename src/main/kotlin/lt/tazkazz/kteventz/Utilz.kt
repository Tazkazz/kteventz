package lt.tazkazz.kteventz

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.msemys.esjc.RecordedEvent
import com.github.msemys.esjc.ResolvedEvent
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

/**
 * Utilities for KtEventz library
 */

val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
    .findAndRegisterModules()
    .configure(DEFAULT_VIEW_INCLUSION, false)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

/**
 * Get metadata for EventStore event
 * @param event RecordedEvent event
 * @return Tzmetadata metadata
 */
fun getEventMetadata(event: RecordedEvent): Tzmetadata = try {
    OBJECT_MAPPER.readValue(event.metadata, Tzmetadata::class.java)
} catch (e: IOException) {
    throw RuntimeException("Cannot read event metadata", e)
}

/**
 * Invoke method on an instance by method name
 * @param target Instance object
 * @param methodName Method name
 * @param arg Method argument
 * @return Result of the invocation
 */
fun invokeMethod(target: Any, methodName: String, arg: Any): Any? = try {
    val method = target::class.java.getMethod(methodName, arg::class.java)
    invokeMethod(target, method, arg)
} catch (e: NoSuchMethodException) {
    throw RuntimeException(e)
}

/**
 * Invoke method on an instance
 * @param target Instance object
 * @param method Method
 * @param arg Method argument
 * @return Result of the invocation
 */
fun invokeMethod(target: Any, method: Method, arg: Any): Any? = try {
    method.isAccessible = true
    method.invoke(target, arg)
} catch (e: IllegalAccessException) {
    throw RuntimeException(e.cause)
} catch (e: InvocationTargetException) {
    throw RuntimeException(e.cause)
}

/**
 * Deserialize ResolvedEvent to Tzevent
 * @param eventMessage ResolvedEvent event
 * @return Tzevent event
 */
@Suppress("UNCHECKED_CAST")
fun ResolvedEvent.deserializeEvent(): Tzevent? {
    try {
        val event = event ?: return null
        val metadata = getEventMetadata(event)
        val eventClass = Class.forName(metadata.eventClass) as Class<out Tzevent>
        return OBJECT_MAPPER.readValue(event.data, eventClass)
    } catch (e: IOException) {
        throw RuntimeException(e)
    } catch (e: ClassNotFoundException) {
        throw RuntimeException(e)
    }
}

/**
 * Check if given Tzubscriber method is for given Tzevent event
 * @param method Tzubscriber method
 * @param eventClass Tzevent event class
 * @return Is the method dedicated for given Tzevent event
 */
fun Method.isMethodForThisEvent(eventClass: Class<out Tzevent>) =
    getAnnotation(Tzhandler::class.java) != null && parameterCount == 1 &&
            (genericParameterTypes[0] as ParameterizedType).actualTypeArguments[0] == eventClass

/**
 * Get Tzevent event class by the type
 * @param eventType Tzevent event type
 * @return Tzevent event class
 */
@Suppress("UNCHECKED_CAST")
fun getEventClass(eventType: String): Class<out Tzevent>? = try {
    Class.forName(eventType) as Class<out Tzevent>
} catch (e: ClassNotFoundException) {
    null
}