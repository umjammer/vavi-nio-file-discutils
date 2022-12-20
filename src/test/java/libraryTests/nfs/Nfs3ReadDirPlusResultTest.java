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

package libraryTests.nfs;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumSet;

import discUtils.core.UnixFilePermissions;
import discUtils.nfs.Nfs3DirectoryEntry;
import discUtils.nfs.Nfs3FileAttributes;
import discUtils.nfs.Nfs3FileHandle;
import discUtils.nfs.Nfs3FileTime;
import discUtils.nfs.Nfs3FileType;
import discUtils.nfs.Nfs3ReadDirPlusResult;
import discUtils.nfs.Nfs3Status;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class Nfs3ReadDirPlusResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3ReadDirPlusResult result = new Nfs3ReadDirPlusResult();
        result.setCookieVerifier(1);
        Nfs3FileAttributes dirAttrs = new Nfs3FileAttributes();
        dirAttrs.accessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        dirAttrs.bytesUsed = 1;
        dirAttrs.changeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        dirAttrs.fileId = 2;
        dirAttrs.fileSystemId = 3;
        dirAttrs.gid = 4;
        dirAttrs.linkCount = 5;
        dirAttrs.mode = UnixFilePermissions.GroupAll;
        dirAttrs.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        dirAttrs.rdevMajor = 6;
        dirAttrs.rdevMinor = 7;
        dirAttrs.size = 8;
        dirAttrs.type = Nfs3FileType.BlockDevice;
        dirAttrs.uid = 9;
        result.setDirAttributes(dirAttrs);
        Nfs3DirectoryEntry entry = new Nfs3DirectoryEntry();
        entry.setCookie(2);
        Nfs3FileAttributes fileAttrs = new Nfs3FileAttributes();
        fileAttrs.accessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 10, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        fileAttrs.bytesUsed = 11;
        fileAttrs.changeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 12, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        fileAttrs.fileId = 12;
        fileAttrs.fileSystemId = 13;
        fileAttrs.gid = 14;
        fileAttrs.linkCount = 15;
        fileAttrs.mode = EnumSet.of(UnixFilePermissions.GroupWrite);
        fileAttrs.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 13, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        fileAttrs.rdevMajor = 16;
        fileAttrs.rdevMinor = 17;
        fileAttrs.size = 18;
        fileAttrs.type = Nfs3FileType.Socket;
        fileAttrs.uid = 19;
        entry.setFileAttributes(fileAttrs);
        Nfs3FileHandle handle = new Nfs3FileHandle();
        handle.setValue(new byte[] {
            0xa
        });
        entry.setFileHandle(handle);
        entry.setFileId(99);
        entry.setName("test");
        result.setDirEntries(Collections.singletonList(entry));
        result.setEof(false);
        result.setStatus(Nfs3Status.Ok);

        Nfs3ReadDirPlusResult clone;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            result.write(writer);
            stream.position(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3ReadDirPlusResult(reader);
        }
        assertEquals(result, clone);
    }
}
