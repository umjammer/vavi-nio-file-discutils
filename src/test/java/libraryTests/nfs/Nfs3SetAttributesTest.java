//
// Copyright (c) 2008-2011, Kenneth Bell
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
import discUtils.nfs.Nfs3FileTime;
import discUtils.nfs.Nfs3SetAttributes;
import discUtils.nfs.Nfs3SetTimeMethod;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class Nfs3SetAttributesTest {
    @Test
    public void roundTripTest() throws Exception {
        Nfs3SetAttributes attributes = new Nfs3SetAttributes();
        attributes.setAccessTime(new Nfs3FileTime(ZonedDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant().toEpochMilli()));
        attributes.setGid(1);
        attributes.setMode(UnixFilePermissions.GroupAll);
        attributes.setModifyTime(new Nfs3FileTime(ZonedDateTime.of(2017, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant().toEpochMilli()));
        attributes.setSetAccessTime(Nfs3SetTimeMethod.ClientTime);
        attributes.setSetGid(true);
        attributes.setSetMode(true);
        attributes.setSetModifyTime(Nfs3SetTimeMethod.ClientTime);
        attributes.setSetSize(true);
        attributes.setSetUid(true);
        attributes.setSize(4);
        attributes.setUid(5);

        Nfs3SetAttributes clone;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            attributes.write(writer);
            stream.position(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new Nfs3SetAttributes(reader);
        }
        assertEquals(attributes, clone);
    }
}
