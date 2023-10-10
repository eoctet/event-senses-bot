/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.action.email;

import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.core.action.AbstractAction;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.ExecuteResult;
import pro.octet.senses.bot.core.handler.EmailManager;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.core.model.EmailConfig;
import pro.octet.senses.bot.exception.ActionExecutionException;

/**
 * Email action
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class EmailAction extends AbstractAction {

    public EmailAction(Action action) {
        super(action);
    }

    @Override
    public ExecuteResult execute() throws ActionExecutionException {
        Summary.of().info("action.start.regexp", getAction().getName());
        ExecuteResult executeResult = new ExecuteResult();
        try {
            EmailConfig emailConfig = getActionConfig(EmailConfig.class);
            EmailManager.getInstance().sendEmail(getActionParam(), emailConfig);
        } catch (Exception e) {
            setExecuteThrowable(e);
        }
        return executeResult;
    }
}
