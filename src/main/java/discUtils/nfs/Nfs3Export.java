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

package discUtils.nfs;

import java.util.ArrayList;
import java.util.List;


public final class Nfs3Export {
    public Nfs3Export(XdrDataReader reader) {
        setDirPath(reader.readString(Nfs3Mount.MaxPathLength));
        List<String> groups = new ArrayList<>();
        while (reader.readBool()) {
            groups.add(reader.readString(Nfs3Mount.MaxNameLength));
        }
        setGroups(groups);
    }

    public Nfs3Export() {
    }

    private String dirPath;

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String value) {
        dirPath = value;
    }

    private List<String> groups;

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> value) {
        groups = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getDirPath());
        for (String group : getGroups()) {
            writer.write(true);
            writer.write(group);
        }
        writer.write(false);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3Export ? (Nfs3Export) obj : null);
    }

    public boolean equals(Nfs3Export other) {
        if (other == null) {
            return false;
        }

        if (!other.getDirPath().equals(getDirPath())) {
            return false;
        }

        if (other.getGroups() == null || getGroups() == null) {
            return false;
        }

        for (int i = 0; i < getGroups().size(); i++) {
            if (!other.getGroups().get(i).equals(getGroups().get(i))) {
                return false;
            }

        }
        return true;
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(getDirPath(), getGroups());
    }
}
