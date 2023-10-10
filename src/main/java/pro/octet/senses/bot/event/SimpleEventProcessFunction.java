/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.event;

import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.core.ExecutionContext;
import pro.octet.senses.bot.core.action.ActionService;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.entity.ResultSet;
import pro.octet.senses.bot.core.entity.Session;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.enums.ProcessStatus;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.List;

@Slf4j
public class SimpleEventProcessFunction extends AbstractEventProcessFunction<ChannelMessage> {

    public SimpleEventProcessFunction(ExecutionContext context) {
        super(context);
    }

    @Override
    public void afterProcess(Session session, ChannelMessage message) {
        String code = CommonUtils.createCode();
        Throwable cause = executeThrowable.get();
        if (cause != null) {
            log.error("{} -> Executing event action error, ID: [{}]", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()), code, cause);
        }
        updateContext(context, code);
        //update result set data
        ResultSet resultSet = new ResultSet();
        resultSet.generateResultSet(context, message);
        session.put(appConfig.getString(ConfigOption.DB_CH_TABLE), resultSet);
        log.debug("{} -> Create event metrics data: {}.", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()), resultSet);
    }

    @Override
    public Session map(ChannelMessage message) {
        // Prepare default session
        context.setEventExecuteStartTime();
        context.setChannelProcessTime(message.getChannelProcessTime());
        Summary.of().info("channel.message.parse.complete.regexp", context.getChannel().getFormat().name(), message.getMessageValuesCount());
        Summary.of().info("channel.message.filter.regexp", message.getChannelProcessStatus());
        Session session = new Session();

        try {
            if (message.getChannelProcessStatus() != ProcessStatus.NORMAL) {
                //if (message.getChannelProcessError() != null) {
                //   Summary.of().error("channel.error.regexp", message.getChannelProcessErrorCode(), message.getChannelProcessError());
                //}
                log.warn("{} -> Channel message status: {}, skip event calculation.", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()), message.getChannelProcessStatus().name());
                Summary.of().info("event.message.warn.regexp", message.getChannelProcessStatus().name());
                return session;
            }
            session.put(Constant.CHANNEL_MESSAGE, message);
            session.put(Constant.EVENT_CONTEXT, context);
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
            afterProcess(session, message);
        }
        return session;
    }

}
