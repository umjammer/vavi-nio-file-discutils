
package DiscUtils.Core;

@FunctionalInterface
public interface TimeConverter {

    /**
     * Converts a time to/from UTC.
     *
     * @param time The time to convert.
     * @param toUtc
     *            {@code true}
     *            to convert FAT time to UTC,
     *            {@code false}
     *            to convert UTC to FAT time.
     * @return The converted time.
     */
    long invoke(long time, boolean toUtc);
}
