/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.action.pattern;

import com.google.common.collect.Maps;
import de.odysseus.el.util.SimpleContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.cep.pattern.conditions.RichIterativeCondition;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.enums.ProcessStatus;
import pro.octet.senses.bot.core.model.SubPattern;
import pro.octet.senses.bot.core.script.ExpressionEngine;
import pro.octet.senses.bot.utils.CommonUtils;

import javax.el.PropertyNotFoundException;
import java.util.Map;

/**
 * Patten condition
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class PatternCondition extends RichIterativeCondition<ChannelMessage> {

    private final SubPattern subPattern;

    public PatternCondition(SubPattern subPattern) {
        this.subPattern = subPattern;
    }

    @Override
    public boolean filter(ChannelMessage message, Context<ChannelMessage> context) throws Exception {
        boolean conditionState = false;
        try {
            if (message.getChannelProcessStatus() != ProcessStatus.NORMAL) {
                log.debug("Event message status: {}, Skip.", message.getChannelProcessStatus().name());
                return false;
            }
            Map<String, Object> parameters = Maps.newHashMap(message.getMessageValue());
            SimpleContext simpleContext = ExpressionEngine.getInstance().buildSimpleContext(parameters);
            conditionState = ExpressionEngine.getInstance().execute(subPattern.getWhere(), simpleContext);
            log.trace("Event pattern: {}, condition: {}, pattern params: {}.", subPattern.getName(), conditionState, subPattern);
        } catch (PropertyNotFoundException pnf) {
            log.trace("Execute pattern condition failed, Event pattern: {}, ID: [{}]", subPattern.getName(), CommonUtils.createCode(), pnf);
        } catch (Exception e) {
            log.error("Execute pattern condition error, Event pattern: {}, ID: [{}]", subPattern.getName(), CommonUtils.createCode(), e);
        }
        return conditionState;
    }
}
