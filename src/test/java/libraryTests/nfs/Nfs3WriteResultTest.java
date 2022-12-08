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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.core.UnixFilePermissions;
import discUtils.nfs.Nfs3FileAttributes;
import discUtils.nfs.Nfs3FileTime;
import discUtils.nfs.Nfs3FileType;
import discUtils.nfs.Nfs3StableHow;
import discUtils.nfs.Nfs3Status;
import discUtils.nfs.Nfs3WeakCacheConsistency;
import discUtils.nfs.Nfs3WeakCacheConsistencyAttr;
import discUtils.nfs.Nfs3WriteResult;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class Nfs3WriteResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3WriteResult result = new Nfs3WriteResult();
        Nfs3WeakCacheConsistencyAttr before = new Nfs3WeakCacheConsistencyAttr();
        before.setChangeTime(new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()));
        before.setModifyTime(new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()));
        before.setSize(3);
        Nfs3FileAttributes after = new Nfs3FileAttributes();
        after.accessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        after.bytesUsed = 2;
        after.changeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        after.fileId = 3;
        after.fileSystemId = 4;
        after.gid = 5;
        after.linkCount = 6;
        after.mode = UnixFilePermissions.GroupAll;
        after.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        after.rdevMajor = 7;
        after.rdevMinor = 8;
        after.size = 9;
        after.type = Nfs3FileType.NamedPipe;
        after.uid = 10;
        Nfs3WeakCacheConsistency wcc = new Nfs3WeakCacheConsistency();
        wcc.setBefore(before);
        wcc.setAfter(after);
        result.setCacheConsistency(wcc);
        result.setCount(1);
        result.setHowCommitted(Nfs3StableHow.Unstable);
        result.setStatus(Nfs3Status.Ok);
        result.setWriteVerifier(3);
        Nfs3WriteResult clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            result.write(writer);
            stream.position(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3WriteResult(reader);
        }
        assertEquals(result, clone);
    }
}
