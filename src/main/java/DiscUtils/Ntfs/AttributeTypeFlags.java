
package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

enum AttributeTypeFlags {
//    None,
    _dummy_01,
    Indexed,
    Multiple,
    NotZero,
    IndexedUnique,
    NamedUnique,
    MustBeResident,
    CanBeNonResident;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<AttributeTypeFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AttributeTypeFlags.class)));
    }

    public static long valueOf(EnumSet<AttributeTypeFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
