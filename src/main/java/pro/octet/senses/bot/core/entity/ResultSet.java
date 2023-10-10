/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import pro.octet.senses.bot.core.ExecutionContext;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class ResultSet extends ConcurrentHashMap<String, Object> implements Serializable {

    private void generateCommonValues(ExecutionContext context) {
        this.put("id", CommonUtils.createCode());
        this.put("event_log", CommonUtils.getEventLogName(context.getEvent().getCode()));
        this.put("event_name", context.getEvent().getName());
        this.put("event_code", context.getEvent().getCode());
        this.put("channel_name", context.getEvent().getChannel().getName());
        this.put("channel_code", context.getEvent().getChannel().getCode());
        this.put("event_publish_time", context.getEventPublishTime());
        this.put("event_execute_start", context.getEventExecuteStartTime());
        this.put("event_execute_end", context.getEventExecuteEndTime());
        this.put("event_summary_log", Summary.of().getSummaryLogAndClear());
        this.put("event_stack_log", Summary.of().getStackLogAndClear());
        this.put("event_execute_status", context.getEventExecuteStatus().getCode());
        this.put("action_error_count", context.getActionErrorCount());
        this.put("action_success_count", context.getActionSuccessCount());
        this.put("event_retry_count", context.getEventRetryCount());
        this.put("total_process_time", context.getTotalProcessTime());
        this.put("channel_process_time", context.getChannelProcessTime());
        this.put("event_process_time", context.getEventProcessTime());
        this.put("createtime", System.currentTimeMillis());
    }

    public void generateResultSet(ExecutionContext context, ChannelMessage message) {
        this.generateCommonValues(context);
        this.put("channel_receive_time", message.getChannelReceiveTime());
        this.put("channel_message", message.getChannelOriginalMessage());
        this.put("channel_message_count", 1);
        this.put("channel_process_status", message.getChannelProcessStatus().name());
        this.put("event_pattern_matched", 0);
    }

    public void generateResultSet(ExecutionContext context, PatternResult patternResult) {
        this.generateCommonValues(context);
        this.put("channel_receive_time", context.getEventExecuteStartTime());
        this.put("channel_message", patternResult.getChannelOriginalMessages());
        this.put("channel_message_count", patternResult.getChannelMessageCount());
        this.put("channel_process_status", patternResult.getProcessStatus().name());
        this.put("event_pattern_matched", patternResult.getEventPatternMatched());
    }
}
