/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.model;

import lombok.Data;
import pro.octet.senses.bot.core.entity.AbstractConfig;
import pro.octet.senses.bot.core.enums.DataFormat;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.List;

@Data
public class ApiConfig extends AbstractConfig {

    private String url;
    private String method;
    private List<HttpParam> request;
    private List<HttpParam> headers;
    private StringBuilder body;
    private int timeout;
    private DataFormat format;

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
