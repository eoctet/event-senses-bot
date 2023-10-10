/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.model;

import lombok.Data;
import pro.octet.senses.bot.core.entity.AbstractConfig;
import pro.octet.senses.bot.utils.CommonUtils;

@Data
public class ConditionConfig extends AbstractConfig {

    private String expression;
    private String truly;
    private String falsely;

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
