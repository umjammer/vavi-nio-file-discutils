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

package LibraryTests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.ChsAddress;
import DiscUtils.Core.Geometry;

public class GeometryTest {
    @Test
    public void create() throws Exception {
        Geometry g = new Geometry(100, 16, 63);
        assertEquals(100, g.getCylinders());
        assertEquals(16, g.getHeadsPerCylinder());
        assertEquals(63, g.getSectorsPerTrack());
    }

    @Test
    public void lBARoundTrip() throws Exception {
        Geometry g = new Geometry(100, 16, 63);
        final int TestCylinder = 54;
        final int TestHead = 15;
        final int TestSector = 63;
        long lba = g.toLogicalBlockAddress(TestCylinder, TestHead, TestSector);
        ChsAddress chs = g.toChsAddress(lba);
        assertEquals(TestCylinder, chs.getCylinder());
        assertEquals(TestHead, chs.getHead());
        assertEquals(TestSector, chs.getSector());
    }

    @Test
    public void totalSectors() throws Exception {
        Geometry g = new Geometry(333, 22, 11);
        assertEquals(333 * 22 * 11, g.getTotalSectorsLong());
    }

    @Test
    public void capacity() throws Exception {
        Geometry g = new Geometry(333, 22, 11);
        assertEquals(333 * 22 * 11 * 512, g.getCapacity());
    }

    @Test
    public void fromCapacity() throws Exception {
        // Check the capacity calculated is no greater than requested, and off by no more than 10%
        final long ThreeTwentyMB = 1024 * 1024 * 320;
        Geometry g = Geometry.fromCapacity(ThreeTwentyMB);
        assertTrue(g.getCapacity() <= ThreeTwentyMB && g.getCapacity() > ThreeTwentyMB * 0.9);
        // Check exact sizes are maintained - do one pass to allow for finding a geometry that matches
        // the algorithm - then expect identical results each time.
        Geometry startGeometry = new Geometry(333, 22, 11);
        Geometry trip1 = Geometry.fromCapacity(startGeometry.getCapacity());
        assertEquals(trip1, Geometry.fromCapacity(trip1.getCapacity()));
    }

    @Test
    public void geometryEquals() throws Exception {
        assertEquals(Geometry.fromCapacity(1024 * 1024 * 32), Geometry.fromCapacity(1024 * 1024 * 32));
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("(333/22/11)", (new Geometry(333, 22, 11)).toString());
    }
}
