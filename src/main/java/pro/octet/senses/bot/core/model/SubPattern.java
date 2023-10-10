/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.model;

import lombok.Data;
import pro.octet.senses.bot.core.enums.PatternMode;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;

/**
 * Sub pattern config
 *
 * @author William
 * @since 22.0816.2.6
 */
@Data
public class SubPattern implements Serializable {

    private String name;
    private String where;
    private int times;
    private boolean optional;
    private PatternMode mode = PatternMode.FOLLOWED_BY;

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
