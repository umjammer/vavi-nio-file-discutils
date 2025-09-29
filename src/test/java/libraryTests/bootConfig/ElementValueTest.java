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

package libraryTests.bootConfig;

import java.util.UUID;

import discUtils.bootConfig.BcdObject;
import discUtils.bootConfig.Element;
import discUtils.bootConfig.ElementValue;
import discUtils.bootConfig.InheritType;
import discUtils.bootConfig.Store;
import discUtils.bootConfig.WellKnownElement;
import discUtils.core.Geometry;
import discUtils.core.VolumeManager;
import discUtils.core.partitions.BiosPartitionTable;
import discUtils.core.partitions.GuidPartitionTable;
import discUtils.core.partitions.WellKnownPartitionType;
import discUtils.registry.RegistryHive;
import discUtils.streams.SparseMemoryStream;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class ElementValueTest {

    private static final String FS = java.io.File.separator;

    @Test
    public void stringValue() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.LibraryApplicationPath, ElementValue.forString("a" + FS + "path" + FS + "to" + FS + "nowhere"));
        el = obj.getElement(WellKnownElement.LibraryApplicationPath);
        assertEquals("a" + FS + "path" + FS + "to" + FS + "nowhere", el.getValue().toString());
    }

    @Test
    public void booleanValue() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.LibraryAutoRecoveryEnabled, ElementValue.forBoolean(true));
        el = obj.getElement(WellKnownElement.LibraryAutoRecoveryEnabled);
        assertEquals(Boolean.TRUE.toString(), el.getValue().toString());
    }

    @Test
    public void deviceValue_Gpt() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(80 * 1024 * 1024);
        GuidPartitionTable gpt = GuidPartitionTable.initialize(ms, Geometry.fromCapacity(ms.getLength()));
        gpt.create(WellKnownPartitionType.WindowsNtfs, true);
        VolumeManager volMgr = new VolumeManager(ms);
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.LibraryApplicationDevice,
                                    ElementValue.forDevice(new UUID(0, 0), volMgr.getPhysicalVolumes().get(0)));
        el = obj.getElement(WellKnownElement.LibraryApplicationDevice);
        assertNotNull(el.getValue().toString());
        assertFalse(el.getValue().toString().isEmpty());
    }

    @Test
    public void deviceValue_Mbr() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(80 * 1024 * 1024);
        BiosPartitionTable pt = BiosPartitionTable.initialize(ms, Geometry.fromCapacity(ms.getLength()));
        pt.create(WellKnownPartitionType.WindowsNtfs, true);
        VolumeManager volMgr = new VolumeManager(ms);
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.LibraryApplicationDevice,
                                    ElementValue.forDevice(new UUID(0, 0), volMgr.getPhysicalVolumes().get(0)));
        el = obj.getElement(WellKnownElement.LibraryApplicationDevice);
        assertNotNull(el.getValue().toString());
        assertFalse(el.getValue().toString().isEmpty());
    }

    @Test
    public void deviceValue_BootDevice() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.LibraryApplicationDevice, ElementValue.forBootDevice());
        el = obj.getElement(WellKnownElement.LibraryApplicationDevice);
        assertNotNull(el.getValue().toString());
        assertFalse(el.getValue().toString().isEmpty());
    }

    @Test
    public void guidValue() throws Exception {
        UUID testGuid = UUID.randomUUID();
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.BootMgrDefaultObject, ElementValue.forGuid(testGuid));
        el = obj.getElement(WellKnownElement.BootMgrDefaultObject);
        assertEquals("{%s}".formatted(testGuid), el.getValue().toString());
    }

    @Test
    public void guidListValue() throws Exception {
        UUID testGuid1 = UUID.randomUUID();
        UUID testGuid2 = UUID.randomUUID();
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.BootMgrDisplayOrder,
                                    ElementValue.forGuidList(new UUID[] {
                                        testGuid1, testGuid2
                                    }));
        el = obj.getElement(WellKnownElement.BootMgrDisplayOrder);
        assertEquals("{%s}".formatted(testGuid1) + "," + "{%s}".formatted(testGuid2), el.getValue().toString());
    }

    @Test
    public void integerValue() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.LibraryTruncatePhysicalMemory, ElementValue.forInteger(1234));
        el = obj.getElement(WellKnownElement.LibraryTruncatePhysicalMemory);
        assertEquals("1234", el.getValue().toString());
    }

    @Test
    public void integerListValue() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        Element el = obj.addElement(WellKnownElement.LibraryBadMemoryList,
                                    ElementValue.forIntegerList(new long[] {
                                        1234, 4132
                                    }));
        el = obj.getElement(WellKnownElement.LibraryBadMemoryList);
        assertNotNull(el.getValue().toString());
        assertFalse(el.getValue().toString().isEmpty());
    }
}
