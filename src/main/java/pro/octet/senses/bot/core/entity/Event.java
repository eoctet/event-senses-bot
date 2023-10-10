/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import lombok.Data;
import pro.octet.senses.bot.core.action.ActionService;

import java.io.Serializable;
import java.util.List;

@Data
public class Event implements Serializable {

    private String name;
    private String code;
    private String catalog;
    private String desc;
    private Channel channel;
    private Integer retry;
    private Integer actionSize;
    private Action patternAction;
    private List<ActionService> actionServices;

}
