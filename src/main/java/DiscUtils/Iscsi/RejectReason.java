
package DiscUtils.Iscsi;

import java.util.Arrays;

public enum RejectReason {
    None,
    Reserved,
    DataDigestError,
    SNACKReject,
    ProtocolError,
    CommandNotSupported,
    ImmediateCommandReject,
    TaskInProgress,
    InvalidDataAck,
    InvalidPduField,
    LongOperationReject,
    NegotiationReset,
    WaitingForLogout;

    public static RejectReason valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
