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
import discUtils.nfs.Nfs3ReadResult;
import discUtils.nfs.Nfs3Status;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class Nfs3ReadResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3ReadResult result = new Nfs3ReadResult();
        result.setCount(1);
        result.setData(new byte[] {
            0x02, 0x03
        });
        result.setEof(false);
        Nfs3FileAttributes attributes = new Nfs3FileAttributes();
        attributes.accessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.bytesUsed = 1;
        attributes.changeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.fileId = 2;
        attributes.fileSystemId = 3;
        attributes.gid = 4;
        attributes.linkCount = 5;
        attributes.mode = UnixFilePermissions.GroupAll;
        attributes.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.rdevMajor = 6;
        attributes.rdevMinor = 7;
        attributes.size = 8;
        attributes.type = Nfs3FileType.BlockDevice;
        attributes.uid = 9;
        result.setFileAttributes(attributes);
        result.setStatus(Nfs3Status.Ok);

        Nfs3ReadResult clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            result.write(writer);
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3ReadResult(reader);
        }
        assertEquals(result, clone);
    }
}
