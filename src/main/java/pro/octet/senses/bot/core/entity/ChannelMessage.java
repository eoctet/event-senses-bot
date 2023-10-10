/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import pro.octet.senses.bot.core.enums.ProcessStatus;
import pro.octet.senses.bot.core.handler.DataTypeConvert;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ChannelMessage implements Serializable {
    private final String channelOriginalMessage;
    private ProcessStatus channelProcessStatus;
    //    private final AtomicReference<Throwable> channelProcessError;
    //    private String channelProcessErrorCode;
    private final long channelReceiveTime;
    private String channelMessageFilter;
    private long channelProcessTime;
    private final MessageValue messageValue;
    private long channelEventTime;

    public ChannelMessage(String message) {
        this.channelOriginalMessage = message;
        this.channelProcessStatus = ProcessStatus.NORMAL;
        // this.channelProcessError = new AtomicReference<>();
        this.channelReceiveTime = System.currentTimeMillis();
        this.messageValue = new MessageValue();
    }

    public void setValues(List<ChannelParam> params, Map<String, Object> messageMaps) {
        params.forEach(p -> {
            String key = p.getParam();
            if (messageMaps.containsKey(key)) {
                messageValue.put(key, DataTypeConvert.getValue(p.getDatatype(), messageMaps.get(key)));
                if (p.isFilter()) {
                    this.channelMessageFilter = String.valueOf(messageMaps.get(key));
                }
                if (p.isEventtime()) {
                    this.channelEventTime = Long.parseLong(String.valueOf(messageMaps.get(key)));
                }
            }
        });
    }

    public String getChannelOriginalMessage() {
        return channelOriginalMessage;
    }

    public ProcessStatus getChannelProcessStatus() {
        return channelProcessStatus;
    }

    public long getChannelReceiveTime() {
        return channelReceiveTime;
    }

    public int getMessageValuesCount() {
        return messageValue.size();
    }

    public String getChannelMessageFilter() {
        return channelMessageFilter;
    }

    public void setChannelProcessStatus(ProcessStatus channelProcessStatus) {
        this.channelProcessStatus = channelProcessStatus;
    }

//    public void setChannelProcessError(String code, Throwable throwable) {
//        channelProcessError.compareAndSet(null, throwable);
//        channelProcessStatus = ProcessStatus.ERROR;
//        channelProcessErrorCode = code;
//    }

//    public Throwable getChannelProcessError() {
//        return channelProcessError.get();
//    }
//
//    public String getChannelProcessErrorCode() {
//        return channelProcessErrorCode;
//    }

    public void setChannelProcessTime() {
        this.channelProcessTime = System.currentTimeMillis() - this.channelReceiveTime;
    }

    public long getChannelProcessTime() {
        return this.channelProcessTime;
    }

    public MessageValue getMessageValue() {
        return messageValue;
    }

    public long getChannelEventTime() {
        return channelEventTime;
    }

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
