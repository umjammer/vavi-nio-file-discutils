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

package LibraryTests.Xva;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Xva.Disk;
import DiscUtils.Xva.VirtualMachine;
import DiscUtils.Xva.VirtualMachineBuilder;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.Stream;


public class VirtualMachineBuilderTest {
    @Test
    public void testEmpty() throws Exception {
        MemoryStream xvaStream = new MemoryStream();
        VirtualMachineBuilder vmb = new VirtualMachineBuilder();
        vmb.addDisk("Foo", new MemoryStream(), Ownership.Dispose);
        vmb.build(xvaStream);
        assertNotEquals(0, xvaStream.getLength());
        VirtualMachine vm = new VirtualMachine(xvaStream);
        List<Disk> disks = new ArrayList<>(vm.getDisks());
        assertEquals(1, disks.size());
        assertEquals(0, disks.get(0).getCapacity());
    }

    @Test
    public void testNotEmpty() throws Exception {
        MemoryStream xvaStream = new MemoryStream();
        VirtualMachineBuilder vmb = new VirtualMachineBuilder();
        MemoryStream ms = new MemoryStream();
        for (int i = 0; i < 1024 * 1024; ++i) {
            ms.setPosition(i * 10);
            ms.writeByte((byte) (i ^ (i >>> 8) ^ (i >>> 16) ^ (i >>> 24)));
        }
        vmb.addDisk("Foo", ms, Ownership.Dispose);
        vmb.build(xvaStream);
        assertNotEquals(0, xvaStream.getLength());
        VirtualMachine vm = new VirtualMachine(xvaStream);
        List<Disk> disks = new ArrayList<>(vm.getDisks());
        assertEquals(1, disks.size());
        assertEquals(10 * 1024 * 1024, disks.get(0).getCapacity());
        Stream diskContent = disks.get(0).getContent();
        for (int i = 0; i < 1024 * 1024; ++i) {
            diskContent.setPosition(i * 10);
            if ((byte) (i ^ (i >>> 8) ^ (i >>> 16) ^ (i >>> 24)) != diskContent.readByte()) {
                assertTrue(false, "Mismatch at offset " + i);
            }
        }
    }
}
