/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.model;

import lombok.Data;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;

@Data
public class HttpParam implements Serializable {

    private String name;
    private String value;
    private String desc;

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
