
package discUtils.nfs;

import java.util.Arrays;

/**
 * NFS status codes.
 */
public enum Nfs3Status {
    /**
     * Indicates the call completed successfully.
     */
    Ok(0),
    /**
     * The operation was not allowed because the caller is either not a privileged
     * user (root) or not the owner of the target of the operation.
     */
    NotOwner(1),
    /**
     * The file or directory name specified does not exist.
     */
    NoSuchEntity(2),
    /**
     * A hard error (for example, a disk error) occurred while processing the
     * requested operation.
     */
    IOError(5),
    /**
     * No such device or address.
     */
    NoSuchDeviceOrAddress(6),
    /**
     * The caller does not have the correct permission to perform the requested
     * operation. Contrast this with NotOwner, which restricts itself to owner or
     * privileged user permission failures.
     */
    AccessDenied(13),
    /**
     * The file specified already exists.
     */
    FileExists(17),
    /**
     * Attempt to do a cross-device hard link.
     */
    AttemptedCrossDeviceHardLink(18),
    /**
     * No such device.
     */
    NoSuchDevice(19),
    /**
     * The caller specified a non-directory in a directory operation.
     */
    NotDirectory(20),
    /**
     * The caller specified a directory in a non-directory operation.
     */
    IsADirectory(21),
    /**
     * Invalid argument or unsupported argument for an operation.
     */
    InvalidArgument(22),
    /**
     * The operation would have caused a file to grow beyond the server's limit.
     */
    FileTooLarge(27),
    /**
     * The operation would have caused the server's file system to exceed its limit.
     */
    NoSpaceAvailable(28),
    /**
     * A modifying operation was attempted on a read-only file system.
     */
    ReadOnlyFileSystem(30),
    /**
     * Too many hard links.
     */
    TooManyHardLinks(31),
    /**
     * The filename in an operation was too long.
     */
    NameTooLong(63),
    /**
     * An attempt was made to remove a directory that was not empty.
     */
    DirectoryNotEmpty(66),
    /**
     * The user's resource limit on the server has been exceeded.
     */
    QuotaHardLimitExceeded(69),
    /**
     * The file referred to no longer exists or access to it has been revoked.
     */
    StaleFileHandle(70),
    /**
     * The file handle given in the arguments referred to a file on a non-local file
     * system on the server.
     */
    TooManyRemoteAccessLevels(71),
    /**
     * The file handle failed internal consistency checks.
     */
    BadFileHandle(10001),
    /**
     * Update synchronization mismatch was detected during a SETATTR operation.
     */
    UpdateSynchronizationError(10002),
    /**
     * Directory enumeration cookie is stale.
     */
    StaleCookie(10003),
    /**
     * Operation is not supported.
     */
    NotSupported(10004),
    /**
     * buffer or request is too small.
     */
    TooSmall(10005),
    /**
     * An error occurred on the server which does not map to any of the legal NFS
     * version 3 protocol error values.
     */
    ServerFault(10006),
    /**
     * An attempt was made to create an object of a type not supported by the
     * server.
     */
    BadType(10007),
    /**
     * The server initiated the request, but was not able to complete it in a timely
     * fashion.
     *
     * The client should wait and then try the request with a new RPC transaction
     * ID. For example, this error should be returned from a server that supports
     * hierarchical storage and receives a request to process a file that has been
     * migrated. In this case, the server should start the immigration process and
     * respond to client with this error.
     */
    SlowJukebox(10008),
    /**
     * An unknown error occured.
     */
    Unknown(-1);

    private final int value;

    public int getValue() {
        return value;
    }

    Nfs3Status(int value) {
        this.value = value;
    }

    public static Nfs3Status valueOf(int value) {
        return Arrays.stream(values()).filter(e -> e.getValue() == value).findFirst().orElseThrow();
    }
}
