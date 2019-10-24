
package DiscUtils.Iscsi;

public enum LogoutResponseCode {
    ClosedSuccessfully,
    ConnectionIdNotFound,
    RecoveryNotSupported,
    CleanupFailed;

    public static LogoutResponseCode valueOf(int value) {
        return values()[value];
    }
}
