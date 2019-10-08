
package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum IndexEntryFlags {
    None,
    Node,
    End;

    public static EnumSet<IndexEntryFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(IndexEntryFlags.class)));
    }

    public static long valueOf(EnumSet<IndexEntryFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
