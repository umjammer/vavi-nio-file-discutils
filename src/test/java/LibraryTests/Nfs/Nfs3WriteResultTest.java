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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Core.UnixFilePermissions;
import DiscUtils.Nfs.Nfs3FileAttributes;
import DiscUtils.Nfs.Nfs3FileTime;
import DiscUtils.Nfs.Nfs3FileType;
import DiscUtils.Nfs.Nfs3StableHow;
import DiscUtils.Nfs.Nfs3Status;
import DiscUtils.Nfs.Nfs3WeakCacheConsistency;
import DiscUtils.Nfs.Nfs3WeakCacheConsistencyAttr;
import DiscUtils.Nfs.Nfs3WriteResult;
import DiscUtils.Nfs.XdrDataReader;
import DiscUtils.Nfs.XdrDataWriter;
import moe.yo3explorer.dotnetio4j.MemoryStream;


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
        after.AccessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        after.BytesUsed = 2;
        after.ChangeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        after.FileId = 3;
        after.FileSystemId = 4;
        after.Gid = 5;
        after.LinkCount = 6;
        after.Mode = UnixFilePermissions.GroupAll;
        after.ModifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        after.RdevMajor = 7;
        after.RdevMinor = 8;
        after.Size = 9;
        after.Type = Nfs3FileType.NamedPipe;
        after.Uid = 10;
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
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3WriteResult(reader);
        }
        assertEquals(result, clone);
    }
}
