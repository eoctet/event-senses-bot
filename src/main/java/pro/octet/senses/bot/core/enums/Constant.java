/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.enums;

public class Constant {
    /**
     * COMMONS CONSTANT PARAMS
     */
    public static final String PREFIX = "app";
    public static final String JDBC_CLIENT_NAME = "event-senses-bot";
    public static final String SEPARATOR = ":";
    public static final String YAML_FILE = "yaml";
    public static final String PARENT_CODE = "0";
    /**
     * SESSION CONSTANT PARAMS
     */
    public static final String EVENT_CONTEXT = "event_context";
    public static final String CHANNEL_MESSAGE = "channel_message";
    public static final String CONDITION_STATE = "condition_state";
    public static final String PATTERN_RESULT = "pattern_result";
    /**
     * ACTION CONSTANT PARAMS
     */
    public static final String LAST_ACTION_OUTPUT = "last_action_output";
    public static final String PARAM_REGEX_PATTERN = "\\$\\{\\s*\\w+(\\[\\d+\\]|.\\w+|.\\w+.\\w+|.\\w+\\[\\d+\\]|.\\w+\\[\\d+\\].\\w+)\\s*\\}";
    public static final String DATE_FORMAT_WITH_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String NONE = "NONE";
    public static final String WITHIN_REGEX_PATTERN = "^\\d+\\s*((MILLISECONDS|SECONDS|MINUTES|HOURS|DAYS)|(milliseconds|seconds|minutes|hours|days))";
}