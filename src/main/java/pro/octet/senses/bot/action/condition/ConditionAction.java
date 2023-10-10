/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.action.condition;

import de.odysseus.el.util.SimpleContext;
import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.core.action.AbstractAction;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.ExecuteResult;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.core.model.ConditionConfig;
import pro.octet.senses.bot.core.script.ExpressionEngine;
import pro.octet.senses.bot.exception.ActionExecutionException;

@Slf4j
public class ConditionAction extends AbstractAction {

    public ConditionAction(Action action) {
        super(action);
    }

    @Override
    public ExecuteResult execute() throws ActionExecutionException {
        Summary.of().info("action.start.regexp", getAction().getName());
        ExecuteResult executeResult = new ExecuteResult();
        boolean conditionState = false;

        try {
            ConditionConfig conditionConfig = getActionConfig(ConditionConfig.class);
            //
            SimpleContext context = ExpressionEngine.getInstance().buildSimpleContext(getActionParam());
            Summary.of().debug("Condition config: {0}", conditionConfig.toString());

            conditionState = ExpressionEngine.getInstance().execute(conditionConfig.getExpression(), context);
        } catch (Exception e) {
            setExecuteThrowable(e);
        }
        addSession(Constant.CONDITION_STATE, conditionState);
        executeResult.put(Constant.CONDITION_STATE, conditionState);
        return executeResult;
    }
}
