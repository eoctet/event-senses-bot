/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core;

import com.google.common.collect.Lists;
import pro.octet.senses.bot.core.action.ActionBuilder;
import pro.octet.senses.bot.core.action.ActionService;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.Channel;
import pro.octet.senses.bot.core.entity.Event;
import pro.octet.senses.bot.core.enums.*;
import pro.octet.senses.bot.exception.EventConfigParseException;
import pro.octet.senses.bot.utils.CommonUtils;
import pro.octet.senses.bot.utils.YamlConfigParser;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Execution context
 *
 * @author William
 * @since 22.0816.2.6
 */
public class ExecutionContext implements Serializable {

    private final Event event;
    private final Channel channel;
    private final ApplicationConfig appConfig;
    private long actionErrorCount;
    private long actionSuccessCount;
    private long eventRetryCount;
    private long channelProcessTime;
    private long eventExecuteStartTime;
    private long eventExecuteEndTime;
    private ExecuteStatus eventExecuteStatus;
    private boolean patternMode;
    private final long eventPublishTime;

    public Event getEvent() {
        return event;
    }

    public Channel getChannel() {
        return channel;
    }

    public ApplicationConfig getAppConfig() {
        return appConfig;
    }

    public long getActionErrorCount() {
        return actionErrorCount;
    }

    public long getActionSuccessCount() {
        return actionSuccessCount;
    }

    public long getEventRetryCount() {
        return eventRetryCount;
    }

    public long getChannelProcessTime() {
        return channelProcessTime;
    }

    public long getEventExecuteStartTime() {
        return eventExecuteStartTime;
    }

    public long getEventExecuteEndTime() {
        return eventExecuteEndTime;
    }

    public ExecuteStatus getEventExecuteStatus() {
        return eventExecuteStatus;
    }

    public boolean isPatternMode() {
        return patternMode;
    }

    public long getEventPublishTime() {
        return eventPublishTime;
    }

    public void setActionErrorCount(long actionErrorCount) {
        this.actionErrorCount = actionErrorCount;
    }

    public void setActionSuccessCount(long actionSuccessCount) {
        this.actionSuccessCount = actionSuccessCount;
    }

    public void setEventRetryCount(long eventRetryCount) {
        this.eventRetryCount = eventRetryCount;
    }

    public void setChannelProcessTime(long channelProcessTime) {
        this.channelProcessTime = channelProcessTime;
    }

    public void setEventExecuteStartTime() {
        this.eventExecuteStartTime = System.currentTimeMillis();
    }

    public void setEventExecuteEndTime() {
        this.eventExecuteEndTime = System.currentTimeMillis();
    }

    public void setEventExecuteStatus(ExecuteStatus eventExecuteStatus) {
        this.eventExecuteStatus = eventExecuteStatus;
    }

    public long getTotalProcessTime() {
        return getEventProcessTime() + channelProcessTime;
    }

    public long getEventProcessTime() {
        return eventExecuteEndTime - eventExecuteStartTime;
    }

    public ExecutionContext(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
        this.eventExecuteStatus = ExecuteStatus.UNKNOWN;
        this.eventPublishTime = appConfig.getLong(ConfigOption.APP_PUBLISH_TIME);
        Map<String, Object> props = YamlConfigParser.parse(appConfig.getString(ConfigOption.APP_YAML));

        this.channel = CommonUtils.parseToObject(props.get(YamlTemplate.CHANNEL_NODE), Channel.class);
        this.event = CommonUtils.parseToObject(props.get(YamlTemplate.EVENT_NODE), Event.class);
        List<ActionService> actionServices = createActionGraph(props);
        this.event.setChannel(this.channel);
        this.event.setActionServices(actionServices);
    }

    protected List<ActionService> createActionGraph(Map<String, Object> props) {
        synchronized (this) {
            List<LinkedHashMap> actionElements = CommonUtils.parseToList(props.get(YamlTemplate.ACTIONS_NODE), LinkedHashMap.class);
            List<ActionService> actionServices = Lists.newLinkedList();
            actionElements.forEach(element -> {
                Action action = CommonUtils.parseToObject(element.get(YamlTemplate.ACTION_NODE), Action.class);
                if (action.getType() == null) {
                    throw new IllegalArgumentException("Illegal action type, Please confirm again");
                }
                if (action.getType() == ActionType.PATTERN) {
                    if (!Constant.PARENT_CODE.equals(action.getParent())) {
                        throw new EventConfigParseException("Pattern action must be start node");
                    }
                    patternMode = true;
                    appConfig.addOption(ConfigOption.JOB_PATTERN_MODE, true);
                    ActionBuilder.getInstance().setPatternConfig(action);
                    this.event.setPatternAction(action);
                } else {
                    if (this.event.getPatternAction() != null && action.getParent().equals(this.event.getPatternAction().getCode())) {
                        action.setParent(Constant.PARENT_CODE);
                    }
                    actionServices.add(ActionBuilder.getInstance().of(action));
                }
            });
            this.event.setActionSize(actionServices.size());
            // create action chain
            Map<String, List<ActionService>> elements = actionServices.stream().filter(node -> !Constant.PARENT_CODE.equals(node.getAction().getParent())).collect(Collectors.groupingBy(node -> node.getAction().getParent()));
            actionServices.forEach(node -> node.getAction().setActionChain(elements.get(node.getAction().getCode())));
            return actionServices.stream().filter(node -> Constant.PARENT_CODE.equals(node.getAction().getParent())).collect(Collectors.toList());
        }
    }

}
