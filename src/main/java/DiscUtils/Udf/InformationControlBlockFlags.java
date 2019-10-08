
package DiscUtils.Udf;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum InformationControlBlockFlags {
    DirectorySorted,
    NonRelocatable,
    Archive,
    SetUid,
    SetGid,
    Sticky,
    Contiguous,
    System,
    Transformed,
    MultiVersions,
    Stream;

    public static EnumSet<InformationControlBlockFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(InformationControlBlockFlags.class)));
    }

    public static long valueOf(EnumSet<InformationControlBlockFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
