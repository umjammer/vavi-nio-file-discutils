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

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class ProtocolDataUnit {

    public ProtocolDataUnit(byte[] headerData, byte[] contentData) {
        this.headerData = headerData;
        this.contentData = contentData;
    }

    private byte[] contentData;

    public byte[] getContentData() {
        return contentData;
    }

    private byte[] headerData;

    public byte[] getHeaderData() {
        return headerData;
    }

    public OpCode getOpCode() {
        return OpCode.valueOf(getHeaderData()[0] & 0x3F);
    }

    public static ProtocolDataUnit readFrom(Stream stream, boolean headerDigestEnabled, boolean dataDigestEnabled) {
        int numRead = 0;
        byte[] headerData = StreamUtilities.readExact(stream, 48);
        numRead += 48;
        byte[] contentData = null;
        if (headerDigestEnabled) {
            @SuppressWarnings("unused")
            int digest = readDigest(stream);
            numRead += 4;
        }

        BasicHeaderSegment bhs = new BasicHeaderSegment();
        bhs.readFrom(headerData, 0);
        if (bhs.dataSegmentLength > 0) {
            contentData = StreamUtilities.readExact(stream, bhs.dataSegmentLength);
            numRead += bhs.dataSegmentLength;
            if (dataDigestEnabled) {
                @SuppressWarnings("unused")
                int digest = readDigest(stream);
                numRead += 4;
            }
        }

        int rem = 4 - numRead % 4;
        if (rem != 4) {
            StreamUtilities.readExact(stream, rem);
        }

        return new ProtocolDataUnit(headerData, contentData);
    }

    private static int readDigest(Stream stream) {
        byte[] data = StreamUtilities.readExact(stream, 4);
        return EndianUtilities.toUInt32BigEndian(data, 0);
    }

}
