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

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Nfs.Nfs3FileAttributes;
import DiscUtils.Nfs.Nfs3FileSystemStat;
import DiscUtils.Nfs.Nfs3FileSystemStatResult;
import DiscUtils.Nfs.Nfs3FileTime;
import DiscUtils.Nfs.XdrDataReader;
import DiscUtils.Nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class Nfs3FileSystemStatResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3FileSystemStatResult result = new Nfs3FileSystemStatResult();
        result.setFileSystemStat(new Nfs3FileSystemStat());
        result.getFileSystemStat().setAvailableFreeFileSlotCount(1);
        result.getFileSystemStat().setAvailableFreeSpaceBytes(2);
        result.getFileSystemStat().setFileSlotCount(3);
        result.getFileSystemStat().setFreeFileSlotCount(4);
        result.getFileSystemStat().setFreeSpaceBytes(5);
        result.getFileSystemStat().setInvariant(Duration.ofSeconds(7).toMillis());
        result.getFileSystemStat().setTotalSizeBytes(8);
        result.setPostOpAttributes(new Nfs3FileAttributes());
        result.getPostOpAttributes().AccessTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        result.getPostOpAttributes().ChangeTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        result.getPostOpAttributes().ModifyTime = new Nfs3FileTime(ZonedDateTime.of(2017, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        Nfs3FileSystemStatResult clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            result.write(writer);
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3FileSystemStatResult(reader);
        }
        assertEquals(result, clone);
    }
}
