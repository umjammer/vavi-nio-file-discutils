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

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.internal.Utilities;
import discUtils.streams.readerWriter.DataReader;
import dotnet4j.io.FileNotFoundException;


public class DirectoryEntry {

    public Map<String, AlternateStreamEntry> alternateStreams;

    public EnumSet<FileAttributes> attributes;

    /** FILETIME raw data */
    public long creationTime;

    public String fileName;

    public int hardLink;

    public byte[] hash;

    /** FILETIME raw data */
    public long lastAccessTime;

    /** FILETIME raw data */
    public long lastWriteTime;

    public long length;

    public int reparseTag;

    public int securityId;

    public String shortName;

    public short streamCount;

    public long subdirOffset;

    public String getSearchName() {
        if (fileName.indexOf('.') == -1) {
            return fileName + ".";
        }

        return fileName;
    }

    public static DirectoryEntry readFrom(DataReader reader) {
        long startPos = reader.position();
        long length = reader.readInt64();
        if (length == 0) {
            return null;
        }

        DirectoryEntry result = new DirectoryEntry();
        result.length = length;
        result.attributes = FileAttributes.valueOf(reader.readUInt32());
        result.securityId = reader.readUInt32();
        result.subdirOffset = reader.readInt64();
        reader.skip(16);
        result.creationTime = reader.readInt64();
        result.lastAccessTime = reader.readInt64();
        result.lastWriteTime = reader.readInt64();
        result.hash = reader.readBytes(20);
        reader.skip(4);
        result.reparseTag = reader.readUInt32();
        result.hardLink = reader.readUInt32();
        result.streamCount = reader.readUInt16();
        int shortNameLength = reader.readUInt16();
        int fileNameLength = reader.readUInt16();
        if (fileNameLength > 0) {
            result.fileName = new String(reader.readBytes(fileNameLength + 2), StandardCharsets.UTF_16LE).replaceFirst("\0*$",
                                                                                                                         "");
        } else {
            result.fileName = "";
        }
        if (shortNameLength > 0) {
            result.shortName = new String(reader.readBytes(shortNameLength + 2), StandardCharsets.UTF_16LE)
                    .replaceFirst("\0*$", "");
        } else {
            result.shortName = null;
        }
        if (startPos + length > reader.position()) {
            int toRead = (int) (startPos + length - reader.position());
            reader.skip(toRead);
        }

        if (result.streamCount > 0) {
            result.alternateStreams = new HashMap<>();
            for (int i = 0; i < result.streamCount; ++i) {
                AlternateStreamEntry stream = AlternateStreamEntry.readFrom(reader);
                result.alternateStreams.put(stream.name, stream);
            }
        }

        return result;
    }

    public byte[] getStreamHash(String streamName) {
        if (streamName != null && streamName.isEmpty()) {
            if (!Utilities.isAllZeros(hash, 0, 20)) {
                return hash;
            }
        }

        if (alternateStreams != null && alternateStreams.containsKey(streamName)) {
            AlternateStreamEntry streamEntry = alternateStreams.get(streamName);
            return streamEntry.hash;
        }

        return new byte[20];
    }

    public long getLength(String streamName) {
        if (streamName != null && streamName.isEmpty()) {
            return length;
        }

        if (alternateStreams != null && alternateStreams.containsKey(streamName)) {
            AlternateStreamEntry streamEntry = alternateStreams.get(streamName);
            return streamEntry.length;
        }

        throw new FileNotFoundException("No such alternate stream '%s' in file '%s'".formatted(streamName, fileName));
    }
}
