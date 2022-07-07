
package discUtils.wim;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

enum FileFlags {
    _dummy_00000001,
    Compression,
    ReadOnly,
    Spanned,
    ResourceOnly,
    MetaDataOnly,
    WriteInProgress,
    ReparsePointFix,
    _dummy_00000100,
    _dummy_00000200,
    _dummy_00000400,
    _dummy_00000800,
    _dummy_00001000,
    _dummy_00002000,
    _dummy_00004000,
    _dummy_00008000,
    _dummy_00010000,
    XpressCompression,
    LzxCompression;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<FileFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileFlags.class)));
    }
}
