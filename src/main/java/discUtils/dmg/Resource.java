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

package discUtils.dmg;

import java.util.Map;


public abstract class Resource {

    protected Resource(String type, Map<String, Object> parts) {
        this.type = type;
        name = parts.get("Name") instanceof String ? (String) parts.get("Name") : null;
        String idStr = parts.get("ID") instanceof String ? (String) parts.get("ID") : null;
        if (idStr != null && !idStr.isEmpty()) {
            try {
                int id = Integer.parseInt(idStr);
                setId(id);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid ID field", e);
            }
        }

        String attrString = parts.get("Attributes") instanceof String ? (String) parts.get("Attributes") : null;
        if (attrString != null && !attrString.isEmpty()) {
            int style = 10;
            if (attrString.startsWith("0x")) {
                style = 16;
                attrString = attrString.substring(2);
            }

            try {
                int attributes = Integer.parseInt(attrString, style);
                setAttributes(attributes);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid Attributes field", e);
            }
        }
    }

    private int attributes;

    public int getAttributes() {
        return attributes;
    }

    public void setAttributes(int value) {
        attributes = value;
    }

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int value) {
        id = value;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    private final String type;

    public String getType() {
        return type;
    }

    public static Resource fromPlist(String type, Map<String, Object> parts) {
        if (type.equals("blkx")) {
            return new BlkxResource(parts);
        } else {
            return new GenericResource(type, parts);
        }
    }
}
