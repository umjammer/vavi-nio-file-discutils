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

import discUtils.bootConfig.ApplicationImageType;
import discUtils.bootConfig.ApplicationType;
import discUtils.bootConfig.BcdObject;
import discUtils.bootConfig.InheritType;
import discUtils.bootConfig.ObjectType;
import discUtils.bootConfig.Store;
import discUtils.registry.RegistryHive;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StoreTest {
    @Test
    public void initialize() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        int i = 0;
        for (BcdObject obj : s.getObjects()) {
            ++i;
        }
        assertEquals(0, i);
    }

    @Test
    public void createApplication() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createApplication(ApplicationImageType.WindowsBoot, ApplicationType.BootManager);
        assertNotEquals(new UUID(0, 0), obj.getIdentity());
        assertEquals(ObjectType.Application, obj.getObjectType());
        BcdObject reGet = s.getObject(obj.getIdentity());
        assertEquals(obj.getIdentity(), reGet.getIdentity());
    }

    @Test
    public void createDevice() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createDevice();
        assertNotEquals(new UUID(0, 0), obj.getIdentity());
        assertEquals(ObjectType.Device, obj.getObjectType());
        BcdObject reGet = s.getObject(obj.getIdentity());
        assertEquals(obj.getIdentity(), reGet.getIdentity());
    }

    @Test
    public void createInherit() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.ApplicationObjects);
        assertNotEquals(new UUID(0, 0), obj.getIdentity());
        assertEquals(ObjectType.Inherit, obj.getObjectType());
        assertTrue(obj.isInheritableBy(ObjectType.Application));
        assertFalse(obj.isInheritableBy(ObjectType.Device));
        BcdObject reGet = s.getObject(obj.getIdentity());
        assertEquals(obj.getIdentity(), reGet.getIdentity());
    }

    @Test
    public void removeObject() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        s.removeObject(obj.getIdentity());
    }

    @Test
    public void removeObject_NonExistent() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        s.removeObject(UUID.randomUUID());
    }
}
