/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.event;

import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.core.ExecutionContext;
import pro.octet.senses.bot.core.action.ActionService;
import pro.octet.senses.bot.core.entity.PatternResult;
import pro.octet.senses.bot.core.entity.ResultSet;
import pro.octet.senses.bot.core.entity.Session;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.enums.ProcessStatus;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.List;

@Slf4j
public class ComplexEventProcessFunction extends AbstractEventProcessFunction<PatternResult> {

    public ComplexEventProcessFunction(ExecutionContext context) {
        super(context);
    }

    @Override
    public void afterProcess(Session session, PatternResult patternResult) {
        String code = CommonUtils.createCode();
        Throwable cause = executeThrowable.get();
        if (cause != null) {
            log.error("{} -> Executing event action error, ID: [{}]", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()), code, cause);
        }
        updateContext(context, code);
        //update result set data
        ResultSet resultSet = new ResultSet();
        resultSet.generateResultSet(context, patternResult);
        session.put(appConfig.getString(ConfigOption.DB_CH_TABLE), resultSet);
        log.debug("{} -> Create event metrics data: {}.", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()), resultSet);
    }

    @Override
    public Session map(PatternResult patternResult) throws Exception {
        // Prepare default session
        context.setEventExecuteStartTime();
        Session session = new Session();

        try {
            if (patternResult.getProcessStatus() != ProcessStatus.NORMAL) {
                if (patternResult.getProcessError() != null) {
                    Summary.of().error("event.error.regexp", patternResult.getProcessErrorCode(), patternResult.getProcessError());
                }
                log.warn("{} -> Channel message status: {}, skip event calculation.", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()), patternResult.getProcessStatus().name());
                Summary.of().info("event.message.warn.regexp", patternResult.getProcessStatus());
                return session;
            }
            context.setChannelProcessTime(patternResult.getChannelProcessTimeCount());
            session.put(Constant.PATTERN_RESULT, patternResult);
            session.put(Constant.EVENT_CONTEXT, context);
            Summary.of().info("event.pattern.matched.regexp", patternResult.getEventPatternMatched(), patternResult.getChannelMessageCount());
            Summary.of().debug("Pattern result: {0}", patternResult);
            Summary.of().info("event.start.regexp");
            Summary.of().info("event.show.action.regexp", context.getEvent().getActionSize());

            List<ActionService> actionServices = context.getEvent().getActionServices();
            if (actionServices == null) {
                log.warn("No event actions are available for Event, Skip.");
                return session;
            }
            execute(session, actionServices);
        } catch (Exception e) {
            executeThrowable.compareAndSet(null, e);
        } finally {
            afterProcess(session, patternResult);
        }
        return session;
    }
}
