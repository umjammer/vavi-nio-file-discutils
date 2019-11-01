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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import DiscUtils.Registry.RegistryHive;
import DiscUtils.Registry.RegistryKey;
import DiscUtils.Registry.RegistryValueType;
import dotnet4j.io.MemoryStream;

public class RegistryKeyTest {
    private RegistryHive hive;

    public RegistryKeyTest() {
        hive = RegistryHive.create(new MemoryStream());
    }

    @Test
    public void setDefaultValue() throws Exception {
        hive.getRoot().setValue("", "A default value");
        assertEquals("A default value", hive.getRoot().getValue(""));
        hive.getRoot().setValue(null, "Foobar");
        assertEquals("Foobar", hive.getRoot().getValue(null));
        hive.getRoot().setValue(null, "asdf");
        assertEquals("asdf", hive.getRoot().getValue(""));
    }

    @Test
    public void valueNameCaseSensitivity() throws Exception {
        hive.getRoot().setValue("nAmE", "value");
        assertEquals("value", hive.getRoot().getValue("NaMe"));
        hive.getRoot().setValue("moreThanFourCharName", "foo");
        assertEquals("foo", hive.getRoot().getValue("moretHANfOURcHARnAME"));
        assertEquals(2, hive.getRoot().getValueCount());
        hive.getRoot().setValue("NaMe", "newvalue");
        assertEquals(2, hive.getRoot().getValueCount());
        assertEquals("newvalue", hive.getRoot().getValue("NaMe"));
    }

    @Test
    public void setLargeValue() throws Exception {
        byte[] buffer = new byte[64 * 1024];
        buffer[5232] = (byte) 0xAD;
        hive.getRoot().setValue("bigvalue", buffer);
        byte[] readVal = (byte[]) hive.getRoot().getValue("bigvalue");
        assertEquals(buffer.length, readVal.length);
        assertEquals((byte) 0xAD, readVal[5232]);
    }

    @Test
    public void setStringValue() throws Exception {
        hive.getRoot().setValue("value", "string");
        assertEquals(RegistryValueType.String, hive.getRoot().getValueType("value"));
        assertEquals("string", hive.getRoot().getValue("value"));
        hive.getRoot().setValue("emptyvalue", "");
        assertEquals(RegistryValueType.String, hive.getRoot().getValueType("emptyvalue"));
        assertEquals("", hive.getRoot().getValue("emptyvalue"));
    }

    @Test
    public void setIntegerValue() throws Exception {
        hive.getRoot().setValue("value", 0x7342BEEF);
        assertEquals(RegistryValueType.Dword, hive.getRoot().getValueType("value"));
        assertEquals(0x7342BEEF, (int) hive.getRoot().getValue("value"));
    }

    @Test
    public void setByteArrayValue() throws Exception {
        hive.getRoot().setValue("value",
                           new byte[] {
                               1, 2, 3, 4
                           });
        assertEquals(RegistryValueType.Binary, hive.getRoot().getValueType("value"));
        byte[] readVal = (byte[]) hive.getRoot().getValue("value");
        assertEquals(4, readVal.length);
        assertEquals(3, readVal[2]);
    }

    @Test
    public void setStringArrayValue() throws Exception {
        hive.getRoot().setValue("value",
                           new String[] {
                               "A", "B", "C"
                           });
        assertEquals(RegistryValueType.MultiString, hive.getRoot().getValueType("value"));
        String[] readVal = (String[]) hive.getRoot().getValue("value");
        assertEquals(3, readVal.length);
        assertEquals("C", readVal[2]);
    }

    @Test
    public void setEnvStringValue() throws Exception {
        hive.getRoot().setValue("value", "string", RegistryValueType.ExpandString);
        assertEquals(RegistryValueType.ExpandString, hive.getRoot().getValueType("value"));
        assertEquals("string", hive.getRoot().getValue("value"));
        hive.getRoot().setValue("value", "str%HOME%ing", RegistryValueType.ExpandString);
        assertEquals(RegistryValueType.ExpandString, hive.getRoot().getValueType("value"));
        assertEquals("str" + System.getenv("HOME") + "ing", hive.getRoot().getValue("value"));
        hive.getRoot().setValue("emptyvalue", "", RegistryValueType.ExpandString);
        assertEquals(RegistryValueType.ExpandString, hive.getRoot().getValueType("emptyvalue"));
        assertEquals("", hive.getRoot().getValue("emptyvalue"));
    }

    @Test
    public void deleteValue() throws Exception {
        hive.getRoot().setValue("aValue", "value");
        hive.getRoot().setValue("nAmE", "value");
        hive.getRoot().setValue("otherValue", "value");
        assertEquals(3, hive.getRoot().getValueCount());
        hive.getRoot().deleteValue("NaMe");
        assertEquals(2, hive.getRoot().getValueCount());
    }

    @Test
    public void deleteOnlyValue() throws Exception {
        hive.getRoot().setValue("nAmE", "value");
        assertEquals(1, hive.getRoot().getValueCount());
        hive.getRoot().deleteValue("NaMe");
        assertEquals(0, hive.getRoot().getValueCount());
    }

    @Test
    public void deleteDefaultValue() throws Exception {
        hive.getRoot().setValue("", "value");
        assertEquals(1, hive.getRoot().getValueCount());
        hive.getRoot().deleteValue(null);
        assertEquals(0, hive.getRoot().getValueCount());
    }

    @Test
    public void enumerateValues() throws Exception {
        hive.getRoot().setValue("C", "");
        hive.getRoot().setValue("A", "");
        hive.getRoot().setValue("B", "");
        List<String> names = hive.getRoot().getValueNames();
        assertEquals(3, names.size());
        assertEquals("A", names.get(0));
        assertEquals("B", names.get(1));
        assertEquals("C", names.get(2));
    }

    @Test
    public void createKey() throws Exception {
        RegistryKey newKey = hive.getRoot().createSubKey("Child\\Grandchild");
        assertNotNull(newKey);
        assertEquals(1, hive.getRoot().getSubKeyCount());
        assertEquals(1, hive.getRoot().openSubKey("cHiLd").getSubKeyCount());
    }

    @Test
    public void createExistingKey() throws Exception {
        RegistryKey newKey = hive.getRoot().createSubKey("Child");
        assertNotNull(newKey);
        assertEquals(1, hive.getRoot().getSubKeyCount());
        newKey = hive.getRoot().createSubKey("cHILD");
        assertNotNull(newKey);
        assertEquals(1, hive.getRoot().getSubKeyCount());
    }

    @Test
    public void deleteKey() throws Exception {
        RegistryKey newKey = hive.getRoot().createSubKey("Child");
        hive.getRoot().openSubKey("Child").setValue("value", "a value");
        assertEquals(1, hive.getRoot().getSubKeyCount());
        hive.getRoot().deleteSubKey("cHiLd");
        assertEquals(0, hive.getRoot().getSubKeyCount());
    }

    @Test
    public void deleteNonEmptyKey() throws Exception {
        RegistryKey newKey = hive.getRoot().createSubKey("Child\\Grandchild");
        assertThrows(UnsupportedOperationException.class, () -> {
            hive.getRoot().deleteSubKey("Child");
        });
    }

    @Test
    public void deleteKeyTree() throws Exception {
        RegistryKey newKey = hive.getRoot().createSubKey("Child\\Grandchild");
        assertEquals(1, hive.getRoot().getSubKeyCount());
        hive.getRoot().deleteSubKeyTree("cHiLd");
        assertEquals(0, hive.getRoot().getSubKeyCount());
    }

    @Test
    public void enumerateSubKeys() throws Exception {
        hive.getRoot().createSubKey("C");
        hive.getRoot().createSubKey("A");
        hive.getRoot().createSubKey("B");
        List<String> names = hive.getRoot().getSubKeyNames();
        assertEquals(3, names.size());
        assertEquals("A", names.get(0));
        assertEquals("B", names.get(1));
        assertEquals("C", names.get(2));
    }
}
