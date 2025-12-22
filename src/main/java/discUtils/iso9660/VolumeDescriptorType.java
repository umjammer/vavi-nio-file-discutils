
package discUtils.iso9660;

import java.util.Arrays;

enum VolumeDescriptorType {
    Boot(0),
    Primary(1),
    Supplementary(2),
    Partition(3),
    SetTerminator(255);

    private final int value;

    public int getValue() {
        return value;
    }

    VolumeDescriptorType(int value) {
        this.value = value;
    }

    public static VolumeDescriptorType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().orElseThrow();
    }
}
