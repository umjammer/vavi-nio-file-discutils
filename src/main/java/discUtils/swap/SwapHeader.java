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

package discUtils.swap;

import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class SwapHeader implements IByteArraySerializable {

    public static final String Magic1 = "SWAP-SPACE";

    public static final String Magic2 = "SWAPSPACE2";

    public static final int PageShift = 12;

    public static final int PageSize = 1 << PageShift;

    private int _version;

    public int getVersion() {
        return _version;
    }

    public void setVersion(int value) {
        _version = value;
    }

    private int _lastPage;

    public long getLastPage() {
        return _lastPage & 0xffffffffL;
    }

    public void setLastPage(int value) {
        _lastPage = value;
    }

    private int _badPages;

    public int getBadPages() {
        return _badPages;
    }

    public void setBadPages(int value) {
        _badPages = value;
    }

    private UUID _uuid;

    public UUID getUuid() {
        return _uuid;
    }

    public void setUuid(UUID value) {
        _uuid = value;
    }

    private String _volume;

    public String getVolume() {
        return _volume;
    }

    public void setVolume(String value) {
        _volume = value;
    }

    private String _magic;

    public String getMagic() {
        return _magic;
    }

    public void setMagic(String value) {
        _magic = value;
    }

    public int size() {
        return PageSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.bytesToString(buffer, PageSize - 10, 10));
        if (!getMagic().equals(Magic1) && !getMagic().equals(Magic2))
            return size();

        _version = EndianUtilities.toUInt32LittleEndian(buffer, 0x400);
        _lastPage = EndianUtilities.toUInt32LittleEndian(buffer, 0x404);
        _badPages = EndianUtilities.toUInt32LittleEndian(buffer, 0x408);
        _uuid = EndianUtilities.toGuidLittleEndian(buffer, 0x40c);
        byte[] volume = EndianUtilities.toByteArray(buffer, 0x41c, 16);
        OptionalInt nullIndex = IntStream.range(0, volume.length).filter(i -> volume[i] == (byte) 0).findFirst();
        if (nullIndex.isPresent())
            setVolume(new String(volume, 0, nullIndex.getAsInt(), StandardCharsets.UTF_8));

        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
