/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import lombok.Data;
import pro.octet.senses.bot.core.enums.DataFormat;

import java.io.Serializable;
import java.util.List;

@Data
public class Channel implements Serializable {

    private String name;
    private String code;
    private String topic;
    private DataFormat format;
    private String groupId;
    private List<ChannelParam> params;

}
