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

import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public final class ObjectIdRecord implements IByteArraySerializable {

    public UUID birthDomainId;

    public UUID birthObjectId;

    public UUID birthVolumeId;

    public FileRecordReference mftReference;

    public int size() {
        return 0x38;
    }

    public int readFrom(byte[] buffer, int offset) {
        mftReference = new FileRecordReference();
        mftReference.readFrom(buffer, offset);
        birthVolumeId = ByteUtil.readLeUUID(buffer, offset + 0x08);
        birthObjectId = ByteUtil.readLeUUID(buffer, offset + 0x18);
        birthDomainId = ByteUtil.readLeUUID(buffer, offset + 0x28);
        return 0x38;
    }

    public void writeTo(byte[] buffer, int offset) {
        mftReference.writeTo(buffer, offset);
        ByteUtil.writeLeUUID(birthVolumeId, buffer, offset + 0x08);
        ByteUtil.writeLeUUID(birthObjectId, buffer, offset + 0x18);
        ByteUtil.writeLeUUID(birthDomainId, buffer, offset + 0x28);
    }

    public String toString() {
        return String.format("[Data-MftRef:%s,BirthVolId:%s,BirthObjId:%s,BirthDomId:%s]",
                mftReference,
                birthVolumeId,
                birthObjectId,
                birthDomainId);
    }
}
