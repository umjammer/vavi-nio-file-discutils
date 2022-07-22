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
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.core.UnixFilePermissions;
import discUtils.nfs.Nfs3FileAttributes;
import discUtils.nfs.Nfs3FileHandle;
import discUtils.nfs.Nfs3FileTime;
import discUtils.nfs.Nfs3FileType;
import discUtils.nfs.Nfs3LookupResult;
import discUtils.nfs.Nfs3Status;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class Nfs3LookupResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3LookupResult result = new Nfs3LookupResult();
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

        Nfs3FileAttributes objAttrs = new Nfs3FileAttributes();
        objAttrs.accessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 10, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        objAttrs.bytesUsed = 11;
        objAttrs.changeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 12, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        objAttrs.fileId = 12;
        objAttrs.fileSystemId = 13;
        objAttrs.gid = 14;
        objAttrs.linkCount = 15;
        objAttrs.mode = EnumSet.of(UnixFilePermissions.GroupWrite);
        objAttrs.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 13, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        objAttrs.rdevMajor = 16;
        objAttrs.rdevMinor = 17;
        objAttrs.size = 18;
        objAttrs.type = Nfs3FileType.Socket;
        objAttrs.uid = 19;
        result.setObjectAttributes(objAttrs);
        Nfs3FileHandle handle = new Nfs3FileHandle();
        handle.setValue(new byte[] {
            0x20
        });
        result.setObjectHandle(handle);
        result.setStatus(Nfs3Status.Ok);

        Nfs3LookupResult clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            result.write(writer);
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3LookupResult(reader);
        }
        assertEquals(result, clone);
    }
}
