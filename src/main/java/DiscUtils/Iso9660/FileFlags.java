
package DiscUtils.Iso9660;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


enum FileFlags {
//    None,
    Hidden,
    Directory,
    AssociatedFile,
    Record,
    Protection,
    __dummyEnum__20,
    __dummyEnum__40,
    MultiExtent;

    // TODO
    public Supplier<Integer> supplier() {
        return () -> 1 << ordinal();
    }

    // TODO
    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };

    public static EnumSet<FileFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileFlags.class)));
    }

    public static long valueOf(EnumSet<FileFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
