
package DiscUtils.Lvm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum VolumeGroupStatus {
    None,
    Read,
    Write,
    __dummyEnum__0,
    Resizeable;

    public static EnumSet<VolumeGroupStatus> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(VolumeGroupStatus.class)));
    }

    public static long valueOf(EnumSet<VolumeGroupStatus> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
