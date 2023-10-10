/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.event;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import pro.octet.senses.bot.core.ApplicationConfig;
import pro.octet.senses.bot.core.ExecutionContext;
import pro.octet.senses.bot.core.action.ActionService;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.entity.ExecuteResult;
import pro.octet.senses.bot.core.entity.Session;
import pro.octet.senses.bot.core.enums.ActionType;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.enums.ExecuteStatus;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.core.model.ConditionConfig;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract event process function
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public abstract class AbstractEventProcessFunction<IN> extends RichMapFunction<IN, Session> implements ResultTypeQueryable<ChannelMessage> {

    protected final ExecutionContext context;
    protected final ApplicationConfig appConfig;
    protected AtomicInteger actionExecuteErrorCount;
    protected AtomicInteger actionExecuteSuccessCount;
    protected AtomicInteger eventExecuteRetryCount;
    protected AtomicReference<Throwable> executeThrowable;

    public AbstractEventProcessFunction(ExecutionContext context) {
        this.context = context;
        this.appConfig = context.getAppConfig();
        this.actionExecuteErrorCount = new AtomicInteger(0);
        this.actionExecuteSuccessCount = new AtomicInteger(0);
        this.eventExecuteRetryCount = new AtomicInteger(0);
        this.executeThrowable = new AtomicReference<>();
    }

    @Override
    public void open(Configuration parameters) {
        Summary.of().build(appConfig);
    }

    protected void conditionHandler(Session session, Action action) {
        if (action.getType() != ActionType.CONDITION || session.get(Constant.CONDITION_STATE) == null) {
            return;
        }
        boolean status = Boolean.parseBoolean(String.valueOf(session.get(Constant.CONDITION_STATE)));
        ConditionConfig conditionConfig = (ConditionConfig) action.getActionConfig();
        String nextActionCode = status ? conditionConfig.getTruly() : conditionConfig.getFalsely();
        Summary.of().info("action.condition.status.regexp", action.getName(), status, Optional.ofNullable(nextActionCode).orElse(Constant.NONE));
        if (StringUtils.isNotEmpty(nextActionCode) && action.getActionChain() != null) {
            action.getActionChain().forEach(a -> {
                if (a.getAction().getCode().equals(nextActionCode)) {
                    List<ActionService> nextActionList = Lists.newArrayList(a);
                    execute(session, nextActionList);
                }
            });
        }
        session.remove(Constant.CONDITION_STATE);
    }

    protected void execute(Session session, List<ActionService> actionServices) {
        // Loop execution action
        for (ActionService actionService : actionServices) {
            //execute event action
            log.debug("{} -> Execute event action: {}.", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()), actionService.getAction().getCode());
            ExecuteResult executeResult = actionService.prepare(session).execute();
            if (actionService.checkError()) {
                this.actionExecuteErrorCount.incrementAndGet();
            } else {
                this.actionExecuteSuccessCount.incrementAndGet();
            }
            //update action output parameters
            actionService.updateOutput(executeResult);
            //
            Action action = actionService.getAction();
            conditionHandler(session, action);
            //
            if (actionService.hasNextAction()) {
                execute(session, action.getActionChain());
            }
        }
    }

    protected void updateContext(ExecutionContext context, String code) {
        //check exception
        Throwable cause = executeThrowable.get();
        context.setEventExecuteStatus(ExecuteStatus.NORMAL);
        if (cause != null) {
            Summary.of().error("event.error.regexp", code, cause);
            context.setEventExecuteStatus(ExecuteStatus.COMPLETED_EXCEPTIONALLY);
        }
        //update execution context vars
        context.setActionSuccessCount(actionExecuteSuccessCount.get());
        context.setActionErrorCount(actionExecuteErrorCount.get());
        context.setEventRetryCount(eventExecuteRetryCount.get());

        Summary.of().info("event.complete.regexp");
        context.setEventExecuteEndTime();
        Summary.of().info("event.calc.time.regexp",
                context.getTotalProcessTime(),
                context.getChannelProcessTime(),
                context.getEventProcessTime());

        actionExecuteErrorCount.set(0);
        actionExecuteSuccessCount.set(0);
        eventExecuteRetryCount.set(0);
        executeThrowable.set(null);
    }

    protected abstract void afterProcess(Session session, IN result);

    @Override
    public abstract Session map(IN value) throws Exception;

    @Override
    public TypeInformation<ChannelMessage> getProducedType() {
        return TypeInformation.of(ChannelMessage.class);
    }
}
