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

package LibraryTests.Vdi;

import org.junit.jupiter.api.Test;

import vavi.util.StringUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Vdi.Disk;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class StreamTest {
    @Test
    public void attributes() throws Exception {
        MemoryStream stream = new MemoryStream();

        try (Disk disk = Disk.initializeDynamic(stream, Ownership.Dispose, 16 * 1024L * 1024 * 1024)) {
            Stream s = disk.getContent();
            assertTrue(s.canRead());
            assertTrue(s.canWrite());
            assertTrue(s.canSeek());
        }
    }

    @Test
    public void readWriteSmall() throws Exception {
        MemoryStream stream = new MemoryStream();

        try (Disk disk = Disk.initializeDynamic(stream, Ownership.None, 16 * 1024L * 1024 * 1024)) {
            byte[] content = new byte[100];
            for (int i = 0; i < content.length; ++i) {
                content[i] = (byte) i;
            }
            Stream s = disk.getContent();
            s.write(content, 10, 40);
            assertEquals(40, s.getPosition());
            s.write(content, 50, 50);
            assertEquals(90, s.getPosition());
            s.setPosition(0);
            byte[] buffer = new byte[100];
            s.read(buffer, 10, 60);
            assertEquals(60, s.getPosition());
            for (int i = 0; i < 10; ++i) {
                assertEquals(0, buffer[i]);
            }
            for (int i = 10; i < 60; ++i) {
                assertEquals(i, buffer[i]);
            }
        }
        // Check the data persisted

        try (Disk disk = new Disk(stream)) {
            Stream s = disk.getContent();
            byte[] buffer = new byte[100];
            s.read(buffer, 10, 20);
            assertEquals(20, s.getPosition());
            for (int i = 0; i < 10; ++i) {
                assertEquals(0, buffer[i]);
            }
            for (int i = 10; i < 20; ++i) {
                assertEquals(i, buffer[i]);
            }
        }
    }

    @Test
    public void readWriteLarge() throws Exception {
        MemoryStream stream = new MemoryStream();

        try (Disk disk = Disk.initializeDynamic(stream, Ownership.Dispose, 16 * 1024L * 1024 * 1024)) {
            byte[] content = new byte[3 * 1024 * 1024];
            for (int i = 0; i < content.length; ++i) {
                content[i] = (byte) i;
            }
            Stream s = disk.getContent();
            s.setPosition(10);
            s.write(content, 0, content.length);
            byte[] buffer = new byte[content.length];
            s.setPosition(10);
            s.read(buffer, 0, buffer.length);
            for (int i = 0; i < content.length; ++i) {
                if (buffer[i] != content[i]) {
                    fail(String.format("Corrupt stream contents: %d, %02x, %02x", i, buffer[i], content[i]));
                }
            }
        }
    }

    @Test
    public void disposeTest() throws Exception {
        Stream contentStream;
        MemoryStream stream = new MemoryStream();

        try (Disk disk = Disk.initializeDynamic(stream, Ownership.None, 16 * 1024L * 1024 * 1024)) {
            contentStream = disk.getContent();
        }

        assertThrows(dotnet4j.io.IOException.class, () -> {
            contentStream.setPosition(0);
            fail("Able to use stream after disposed");
        });
    }

    @Test
    public void readNotPresent() throws Exception {
        MemoryStream stream = new MemoryStream();

        try (Disk disk = Disk.initializeDynamic(stream, Ownership.Dispose, 16 * 1024L * 1024 * 1024)) {
            byte[] buffer = new byte[100];
            disk.getContent().seek(2 * 1024 * 1024, SeekOrigin.Current);
            disk.getContent().read(buffer, 0, buffer.length);
            for (int i = 0; i < 100; ++i) {
                if (buffer[i] != 0) {
                    fail();
                }
            }
        }
    }
}
