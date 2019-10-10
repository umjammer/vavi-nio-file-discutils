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

package DiscUtils.BootConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import DiscUtils.Registry.RegistryKey;
import DiscUtils.Registry.RegistryValueType;


public class DiscUtilsRegistryStorage extends BaseStorage {

    private static final String FS = File.separator;

    private static final String ElementsPathTemplate = "Objects" + FS +"s%" + "Elements";

    private static final String ElementPathTemplate = "Objects" + FS +"%s" + FS +"Elements" + FS + "%8X";

    private static final String ObjectTypePathTemplate = "Objects" + FS + "%s" + FS + "Description";

    private static final String ObjectsPath = "Objects";

    private final RegistryKey _rootKey;

    public DiscUtilsRegistryStorage(RegistryKey key) {
        _rootKey = key;
    }

    public String getString(UUID obj, int element) {
        return getValue(obj, element) instanceof String ? (String) getValue(obj, element) : (String) null;
    }

    public void setString(UUID obj, int element, String value) {
        setValue(obj, element, value);
    }

    public byte[] getBinary(UUID obj, int element) {
        return getValue(obj, element) instanceof byte[] ? (byte[]) getValue(obj, element) : (byte[]) null;
    }

    public void setBinary(UUID obj, int element, byte[] value) {
        setValue(obj, element, value);
    }

    public String[] getMultiString(UUID obj, int element) {
        return getValue(obj, element) instanceof String[] ? (String[]) getValue(obj, element) : (String[]) null;
    }

    public void setMultiString(UUID obj, int element, String[] values) {
        setValue(obj, element, values);
    }

    public List<UUID> enumerateObjects() {
        List<UUID> result = new ArrayList<>();
        RegistryKey parentKey = _rootKey.openSubKey("Objects");
        for (Object __dummyForeachVar0 : parentKey.getSubKeyNames()) {
            String key = (String) __dummyForeachVar0;
            result.add(UUID.fromString(key));
        }
        return result;
    }

    public List<Integer> enumerateElements(UUID obj) {
        List<Integer> result = new ArrayList<>();
        String path = String.format(ElementsPathTemplate, String.format("%d", obj)); // TODO "B"
        RegistryKey parentKey = _rootKey.openSubKey(path);
        for (Object __dummyForeachVar1 : parentKey.getSubKeyNames())
        {
            String key = (String)__dummyForeachVar1;
            result.add(Integer.parseInt(key, 16));
        }
        return result;
    }

    public int getObjectType(UUID obj) {
        String path = String.format(ObjectTypePathTemplate, String.format("%d", obj)); // TODO "B"
        RegistryKey descKey = _rootKey.openSubKey(path);
        Object val = descKey.getValue("Type");
        return (Integer) val;
    }

    public boolean hasValue(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("%d", obj), element); // TODO "B"
        return _rootKey.openSubKey(path) != null;
    }

    public boolean objectExists(UUID obj) {
        String path = String.format(ObjectTypePathTemplate, String.format("%d", obj)); // TODO "B"
        return _rootKey.openSubKey(path) != null;
    }

    public UUID createObject(UUID obj, int type) {
        UUID realObj = obj == new UUID(0, 0) ? UUID.randomUUID() : obj;
        String path = String.format(ObjectTypePathTemplate, String.format("%d", realObj)); // TODO "B"
        RegistryKey key = _rootKey.createSubKey(path);
        key.setValue("Type", type, RegistryValueType.Dword);
        return realObj;
    }

    public void createElement(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("%d", obj), element); // TODO "B"
        _rootKey.createSubKey(path);
    }

    public void deleteObject(UUID obj) {
        String path = String.format(ObjectTypePathTemplate, String.format("%d", obj)); // TODO "B"
        _rootKey.deleteSubKeyTree(path);
    }

    public void deleteElement(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("%d", obj), element); // TODO "B"
        _rootKey.deleteSubKeyTree(path);
    }

    private Object getValue(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("%d", obj), element); // TODO "B"
        RegistryKey key = _rootKey.openSubKey(path);
        return key.getValue("Element");
    }

    private void setValue(UUID obj, int element, Object value) {
        String path = String.format(ElementPathTemplate, String.format("%d", obj), element); // TODO "B"
        RegistryKey key = _rootKey.openSubKey(path);
        key.setValue("Element", value);
    }
}
