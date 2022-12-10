//
// Aaru Data Preservation Suite
//
//
// Filename       : CRC16CCITT.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Aaru unit testing.
//
// License
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as
//     published by the Free Software Foundation, either version 3 of the
//     License, or (at your option) any later version.
//
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.checksum;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


@Disabled("where are aaru test files?")
class Crc16CcittTest {

    static final String TEST_FILES_ROOT = "src/test/resources";

    static final byte[] expectedEmpty = {
            (byte) 0xFF, (byte) 0xFF
    };
    static final byte[] expectedRandom = {
            0x36, 0x40
    };
    static final byte[] expectedRandom15 = {
            0x16, 0x6e
    };
    static final byte[] expectedRandom31 = {
            (byte) 0xd0, 0x16
    };
    static final byte[] expectedRandom63 = {
            0x73, (byte) 0xc4
    };
    static final byte[] expectedRandom2352 = {
            0x19, 0x46
    };

    @Test
    void emptyData() {
        byte[] data = new byte[1048576];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "empty"), FileMode.Open,
                FileAccess.Read);

        fs.read(data, 0, 1048576);
        fs.close();
        byte[][] result = new byte[1][];
        CRC16CCITTContext.data(data, /*out byte[]*/ result);
        assertArrayEquals(expectedEmpty, result[0]);
    }

    @Test
    void emptyFile() {
        byte[] result =
                CRC16CCITTContext.file(Path.combine(TEST_FILES_ROOT, "Checksum test files", "empty"));

        assertArrayEquals(expectedEmpty, result);
    }

    @Test
    void emptyInstance() {
        byte[] data = new byte[1048576];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "empty"), FileMode.Open,
                FileAccess.Read);

        fs.read(data, 0, 1048576);
        fs.close();
        CRC16CCITTContext ctx = new CRC16CCITTContext();
        ctx.update(data);
        byte[] result = ctx.doFinal();
        assertArrayEquals(expectedEmpty, result);
    }

    @Test
    void randomData() {
        byte[] data = new byte[1048576];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "random"),
                FileMode.Open, FileAccess.Read);

        fs.read(data, 0, 1048576);
        fs.close();
        byte[][] result = new byte[1][];
        CRC16CCITTContext.data(data, /*out byte[]*/ result);
        assertArrayEquals(expectedRandom, result[0]);
    }

    @Test
    void randomFile() {
        byte[] result =
                CRC16CCITTContext.file(Path.combine(TEST_FILES_ROOT, "Checksum test files", "random"));

        assertArrayEquals(expectedRandom, result);
    }

    @Test
    void randomInstance() {
        byte[] data = new byte[1048576];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "random"),
                FileMode.Open, FileAccess.Read);

        fs.read(data, 0, 1048576);
        fs.close();
        CRC16CCITTContext ctx = new CRC16CCITTContext();
        ctx.update(data);
        byte[] result = ctx.doFinal();
        assertArrayEquals(expectedRandom, result);
    }

    @Test
    void partialInstance15() {
        byte[] data = new byte[15];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "random"),
                FileMode.Open, FileAccess.Read);

        fs.read(data, 0, 15);
        fs.close();
        CRC16CCITTContext ctx = new CRC16CCITTContext();
        ctx.update(data);
        byte[] result = ctx.doFinal();
        assertArrayEquals(expectedRandom15, result);
    }

    @Test
    void partialInstance31() {
        byte[] data = new byte[31];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "random"),
                FileMode.Open, FileAccess.Read);

        fs.read(data, 0, 31);
        fs.close();
        CRC16CCITTContext ctx = new CRC16CCITTContext();
        ctx.update(data);
        byte[] result = ctx.doFinal();
        assertArrayEquals(expectedRandom31, result);
    }

    @Test
    void partialInstance63() {
        byte[] data = new byte[63];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "random"),
                FileMode.Open, FileAccess.Read);

        fs.read(data, 0, 63);
        fs.close();
        CRC16CCITTContext ctx = new CRC16CCITTContext();
        ctx.update(data);
        byte[] result = ctx.doFinal();
        assertArrayEquals(expectedRandom63, result);
    }

    @Test
    void partialInstance2352() {
        byte[] data = new byte[2352];

        FileStream fs = new FileStream(Path.combine(TEST_FILES_ROOT, "Checksum test files", "random"),
                FileMode.Open, FileAccess.Read);

        fs.read(data, 0, 2352);
        fs.close();
        CRC16CCITTContext ctx = new CRC16CCITTContext();
        ctx.update(data);
        byte[] result = ctx.doFinal();
        assertArrayEquals(expectedRandom2352, result);
    }
}
