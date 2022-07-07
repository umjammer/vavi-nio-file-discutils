
package discUtils.core;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Enumeration of possible cluster roles.
 * A cluster may be in more than one role.
 */
public enum ClusterRoles {
    /**
     * Unknown, or unspecified role.
     */
//    None,
    /**
     * Cluster is free.
     */
    Free,
    /**
     * Cluster is in use by a normal file.
     */
    DataFile,
    /**
     * Cluster is in use by a system file.
     * This isn't a file marked with the 'system' attribute,
     * rather files that form part of the file system namespace but also
     * form part of the file system meta-data.
     */
    SystemFile,
    /**
     * Cluster is in use for meta-data.
     */
    Metadata,
    /**
     * Cluster contains the boot region.
     */
    BootArea,
    /**
     * Cluster is marked bad.
     */
    Bad;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<ClusterRoles> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClusterRoles.class)));
    }

    public static long valueOf(EnumSet<ClusterRoles> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
