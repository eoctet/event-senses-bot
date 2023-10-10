/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.exception;


public class EventConfigParseException extends RuntimeException {

    public EventConfigParseException(String message) {
        super(message);
    }

    public EventConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
