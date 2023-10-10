/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.channel;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import pro.octet.senses.bot.core.entity.Channel;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.entity.ChannelParam;
import pro.octet.senses.bot.core.enums.ProcessStatus;
import pro.octet.senses.bot.utils.CommonUtils;
import pro.octet.senses.bot.utils.XmlParser;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Event message deserialization schema
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class MessageDeserializationSchema implements DeserializationSchema<ChannelMessage>, SerializationSchema<ChannelMessage> {

    private final Channel channel;

    public MessageDeserializationSchema(Channel channel) {
        this.channel = channel;
    }

    @Override
    public ChannelMessage deserialize(byte[] bytes) {
        String originalMessage = new String(bytes, StandardCharsets.UTF_8);
        if (StringUtils.isBlank(originalMessage)) {
            log.warn("An empty channel message was received and discarded.");
            return null;
        }
        ChannelMessage message = new ChannelMessage(originalMessage);
        log.debug("Receive a channel message, start parsing the message: {}.", originalMessage);
        try {
            Map<String, Object> messageMaps;
            switch (channel.getFormat()) {
                case XML:
                    messageMaps = XmlParser.parseXmlToMap(originalMessage);
                    break;
                case JSON:
                    messageMaps = CommonUtils.parseJsonToMap(originalMessage, String.class, Object.class);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported message data format");
            }
            List<ChannelParam> params = channel.getParams();
            message.setValues(params, messageMaps);
        } catch (Exception e) {
            log.error("Channel message parse error, ID: [{}]", CommonUtils.createCode(), e);
            //message.setChannelProcessError(code, e);
        } finally {
            if (message.getChannelProcessStatus() != ProcessStatus.ERROR && message.getMessageValuesCount() == 0) {
                message.setChannelProcessStatus(ProcessStatus.NOTMATCH);
            }
        }
        return message;
    }

    @Override
    public boolean isEndOfStream(ChannelMessage channelMessage) {
        return false;
    }

    @Override
    public TypeInformation<ChannelMessage> getProducedType() {
        return TypeInformation.of(ChannelMessage.class);
    }

    @Override
    public byte[] serialize(ChannelMessage element) {
        return element.getChannelOriginalMessage().getBytes(StandardCharsets.UTF_8);
    }
}
