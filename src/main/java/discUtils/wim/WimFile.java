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

package discUtils.wim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import vavi.util.ByteUtil;


/**
 * Provides access to the contents of WIM (Windows Imaging) files.
 */
public class WimFile {

    private final FileHeader fileHeader;

    private final Stream fileStream;

    private Map<Integer, List<ResourceInfo>> resources;

    /**
     * Initializes a new instance of the WimFile class.
     *
     * @param stream A stream of the WIM file contents.
     */
    public WimFile(Stream stream) {
        fileStream = stream;
        byte[] buffer = StreamUtilities.readExact(stream, 512);
        fileHeader = new FileHeader();
        fileHeader.read(buffer, 0);
        if (!fileHeader.isValid()) {
            throw new dotnet4j.io.IOException("Not a valid WIM file");
        }

        if (fileHeader.totalParts != 1) {
            throw new UnsupportedOperationException("Multi-part WIM file");
        }

        readResourceTable();
    }

    /**
     * Gets the (zero-based) index of the bootable image.
     */
    public int getBootImage() {
        return fileHeader.bootIndex;
    }

    /**
     * Gets the version of the file format.
     */
    public int getFileFormatVersion() {
        return fileHeader.version;
    }

    /**
     * Gets the identifying GUID for this WIM file.
     */
    public UUID getGuid() {
        return fileHeader.wimGuid;
    }

    /**
     * Gets the number of disk images within this file.
     */
    public int getImageCount() {
        return fileHeader.imageCount;
    }

    /**
     * Gets the embedded manifest describing the file and the contained images.
     */
    public String getManifest() {
        try (StreamReader reader = new StreamReader(openResourceStream(fileHeader.xmlDataHeader), true)) {
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
        try (Stream s = openResourceStream(fileHeader.offsetTableHeader)) {
            long numRead = 0;
            while (numRead < s.getLength()) {
                byte[] resBuffer = StreamUtilities.readExact(s, ResourceInfo.Size);
                numRead += ResourceInfo.Size;
                ResourceInfo info = new ResourceInfo();
                info.read(resBuffer, 0);
                if (info.header.flags.contains(ResourceFlags.MetaData)) {
                    if (i == index) {
                        return info.header;
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
        int hashHash = ByteUtil.readLeInt(hash, 0);
        if (!resources.containsKey(hashHash)) {
            return null;
        }

        for (ResourceInfo header : resources.get(hashHash)) {
            if (Arrays.equals(header.hash, hash)) {
                return header.header;
            }
        }
        return null;
    }

    public SparseStream openResourceStream(ShortResourceHeader hdr) {
        SparseStream fileSectionStream = new SubStream(fileStream, Ownership.None, hdr.fileOffset, hdr.compressedSize);
        if (!hdr.flags.contains(ResourceFlags.Compressed)) {
            return fileSectionStream;
        }

        return new FileResourceStream(fileSectionStream,
                                      hdr,
                                      fileHeader.flags.contains(FileFlags.LzxCompression),
                                      fileHeader.compressionSize);
    }

    private void readResourceTable() {
        resources = new HashMap<>();
        try (Stream s = openResourceStream(fileHeader.offsetTableHeader)) {
            long numRead = 0;
            while (numRead < s.getLength()) {
                byte[] resBuffer = StreamUtilities.readExact(s, ResourceInfo.Size);
                numRead += ResourceInfo.Size;
                ResourceInfo info = new ResourceInfo();
                info.read(resBuffer, 0);
                int hashHash = ByteUtil.readLeInt(info.hash, 0);
                if (!resources.containsKey(hashHash)) {
                    resources.put(hashHash, new ArrayList<>(1));
                }

                resources.get(hashHash).add(info);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
