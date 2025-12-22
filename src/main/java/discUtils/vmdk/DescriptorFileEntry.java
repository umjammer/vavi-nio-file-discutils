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

package discUtils.vmdk;

public class DescriptorFileEntry {

    private final DescriptorFileEntryType type;

    public DescriptorFileEntry(String key, String value, DescriptorFileEntryType type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    private final String key;

    public String getKey() {
        return key;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
        return toString(true);
    }

    public String toString(boolean spaceOut) {
        // VMware workstation appears to be sensitive to spaces, wants them for 'header' values, not for DiskDataBase...
        String sep = spaceOut ? " " : "";
        return switch (type) {
            case NoValue -> getKey();
            case Plain -> getKey() + sep + "=" + sep + getValue();
            case Quoted -> getKey() + sep + "=" + sep + "\"" + getValue() + "\"";
        };
    }
}
