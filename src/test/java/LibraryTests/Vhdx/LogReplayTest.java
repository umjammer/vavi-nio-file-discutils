//
// Copyright (c) 2017 Bianco Veigel
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

package LibraryTests.Vhdx;

import java.io.File;
import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Vhdx.Disk;
import DiscUtils.Vhdx.DiskImageFile;
import LibraryTests.Utilities.ZipUtilities;
import dotnet4j.io.Stream;


public class LogReplayTest {
    @Test
    public void replayLog() throws Exception {
        File fs = new File(URI.create(getClass().getResource("vhdx-log-replay.zip").toString()));
        try (Stream vhdx = ZipUtilities.readFileFromZip(fs, null);
                DiskImageFile diskImage = new DiskImageFile(vhdx, Ownership.Dispose);
                Disk disk = new Disk(Collections.singletonList(diskImage), Ownership.Dispose)) {
            assertTrue(disk.isPartitioned());
            assertEquals(2, disk.getPartitions().getCount());
        }
    }
}
