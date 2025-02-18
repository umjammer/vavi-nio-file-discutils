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
     * @param message The exception message. 
     */
    public Nfs3Exception(String message) {
        super(message);
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * @param message The exception message. 
     * @param status The status result of an NFS procedure. 
     */
    public Nfs3Exception(String message, Nfs3Status status) {
        super(message);
        nfsStatus = status;
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * @param message The exception message. 
     * @param innerException The inner exception. 
     */
    public Nfs3Exception(String message, Exception innerException) {
        super(message, innerException);
    }

    /**
     * Initializes a new instance of the Nfs3Exception class.
     * @param status The status result of an NFS procedure. 
     */
    Nfs3Exception(Nfs3Status status) {
        this(generateMessage(status));
        nfsStatus = status;
    }

    /**
     * Gets the NFS status code that lead to the exception.
     */
    public Nfs3Status getNfs3Status() {
        return nfsStatus;
    }

    private Nfs3Status nfsStatus = Nfs3Status.Unknown;

    private static String generateMessage(Nfs3Status status) {
        return switch (status) {
            case Ok -> "OK";
            case NotOwner -> "Not owner";
            case NoSuchEntity -> "No such file or directory";
            case IOError -> "Hardware I/O error";
            case NoSuchDeviceOrAddress -> "I/O error - no such device or address";
            case AccessDenied -> "Permission denied";
            case FileExists -> "File exists";
            case AttemptedCrossDeviceHardLink -> "Attempted cross-device hard link";
            case NoSuchDevice -> "No such device";
            case NotDirectory -> "Not a directory";
            case IsADirectory -> "Is a directory";
            case InvalidArgument -> "Invalid or unsupported argument";
            case FileTooLarge -> "File too large";
            case NoSpaceAvailable -> "No space left on device";
            case ReadOnlyFileSystem -> "Read-only file system";
            case TooManyHardLinks -> "Too many hard links";
            case NameTooLong -> "Name too long";
            case DirectoryNotEmpty -> "Directory not empty";
            case QuotaHardLimitExceeded -> "Quota hard limit exceeded";
            case StaleFileHandle -> "Invalid (stale) file handle";
            case TooManyRemoteAccessLevels -> "Too many levels of remote access";
            case BadFileHandle -> "Illegal NFS file handle";
            case UpdateSynchronizationError -> "Update synchronization error";
            case StaleCookie -> "Read directory cookie stale";
            case NotSupported -> "Operation is not supported";
            case TooSmall -> "buffer or request is too small";
            case ServerFault -> "Server fault";
            case BadType -> "Server doesn't support object type";
            case SlowJukebox -> "Unable to complete in timely fashion";
            default -> "Unknown error: " + status;
        };
    }
}
