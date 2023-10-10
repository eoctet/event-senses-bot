/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.action;

import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.ExecuteResult;
import pro.octet.senses.bot.core.entity.Session;
import pro.octet.senses.bot.exception.ActionExecutionException;

/**
 * Event action behavior definition interface
 *
 * @author William
 * @since 22.0816.2.6
 */
public interface ActionService {

    /**
     * Prepare the parameters required for the initialization action
     *
     * @param session action execute session
     * @return ActionService action service
     */
    ActionService prepare(Session session);

    /**
     * Execute event action
     *
     * @return ExecuteResult - result set of event action
     * @throws ActionExecutionException
     */
    ExecuteResult execute() throws ActionExecutionException;

    /**
     * Update event action output parameters
     *
     * @param executeResult action execute result
     */
    void updateOutput(ExecuteResult executeResult);

    /**
     * Check action error
     *
     * @return boolean
     */
    boolean checkError();

    /**
     * Get action object
     *
     * @return Action
     */
    Action getAction();


    /**
     * if it has next action return true else false
     *
     * @return boolean
     */
    boolean hasNextAction();

}
