//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.nfs;

import dotnet4j.io.IOException;


/**
 * Exception thrown when some invalid file system data is found, indicating
 * probably corruption.
 */
public class Nfs3Exception extends IOException {
    /**
     * Initializes a new instance of the Nfs3Exception class.
     */
    public Nfs3Exception() {
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * <param name="message">The exception message.</param>
     */
    public Nfs3Exception(String message) {
        super(message);
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * <param name="message">The exception message.</param>
     * <param name="status">The status result of an NFS procedure.</param>
     */
    public Nfs3Exception(String message, Nfs3Status status) {
        super(message);
        NfsStatus = status;
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * <param name="message">The exception message.</param>
     * <param name="innerException">The inner exception.</param>
     */
    public Nfs3Exception(String message, Exception innerException) {
        super(message, innerException);
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * <param name="status">The status result of an NFS procedure.</param>
     */
    Nfs3Exception(Nfs3Status status) {
        this(generateMessage(status));
        NfsStatus = status;
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * <param name="info">The serialization info.</param>
     * <param name="context">The streaming context.</param>
     */
//    private Nfs3Exception(SerializationInfo info, StreamingContext context) {
//        this(info, context);
//        NfsStatus = (Nfs3Status) info.GetInt32("Status");
//    }

    /**
     * Gets the NFS status code that lead to the exception.
     */
    public Nfs3Status getNfs3Status() {
        return NfsStatus;
    }

    private Nfs3Status NfsStatus = Nfs3Status.Unknown;

    /**
     * Serializes this exception.
     * <param name="info">The object to populate with serialized data.</param>
     * <param name="context">The context for this serialization.</param>
     * [SecurityPermission(SecurityAction.LinkDemand, Flags =
     * SecurityPermissionFlag.SerializationFormatter)]
     */
//    public void getObjectData(SerializationInfo info, StreamingContext context) {
//        info.AadValue("Status", (int) NfsStatus.ordinal());
//        super.getObjectData(info, context);
//    }

    private static String generateMessage(Nfs3Status status) {
        switch (status) {
        case Ok:
            return "OK";
        case NotOwner:
            return "Not owner";
        case NoSuchEntity:
            return "No such file or directory";
        case IOError:
            return "Hardware I/O error";
        case NoSuchDeviceOrAddress:
            return "I/O error - no such device or address";
        case AccessDenied:
            return "Permission denied";
        case FileExists:
            return "File exists";
        case AttemptedCrossDeviceHardLink:
            return "Attempted cross-device hard link";
        case NoSuchDevice:
            return "No such device";
        case NotDirectory:
            return "Not a directory";
        case IsADirectory:
            return "Is a directory";
        case InvalidArgument:
            return "Invalid or unsupported argument";
        case FileTooLarge:
            return "File too large";
        case NoSpaceAvailable:
            return "No space left on device";
        case ReadOnlyFileSystem:
            return "Read-only file system";
        case TooManyHardLinks:
            return "Too many hard links";
        case NameTooLong:
            return "Name too long";
        case DirectoryNotEmpty:
            return "Directory not empty";
        case QuotaHardLimitExceeded:
            return "Quota hard limit exceeded";
        case StaleFileHandle:
            return "Invalid (stale) file handle";
        case TooManyRemoteAccessLevels:
            return "Too many levels of remote access";
        case BadFileHandle:
            return "Illegal NFS file handle";
        case UpdateSynchronizationError:
            return "Update synchronization error";
        case StaleCookie:
            return "Read directory cookie stale";
        case NotSupported:
            return "Operation is not supported";
        case TooSmall:
            return "buffer or request is too small";
        case ServerFault:
            return "Server fault";
        case BadType:
            return "Server doesn't support object type";
        case SlowJukebox:
            return "Unable to complete in timely fashion";
        default:
            return "Unknown error: " + status;
        }
    }
}
