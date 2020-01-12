package lt.tazkazz.kteventz

/**
 * Tzentity entity abstract implementation
 * @param <T> Tzentity entity
 * @param <C> Tzcommand base entity command
 */
abstract class Tzentity<T : Tzentity<T, C>, C : Tzcommand> {
    /**
     * Apply event on the entity
     * @param event Tzevent event
     */
    fun applyEvent(event: Tzevent) = invokeMethod(this, "apply", event)

    /**
     * Process command on the entity
     * @param command Tzcommand command
     * @return Tzevent events
     */
    @Suppress("UNCHECKED_CAST")
    fun processCommand(command: C) = invokeMethod(this, "process", command) as List<Tzevent>
}