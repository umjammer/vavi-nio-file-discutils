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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.nfs.Nfs3DirectoryEntry;
import discUtils.nfs.Nfs3FileAttributes;
import discUtils.nfs.Nfs3FileHandle;
import discUtils.nfs.Nfs3FileTime;
import discUtils.nfs.Nfs3ReadDirResult;
import discUtils.nfs.Nfs3Status;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class Nfs3ReadDirResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3ReadDirResult result = new Nfs3ReadDirResult();
        result.setStatus(Nfs3Status.Ok);
        result.setEof(false);
        result.setCookieVerifier(1);
        Nfs3FileAttributes fileAttrs = new Nfs3FileAttributes();
        fileAttrs.accessTime = new Nfs3FileTime(ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        fileAttrs.changeTime = new Nfs3FileTime(ZonedDateTime.of(2018, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        fileAttrs.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2018, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        result.setDirAttributes(fileAttrs);
        Nfs3DirectoryEntry entry = new Nfs3DirectoryEntry();
        entry.setCookie(2);
        Nfs3FileAttributes dirAttrs = new Nfs3FileAttributes();
        dirAttrs.accessTime = new Nfs3FileTime(ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        dirAttrs.changeTime = new Nfs3FileTime(ZonedDateTime.of(2018, 2, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        dirAttrs.modifyTime = new Nfs3FileTime(ZonedDateTime.of(2018, 2, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
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
        result.setDirEntries(Collections.emptyList());

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
