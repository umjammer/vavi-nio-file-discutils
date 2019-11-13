
package DiscUtils.Iscsi;

import java.util.Arrays;

/**
 * The known classes of SCSI device.
 */
public enum LunClass {
    /**
     * Device is block storage (i.e. normal disk).
     */
    BlockStorage(0x00),
    /**
     * Device is sequential access storage.
     */
    TapeStorage(0x01),
    /**
     * Device is a printer.
     */
    Printer(0x02),
    /**
     * Device is a SCSI processor.
     */
    Processor(0x03),
    /**
     * Device is write-once storage.
     */
    WriteOnceStorage(0x04),
    /**
     * Device is a CD/DVD drive.
     */
    OpticalDisc(0x05),
    /**
     * Device is a scanner (obsolete).
     */
    Scanner(0x06),
    /**
     * Device is optical memory (some optical discs).
     */
    OpticalMemory(0x07),
    /**
     * Device is a media changer device.
     */
    Jukebox(0x08),
    /**
     * Communications device (obsolete).
     */
    Communications(0x09),
    /**
     * Device is a Storage Array (e.g. RAID).
     */
    StorageArray(0x0C),
    /**
     * Device is Enclosure Services.
     */
    EnclosureServices(0x0D),
    /**
     * Device is a simplified block device.
     */
    SimplifiedDirectAccess(0x0E),
    /**
     * Device is an optical card reader/writer device.
     */
    OpticalCard(0x0F),
    /**
     * Device is a Bridge Controller.
     */
    BridgeController(0x10),
    /**
     * Device is an object-based storage device.
     */
    ObjectBasedStorage(0x11),
    /**
     * Device is an Automation/Drive interface.
     */
    AutomationDriveInterface(0x12),
    /**
     * Device is a Security Manager.
     */
    SecurityManager(0x13),
    /**
     * Device is a well-known device, as defined by SCSI specifications.
     */
    WellKnown(0x1E),
    /**
     * Unknown LUN class.
     */
    Unknown(0xFF);

    private int value;

    public int getValue() {
        return value;
    }

    private LunClass(int value) {
        this.value = value;
    }

    public static LunClass valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
