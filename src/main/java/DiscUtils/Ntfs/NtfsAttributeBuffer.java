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

package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.Buffer;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Buffer.IMappedBuffer;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;


public class NtfsAttributeBuffer extends Buffer implements IMappedBuffer {
    private final NtfsAttribute _attribute;

    private final File _file;

    public NtfsAttributeBuffer(File file, NtfsAttribute attribute) {
        _file = file;
        _attribute = attribute;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return _file.getContext().getRawStream().canWrite();
    }

    public long getCapacity() {
        return _attribute.getPrimaryRecord().getDataLength();
    }

    public long mapPosition(long pos) {
        if (_attribute.getIsNonResident()) {
            return ((IMappedBuffer) _attribute.getRawBuffer()).mapPosition(pos);
        }

        AttributeReference attrRef = new AttributeReference(_file.getMftReference(),
                                                            _attribute.getPrimaryRecord().getAttributeId());
        ResidentAttributeRecord attrRecord = (ResidentAttributeRecord) _file.getAttribute(attrRef).getPrimaryRecord();
        long attrStart = _file.getAttributeOffset(attrRef);
        long mftPos = attrStart + attrRecord.getDataOffset() + pos;
        return _file.getContext()
                .getGetFileByIndex()
                .invoke(MasterFileTable.MftIndex)
                .getAttribute(AttributeType.Data, null)
                .offsetToAbsolutePos(mftPos);
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        AttributeRecord record = _attribute.getPrimaryRecord();
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
                        (int) (pos + toRead - record.getInitializedDataLength()),
                        (byte) 0);
            toRead = (int) (record.getInitializedDataLength() - pos);
        }

        int numRead = 0;
        while (numRead < toRead) {
            IBuffer extentBuffer = _attribute.getRawBuffer();
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

        _attribute.getRawBuffer().setCapacity(value);
        _file.markMftRecordDirty();
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        AttributeRecord record = _attribute.getPrimaryRecord();
        if (!canWrite()) {
            throw new IOException("Attempt to write to file not opened for write");
        }

        StreamUtilities.assertBufferParameters(buffer, offset, count);
        if (count == 0) {
            return;
        }

        _attribute.getRawBuffer().write(pos, buffer, offset, count);
        if (!record.isNonResident()) {
            _file.markMftRecordDirty();
        }

    }

    public void clear(long pos, int count) {
        AttributeRecord record = _attribute.getPrimaryRecord();
        if (!canWrite()) {
            throw new IOException("Attempt to write to file not opened for write");
        }

        if (count == 0) {
            return;
        }

        _attribute.getRawBuffer().clear(pos, count);
        if (!record.isNonResident()) {
            _file.markMftRecordDirty();
        }

    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return StreamExtent.intersect(_attribute.getRawBuffer().getExtentsInRange(start, count),
                                      new StreamExtent(0, getCapacity()));
    }

}
