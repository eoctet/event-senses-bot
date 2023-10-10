/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.action;

import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.action.api.ApiAction;
import pro.octet.senses.bot.action.condition.ConditionAction;
import pro.octet.senses.bot.action.email.EmailAction;
import pro.octet.senses.bot.action.script.ScriptAction;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.Parameter;
import pro.octet.senses.bot.core.enums.ActionType;
import pro.octet.senses.bot.core.enums.YamlTemplate;
import pro.octet.senses.bot.core.model.*;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.Map;

/**
 * Event action factory class
 * Generate event actions based on event action types
 *
 * @author William
 * @see ActionType
 * @since 22.0816.2.6
 */
@Slf4j
public final class ActionBuilder {

    private static volatile ActionBuilder builder;

    private ActionBuilder() {
    }

    /**
     * Get an instance of ActionBuilder
     *
     * @return ActionBuilder
     */
    public static ActionBuilder getInstance() {
        if (builder == null) {
            synchronized (ActionBuilder.class) {
                if (builder == null) {
                    builder = new ActionBuilder();
                }
            }
        }
        return builder;
    }

    public void setPatternConfig(Action action) {
        Map<String, Object> actionConfigMap = action.getConfig();
        Object value = actionConfigMap.get(action.getType().toString().toLowerCase());
        log.debug("Load action [{}], action type: {}, config mapping: {}.", action.getName(), action.getType().name(), value);
        action.setActionConfig(CommonUtils.parseToObject(value, PatternConfig.class));
    }

    /**
     * Generate event actions based on event action types
     *
     * @param action Action config
     * @return ActionService - return event action implementation
     * @throws IllegalArgumentException An exception may be thrown when the event action types do not match
     */
    public ActionService of(Action action) {
        ActionService service;
        //
        Map<String, Object> actionConfigMap = action.getConfig();
        if (actionConfigMap.get(YamlTemplate.ACTION_OUTPUT_NODE) != null) {
            action.setActionOutput(CommonUtils.parseToList(actionConfigMap.get(YamlTemplate.ACTION_OUTPUT_NODE), Parameter.class));
        }
        Object value = actionConfigMap.get(action.getType().toString().toLowerCase());
        log.debug("Load action [{}], action type: {}, output mapping: {}.", action.getName(), action.getType().name(), actionConfigMap);
        log.debug("Load action [{}], action type: {}, config mapping: {}.", action.getName(), action.getType().name(), value);
        //
        switch (action.getType()) {
            case API:
                action.setActionConfig(CommonUtils.parseToObject(value, ApiConfig.class));
                service = new ApiAction(action);
                break;
            case SCRIPT:
                action.setActionConfig(CommonUtils.parseToObject(value, ScriptConfig.class));
                service = new ScriptAction(action);
                break;
            case CONDITION:
                action.setActionConfig(CommonUtils.parseToObject(value, ConditionConfig.class));
                service = new ConditionAction(action);
                break;
            case EMAIL:
                action.setActionConfig(CommonUtils.parseToObject(value, EmailConfig.class));
                service = new EmailAction(action);
                break;
            case APP:
            case SQL:
            case MESSAGE:
            case DATABASE:
            default:
                throw new IllegalArgumentException("Unsupported event action type");
        }
        return service;
    }

}
