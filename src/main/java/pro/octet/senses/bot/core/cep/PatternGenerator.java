/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.cep;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import pro.octet.senses.bot.action.pattern.PatternCondition;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.model.PatternConfig;
import pro.octet.senses.bot.core.model.SubPattern;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.List;

/**
 * Event pattern generator
 *
 * @author William
 * @since 22.0816.2.6
 */
public class PatternGenerator {

    private static Pattern<ChannelMessage, ChannelMessage> setParams(Pattern<ChannelMessage, ChannelMessage> pattern, SubPattern subPattern) {
        if (subPattern.getTimes() > 1) {
            pattern = pattern.times(subPattern.getTimes());
        }
        if (subPattern.isOptional()) {
            pattern = pattern.optional();
        }
        return pattern;
    }

    private static AfterMatchSkipStrategy getMatchSkipStrategy(PatternConfig config) {
        switch (config.getMatchSkipStrategy()) {
            case SKIP_TO_LAST:
                Preconditions.checkArgument(StringUtils.isNotBlank(config.getMatchSkipPattern()), "Match skip pattern cannot be null.");
                return AfterMatchSkipStrategy.skipToLast(config.getMatchSkipPattern());
            case SKIP_TO_NEXT:
                return AfterMatchSkipStrategy.skipToNext();
            case SKIP_TO_FIRST:
                Preconditions.checkArgument(StringUtils.isNotBlank(config.getMatchSkipPattern()), "Match skip pattern cannot be null.");
                return AfterMatchSkipStrategy.skipToFirst(config.getMatchSkipPattern());
            case SKIP_PAST_LAST_EVENT:
                return AfterMatchSkipStrategy.skipPastLastEvent();
            case NO_SKIP:
                return AfterMatchSkipStrategy.noSkip();
            default:
                throw new IllegalArgumentException("Unsupported event pattern MatchSkipStrategy type.");
        }
    }

    private static Pattern<ChannelMessage, ChannelMessage> getNextSubPattern(Pattern<ChannelMessage, ChannelMessage> pattern, SubPattern subPattern) {
        PatternCondition condition = new PatternCondition(subPattern);
        switch (subPattern.getMode()) {
            case NEXT:
                return pattern.next(subPattern.getName()).where(condition);
            case NOT_NEXT:
                return pattern.notNext(subPattern.getName()).where(condition);
            case FOLLOWED_BY:
                return pattern.followedBy(subPattern.getName()).where(condition);
            case FOLLOWED_BY_ANY:
                return pattern.followedByAny(subPattern.getName()).where(condition);
            case NOT_FOLLOWED_BY:
                return pattern.notFollowedBy(subPattern.getName()).where(condition);
            default:
                throw new IllegalArgumentException("Unsupported pattern mode type");
        }
    }

    public static Pattern<ChannelMessage, ChannelMessage> generate(Action patternAction) {
        PatternConfig config = (PatternConfig) patternAction.getActionConfig();
        Preconditions.checkNotNull(config, "Event pattern config cannot be null.");
        List<SubPattern> subPatterns = config.getPatterns();
        Preconditions.checkArgument(subPatterns != null && !subPatterns.isEmpty(), "Event sub pattern cannot be null.");

        SubPattern first = subPatterns.get(0);
        Pattern<ChannelMessage, ChannelMessage> pattern = Pattern.<ChannelMessage>begin(first.getName(), getMatchSkipStrategy(config)).where(new PatternCondition(first));
        pattern = setParams(pattern, first);
        for (int i = 1; i < subPatterns.size(); i++) {
            SubPattern subPattern = subPatterns.get(i);
            pattern = getNextSubPattern(pattern, subPattern);
            pattern = setParams(pattern, subPattern);
        }
        if (StringUtils.isNotBlank(config.getWithin())) {
            pattern = pattern.within(CommonUtils.parseTime(config.getWithin()));
        }
        return pattern;
    }
}
