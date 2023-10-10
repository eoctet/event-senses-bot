/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import lombok.Data;
import pro.octet.senses.bot.core.action.ActionService;
import pro.octet.senses.bot.core.enums.ActionType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class Action implements Serializable {

    private String name;
    private String code;
    private ActionType type;
    private String parent;
    private Map<String, Object> config;
    private List<ActionService> actionChain;
    private AbstractConfig actionConfig;
    private List<Parameter> actionOutput;

}
