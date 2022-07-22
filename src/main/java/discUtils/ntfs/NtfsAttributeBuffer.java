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

package discUtils.ntfs;

import java.util.Arrays;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.Buffer;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.buffer.IMappedBuffer;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;


public class NtfsAttributeBuffer extends Buffer implements IMappedBuffer {

    private final NtfsAttribute attribute;

    private final File file;

    public NtfsAttributeBuffer(File file, NtfsAttribute attribute) {
        this.file = file;
        this.attribute = attribute;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return file.getContext().getRawStream().canWrite();
    }

    public long getCapacity() {
        return attribute.getPrimaryRecord().getDataLength();
    }

    public long mapPosition(long pos) {
        if (attribute.isNonResident()) {
            return ((IMappedBuffer) attribute.getRawBuffer()).mapPosition(pos);
        }

        AttributeReference attrRef = new AttributeReference(file.getMftReference(),
                                                            attribute.getPrimaryRecord().getAttributeId());
        ResidentAttributeRecord attrRecord = (ResidentAttributeRecord) file.getAttribute(attrRef).getPrimaryRecord();
        long attrStart = file.getAttributeOffset(attrRef);
        long mftPos = attrStart + attrRecord.getDataOffset() + pos;
        return file.getContext()
                .getGetFileByIndex()
                .invoke(MasterFileTable.MftIndex)
                .getAttribute(AttributeType.Data, null)
                .offsetToAbsolutePos(mftPos);
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        AttributeRecord record = attribute.getPrimaryRecord();
        if (!canRead()) {
            throw new IOException("Attempt to read from file not opened for read");
        }

        StreamUtilities.assertBufferParameters(buffer, offset, count);
        if (pos >= getCapacity()) {
            return 0;
        }

        // Limit read to length of attribute
        int totalToRead = (int) Math.min(count, getCapacity() - pos);
        int toRead = totalToRead;

        // Handle uninitialized bytes at end of attribute
        if (pos + totalToRead > record.getInitializedDataLength()) {
            if (pos >= record.getInitializedDataLength()) {
                // We're just reading zero bytes from the uninitialized area
                Arrays.fill(buffer, offset, offset + totalToRead, (byte) 0);
                pos += totalToRead;
                return totalToRead;
            }

            // Partial read of uninitialized area
            Arrays.fill(buffer,
                        offset + (int) (record.getInitializedDataLength() - pos),
                        offset + toRead,
                        (byte) 0);
            toRead = (int) (record.getInitializedDataLength() - pos);
        }

        int numRead = 0;
        while (numRead < toRead) {
            IBuffer extentBuffer = attribute.getRawBuffer();

            int justRead = extentBuffer.read(pos + numRead, buffer, offset + numRead, toRead - numRead);
            if (justRead == 0) {
                break;
            }

            numRead += justRead;
        }

        return totalToRead;
    }

    public void setCapacity(long value) {
        if (!canWrite()) {
            throw new IOException("Attempt to change length of file not opened for write");
        }

        if (value == getCapacity()) {
            return;
        }

        attribute.getRawBuffer().setCapacity(value);
        file.markMftRecordDirty();
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        AttributeRecord record = attribute.getPrimaryRecord();

        if (!canWrite()) {
            throw new IOException("Attempt to write to file not opened for write");
        }

        StreamUtilities.assertBufferParameters(buffer, offset, count);

        if (count == 0) {
            return;
        }

        attribute.getRawBuffer().write(pos, buffer, offset, count);

        if (!record.isNonResident()) {
            file.markMftRecordDirty();
        }
    }

    public void clear(long pos, int count) {
        AttributeRecord record = attribute.getPrimaryRecord();

        if (!canWrite()) {
            throw new IOException("Attempt to write to file not opened for write");
        }

        if (count == 0) {
            return;
        }

        attribute.getRawBuffer().clear(pos, count);

        if (!record.isNonResident()) {
            file.markMftRecordDirty();
        }
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
//Debug.println(count + ", " + attribute.getRawBuffer().getExtentsInRange(start, count) + ", " + new StreamExtent(0, getCapacity()));
        return StreamExtent.intersect(attribute.getRawBuffer().getExtentsInRange(start, count),
                                      new StreamExtent(0, getCapacity()));
    }
}
