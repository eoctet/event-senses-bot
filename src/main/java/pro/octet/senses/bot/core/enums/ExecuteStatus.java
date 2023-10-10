/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.enums;

/**
 * Event execute status
 *
 * @author William
 * @since 22.0816.2.6
 */
public enum ExecuteStatus {

    NORMAL(0, "Normal"),
    STOP_EXCEPTIONALLY(1, "Stop exceptionally"),
    COMPLETED_EXCEPTIONALLY(2, "Completed exceptionally"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String desc;

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    ExecuteStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;

    }
}
