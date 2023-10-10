/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.enums;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Global data types
 *
 * @author William
 * @since 22.0816.2.6
 */
public enum DataType {

    STRING(""),
    LONG(0L),
    INT(0),
    DOUBLE(0.0D),
    DECIMAL(BigDecimal.valueOf(0, 10)),
    DATE(new Date()),
    BOOLEAN(false);

    private static final Map<String, DataType> TYPES;

    static {
        Map<String, DataType> map = Maps.newHashMap();

        for (DataType type : values()) {
            if (map.put(type.name(), type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }


    private final Serializable defaultValue;

    private final Class<? extends Serializable> clazz;

    public Serializable getDefaultValue() {
        return defaultValue;
    }


    public Class<? extends Serializable> getClassType() {
        return clazz;
    }

    public static DataType valueOfType(String key) {
        return TYPES.get(key.trim().toUpperCase());
    }

    <T extends Serializable> DataType(T defaultValue) {
        this.defaultValue = defaultValue;
        this.clazz = defaultValue.getClass();
    }

}
