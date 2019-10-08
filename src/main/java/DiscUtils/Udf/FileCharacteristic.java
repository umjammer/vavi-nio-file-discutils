
package DiscUtils.Udf;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum FileCharacteristic {
    __dummyEnum__0,
    Existence,
    Directory,
    __dummyEnum__1,
    Deleted,
    __dummyEnum__2,
    __dummyEnum__3,
    __dummyEnum__4,
    Parent,
    __dummyEnum__5,
    __dummyEnum__6,
    __dummyEnum__7,
    __dummyEnum__8,
    __dummyEnum__9,
    __dummyEnum__10,
    __dummyEnum__11,
    Metadata;

    public static EnumSet<FileCharacteristic> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileCharacteristic.class)));
    }

    public static long valueOf(EnumSet<FileCharacteristic> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
