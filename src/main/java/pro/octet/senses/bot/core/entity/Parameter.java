/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import lombok.Data;
import pro.octet.senses.bot.core.enums.DataType;

import java.io.Serializable;

@Data
public class Parameter implements Serializable {

    private String param;
    private DataType datatype;
    private Object value;
    private String desc;

    public Parameter(String param, DataType datatype, Object value, String desc) {
        this.param = param;
        this.datatype = datatype;
        this.value = value;
        this.desc = desc;
    }

    public Parameter() {
    }
}
