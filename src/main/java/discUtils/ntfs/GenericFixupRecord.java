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

public final class GenericFixupRecord extends FixupRecordBase {
    private final int bytesPerSector;

    public GenericFixupRecord(int bytesPerSector) {
        super(null, bytesPerSector);
        this.bytesPerSector = bytesPerSector;
    }

    private byte[] content;

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] value) {
        content = value;
    }

    protected void read(byte[] buffer, int offset) {
        setContent(new byte[(getUpdateSequenceCount() - 1) * bytesPerSector]);
        System.arraycopy(buffer, offset, content, 0, content.length);
    }

    protected short write(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    protected int calcSize() {
        throw new UnsupportedOperationException();
    }
}
