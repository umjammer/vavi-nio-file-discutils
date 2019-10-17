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

import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.ReaderWriter.DataReader;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;


public class DirectoryEntry {
    public Map<String, AlternateStreamEntry> AlternateStreams;

    public EnumSet<FileAttributes> Attributes;

    /** FILETIME raw data */
    public long CreationTime;

    public String FileName;

    public int HardLink;

    public byte[] Hash;

    /** FILETIME raw data */
    public long LastAccessTime;

    /** FILETIME raw data */
    public long LastWriteTime;

    public long Length;

    public int ReparseTag;

    public int SecurityId;

    public String ShortName;

    public short StreamCount;

    public long SubdirOffset;

    public String getSearchName() {
        if (FileName.indexOf('.') == -1) {
            return FileName + ".";
        }

        return FileName;
    }

    public static DirectoryEntry readFrom(DataReader reader) {
        long startPos = reader.getPosition();
        long length = reader.readInt64();
        if (length == 0) {
            return null;
        }

        DirectoryEntry result = new DirectoryEntry();
        result.Length = length;
        result.Attributes = FileAttributes.valueOf(reader.readUInt32());
        result.SecurityId = reader.readUInt32();
        result.SubdirOffset = reader.readInt64();
        reader.skip(16);
        result.CreationTime = reader.readInt64();
        result.LastAccessTime = reader.readInt64();
        result.LastWriteTime = reader.readInt64();
        result.Hash = reader.readBytes(20);
        reader.skip(4);
        result.ReparseTag = reader.readUInt32();
        result.HardLink = reader.readUInt32();
        result.StreamCount = reader.readUInt16();
        int shortNameLength = reader.readUInt16();
        int fileNameLength = reader.readUInt16();
        if (fileNameLength > 0) {
            result.FileName = new String(reader.readBytes(fileNameLength + 2), Charset.forName("UTF-16LE")).replaceFirst("\0*$",
                                                                                                                         "");
        } else {
            result.FileName = "";
        }
        if (shortNameLength > 0) {
            result.ShortName = new String(reader.readBytes(shortNameLength + 2), Charset.forName("UTF-16LE"))
                    .replaceFirst("\0*$", "");
        } else {
            result.ShortName = null;
        }
        if (startPos + length > reader.getPosition()) {
            int toRead = (int) (startPos + length - reader.getPosition());
            reader.skip(toRead);
        }

        if (result.StreamCount > 0) {
            result.AlternateStreams = new HashMap<>();
            for (int i = 0; i < result.StreamCount; ++i) {
                AlternateStreamEntry stream = AlternateStreamEntry.readFrom(reader);
                result.AlternateStreams.put(stream.Name, stream);
            }
        }

        return result;
    }

    public byte[] getStreamHash(String streamName) {
        if (streamName != null && streamName.isEmpty()) {
            if (!Utilities.isAllZeros(Hash, 0, 20)) {
                return Hash;
            }

        }

        boolean result = AlternateStreams != null && AlternateStreams.containsKey(streamName);
        if (result) {
            AlternateStreamEntry streamEntry = AlternateStreams.get(streamName);
            return streamEntry.Hash;
        }

        return new byte[20];
    }

    public long getLength(String streamName) {
        if (streamName != null && streamName.isEmpty()) {
            return Length;
        }

        boolean result = AlternateStreams != null && AlternateStreams.containsKey(streamName);
        if (result) {
            AlternateStreamEntry streamEntry = AlternateStreams.get(streamName);
            return streamEntry.Length;
        }

        throw new FileNotFoundException(String.format("No such alternate stream '%s' in file '%s'", streamName, FileName));
    }
}
