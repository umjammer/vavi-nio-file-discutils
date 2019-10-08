//
// Copyright (c) 2017, Bianco Veigel
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

package DiscUtils.Swap;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class SwapHeader implements IByteArraySerializable {

    public static final String Magic1 = "SWAP-SPACE";

    public static final String Magic2 = "SWAPSPACE2";

    public static final int PageShift = 12;

    public static final int PageSize = 1 << PageShift;

    private int __Version;

    public int getVersion() {
        return __Version;
    }

    public void setVersion(int value) {
        __Version = value;
    }

    private int __LastPage;

    public int getLastPage() {
        return __LastPage;
    }

    public void setLastPage(int value) {
        __LastPage = value;
    }

    private int __BadPages;

    public int getBadPages() {
        return __BadPages;
    }

    public void setBadPages(int value) {
        __BadPages = value;
    }

    private UUID __Uuid;

    public UUID getUuid() {
        return __Uuid;
    }

    public void setUuid(UUID value) {
        __Uuid = value;
    }

    private String __Volume;

    public String getVolume() {
        return __Volume;
    }

    public void setVolume(String value) {
        __Volume = value;
    }

    private String __Magic;

    public String getMagic() {
        return __Magic;
    }

    public void setMagic(String value) {
        __Magic = value;
    }

    public long getSize() {
        return PageSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.bytesToString(buffer, PageSize - 10, 10));
        if (!getMagic().equals(Magic1) && !getMagic().equals(Magic2))
            return (int) getSize();

        setVersion(EndianUtilities.toUInt32LittleEndian(buffer, 0x400));
        setLastPage(EndianUtilities.toUInt32LittleEndian(buffer, 0x404));
        setBadPages(EndianUtilities.toUInt32LittleEndian(buffer, 0x408));
        setUuid(EndianUtilities.toGuidLittleEndian(buffer, 0x40c));
        byte[] volume = EndianUtilities.toByteArray(buffer, 0x41c, 16);
        int nullIndex = Arrays.binarySearch(volume, (byte) 0);
        if (nullIndex > 0)
            setVolume(new String(volume, 0, nullIndex, Charset.forName("UTF8")));

        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
