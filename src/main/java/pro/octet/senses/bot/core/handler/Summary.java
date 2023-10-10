/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.context.support.ResourceBundleMessageSource;
import pro.octet.senses.bot.core.ApplicationConfig;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.enums.LogType;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Summary Logger
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public final class Summary implements Serializable {
    private final static ResourceBundleMessageSource MESSAGE_SOURCE;
    private final static List<String> SKIP_MESSAGE;
    private static volatile Summary summary;
    private StringBuffer summaryLog;
    private StringBuffer stackLog;
    private Locale defaultLocale;
    private LogType logType;
    private boolean patternMode;

    static {
        MESSAGE_SOURCE = new ResourceBundleMessageSource();
        MESSAGE_SOURCE.setBasename("i18n/language");
        MESSAGE_SOURCE.setDefaultEncoding(StandardCharsets.UTF_8.toString());

        SKIP_MESSAGE = Arrays.asList(
                "channel.receive.message.regexp",
                "channel.message.parse.complete.regexp",
                "channel.message.filter.regexp"
        );
    }

    private Summary() {
    }

    public static Summary of() {
        if (summary == null) {
            synchronized (Summary.class) {
                if (summary == null) {
                    summary = new Summary();
                }
            }
        }
        return summary;
    }

    public void build(ApplicationConfig appConfig) {
        this.summaryLog = new StringBuffer();
        this.stackLog = new StringBuffer();
        this.defaultLocale = Locale.forLanguageTag(appConfig.getString(ConfigOption.APP_LANG));
        this.logType = LogType.valueOf(appConfig.getString(ConfigOption.APP_LOGGER));
        this.patternMode = appConfig.getBoolean(ConfigOption.JOB_PATTERN_MODE);
    }

    private void append(String message) {
        summaryLog.append("[").append(DateTime.now().toString(Constant.DATE_FORMAT_WITH_MILLIS)).append("] - ");
        summaryLog.append(message).append("\n");
    }

    private void append(String regexp, String code, String name, Throwable throwable) {
        String time = DateTime.now().toString(Constant.DATE_FORMAT_WITH_MILLIS);
        summaryLog.append("[").append(time).append("] - ");
        if (name != null) {
            summaryLog.append(convert(regexp, name, CommonUtils.getShortStackTrace(code, throwable))).append("\n");
        } else {
            summaryLog.append(convert(regexp, CommonUtils.getShortStackTrace(code, throwable))).append("\n");
        }
        stackLog.append("[").append(time).append("] - ");
        if (name != null) {
            stackLog.append(convert(regexp, name, CommonUtils.getStackTrace(code, throwable))).append("\n");
        } else {
            stackLog.append(convert(regexp, CommonUtils.getStackTrace(code, throwable))).append("\n");
        }
    }

    private String convert(String regexp) {
        return MESSAGE_SOURCE.getMessage(regexp, null, defaultLocale);
    }

    private String convert(String regexp, Object... args) {
        return MESSAGE_SOURCE.getMessage(regexp, args, defaultLocale);
    }

    public void info(String regexp) {
        if (patternMode && SKIP_MESSAGE.contains(regexp)) {
            return;
        }
        String message = convert(regexp);
        append(message);
        log.info(message);
    }

    public void info(String regexp, Object... args) {
        if (patternMode && SKIP_MESSAGE.contains(regexp)) {
            return;
        }
        String message = convert(regexp, args);
        append(message);
        log.info(message);
    }

    public void debug(String message, Object... args) {
        if (logType == LogType.INFO) {
            return;
        }
        String text = convert("event.debug.message.regexp", CommonUtils.format(message, args));
        append(text);
        log.info(text);
    }

    public void error(String regexp, String code, String name, Throwable throwable) {
        if (patternMode && SKIP_MESSAGE.contains(regexp)) {
            return;
        }
        append(regexp, code, name, throwable);
    }

    public void error(String regexp, String code, Throwable throwable) {
        if (patternMode && SKIP_MESSAGE.contains(regexp)) {
            return;
        }
        append(regexp, code, null, throwable);
    }

    public String getSummaryLogAndClear() {
        try {
            return summaryLog.toString();
        } finally {
            summaryLog.setLength(0);
        }
    }

    public String getStackLogAndClear() {
        try {
            return stackLog.toString();
        } finally {
            stackLog.setLength(0);
        }
    }

}
