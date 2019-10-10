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

package LibraryTests.BootConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.BootConfig.BcdObject;
import DiscUtils.BootConfig.ElementValue;
import DiscUtils.BootConfig.InheritType;
import DiscUtils.BootConfig.Store;
import DiscUtils.BootConfig.WellKnownElement;
import DiscUtils.Registry.RegistryHive;
import moe.yo3explorer.dotnetio4j.MemoryStream;

public class BcdObjectTest {
    public void addElement() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        assertFalse(obj.hasElement(WellKnownElement.LibraryApplicationPath));
        obj.addElement(WellKnownElement.LibraryApplicationPath, ElementValue.forString("\\a\\path\\to\\nowhere"));
        assertTrue(obj.hasElement(WellKnownElement.LibraryApplicationPath));
        assertEquals("\\a\\path\\to\\nowhere", obj.getElement(WellKnownElement.LibraryApplicationPath).getValue().toString());
    }

    public void addElement_WrongType() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        assertThrows(IllegalArgumentException.class, () -> {
            obj.addElement(WellKnownElement.LibraryApplicationDevice, ElementValue.forString("\\a\\path\\to\\nowhere"));
        });
    }

    public void removeElement() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        obj.addElement(WellKnownElement.LibraryApplicationPath, ElementValue.forString("\\a\\path\\to\\nowhere"));
        obj.removeElement(WellKnownElement.LibraryApplicationPath);
        assertFalse(obj.hasElement(WellKnownElement.LibraryApplicationPath));
    }

    public void removeElement_NonExistent() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        obj.removeElement(WellKnownElement.LibraryApplicationPath);
    }

    public void friendlyName() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        assertEquals(String.format("%d", obj.getIdentity()), obj.getFriendlyName()); // "B"
    }
}
