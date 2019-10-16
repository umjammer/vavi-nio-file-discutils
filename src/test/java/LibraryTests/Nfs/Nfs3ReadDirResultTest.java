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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Nfs.Nfs3DirectoryEntry;
import DiscUtils.Nfs.Nfs3FileAttributes;
import DiscUtils.Nfs.Nfs3FileHandle;
import DiscUtils.Nfs.Nfs3FileTime;
import DiscUtils.Nfs.Nfs3ReadDirResult;
import DiscUtils.Nfs.Nfs3Status;
import DiscUtils.Nfs.XdrDataReader;
import DiscUtils.Nfs.XdrDataWriter;
import moe.yo3explorer.dotnetio4j.MemoryStream;


public class Nfs3ReadDirResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3ReadDirResult result = new Nfs3ReadDirResult();
        result.setStatus(Nfs3Status.Ok);
        result.setEof(false);
        result.setCookieVerifier(1);
        Nfs3FileAttributes fileAttrs = new Nfs3FileAttributes();
        fileAttrs.AccessTime = new Nfs3FileTime(LocalDateTime.of(2018, 1, 1, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        fileAttrs.ChangeTime = new Nfs3FileTime(LocalDateTime.of(2018, 1, 2, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        fileAttrs.ModifyTime = new Nfs3FileTime(LocalDateTime.of(2018, 1, 3, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        result.setDirAttributes(fileAttrs);
        Nfs3DirectoryEntry entry = new Nfs3DirectoryEntry();
        entry.setCookie(2);
        Nfs3FileAttributes dirAttrs = new Nfs3FileAttributes();
        dirAttrs.AccessTime = new Nfs3FileTime(LocalDateTime.of(2018, 2, 1, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        dirAttrs.ChangeTime = new Nfs3FileTime(LocalDateTime.of(2018, 2, 2, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        dirAttrs.ModifyTime = new Nfs3FileTime(LocalDateTime.of(2018, 2, 3, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        entry.setFileAttributes(dirAttrs);
        Nfs3FileHandle handle = new Nfs3FileHandle();
        handle.setValue(new byte[] {
            0x20, 0x18
        });
        entry.setFileHandle(handle);
        entry.setFileId(2018);
        entry.setName("test.bin");
        result.setDirEntries(Arrays.asList());

        Nfs3ReadDirResult clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            result.write(writer);
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3ReadDirResult(reader);
        }
        assertEquals(result, clone);
    }
}
