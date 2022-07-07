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

package discUtils.iscsi;

/**
 * Exception thrown when a low-level iSCSI failure is detected.
 */
public class ScsiCommandException extends IscsiException {
    private byte[] _senseData;

    /**
     * Initializes a new instance of the ScsiCommandException class.
     */
    public ScsiCommandException() {
        _status = ScsiStatus.Good;
    }

    /**
     * Initializes a new instance of the ScsiCommandException class.
     *
     * @param status The SCSI status code.
     */
    public ScsiCommandException(ScsiStatus status) {
        _status = status;
    }

    /**
     * Initializes a new instance of the ScsiCommandException class.
     *
     * @param message The reason for the exception.
     */
    public ScsiCommandException(String message) {
        super(message);
        _status = ScsiStatus.Good;
    }

    /**
     * Initializes a new instance of the ScsiCommandException class.
     *
     * @param status The SCSI status code.
     * @param message The reason for the exception.
     */
    public ScsiCommandException(ScsiStatus status, String message) {
        super(message);
        _status = status;
    }

    /**
     * Initializes a new instance of the ScsiCommandException class.
     *
     * @param status The SCSI status code.
     * @param message The reason for the exception.
     * @param senseData The SCSI sense data.
     */
    public ScsiCommandException(ScsiStatus status, String message, byte[] senseData) {
        super(message);
        _status = status;
        _senseData = senseData;
    }

    /**
     * Initializes a new instance of the ScsiCommandException class.
     *
     * @param message The reason for the exception.
     * @param innerException The inner exception.
     */
    public ScsiCommandException(String message, Exception innerException) {
        super(message, innerException);
        _status = ScsiStatus.Good;
    }

    /**
     * Initializes a new instance of the ScsiCommandException class.
     *
     * @param status The SCSI status code.
     * @param message The reason for the exception.
     * @param innerException The inner exception.
     */
    public ScsiCommandException(ScsiStatus status, String message, Exception innerException) {
        super(message, innerException);
        _status = status;
    }

    /**
     * Initializes a new instance of the ScsiCommandException class.
     *
     * @param info The serialization info.
     * @param context Ther context.
     */
//    protected ScsiCommandException(SerializationInfo info, StreamingContext context) {
//        super(info, context);
//        _status = (ScsiStatus) info.getByte("status");
//        _senseData = (byte[]) info.getValue("senseData", byte[].class);
//    }

    /**
     * Gets the SCSI status associated with this exception.
     */
    private ScsiStatus _status = ScsiStatus.Good;

    public ScsiStatus getStatus() {
        return _status;
    }

    /**
     * Gets the serialized state of this exception.
     *
     * @param info The serialization info.
     * @param context The serialization context.
     */
//    public void getObjectData(SerializationInfo info, StreamingContext context) {
//        super.GetObjectData(info, context);
//        info.addValue("status", (byte) _status.getValue());
//        info.addValue("senseData", _senseData);
//    }

    /**
     * Gets the SCSI sense data (if any) associated with this exception.
     *
     * @return The SCSI sense data, or {@code null} .
     */
    public byte[] getSenseData() {
        return _senseData;
    }
}
