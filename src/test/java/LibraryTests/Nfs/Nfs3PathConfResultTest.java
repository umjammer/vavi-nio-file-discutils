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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Nfs.Nfs3FileAttributes;
import DiscUtils.Nfs.Nfs3FileTime;
import DiscUtils.Nfs.Nfs3PathConfResult;
import DiscUtils.Nfs.Nfs3Status;
import DiscUtils.Nfs.XdrDataReader;
import DiscUtils.Nfs.XdrDataWriter;
import moe.yo3explorer.dotnetio4j.MemoryStream;


public class Nfs3PathConfResultTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3PathConfResult authentication = new Nfs3PathConfResult();
        authentication.setStatus(Nfs3Status.Ok);
        authentication.setCaseInsensitive(true);
        authentication.setCasePreserving(true);
        authentication.setChownRestricted(true);
        authentication.setLinkMax(1);
        authentication.setNameMax(2);
        authentication.setNoTrunc(true);
        Nfs3FileAttributes attributes = new Nfs3FileAttributes();
        attributes.AccessTime = new Nfs3FileTime(LocalDateTime.of(2017, 1, 1, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.ChangeTime = new Nfs3FileTime(LocalDateTime.of(2017, 1, 2, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        attributes.ModifyTime = new Nfs3FileTime(LocalDateTime.of(2017, 1, 2, 0, 0, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli());
        authentication.setObjectAttributes(attributes);

        Nfs3PathConfResult clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            authentication.write(writer);
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3PathConfResult(reader);
        }
        assertEquals(authentication, clone);
    }
}
