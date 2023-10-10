/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.model;

import lombok.Data;
import pro.octet.senses.bot.core.entity.AbstractConfig;
import pro.octet.senses.bot.core.enums.PatternMatchSkipStrategy;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.List;

@Data
public class PatternConfig extends AbstractConfig {

    private List<SubPattern> patterns;
    private PatternMatchSkipStrategy matchSkipStrategy = PatternMatchSkipStrategy.NO_SKIP;
    private String matchSkipPattern;
    private String within;

    @Override
    public String toString() {
        return CommonUtils.toJson(this);
    }
}
