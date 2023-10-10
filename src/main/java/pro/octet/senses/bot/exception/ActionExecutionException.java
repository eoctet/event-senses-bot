/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.exception;


/**
 * Action execution exception<br/>
 * This exception may be thrown during the execution of the event action
 *
 * @author William
 * @since 22.0816.2.6
 */
public class ActionExecutionException extends RuntimeException {

    public ActionExecutionException(String message) {
        super(message);
    }

    public ActionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
