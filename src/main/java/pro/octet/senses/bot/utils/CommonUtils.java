/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.ParserConfig;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.joda.time.DateTime;
import pro.octet.senses.bot.core.entity.ActionParam;
import pro.octet.senses.bot.core.enums.Constant;

import javax.annotation.Nullable;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common utils
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class CommonUtils {
    private static final Pattern PATTERN = Pattern.compile(Constant.PARAM_REGEX_PATTERN);
    private static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();
    private static final Joiner JOINER = Joiner.on(".").skipNulls();
    private static final String NUMBER_REGEX = "^\\d+";
    private static final int MAX_LOG_LINES = 4;
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static Time parseTime(String timeStr) {
        Preconditions.checkNotNull(timeStr, "Time condition cannot be null.");
        timeStr = StringUtils.trim(timeStr);
        boolean matched = Pattern.matches(Constant.WITHIN_REGEX_PATTERN, timeStr);
        if (matched) {
            Matcher matcher = Pattern.compile(NUMBER_REGEX).matcher(timeStr);
            if (matcher.find()) {
                String number = StringUtils.trim(matcher.group());
                String unit = StringUtils.substringAfter(timeStr, number).trim().toUpperCase();
                return Time.of(Long.parseLong(number), TimeUnit.valueOf(unit));
            }
        }
        return null;
    }

    public static String parameterBindingFormat(ActionParam params, String content) {
        Matcher matcher = PATTERN.matcher(content);
        while (matcher.find()) {
            String expression = matcher.group();
            //
            Object rs = null;
            try {
                SimpleContext simpleContext = new SimpleContext();
                params.forEach((key, value) -> simpleContext.setVariable(key, EXPRESSION_FACTORY.createValueExpression(value, value.getClass())));
                ValueExpression expr = EXPRESSION_FACTORY.createValueExpression(simpleContext, expression, Object.class);
                rs = expr.getValue(simpleContext);
            } catch (Exception e) {
                log.error("Parameter binding format error", e);
            }
            String realValue = String.valueOf(Optional.ofNullable(rs).orElse(expression));
            content = StringUtils.replaceOnce(content, expression, realValue);
        }
        return content;
    }

    public static <T> T parseToObject(String json, @Nullable Class<T> clazz) {
        return JSON.parseObject(json, clazz, ParserConfig.getGlobalInstance());
    }

    public static <T> T parseToObject(Object obj, @Nullable Class<T> clazz) {
        return parseToObject(JSON.toJSONString(obj), clazz);
    }

    public static <T> List<T> parseToList(Object obj, @Nullable Class<T> clazz) {
        return JSON.parseArray(JSON.toJSONString(obj), clazz, ParserConfig.getGlobalInstance());
    }

    public static String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static <K, V> LinkedHashMap<K, V> parseJsonToMap(String json, @Nullable Class<K> key, @Nullable Class<V> value) {
        return JSON.parseObject(json, new TypeReference<LinkedHashMap<K, V>>(key, value) {
        });
    }

    public static <K, V> LinkedHashMap<K, V> parseJsonToMap(Object obj, @Nullable Class<K> key, @Nullable Class<V> value) {
        return parseJsonToMap(JSON.toJSONString(obj), key, value);
    }

    public static String getStackTrace(String code, Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
        } catch (Exception e) {
            log.warn("", e);
        }
        return format("ID: [{0}] {1}", code, stringWriter.toString());
    }

    public static String getShortStackTrace(String code, Throwable throwable) {
        StringBuilder builder = new StringBuilder(throwable.toString() + "\n");
        StackTraceElement[] trace = throwable.getStackTrace();
        for (int i = 0; i < MAX_LOG_LINES; i++) {
            builder.append("\tat ").append(trace[i]).append("\n");
        }
        builder.append("\t... ...");
        return format("ID: [{0}] {1}", code, builder);
    }

    public static String getEventLogName(String code) {
        return StringUtils.join("LOG-", code, "-", DateTime.now().toString("yyyyMMddHHmmss"));
    }

    public static String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return MessageFormat.format(message, args);
    }

    public static String createCode() {
        return UUID.randomUUID().toString().toLowerCase();
    }

    public static void convertFlatMap(String prefix, Map<String, Object> original, Map<String, Object> target) {
        Preconditions.checkNotNull(original, "Original map cannot be null");
        Preconditions.checkNotNull(target, "Target map cannot be null");

        original.forEach((key, value) -> {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> elements = (Map<String, Object>) value;
                convertFlatMap(JOINER.join(prefix, key), elements, target);
            } else {
                target.put(JOINER.join(prefix, key), value);
            }
        });
    }

    public static String getTaskNameWithIndex(String taskName, RuntimeContext context) {
        return StringUtils.join(taskName, " (" + (context.getIndexOfThisSubtask() + 1), "/", context.getNumberOfParallelSubtasks() + ")");
    }

    public static String getTaskNameWithIndex(RuntimeContext context) {
        return StringUtils.join("Event-Process-Task (" + (context.getIndexOfThisSubtask() + 1), "/", context.getNumberOfParallelSubtasks() + ")");
    }

    public static String getMd5(String plainText) {
        try {
            byte[] btInput = plainText.getBytes(Charsets.UTF_8);
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public static String createEmailContentId() {
        return StringUtils.join("Event-senses-", StringUtils.substringAfterLast(UUID.randomUUID().toString().toLowerCase(), "-"), "-", System.currentTimeMillis());
    }

}
