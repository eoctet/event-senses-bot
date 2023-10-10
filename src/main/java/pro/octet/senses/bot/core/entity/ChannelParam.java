/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import lombok.Data;
import pro.octet.senses.bot.core.enums.DataType;

import java.io.Serializable;

@Data
public class ChannelParam implements Serializable {

    private String param;
    private boolean eventtime;
    private boolean filter;
    private boolean nullable;
    private DataType datatype;
    private String desc;

}
