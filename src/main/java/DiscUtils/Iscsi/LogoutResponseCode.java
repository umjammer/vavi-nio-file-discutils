
package DiscUtils.Iscsi;

import java.util.Arrays;

public enum LogoutResponseCode {
    ClosedSuccessfully,
    ConnectionIdNotFound,
    RecoveryNotSupported,
    CleanupFailed;

    public static LogoutResponseCode valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
