/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.entity;

import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class ActionParam extends ConcurrentHashMap<String, Object> implements Serializable {

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
