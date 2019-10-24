
package DiscUtils.Udf;

public enum OSIdentifier {
    DosOrWindows3,
    Os2,
    MacintoshOs9,
    MacintoshOsX,
    UnixGeneric,
    UnixAix,
    UnixSunOS,
    UnixHPUX,
    UnixIrix,
    UnixLinux,
    UnixMkLinux,
    UnixFreeBsd,
    UnixNetBsd,
    Windows9x,
    WindowsNt,
    Os400,
    BeOS,
    WindowsCe;

    public static OSIdentifier valueOf(int value) {
        return values()[value];
    }
}
