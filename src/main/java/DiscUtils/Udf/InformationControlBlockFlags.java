
package DiscUtils.Udf;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

enum InformationControlBlockFlags {
    _dummy_0001,
    _dummy_0002,
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

    // TODO
    public Supplier<Integer> supplier() {
        return () -> 1 << ordinal();
    }

    // TODO
    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };

    public static EnumSet<InformationControlBlockFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(InformationControlBlockFlags.class)));
    }
}
