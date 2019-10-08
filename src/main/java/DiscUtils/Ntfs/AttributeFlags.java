
package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum AttributeFlags {
    /**
     * Flags indicating how an attribute's content is stored on disk.
     *
     * The data is stored in linear form.
     */
    None,
    /**
     * The data is compressed.
     */
    Compressed,
    /**
     * The data is encrypted.
     */
    Encrypted,
    /**
     * The data is stored in sparse form.
     */
    Sparse;

    public static EnumSet<AttributeFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AttributeFlags.class)));
    }

    public static long valueOf(EnumSet<AttributeFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
