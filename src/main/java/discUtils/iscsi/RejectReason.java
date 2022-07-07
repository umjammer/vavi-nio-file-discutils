
package discUtils.iscsi;

enum RejectReason {
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
    WaitingForLogout
}
