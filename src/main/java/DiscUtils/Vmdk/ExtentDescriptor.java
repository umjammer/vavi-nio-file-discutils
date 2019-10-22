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

import java.util.ArrayList;
import java.util.List;


public class ExtentDescriptor {
    public ExtentDescriptor() {
    }

    public ExtentDescriptor(ExtentAccess access, long size, ExtentType type, String fileName, long offset) {
        setAccess(access);
        setSizeInSectors(size);
        setType(type);
        setFileName(fileName);
        setOffset(offset);
    }

    private ExtentAccess __Access = ExtentAccess.None;

    public ExtentAccess getAccess() {
        return __Access;
    }

    public void setAccess(ExtentAccess value) {
        __Access = value;
    }

    private String __FileName;

    public String getFileName() {
        return __FileName;
    }

    public void setFileName(String value) {
        __FileName = value;
    }

    private long __Offset;

    public long getOffset() {
        return __Offset;
    }

    public void setOffset(long value) {
        __Offset = value;
    }

    private long __SizeInSectors;

    public long getSizeInSectors() {
        return __SizeInSectors;
    }

    public void setSizeInSectors(long value) {
        __SizeInSectors = value;
    }

    private ExtentType __Type = ExtentType.Flat;

    public ExtentType getType() {
        return __Type;
    }

    public void setType(ExtentType value) {
        __Type = value;
    }

    public static ExtentDescriptor parse(String descriptor) {
        List<String> elems = splitQuotedString(descriptor);
        if (elems.size() < 4) {
            throw new moe.yo3explorer.dotnetio4j.IOException(String.format("Invalid extent descriptor: %s", descriptor));
        }

        ExtentDescriptor result = new ExtentDescriptor();
        result.setAccess(parseAccess(elems.get(0)));
        result.setSizeInSectors(Long.parseLong(elems.get(1)));
        result.setType(parseType(elems.get(2)));
        result.setFileName(elems.get(3).replaceFirst("^\"", "").replaceFirst("\"$", ""));
        if (elems.size() > 4) {
            result.setOffset(Long.parseLong(elems.get(4)));
        }

        return result;
    }

    public static ExtentAccess parseAccess(String access) {
        if ("NOACCESS".equals(access)) {
            return ExtentAccess.None;
        }

        if ("RDONLY".equals(access)) {
            return ExtentAccess.ReadOnly;
        }

        if ("RW".equals(access)) {
            return ExtentAccess.ReadWrite;
        }

        throw new IllegalArgumentException("Unknown access type");
    }

    public static String formatAccess(ExtentAccess access) {
        switch (access) {
        case None:
            return "NOACCESS";
        case ReadOnly:
            return "RDONLY";
        case ReadWrite:
            return "RW";
        default:
            throw new IllegalArgumentException("Unknown access type");

        }
    }

    public static ExtentType parseType(String type) {
        if ("FLAT".equals(type)) {
            return ExtentType.Flat;
        }

        if ("SPARSE".equals(type)) {
            return ExtentType.Sparse;
        }

        if ("ZERO".equals(type)) {
            return ExtentType.Zero;
        }

        if ("VMFS".equals(type)) {
            return ExtentType.Vmfs;
        }

        if ("VMFSSPARSE".equals(type)) {
            return ExtentType.VmfsSparse;
        }

        if ("VMFSRDM".equals(type)) {
            return ExtentType.VmfsRdm;
        }

        if ("VMFSRAW".equals(type)) {
            return ExtentType.VmfsRaw;
        }

        throw new IllegalArgumentException("Unknown extent type");
    }

    public static String formatExtentType(ExtentType type) {
        switch (type) {
        case Flat:
            return "FLAT";
        case Sparse:
            return "SPARSE";
        case Zero:
            return "ZERO";
        case Vmfs:
            return "VMFS";
        case VmfsSparse:
            return "VMFSSPARSE";
        case VmfsRdm:
            return "VMFSRDM";
        case VmfsRaw:
            return "VMFSRAW";
        default:
            throw new IllegalArgumentException("Unknown extent type");

        }
    }

    public String toString() {
        try {
            String basic = formatAccess(getAccess()) + " " + getSizeInSectors() + " " + formatExtentType(getType()) + " \"" +
                           getFileName() + "\"";
            if (getType() != ExtentType.Sparse && getType() != ExtentType.VmfsSparse && getType() != ExtentType.Zero) {
                return basic + " " + getOffset();
            }

            return basic;
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    private static List<String> splitQuotedString(String source) {
        List<String> result = new ArrayList<>();
        int idx = 0;
        while (idx < source.length()) {
            while (source.charAt(idx) == ' ' && idx < source.length()) {
                // Skip spaces
                idx++;
            }
            if (source.charAt(idx) == '"') {
                // A quoted value, find end of quotes...
                int start = idx;
                idx++;
                while (idx < source.length() && source.charAt(idx) != '"') {
                    idx++;
                }
                result.add(source.substring(start, idx + 1));
            } else {
                // An unquoted value, find end of value
                int start = idx;
                idx++;
                while (idx < source.length() && source.charAt(idx) != ' ') {
                    idx++;
                }
                result.add(source.substring(start, idx));
            }
            idx++;
        }
        return result;
    }

}
