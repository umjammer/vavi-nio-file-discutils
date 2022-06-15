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

package DiscUtils.Wim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;


/**
 * Provides access to the contents of WIM (Windows Imaging) files.
 */
public class WimFile {
    private final FileHeader _fileHeader;

    private final Stream _fileStream;

    private Map<Integer, List<ResourceInfo>> _resources;

    /**
     * Initializes a new instance of the WimFile class.
     *
     * @param stream A stream of the WIM file contents.
     */
    public WimFile(Stream stream) {
        _fileStream = stream;
        byte[] buffer = StreamUtilities.readExact(stream, 512);
        _fileHeader = new FileHeader();
        _fileHeader.read(buffer, 0);
        if (!_fileHeader.isValid()) {
            throw new dotnet4j.io.IOException("Not a valid WIM file");
        }

        if (_fileHeader.TotalParts != 1) {
            throw new UnsupportedOperationException("Multi-part WIM file");
        }

        readResourceTable();
    }

    /**
     * Gets the (zero-based) index of the bootable image.
     */
    public int getBootImage() {
        return _fileHeader.BootIndex;
    }

    /**
     * Gets the version of the file format.
     */
    public int getFileFormatVersion() {
        return _fileHeader.Version;
    }

    /**
     * Gets the identifying GUID for this WIM file.
     */
    public UUID getGuid() {
        return _fileHeader.WimGuid;
    }

    /**
     * Gets the number of disk images within this file.
     */
    public int getImageCount() {
        return _fileHeader.ImageCount;
    }

    /**
     * Gets the embedded manifest describing the file and the contained images.
     */
    public String getManifest() {
        try (StreamReader reader = new StreamReader(openResourceStream(_fileHeader.XmlDataHeader), true)) {
            return reader.readToEnd();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets a particular image within the file (zero-based index).
     *
     * The XML manifest file uses a one-based index, whereas this method is
     * zero-based.
     *
     * @param index The index of the image to retrieve.
     * @return The image as a file system.
     */
    public WimFileSystem getImage(int index) {
        return new WimFileSystem(this, index);
    }

    public ShortResourceHeader locateImage(int index) {
        int i = 0;
        try (Stream s = openResourceStream(_fileHeader.OffsetTableHeader)) {
            long numRead = 0;
            while (numRead < s.getLength()) {
                byte[] resBuffer = StreamUtilities.readExact(s, ResourceInfo.Size);
                numRead += ResourceInfo.Size;
                ResourceInfo info = new ResourceInfo();
                info.read(resBuffer, 0);
                if (info.Header.Flags.contains(ResourceFlags.MetaData)) {
                    if (i == index) {
                        return info.Header;
                    }

                    ++i;
                }
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        return null;
    }

    public ShortResourceHeader locateResource(byte[] hash) {
        int hashHash = EndianUtilities.toUInt32LittleEndian(hash, 0);
        if (!_resources.containsKey(hashHash)) {
            return null;
        }

        for (ResourceInfo header : _resources.get(hashHash)) {
            if (Utilities.areEqual(header.Hash, hash)) {
                return header.Header;
            }
        }
        return null;
    }

    public SparseStream openResourceStream(ShortResourceHeader hdr) {
        SparseStream fileSectionStream = new SubStream(_fileStream, Ownership.None, hdr.FileOffset, hdr.CompressedSize);
        if (!hdr.Flags.contains(ResourceFlags.Compressed)) {
            return fileSectionStream;
        }

        return new FileResourceStream(fileSectionStream,
                                      hdr,
                                      _fileHeader.Flags.contains(FileFlags.LzxCompression),
                                      _fileHeader.CompressionSize);
    }

    private void readResourceTable() {
        _resources = new HashMap<>();
        try (Stream s = openResourceStream(_fileHeader.OffsetTableHeader)) {
            long numRead = 0;
            while (numRead < s.getLength()) {
                byte[] resBuffer = StreamUtilities.readExact(s, ResourceInfo.Size);
                numRead += ResourceInfo.Size;
                ResourceInfo info = new ResourceInfo();
                info.read(resBuffer, 0);
                int hashHash = EndianUtilities.toUInt32LittleEndian(info.Hash, 0);
                if (!_resources.containsKey(hashHash)) {
                    _resources.put(hashHash, new ArrayList<>(1));
                }

                _resources.get(hashHash).add(info);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
