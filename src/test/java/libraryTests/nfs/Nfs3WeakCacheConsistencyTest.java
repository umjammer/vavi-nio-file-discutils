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

import discUtils.core.UnixFilePermissions;
import discUtils.nfs.Nfs3FileAttributes;
import discUtils.nfs.Nfs3FileTime;
import discUtils.nfs.Nfs3FileType;
import discUtils.nfs.Nfs3WeakCacheConsistency;
import discUtils.nfs.Nfs3WeakCacheConsistencyAttr;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class Nfs3WeakCacheConsistencyTest {

    @Test
    void roundTripTest() throws Exception {
        Nfs3WeakCacheConsistency consistency = new Nfs3WeakCacheConsistency();
        Nfs3WeakCacheConsistencyAttr before = new Nfs3WeakCacheConsistencyAttr();
        before.setChangeTime(new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant().toEpochMilli()));
        before.setModifyTime(new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant().toEpochMilli()));
        before.setSize(3);
        Nfs3FileAttributes after = new Nfs3FileAttributes();
        after.accessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant().toEpochMilli());
        after.bytesUsed = 2;
        after.changeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant().toEpochMilli());
        after.fileId = 3;
        after.fileSystemId = 4;
        after.gid = 5;
        after.linkCount = 6;
        after.mode = UnixFilePermissions.GroupAll;
        after.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant().toEpochMilli());
        after.rdevMajor = 7;
        after.rdevMinor = 8;
        after.size = 9;
        after.type = Nfs3FileType.NamedPipe;
        after.uid = 10;
        consistency.setBefore(before);
        consistency.setAfter(after);

        Nfs3WeakCacheConsistency clone;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            consistency.write(writer);
            stream.position(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3WeakCacheConsistency(reader);
        }
        assertEquals(consistency, clone);
    }
}
