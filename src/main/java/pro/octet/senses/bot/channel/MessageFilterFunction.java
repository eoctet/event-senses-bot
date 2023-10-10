/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.channel;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import pro.octet.senses.bot.core.ApplicationConfig;
import pro.octet.senses.bot.core.data.CacheManager;
import pro.octet.senses.bot.core.entity.Channel;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.ProcessStatus;
import pro.octet.senses.bot.utils.CommonUtils;

/**
 * Channel message filter function
 * If duplicate filtering is enabled, filter will be work.
 *
 * @author William
 * @see org.apache.flink.api.common.functions.RichFilterFunction
 * @see ChannelMessage
 * @see Channel
 * @since 22.0816.2.6
 */
@Slf4j
public class MessageFilterFunction extends RichFilterFunction<ChannelMessage> implements ResultTypeQueryable<ChannelMessage> {

    private final ApplicationConfig appConfig;
    private final boolean doFilter;
    private final long jobFilterMaxCacheExpire;

    public MessageFilterFunction(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
        this.doFilter = appConfig.getBoolean(ConfigOption.JOB_FILTER_ENABLED);
        this.jobFilterMaxCacheExpire = appConfig.getLong(ConfigOption.JOB_FILTER_MAX_CACHE_EXPIRE);
    }

    @Override
    public void open(Configuration parameters) {
        CacheManager.getInstance().build(appConfig);
    }

    @Override
    public boolean filter(ChannelMessage message) {
        log.debug("{} -> Execute message filter.", CommonUtils.getTaskNameWithIndex(this.getRuntimeContext()));
        if (doFilter && message.getChannelProcessStatus() == ProcessStatus.NORMAL) {
            String filterKeyName = message.getChannelMessageFilter();
            if (filterKeyName != null) {
                boolean locked = CacheManager.getInstance().setLock(
                        filterKeyName,
                        filterKeyName,
                        jobFilterMaxCacheExpire);
                if (!locked) {
                    message.setChannelProcessStatus(ProcessStatus.REPEATED);
                }
                log.debug("Event channel message duplicate filtering, Repeat status: {}.", !locked);
            }
        }
        message.setChannelProcessTime();
        // PS: true means pass, false will be filtered
        return true;
    }

    @Override
    public TypeInformation<ChannelMessage> getProducedType() {
        return TypeInformation.of(ChannelMessage.class);
    }
}
