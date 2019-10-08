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

package DiscUtils.Sdi;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Class for accessing the contents of Simple Deployment Image (.sdi) files.
 * SDI files are primitive disk images, containing multiple blobs.
 */
public final class SdiFile implements Closeable {
    private final FileHeader _header;

    private final Ownership _ownership;

    private final List<SectionRecord> _sections;

    private Stream _stream;

    /**
     * Initializes a new instance of the SdiFile class.
     *
     * @param stream The stream formatted as an SDI file.
     */
    public SdiFile(Stream stream) {
        this(stream, Ownership.None);
    }

    /**
     * Initializes a new instance of the SdiFile class.
     *
     * @param stream The stream formatted as an SDI file.
     * @param ownership Whether to pass ownership of
     *            {@code stream}
     *            to the new instance.
     */
    public SdiFile(Stream stream, Ownership ownership) {
        _stream = stream;
        _ownership = ownership;
        byte[] page = StreamUtilities.readExact(_stream, 512);
        _header = new FileHeader();
        _header.readFrom(page, 0);
        _stream.setPosition(_header.PageAlignment * 512);
        byte[] toc = StreamUtilities.readExact(_stream, (int) (_header.PageAlignment * 512));
        _sections = new ArrayList<>();
        int pos = 0;
        while (EndianUtilities.toUInt64LittleEndian(toc, pos) != 0) {
            SectionRecord record = new SectionRecord();
            record.readFrom(toc, pos);
            _sections.add(record);
            pos += SectionRecord.RecordSize;
        }
    }

    /**
     * Gets all of the sections within the file.
     */
    public List<Section> getSections() {
        List<Section> result = new ArrayList<>();
        int i = 0;
        for (SectionRecord section : _sections) {
            result.add(new Section(section, i++));
        }
        return result;
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (_ownership == Ownership.Dispose && _stream != null) {
            _stream.close();
            _stream = null;
        }
    }

    /**
     * Opens a stream to access a particular section.
     *
     * @param index The zero-based index of the section.
     * @return A stream that can be used to access the section.
     */
    public Stream openSection(int index) {
        return new SubStream(_stream, _sections.get(index).Offset, _sections.get(index).Size);
    }
}
