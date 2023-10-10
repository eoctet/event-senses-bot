/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import pro.octet.senses.bot.core.entity.AbstractConfig;

/**
 * Email config model
 *
 * @author William
 * @since 22.0816.2.6
 */
@Data
public class EmailConfig extends AbstractConfig {

    private String server;
    private String smtp;
    private boolean ssl;
    private boolean tls;
    private String username;
    private String password;
    private String subject;
    private String from;
    private String to;
    private String cc;
    private String content;

    public String[] getRecipients() {
        if (StringUtils.isNotBlank(to)) {
            return StringUtils.split(to, ",");
        }
        return null;
    }

    public String[] getCarbonCopies() {
        if (StringUtils.isNotBlank(cc)) {
            return StringUtils.split(cc, ",");
        }
        return null;
    }

}
