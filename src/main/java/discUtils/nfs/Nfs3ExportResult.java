//
// Copyright (c) 2017, Quamotion
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


public final class Nfs3ExportResult extends Nfs3CallResult {

    public Nfs3ExportResult(XdrDataReader reader) {
        exports = new ArrayList<>();
        while (reader.readBool()) {
            exports.add(new Nfs3Export(reader));
        }
    }

    public Nfs3ExportResult() {
    }

    private List<Nfs3Export> exports;

    public List<Nfs3Export> getExports() {
        return exports;
    }

    public void setExports(List<Nfs3Export> value) {
        exports = value;
    }

    public void write(XdrDataWriter writer) {
        for (Nfs3Export export : exports) {
            writer.write(true);
            export.write(writer);
        }
        writer.write(false);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3ExportResult ? (Nfs3ExportResult) obj : null);
    }

    public boolean equals(Nfs3ExportResult other) {
        if (other == null) {
            return false;
        }

        if (other.getExports() == null || exports == null) {
            return false;
        }

        if (other.getExports().size() != exports.size()) {
            return false;
        }

        for (int i = 0; i < getExports().size(); i++) {
            if (!other.getExports().get(i).equals(exports.get(i))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return exports.hashCode();
    }
}
