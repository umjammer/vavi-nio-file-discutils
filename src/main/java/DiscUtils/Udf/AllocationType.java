
package DiscUtils.Udf;

public enum AllocationType {
    ShortDescriptors,
    LongDescriptors,
    ExtendedDescriptors,
    Embedded;

    public static AllocationType valueOf(int value) {
        return values()[value];
    }
}
