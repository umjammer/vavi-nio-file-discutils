
package DiscUtils.Udf;

// use ordinal()
public enum ShortAllocationFlags {
    RecordedAndAllocated,
    AllocatedNotRecorded,
    NotRecordedNotAllocated,
    NextExtentOfAllocationDescriptors;

    public static ShortAllocationFlags valueOf(int value) {
        return values()[value];
    }
}
