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

package discUtils.udf;

import java.util.EnumSet;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class InformationControlBlock implements IByteArraySerializable {

    public AllocationType allocationType = AllocationType.ShortDescriptors;

    public FileType fileType = FileType.None;

    public EnumSet<InformationControlBlockFlags> flags;

    public short maxEntries;

    public LogicalBlockAddress parentICBLocation;

    public int priorDirectEntries;

    public short strategyParameter;

    public short strategyType;

    public int size() {
        return 20;
    }

    public int readFrom(byte[] buffer, int offset) {
        priorDirectEntries = EndianUtilities.toUInt32LittleEndian(buffer, offset);
        strategyType = EndianUtilities.toUInt16LittleEndian(buffer, offset + 4);
        strategyParameter = EndianUtilities.toUInt16LittleEndian(buffer, offset + 6);
        maxEntries = EndianUtilities.toUInt16LittleEndian(buffer, offset + 8);
        fileType = FileType.valueOf(buffer[offset + 11]);
        parentICBLocation = EndianUtilities.toStruct(LogicalBlockAddress.class, buffer, offset + 12);
        short flagsField = EndianUtilities.toUInt16LittleEndian(buffer, offset + 18);
        allocationType = AllocationType.values()[flagsField & 0x3];
        flags = InformationControlBlockFlags.valueOf(flagsField & 0xFFFC);
        return 20;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
