/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.action;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.core.entity.*;
import pro.octet.senses.bot.core.enums.ActionType;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.handler.DataTypeConvert;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract action base class, which specifies the behavior of action.
 * Subclasses need to implement ActionService
 *
 * @author William
 * @see ActionService
 * @since 22.0816.2.6
 */
@Slf4j
public abstract class AbstractAction implements ActionService, Serializable {

    private final Action action;
    private final ActionParam actionParam;
    private final AtomicReference<Throwable> actionExecuteThrowable = new AtomicReference<>();
    private Session session;

    public AbstractAction(Action action) {
        this.action = action;
        this.actionParam = new ActionParam();
        log.info("Create event action object, action name: [{}], action code: {}", action.getName(), action.getCode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public ActionService prepare(Session session) {
        Summary.of().info("action.prepare.regexp", this.action.getName(), this.action.getCode());
        this.session = session;
        this.actionExecuteThrowable.set(null);
        this.actionParam.clear();
        //
        if (session.get(Constant.CHANNEL_MESSAGE) != null) {
            ChannelMessage channelMessage = (ChannelMessage) session.get(Constant.CHANNEL_MESSAGE);
            this.actionParam.putAll(channelMessage.getMessageValue());
        }
        if (session.get(Constant.PATTERN_RESULT) != null) {
            PatternResult patternResult = (PatternResult) session.get(Constant.PATTERN_RESULT);
            this.actionParam.put("Pattern", patternResult.getChannelMessages());
        }
        if (session.get(Constant.LAST_ACTION_OUTPUT) != null) {
            List<Parameter> lastActionOutput = (List<Parameter>) session.get(Constant.LAST_ACTION_OUTPUT);
            lastActionOutput.forEach(param -> this.actionParam.put(param.getParam(), param.getValue()));
            Summary.of().info("action.prepare.param.regexp", this.action.getName(), lastActionOutput.size());
        }
        Summary.of().info("action.prepare.complete.regexp", this.action.getName(), this.actionParam.entrySet().size());
        Summary.of().debug("Action parameters: {0}", actionParam);
        return this;
    }

    @Override
    public void updateOutput(ExecuteResult executeResult) {
        Summary.of().info("action.output.complete.regexp", this.action.getName(), executeResult.size());

        this.session.remove(Constant.LAST_ACTION_OUTPUT);
        List<Parameter> output = this.action.getActionOutput();

        if (output != null && !output.isEmpty()) {
            List<Parameter> result = Lists.newArrayList();
            output.forEach(param -> {
                String key = param.getParam();
                Object value = param.getValue();
                if (executeResult.containsKey(key)) {
                    value = DataTypeConvert.getValue(param.getDatatype(), executeResult.get(key));
                }
                if (value != null) {
                    result.add(new Parameter(param.getParam(), param.getDatatype(), value, param.getDesc()));
                }
            });
            this.session.put(Constant.LAST_ACTION_OUTPUT, result);
            Summary.of().debug("Action output parameters: {0}", CommonUtils.toJson(result));
            Summary.of().info("action.output.update.regexp", this.action.getName(), result.size());
        }
    }

    @Override
    public boolean checkError() {
        Throwable cause = this.actionExecuteThrowable.get();
        if (cause != null) {
            String code = CommonUtils.createCode();
            Summary.of().error("action.error.message.regexp", code, action.getName(), cause);
            log.error("An error occurred, ID: [{}], Action name: {}, action code: {}.", code, action.getName(), action.getCode(), cause);
            return true;
        }
        return false;
    }

    @Override
    public Action getAction() {
        return this.action;
    }

    @Override
    public boolean hasNextAction() {
        return this.action.getType() != ActionType.CONDITION && this.action.getActionChain() != null && !this.action.getActionChain().isEmpty();
    }

    protected <T> T getActionConfig(Class<T> clazz) {
        return clazz.cast(this.action.getActionConfig());
    }

    protected ActionParam getActionParam() {
        return this.actionParam;
    }

    protected void setExecuteThrowable(Throwable throwable) {
        this.actionExecuteThrowable.compareAndSet(null, throwable);
    }

    protected void addSession(String key, Object value) {
        this.session.put(key, value);
    }
}
