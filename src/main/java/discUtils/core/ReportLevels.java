
package discUtils.core;

import java.util.EnumSet;

public enum ReportLevels {
    /**
     * flags for the amount of detail to include in a report.
     *
     * Report no information.
     */
    None,
    /**
     * Report informational level items.
     */
    Information,
    /**
     * Report warning level items.
     */
    Warnings,
    __dummyEnum__0,
    /**
     * Report error level items.
     */
    Errors;
    /**
     * Report all items.
     */
    public static final EnumSet<ReportLevels> All = EnumSet.of(Information, Warnings, Errors);
}
