
package DiscUtils.Udf;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

enum FileCharacteristic {
    Existence,
    Directory,
    Deleted,
    Parent,
    Metadata;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<FileCharacteristic> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileCharacteristic.class)));
    }

    public static long valueOf(EnumSet<FileCharacteristic> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
