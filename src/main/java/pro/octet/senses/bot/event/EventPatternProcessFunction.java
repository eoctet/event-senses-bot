/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.functions.TimedOutPartialMatchHandler;
import org.apache.flink.util.Collector;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.entity.PatternResult;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class EventPatternProcessFunction extends PatternProcessFunction<ChannelMessage, PatternResult> implements TimedOutPartialMatchHandler<ChannelMessage> {

    @Override
    public void processMatch(Map<String, List<ChannelMessage>> map, Context context, Collector<PatternResult> collector) {
        PatternResult patternResult = new PatternResult();
        try {
            patternResult.setResult(map);
        } catch (Exception e) {
            String code = CommonUtils.createCode();
            log.error("Process event pattern message error, ID: [{}]", code, e);
            patternResult.setProcessError(code, e);
        } finally {
            collector.collect(patternResult);
        }
    }

    @Override
    public void processTimedOutMatch(Map<String, List<ChannelMessage>> map, Context context) {
        //OutputTag<ChannelMessage> lateDataOutputTag = new OutputTag<>("late-message", TypeInformation.of(ChannelMessage.class));
        map.forEach((event, messages) -> {
            //messages.forEach(e -> context.output(lateDataOutputTag, e));
            log.warn("Process timeout event messages, Event name: {}, Message size: {}.", event, messages.size());
        });
    }
}
