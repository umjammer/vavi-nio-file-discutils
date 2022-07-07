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

package discUtils.streams;

/**
 * Event arguments indicating progress on pumping a stream.
 */
public class PumpProgressEventArgs /* extends EventArgs */ {
    /**
     * Gets or sets the number of bytes read from
     * {@code InputStream}
     * .
     */
    private long __BytesRead;

    public long getBytesRead() {
        return __BytesRead;
    }

    public void setBytesRead(long value) {
        __BytesRead = value;
    }

    /**
     * Gets or sets the number of bytes written to
     * {@code OutputStream}
     * .
     */
    private long __BytesWritten;

    public long getBytesWritten() {
        return __BytesWritten;
    }

    public void setBytesWritten(long value) {
        __BytesWritten = value;
    }

    /**
     * Gets or sets the absolute position in
     * {@code OutputStream}
     * .
     */
    private long __DestinationPosition;

    public long getDestinationPosition() {
        return __DestinationPosition;
    }

    public void setDestinationPosition(long value) {
        __DestinationPosition = value;
    }

    /**
     * Gets or sets the absolute position in
     * {@code InputStream}
     * .
     */
    private long __SourcePosition;

    public long getSourcePosition() {
        return __SourcePosition;
    }

    public void setSourcePosition(long value) {
        __SourcePosition = value;
    }

}
