/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.handler;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.enums.DataType;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * Data type convert
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class DataTypeConvert implements Serializable {

    private static final String POINT = ".";
    private static final String TRUE_NUMBER = "1";
    private static final String TRUE_FLAG = "yes";
    private static final String FALSE_NUMBER = "0";
    private static final String FALSE_FLAG = "no";
    private static final int DECIMAL_SCALE = 10;

    public static <T extends Serializable> T getValue(String dataType, Object value) {
        if (StringUtils.isEmpty(dataType)) {
            throw new IllegalArgumentException("DataType cannot be null");
        }
        DataType dt = DataType.valueOfType(dataType);
        return getValue(dt, value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T getValue(DataType dataType, Object value) {
        Preconditions.checkNotNull(dataType, CommonUtils.format("Unsupported data type {0}", dataType));
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            T defaultValue = (T) dataType.getDefaultValue();
            log.warn("This value is null, return the default value '{}'", defaultValue);
            return defaultValue;
        }
        return (T) convert(dataType.getClassType(), String.valueOf(value));
    }

    public static <T extends Serializable> T convert(Class<T> clazz, String value) {
        if (clazz == Integer.class) {
            if (value.contains(POINT)) {
                value = StringUtils.substringBefore(value, POINT);
            }
            return clazz.cast(Integer.parseInt(value));
        } else if (clazz == Long.class) {
            if (value.contains(POINT)) {
                value = StringUtils.substringBefore(value, POINT);
            }
            return clazz.cast(Long.parseLong(value));
        } else if (clazz == String.class) {
            return clazz.cast(value);
        } else if (clazz == Double.class) {
            return clazz.cast(Double.parseDouble(value));
        } else if (clazz == Boolean.class) {
            boolean b;
            if (StringUtils.equalsAny(value, TRUE_NUMBER, FALSE_NUMBER)) {
                b = TRUE_NUMBER.equals(value);
            } else if (StringUtils.equalsAnyIgnoreCase(value, TRUE_FLAG, FALSE_FLAG)) {
                b = TRUE_FLAG.equalsIgnoreCase(value);
            } else {
                b = Boolean.parseBoolean(value);
            }
            return clazz.cast(b);
        } else if (clazz == BigDecimal.class) {
            return clazz.cast(new BigDecimal(value).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP));
        } else if (clazz == Date.class) {
            return clazz.cast(DateTime.parse(value, DateTimeFormat.forPattern(Constant.DATE_FORMAT)).toDate());
        }
        return clazz.cast(value);
    }


}
