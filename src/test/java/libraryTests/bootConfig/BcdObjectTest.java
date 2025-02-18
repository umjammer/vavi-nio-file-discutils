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

import discUtils.bootConfig.BcdObject;
import discUtils.bootConfig.ElementValue;
import discUtils.bootConfig.InheritType;
import discUtils.bootConfig.Store;
import discUtils.bootConfig.WellKnownElement;
import discUtils.registry.RegistryHive;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BcdObjectTest {

    private static final String FS = java.io.File.separator;

    @Test
    public void addElement() {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        assertFalse(obj.hasElement(WellKnownElement.LibraryApplicationPath));
        obj.addElement(WellKnownElement.LibraryApplicationPath,
                ElementValue.forString(FS + "a" + FS + "path" + FS + "to" + FS + "nowhere"));
        assertTrue(obj.hasElement(WellKnownElement.LibraryApplicationPath));
        assertEquals(FS + "a" + FS + "path" + FS + "to" + FS + "nowhere", obj.getElement(WellKnownElement.LibraryApplicationPath).getValue().toString());
    }

    @Test
    public void addElement_WrongType() {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        assertThrows(IllegalArgumentException.class,
                () -> obj.addElement(WellKnownElement.LibraryApplicationDevice,
                        ElementValue.forString(FS + "a" + FS + "path" + FS + "to" + FS + "nowhere")));
    }

    @Test
    public void removeElement() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        obj.addElement(WellKnownElement.LibraryApplicationPath,
                ElementValue.forString(FS + "a" + FS + "path" + FS + "to" + FS + "nowhere"));
        obj.removeElement(WellKnownElement.LibraryApplicationPath);
        assertFalse(obj.hasElement(WellKnownElement.LibraryApplicationPath));
    }

    @Test
    public void removeElement_NonExistent() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        obj.removeElement(WellKnownElement.LibraryApplicationPath);
    }

    @Test
    public void friendlyName() throws Exception {
        RegistryHive hive = RegistryHive.create(new MemoryStream());
        Store s = Store.initialize(hive.getRoot());
        BcdObject obj = s.createInherit(InheritType.AnyObject);
        assertEquals(String.format("{%s}", obj.getIdentity()), obj.getFriendlyName());
    }
}
