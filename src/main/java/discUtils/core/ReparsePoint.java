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

package discUtils.core;

/**
 * Represents a Reparse Point, which can be associated with a file or directory.
 */
public final class ReparsePoint {

    /**
     * Initializes a new instance of the ReparsePoint class.
     *
     * @param tag The defined reparse point tag.
     * @param content The reparse point's content.
     */
    public ReparsePoint(int tag, byte[] content) {
        setTag(tag);
        setContent(content);
    }

    /**
     * Gets or sets the reparse point's content.
     */
    private byte[] content;

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] value) {
        content = value;
    }

    /**
     * Gets or sets the defined reparse point tag.
     */
    private int tag;

    public int getTag() {
        return tag;
    }

    public void setTag(int value) {
        tag = value;
    }
}
