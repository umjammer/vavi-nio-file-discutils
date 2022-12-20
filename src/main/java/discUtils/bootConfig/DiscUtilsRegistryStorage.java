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

package discUtils.bootConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import discUtils.registry.RegistryKey;
import discUtils.registry.RegistryValueType;


public class DiscUtilsRegistryStorage extends BaseStorage {

    private static final String FS = java.io.File.separator;

    private static final UUID EMPTY = new UUID(0, 0);

    private static final String ElementsPathTemplate = "Objects" + FS + "%s" + FS + "Elements";

    private static final String ElementPathTemplate = "Objects" + FS + "%s" + FS + "Elements" + FS + "%8X";

    private static final String ObjectTypePathTemplate = "Objects" + FS + "%s" + FS + "description";

    private static final String ObjectsPath = "Objects";

    private final RegistryKey rootKey;

    public DiscUtilsRegistryStorage(RegistryKey key) {
        rootKey = key;
    }

    @Override public String getString(UUID obj, int element) {
        Object r = getValue(obj, element);
        return r instanceof String ? (String) r : null;
    }

    @Override public void setString(UUID obj, int element, String value) {
        setValue(obj, element, value);
    }

    @Override public byte[] getBinary(UUID obj, int element) {
        Object r = getValue(obj, element);
        return r instanceof byte[] ? (byte[]) r : null;
    }

    @Override public void setBinary(UUID obj, int element, byte[] value) {
        setValue(obj, element, value);
    }

    @Override public String[] getMultiString(UUID obj, int element) {
        Object r = getValue(obj, element);
        return r instanceof String[] ? (String[]) r : null;
    }

    @Override public void setMultiString(UUID obj, int element, String[] values) {
        setValue(obj, element, values);
    }

    @Override public List<UUID> enumerateObjects() {
        List<UUID> result = new ArrayList<>();
        RegistryKey parentKey = rootKey.openSubKey(ObjectsPath);
        for (String key : parentKey.getSubKeyNames()) {
            result.add(UUID.fromString(key.replaceAll("(^\\{|\\}$)", "")));
        }
        return result;
    }

    @Override public List<Integer> enumerateElements(UUID obj) {
        List<Integer> result = new ArrayList<>();
        String path = String.format(ElementsPathTemplate, String.format("{%s}", obj));
        RegistryKey parentKey = rootKey.openSubKey(path);
        for (String key : parentKey.getSubKeyNames()) {
            result.add(Integer.parseInt(key, 16));
        }
        return result;
    }

    @Override public int getObjectType(UUID obj) {
        String path = String.format(ObjectTypePathTemplate, String.format("{%s}", obj));
        RegistryKey descKey = rootKey.openSubKey(path);
        Object val = descKey.getValue("Type");
        return val == null ? 0 : (Integer) val;
    }

    @Override public boolean hasValue(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("{%s}", obj), element);
        return rootKey.openSubKey(path) != null;
    }

    @Override public boolean objectExists(UUID obj) {
        String path = String.format(ObjectTypePathTemplate, String.format("{%s}", obj));
        return rootKey.openSubKey(path) != null;
    }

    @Override public UUID createObject(UUID obj, int type) {
        UUID realObj = obj.equals(EMPTY) ? UUID.randomUUID() : obj;
        String path = String.format(ObjectTypePathTemplate, String.format("{%s}", realObj));
        RegistryKey key = rootKey.createSubKey(path);
        key.setValue("Type", type, RegistryValueType.Dword);
        return realObj;
    }

    @Override public void createElement(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("{%s}", obj), element);
        rootKey.createSubKey(path);
    }

    @Override public void deleteObject(UUID obj) {
        String path = String.format(ObjectTypePathTemplate, String.format("{%s}", obj));
        rootKey.deleteSubKeyTree(path);
    }

    @Override public void deleteElement(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("{%s}", obj), element);
        rootKey.deleteSubKeyTree(path);
    }

    private Object getValue(UUID obj, int element) {
        String path = String.format(ElementPathTemplate, String.format("{%s}", obj), element);
        RegistryKey key = rootKey.openSubKey(path);
        return key.getValue("Element");
    }

    private void setValue(UUID obj, int element, Object value) {
        String path = String.format(ElementPathTemplate, String.format("{%s}", obj), element);
        RegistryKey key = rootKey.openSubKey(path);
        key.setValue("Element", value);
    }
}
