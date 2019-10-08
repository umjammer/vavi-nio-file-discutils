
package DiscUtils.Nfs;

import java.util.Arrays;

public enum Nfs3Status {
    /**
     * NFS status codes.
     *
     * Indicates the call completed successfully.
     */
    Ok,
    /**
     * The operation was not allowed because the caller is either not a
     * privileged user (root) or not the owner of the target of the operation.
     */
    NotOwner,
    /**
     * The file or directory name specified does not exist.
     */
    NoSuchEntity,
    /**
     * A hard error (for example, a disk error) occurred while processing
     * the requested operation.
     */
    IOError,
    /**
     * No such device or address.
     */
    NoSuchDeviceOrAddress,
    /**
     * The caller does not have the correct permission to perform the requested
     * operation. Contrast this with NotOwner, which restricts itself to owner
     * or privileged user permission failures.
     */
    AccessDenied,
    /**
     * The file specified already exists.
     */
    FileExists,
    /**
     * Attempt to do a cross-device hard link.
     */
    AttemptedCrossDeviceHardLink,
    /**
     * No such device.
     */
    NoSuchDevice,
    /**
     * The caller specified a non-directory in a directory operation.
     */
    NotDirectory,
    /**
     * The caller specified a directory in a non-directory operation.
     */
    IsADirectory,
    /**
     * Invalid argument or unsupported argument for an operation.
     */
    InvalidArgument,
    /**
     * The operation would have caused a file to grow beyond the server's
     * limit.
     */
    FileTooLarge,
    /**
     * The operation would have caused the server's file system to exceed its
     * limit.
     */
    NoSpaceAvailable,
    /**
     * A modifying operation was attempted on a read-only file system.
     */
    ReadOnlyFileSystem,
    /**
     * Too many hard links.
     */
    TooManyHardLinks,
    /**
     * The filename in an operation was too long.
     */
    NameTooLong,
    /**
     * An attempt was made to remove a directory that was not empty.
     */
    DirectoryNotEmpty,
    /**
     * The user's resource limit on the server has been exceeded.
     */
    QuotaHardLimitExceeded,
    /**
     * The file referred to no longer exists or access to it has been revoked.
     */
    StaleFileHandle,
    /**
     * The file handle given in the arguments referred to a file on a non-local
     * file system on the server.
     */
    TooManyRemoteAccessLevels,
    /**
     * The file handle failed internal consistency checks.
     */
    BadFileHandle,
    /**
     * Update synchronization mismatch was detected during a SETATTR operation.
     */
    UpdateSynchronizationError,
    /**
     * Directory enumeration cookie is stale.
     */
    StaleCookie,
    /**
     * Operation is not supported.
     */
    NotSupported,
    /**
     * Buffer or request is too small.
     */
    TooSmall,
    /**
     * An error occurred on the server which does not map to any of the legal
     * NFS
     * version 3 protocol error values.
     */
    ServerFault,
    /**
     * An attempt was made to create an object of a type not supported by the
     * server.
     */
    BadType,
    /**
     * The server initiated the request, but was not able to complete it in a
     * timely fashion.
     *
     * The client should wait and then try the request with a new RPC
     * transaction ID.
     * For example, this error should be returned from a server that supports
     * hierarchical storage and receives a request to process a file that has
     * been
     * migrated. In this case, the server should start the immigration process
     * and
     * respond to client with this error.
     */
    SlowJukebox,
    /**
     * An unknown error occured.
     */
    Unknown;

    public static Nfs3Status valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
