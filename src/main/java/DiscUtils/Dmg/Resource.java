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

package DiscUtils.Dmg;

import java.util.Map;


public abstract class Resource {
    protected Resource(String type, Map<String, Object> parts) {
        __Type = type;
        setName(parts.get("Name") instanceof String ? (String) parts.get("Name") : (String) null);
        String idStr = parts.get("ID") instanceof String ? (String) parts.get("ID") : (String) null;
        if (idStr != null && !idStr.isEmpty()) {
            try {
                int id = Integer.parseInt(idStr);
                setId(id);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid ID field", e);
            }
        }

        String attrString = parts.get("Attributes") instanceof String ? (String) parts.get("Attributes") : (String) null;
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

    private int __Attributes;

    public int getAttributes() {
        return __Attributes;
    }

    public void setAttributes(int value) {
        __Attributes = value;
    }

    private int __Id;

    public int getId() {
        return __Id;
    }

    public void setId(int value) {
        __Id = value;
    }

    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    private String __Type;

    public String getType() {
        return __Type;
    }

    public static Resource fromPlist(String type, Map<String, Object> parts) {
        if (type.equals("blkx")) {
            return new BlkxResource(parts);
        } else {
            return new GenericResource(type, parts);
        }
    }
}
