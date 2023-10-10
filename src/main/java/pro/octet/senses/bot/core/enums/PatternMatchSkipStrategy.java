/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.enums;

/**
 * Pattern match skip strategy
 *
 * @author William
 * @since 22.0816.2.6
 */
public enum PatternMatchSkipStrategy {
    NO_SKIP,
    SKIP_PAST_LAST_EVENT,
    SKIP_TO_FIRST,
    SKIP_TO_LAST,
    SKIP_TO_NEXT
}
