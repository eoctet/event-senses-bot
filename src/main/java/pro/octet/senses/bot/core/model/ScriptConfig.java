/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.model;

import lombok.Data;
import pro.octet.senses.bot.core.entity.AbstractConfig;
import pro.octet.senses.bot.core.enums.DataFormat;
import pro.octet.senses.bot.utils.CommonUtils;

@Data
public class ScriptConfig extends AbstractConfig {

    private String lang;
    private StringBuilder code;
    private DataFormat format;

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
