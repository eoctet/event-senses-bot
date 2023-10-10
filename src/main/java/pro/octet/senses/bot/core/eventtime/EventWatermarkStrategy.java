/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.eventtime;

import org.apache.flink.api.common.eventtime.*;
import pro.octet.senses.bot.core.entity.ChannelMessage;

import java.time.Duration;

/**
 * Event watermark strategy
 *
 * @author William
 * @since 22.0816.2.6
 */
public class EventWatermarkStrategy implements WatermarkStrategy<ChannelMessage> {
    @Override
    public WatermarkGenerator<ChannelMessage> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context) {
        return new EventOrderTimeWatermarks(Duration.ofMillis(0));
    }

    @Override
    public TimestampAssigner<ChannelMessage> createTimestampAssigner(TimestampAssignerSupplier.Context context) {
        return new EventTimestampAssigner();
    }
}
