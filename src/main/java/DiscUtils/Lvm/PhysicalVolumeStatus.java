
package DiscUtils.Lvm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum PhysicalVolumeStatus {
    None,
    Read,
    __dummyEnum__0,
    __dummyEnum__1,
    Write,
    __dummyEnum__2,
    __dummyEnum__3,
    __dummyEnum__4,
    Allocatable;

    public static EnumSet<PhysicalVolumeStatus> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PhysicalVolumeStatus.class)));
    }

    public static long valueOf(EnumSet<PhysicalVolumeStatus> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
