
package DiscUtils.Iscsi;

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
        return values()[value];
    }
}
