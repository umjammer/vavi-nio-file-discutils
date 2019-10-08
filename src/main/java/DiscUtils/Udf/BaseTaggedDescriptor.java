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

package DiscUtils.Udf;

import DiscUtils.Streams.IByteArraySerializable;


public abstract class BaseTaggedDescriptor implements IByteArraySerializable {
    public final TagIdentifier RequiredTagIdentifier;

    public DescriptorTag Tag;

    protected BaseTaggedDescriptor(TagIdentifier id) {
        RequiredTagIdentifier = id;
    }

    public long getSize() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        if (!DescriptorTag.isValid(buffer, offset)) {
            throw new IllegalStateException("Invalid Anchor Volume Descriptor Pointer (invalid tag)");
        }

        Tag = new DescriptorTag();
        Tag.readFrom(buffer, offset);
        if (UdfUtilities.computeCrc(buffer, offset + (int) Tag.getSize(), Tag.DescriptorCrcLength) != Tag.DescriptorCrc) {
            throw new IllegalStateException("Invalid Anchor Volume Descriptor Pointer (invalid CRC)");
        }

        return parse(buffer, offset);
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public abstract int parse(byte[] buffer, int offset);
}
