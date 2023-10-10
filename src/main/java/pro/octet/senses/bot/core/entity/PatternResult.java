/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.compress.utils.Lists;
import pro.octet.senses.bot.core.enums.ProcessStatus;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PatternResult implements Serializable {
    private String channelOriginalMessages;
    private int channelMessageCount;
    private int eventPatternMatched;
    private Map<String, List<MessageValue>> channelMessages;
    private long channelProcessTimeCount;
    private ProcessStatus processStatus;
    private AtomicReference<Throwable> processError;
    private String processErrorCode;

    public PatternResult() {
        this.channelOriginalMessages = null;
        this.channelMessageCount = 0;
        this.eventPatternMatched = 0;
        this.channelMessages = null;
        this.channelProcessTimeCount = 0;
        this.processStatus = ProcessStatus.UNKNOWN;
        this.processError = new AtomicReference<>();
    }

    public void setResult(Map<String, List<ChannelMessage>> patternMap) {
        Preconditions.checkNotNull(patternMap, "Pattern map cannot be null.");
        StringBuffer originalMessages = new StringBuffer();
        AtomicInteger messageCount = new AtomicInteger(0);
        AtomicLong channelProcessTimeCount = new AtomicLong(0);

        Map<String, List<MessageValue>> channelMessages = Maps.newHashMap();
        patternMap.forEach((key, value) -> {
            List<MessageValue> messages = Lists.newArrayList();
            value.forEach(m -> {
                messageCount.getAndIncrement();
                originalMessages.append(m.getChannelOriginalMessage()).append("\n");
                channelProcessTimeCount.set(channelProcessTimeCount.get() + m.getChannelProcessTime());
                messages.add(m.getMessageValue());
            });
            channelMessages.put(key, messages);
        });
        this.channelOriginalMessages = originalMessages.toString();
        this.channelMessageCount = messageCount.get();
        this.eventPatternMatched = patternMap.keySet().size();
        this.channelMessages = channelMessages;
        this.channelProcessTimeCount = channelProcessTimeCount.get();
        this.processStatus = ProcessStatus.NORMAL;
        this.processError = new AtomicReference<>();
    }

    public String getChannelOriginalMessages() {
        return channelOriginalMessages;
    }

    public int getChannelMessageCount() {
        return channelMessageCount;
    }

    public int getEventPatternMatched() {
        return eventPatternMatched;
    }

    public Map<String, List<MessageValue>> getChannelMessages() {
        return channelMessages;
    }

    public long getChannelProcessTimeCount() {
        return channelProcessTimeCount;
    }

    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

    public void setProcessError(String code, Throwable throwable) {
        processError.compareAndSet(null, throwable);
        processStatus = ProcessStatus.ERROR;
        processErrorCode = code;
    }

    public Throwable getProcessError() {
        return processError.get();
    }

    public String getProcessErrorCode() {
        return processErrorCode;
    }

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
