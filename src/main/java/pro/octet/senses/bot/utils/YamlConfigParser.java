/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.utils;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import pro.octet.senses.bot.exception.EventConfigNotFoundException;
import pro.octet.senses.bot.exception.EventConfigParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

/**
 * YAML CONFIG FILE PARSE UTIL
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class YamlConfigParser {

    /**
     * Parse YAML configuration file
     */
    public static Map<String, Object> parse(String path) {
        File file = new File(path);
        if (!file.isFile() || !file.exists()) {
            throw new EventConfigNotFoundException("Can not read yaml configuration file, please make sure it is valid");
        }
        Map<String, Object> properties;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            Yaml yml = new Yaml();
            properties = yml.load(bufferedReader);
            Preconditions.checkNotNull(properties, "Yaml configuration cannot be null");
        } catch (Exception e) {
            throw new EventConfigParseException("Parse yaml configuration file error", e);
        }
        log.debug("Yaml configuration:\n{}", properties);
        return properties;
    }

}