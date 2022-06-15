
package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public enum AttributeFlags {
//    None,
    Compressed,
    _dummy_0002,
    _dummy_0004,
    _dummy_0008,
    _dummy_0010,
    _dummy_0020,
    _dummy_0040,
    _dummy_0080,
    _dummy_0100,
    _dummy_0200,
    _dummy_0400,
    _dummy_0800,
    _dummy_1000,
    _dummy_2000,
    Encrypted,
    Sparse;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<AttributeFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AttributeFlags.class)));
    }

    public static long valueOf(EnumSet<AttributeFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
