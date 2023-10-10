/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.enums.YamlTemplate;
import pro.octet.senses.bot.core.handler.DataTypeConvert;
import pro.octet.senses.bot.utils.CommonUtils;
import pro.octet.senses.bot.utils.YamlConfigParser;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Application global config
 *
 * @author William
 * @see ConfigOption
 * @since 22.0816.2.6
 */
@Slf4j
public class ApplicationConfig implements Serializable {

    private final Map<ConfigOption, Serializable> options;

    public ApplicationConfig() {
        this.options = Maps.newLinkedHashMap();
    }

    public void loadConfig() {
        log.info("Loading configuration file...");
        //
        Map<String, Object> properties = YamlConfigParser.parse(getString(ConfigOption.APP_YAML));
        //
        Map<String, Object> original = CommonUtils.parseJsonToMap(properties.get(YamlTemplate.APP_NODE), String.class, Object.class);
        Map<String, Object> target = Maps.newLinkedHashMap();
        CommonUtils.convertFlatMap(Constant.PREFIX, original, target);

        target.forEach((key, value) -> options.put(ConfigOption.valueOfKey(key), (Serializable) value));
    }

    public Map<ConfigOption, Serializable> getAllOptions() {
        return Collections.unmodifiableMap(this.options);
    }

    public Serializable getOption(ConfigOption option) {
        return this.options.getOrDefault(option, option.getDefaultValue());
    }

    public String getString(ConfigOption option) {
        return getOption(option, String.class);
    }

    public boolean getBoolean(ConfigOption option) {
        return getOption(option, Boolean.class);
    }

    public int getInt(ConfigOption option) {
        return getOption(option, Integer.class);
    }

    public long getLong(ConfigOption option) {
        return getOption(option, Long.class);
    }

    public void addOption(ConfigOption option, Serializable value) {
        this.options.put(option, value);
    }

    private <T extends Serializable> T getOption(ConfigOption option, Class<T> clazz) {
        Serializable value = this.options.getOrDefault(option, option.getDefaultValue());
        return DataTypeConvert.convert(clazz, String.valueOf(value));
    }
}
