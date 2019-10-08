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

package DiscUtils.Vmdk;

public class DescriptorFileEntry {
    private final DescriptorFileEntryType _type;

    public DescriptorFileEntry(String key, String value, DescriptorFileEntryType type) {
        __Key = key;
        setValue(value);
        _type = type;
    }

    private String __Key;

    public String getKey() {
        return __Key;
    }

    private String __Value;

    public String getValue() {
        return __Value;
    }

    public void setValue(String value) {
        __Value = value;
    }

    public static DescriptorFileEntry parse(String value) {
        String[] parts = value.split("=", 2);
        for (int i = 0; i < parts.length; ++i) {
            parts[i] = parts[i].trim();
        }
        if (parts.length > 1) {
            if (parts[1].startsWith("\"")) {
                return new DescriptorFileEntry(parts[0], parts[1].replace("\"", ""), DescriptorFileEntryType.Quoted);
            }

            return new DescriptorFileEntry(parts[0], parts[1], DescriptorFileEntryType.Plain);
        }

        return new DescriptorFileEntry(parts[0], "", DescriptorFileEntryType.NoValue);
    }

    public String toString() {
        try {
            return toString(true);
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    public String toString(boolean spaceOut) {
        // VMware workstation appears to be sensitive to spaces, wants them for 'header' values, not for DiskDataBase...
        String sep = spaceOut ? " " : "";
        switch (_type) {
        case NoValue:
            return getKey();
        case Plain:
            return getKey() + sep + "=" + sep + getValue();
        case Quoted:
            return getKey() + sep + "=" + sep + "\"" + getValue() + "\"";
        default:
            throw new IllegalStateException(String.format("Unknown type: %s", _type));

        }
    }

}