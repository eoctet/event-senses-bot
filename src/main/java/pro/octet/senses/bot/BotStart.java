/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import pro.octet.senses.bot.core.ApplicationConfig;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.event.EventStream;

/**
 * Event senses bot start main class
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class BotStart {

    private static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption(Constant.YAML_FILE, true, "YAML configuration file path");
    }

    public static void main(String[] args) throws Exception {
        log.info("Starting event senses job...");
        //init event context
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(OPTIONS, args);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("event-senses", OPTIONS);

        final ApplicationConfig config = new ApplicationConfig();
        config.addOption(ConfigOption.APP_YAML, StringUtils.trim(cmd.getOptionValue(Constant.YAML_FILE)));
        config.addOption(ConfigOption.APP_PUBLISH_TIME, System.currentTimeMillis());
        config.loadConfig();

        // Begin process events
        EventStream eventStream = new EventStream(config);
        eventStream.execute();
    }
}
