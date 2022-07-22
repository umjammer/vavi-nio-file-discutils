//
// Copyright (c) 2008-2013, Kenneth Bell
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

package discUtils.vhdx;

import java.util.UUID;

import discUtils.streams.util.EndianUtilities;


/**
 * Class representing a VHDX header.
 */
public final class HeaderInfo {

    private final VhdxHeader header;

    public HeaderInfo(VhdxHeader header) {
        this.header = header;
    }

    /**
     * Gets the checksum of the header information.
     */
    public int getChecksum() {
        return header.checksum;
    }

    /**
     * Gets a unique GUID indicating when the content of a VHDX file has
     * changed.
     */
    public UUID getDataWriteGuid() {
        return header.dataWriteGuid;
    }

    /**
     * Gets a unique GUID indicating when a VHDX file has been substantively
     * modified.
     */
    public UUID getFileWriteGuid() {
        return header.fileWriteGuid;
    }

    /**
     * Gets the GUID indicating which log records are valid.
     *
     * The NULL GUID indicates there are no log records to replay.
     */
    public UUID getLogGuid() {
        return header.logGuid;
    }

    /**
     * Gets the length of the VHDX log.
     */
    public long getLogLength() {
        return header.logLength;
    }

    /**
     * Gets the offset of the VHDX log within the file.
     */
    public long getLogOffset() {
        return header.logOffset;
    }

    /**
     * Gets the version of the log information, expected to be Zero.
     */
    public int getLogVersion() {
        return header.logVersion;
    }

    /**
     * Gets the sequence number of the header information.
     *
     * VHDX files contain two copies of the header, both contain a sequence
     * number, the highest
     * sequence number represents the current header information.
     */
    public long getSequenceNumber() {
        return header.sequenceNumber;
    }

    /**
     * Gets the signature of the header.
     */
    public String getSignature() {
        byte[] buffer = new byte[4];
        EndianUtilities.writeBytesLittleEndian(header.signature, buffer, 0);
        return EndianUtilities.bytesToString(buffer, 0, 4);
    }

    /**
     * Gets the VHDX file format version, expected to be One.
     */
    public int getVersion() {
        return header.version;
    }
}
