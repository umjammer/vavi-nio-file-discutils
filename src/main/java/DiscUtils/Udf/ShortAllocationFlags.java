
package DiscUtils.Udf;

import java.util.Arrays;

public enum ShortAllocationFlags {
    RecordedAndAllocated,
    AllocatedNotRecorded,
    NotRecordedNotAllocated,
    NextExtentOfAllocationDescriptors;

    public static ShortAllocationFlags valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
