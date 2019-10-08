
package DiscUtils.Lvm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum LogicalVolumeStatus {
    None,
    Read,
    Write,
    __dummyEnum__0,
    Visible;

    public static EnumSet<LogicalVolumeStatus> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(LogicalVolumeStatus.class)));
    }

    public static long valueOf(EnumSet<LogicalVolumeStatus> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
