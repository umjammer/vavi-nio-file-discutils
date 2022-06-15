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

package LibraryTests.Registry;

import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import DiscUtils.Registry.RegistryHive;
import DiscUtils.Registry.RegistryKey;
import DiscUtils.Registry.RegistryKeyFlags;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.security.accessControl.AccessControlSections;


public class RegistryHiveTest {
    @Test
    @DisplayName("Issue #14")
    public void create() throws Exception {
        MemoryStream ms = new MemoryStream();
        RegistryHive hive = RegistryHive.create(ms);
        assertNull(hive.getRoot().getParent());
        assertEquals(0, hive.getRoot().getValueCount());
        assertEquals(0, hive.getRoot().getSubKeyCount());
        assertNotNull(hive.getRoot().getSubKeys());
        assertEquals("O:BAG:BAD:PAI(A;;KA;;;SY)(A;CI;KA;;;BA)",
                     hive.getRoot().getAccessControl().getSecurityDescriptorSddlForm(AccessControlSections.All));
        assertEquals(EnumSet.of(RegistryKeyFlags.Root, RegistryKeyFlags.Normal), hive.getRoot().getFlags());
    }

    @Test
    public void create_Null() throws Exception {
        assertThrows(NullPointerException.class, () -> RegistryHive.create((Stream) null));
    }

    /**
     * @param args 0: registry file
     */
    public static void main(String[] args) throws Exception {
        RegistryHive hive = new RegistryHive(new FileStream(args[0], FileMode.Open));
        hive.getRoot().getSubKeys().forEach(RegistryHiveTest::dump);
    }

    /** dump keys */
    private static void dump(RegistryKey key) {
        System.err.println(key);
        key.getSubKeys().forEach(RegistryHiveTest::dump);
    }
}
