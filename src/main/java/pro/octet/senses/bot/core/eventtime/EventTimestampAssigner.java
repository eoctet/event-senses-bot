/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.eventtime;

import org.apache.flink.api.common.eventtime.TimestampAssigner;
import pro.octet.senses.bot.core.entity.ChannelMessage;

/**
 * Event timestamp assigner
 *
 * @author William
 * @since 22.0816.2.6
 */
public class EventTimestampAssigner implements TimestampAssigner<ChannelMessage> {

    @Override
    public long extractTimestamp(ChannelMessage element, long recordTimestamp) {
        return element.getChannelEventTime() > 0 ? element.getChannelEventTime() : recordTimestamp;
    }

}
