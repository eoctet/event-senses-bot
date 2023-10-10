/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.exception;


public class EventConfigNotFoundException extends RuntimeException {

    public EventConfigNotFoundException(String message) {
        super(message);
    }

    public EventConfigNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
