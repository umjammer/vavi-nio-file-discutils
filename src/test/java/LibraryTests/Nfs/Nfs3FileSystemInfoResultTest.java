//
// Copyright (c) 2017, Quamotion
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

package LibraryTests.Nfs;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Core.UnixFilePermissions;
import DiscUtils.Nfs.Nfs3FileAttributes;
import DiscUtils.Nfs.Nfs3FileSystemInfo;
import DiscUtils.Nfs.Nfs3FileSystemInfoResult;
import DiscUtils.Nfs.Nfs3FileSystemProperties;
import DiscUtils.Nfs.Nfs3FileTime;
import DiscUtils.Nfs.Nfs3FileType;
import DiscUtils.Nfs.Nfs3Status;
import DiscUtils.Nfs.XdrDataReader;
import DiscUtils.Nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class Nfs3FileSystemInfoResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3FileSystemInfoResult result = new Nfs3FileSystemInfoResult();
        Nfs3FileSystemInfo info = new Nfs3FileSystemInfo();
        info.setDirectoryPreferredBytes(1);
        info.setFileSystemProperties(Nfs3FileSystemProperties.HardLinks);
        info.setMaxFileSize(3);
        info.setReadMaxBytes(4);
        info.setReadMultipleSize(5);
        info.setReadPreferredBytes(6);
        info.setTimePrecision(Nfs3FileTime.getPrecision());
        info.setWriteMaxBytes(8);
        info.setWriteMultipleSize(9);
        info.setWritePreferredBytes(10);
        result.setFileSystemInfo(info);
        Nfs3FileAttributes attributes = new Nfs3FileAttributes();
        attributes.AccessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.BytesUsed = 2;
        attributes.ChangeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.FileId = 4;
        attributes.FileSystemId = 5;
        attributes.Gid = 6;
        attributes.LinkCount = 7;
        attributes.Mode = EnumSet.of(UnixFilePermissions.OthersExecute);
        attributes.ModifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 8, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.RdevMajor = 9;
        attributes.RdevMinor = 10;
        attributes.Size = 11;
        attributes.Type = Nfs3FileType.BlockDevice;
        attributes.Uid = 12;
        result.setPostOpAttributes(attributes);
        result.setStatus(Nfs3Status.Ok);

        Nfs3FileSystemInfoResult clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            result.write(writer);
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3FileSystemInfoResult(reader);
        }
        assertEquals(result, clone);
    }
}
