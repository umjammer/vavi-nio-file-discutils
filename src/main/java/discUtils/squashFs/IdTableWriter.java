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

package discUtils.squashFs;

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.util.EndianUtilities;


public final class IdTableWriter {

    private final BuilderContext context;

    private final List<Integer> ids;

    public IdTableWriter(BuilderContext context) {
        this.context = context;
        ids = new ArrayList<>();
    }

    public int getIdCount() {
        return ids.size();
    }

    /**
     * Allocates space for a User / Group id.
     *
     * @param id The id to allocate.
     * @return The key of the id.
     */
    public short allocateId(int id) {
        for (int i = 0; i < ids.size(); ++i) {
            if (ids.get(i) == id) {
                return (short) i;
            }
        }
        ids.add(id);
        return (short) (ids.size() - 1);
    }

    public long persist() {
        if (ids.size() <= 0) {
            return -1;
        }

        if (ids.size() * 4 > context.getDataBlockSize()) {
            throw new UnsupportedOperationException("Large numbers of user / group id's");
        }

        for (int i = 0; i < ids.size(); ++i) {
            EndianUtilities.writeBytesLittleEndian(ids.get(i), context.getIoBuffer(), i * 4);
        }
        // Persist the actual Id's
        long blockPos = context.getRawStream().position();
        MetablockWriter writer = new MetablockWriter();
        writer.write(context.getIoBuffer(), 0, ids.size() * 4);
        writer.persist(context.getRawStream());
        // Persist the table that references the block containing the id's
        long tablePos = context.getRawStream().position();
        byte[] tableBuffer = new byte[8];
        EndianUtilities.writeBytesLittleEndian(blockPos, tableBuffer, 0);
        context.getRawStream().write(tableBuffer, 0, 8);
        return tablePos;
    }
}
