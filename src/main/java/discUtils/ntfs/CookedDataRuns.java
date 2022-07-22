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

package discUtils.ntfs;

import java.util.ArrayList;
import java.util.List;

import dotnet4j.io.IOException;


class CookedDataRuns {

    private int firstDirty = Integer.MAX_VALUE;

    private int lastDirty;

    private final List<CookedDataRun> runs;

    public CookedDataRuns() {
        runs = new ArrayList<>();
    }

    public CookedDataRuns(List<DataRun> rawRuns, NonResidentAttributeRecord attributeExtent) {
        runs = new ArrayList<>();
        append(rawRuns, attributeExtent);
    }

    public int getCount() {
        return runs.size();
    }

    public CookedDataRun get(int index) {
        return runs.get(index);
    }

    public CookedDataRun getLast() {
        if (runs.size() == 0) {
            return null;
        }
        return runs.get(runs.size() - 1);
    }

    public long getNextVirtualCluster() {
        if (runs.size() == 0) {
            return 0;
        }
        int lastRun = runs.size() - 1;
        return runs.get(lastRun).getStartVcn() + runs.get(lastRun).getLength();
    }

    public int findDataRun(long vcn, int startIdx) {
        int numRuns = runs.size();
        if (numRuns > 0) {
            CookedDataRun run = runs.get(numRuns - 1);
            if (vcn >= run.getStartVcn()) {
                if (run.getStartVcn() + run.getLength() > vcn) {
                    return numRuns - 1;
                }
                throw new IOException("Looking for VCN outside of data runs");
            }

            for (int i = startIdx; i < numRuns; ++i) {
                run = runs.get(i);
                if (run.getStartVcn() + run.getLength() > vcn) {
                    return i;
                }
            }
        }

        throw new IOException("Looking for VCN outside of data runs");
    }

    public void append(DataRun rawRun, NonResidentAttributeRecord attributeExtent) {
        CookedDataRun last = getLast();
        runs.add(new CookedDataRun(rawRun, getNextVirtualCluster(), last == null ? 0 : last.getStartLcn(), attributeExtent));
    }

    public void append(List<DataRun> rawRuns, NonResidentAttributeRecord attributeExtent) {
        long vcn = getNextVirtualCluster();
        long lcn = 0;
        for (DataRun run : rawRuns) {
            runs.add(new CookedDataRun(run, vcn, lcn, attributeExtent));
            vcn += run.getRunLength();
            lcn += run.getRunOffset();
        }
    }

    public void makeSparse(int index) {
        if (index < firstDirty) {
            firstDirty = index;
        }

        if (index > lastDirty) {
            lastDirty = index;
        }

        long prevLcn = index == 0 ? 0 : runs.get(index - 1).getStartLcn();
        CookedDataRun run = runs.get(index);

        if (run.isSparse()) {
            throw new IllegalArgumentException("index: Run is already sparse: " + index);
        }

        runs.set(index,
                  new CookedDataRun(new DataRun(0, run.getLength(), true),
                                    run.getStartVcn(),
                                    prevLcn,
                                    run.getAttributeExtent()));
        run.getAttributeExtent().replaceRun(run.getDataRun(), runs.get(index).getDataRun());

        for (int i = index + 1; i < runs.size(); ++i) {
            if (!runs.get(i).isSparse()) {
                runs.get(i).getDataRun().setRunOffset(runs.get(i).getDataRun().getRunOffset() + run.getStartLcn() - prevLcn);
                break;
            }
        }
    }

    public void makeNonSparse(int index, List<DataRun> rawRuns) {
        if (index < firstDirty) {
            firstDirty = index;
        }

        if (index > lastDirty) {
            lastDirty = index;
        }

        long prevLcn = index == 0 ? 0 : runs.get(index - 1).getStartLcn();
        CookedDataRun run = runs.get(index);

        if (!run.isSparse()) {
            throw new IllegalArgumentException("index: Run is already non-sparse: " + index);
        }

        runs.remove(index);
        int insertIdx = run.getAttributeExtent().removeRun(run.getDataRun());

        CookedDataRun lastNewRun = null;
        long lcn = prevLcn;
        long vcn = run.getStartVcn();
        for (DataRun rawRun : rawRuns) {
            CookedDataRun newRun = new CookedDataRun(rawRun, vcn, lcn, run.getAttributeExtent());

            runs.add(index, newRun);
            run.getAttributeExtent().insertRun(insertIdx, rawRun);

            vcn += rawRun.getRunLength();
            lcn += rawRun.getRunOffset();

            lastNewRun = newRun;
            insertIdx++;

            index++;
        }

        for (int i = index; i < runs.size(); ++i) {
            if (runs.get(i).isSparse()) {
                runs.get(i).setStartLcn(lastNewRun.getStartLcn());
            } else {
                runs.get(i).getDataRun().setRunOffset(runs.get(i).getStartLcn() - lastNewRun.getStartLcn());
                break;
            }
        }
    }

    public void splitRun(int runIdx, long vcn) {
        if (runIdx < firstDirty) {
            firstDirty = runIdx;
        }

        if (runIdx > lastDirty) {
            lastDirty = runIdx;
        }

        CookedDataRun run = runs.get(runIdx);

        if (run.getStartVcn() >= vcn || run.getStartVcn() + run.getLength() <= vcn) {
            throw new IllegalArgumentException("vcn: Attempt to split run outside of it's range: " + vcn);
        }

        long distance = vcn - run.getStartVcn();
        long offset = run.isSparse() ? 0 : distance;
        CookedDataRun newRun = new CookedDataRun(new DataRun(offset, run.getLength() - distance, run.isSparse()),
                                                 vcn,
                                                 run.getStartLcn(),
                                                 run.getAttributeExtent());

        run.setLength(distance);

        runs.add(runIdx + 1, newRun);
        run.getAttributeExtent().insertRun(run.getDataRun(), newRun.getDataRun());

        for (int i = runIdx + 2; i < runs.size(); ++i) {
            if (runs.get(i).isSparse()) {
                runs.get(i).setStartLcn(runs.get(i).getStartLcn() + offset);
            } else {
                runs.get(i).getDataRun().setRunOffset(runs.get(i).getDataRun().getRunOffset() - offset);
                break;
            }
        }
    }

    /**
     * Truncates the set of data runs.
     *
     * @param index The first run to be truncated.
     */
    public void truncateAt(int index) {
        while (index < runs.size()) {
            runs.get(index).getAttributeExtent().removeRun(runs.get(index).getDataRun());
            runs.remove(index);
        }
    }

    void collapseRuns() {
        int i = firstDirty > 1 ? firstDirty - 1 : 0;
        while (i < runs.size() - 1 && i <= lastDirty + 1) {
            if (runs.get(i).isSparse() && runs.get(i + 1).isSparse()) {
                runs.get(i).setLength(runs.get(i).getLength() + runs.get(i + 1).getLength());
                runs.get(i + 1).getAttributeExtent().removeRun(runs.get(i + 1).getDataRun());
                runs.remove(i + 1);
            } else if (!runs.get(i).isSparse() && !runs.get(i + 1).isSparse() &&
                       runs.get(i).getStartLcn() + runs.get(i).getLength() == runs.get(i + 1).getStartLcn()) {
                runs.get(i).setLength(runs.get(i).getLength() + runs.get(i + 1).getLength());
                runs.get(i + 1).getAttributeExtent().removeRun(runs.get(i + 1).getDataRun());
                runs.remove(i + 1);

                for (int j = i + 1; j < runs.size(); ++j) {
                    if (runs.get(j).isSparse()) {
                        runs.get(j).setStartLcn(runs.get(i).getStartLcn());
                    } else {
                        runs.get(j).getDataRun().setRunOffset(runs.get(j).getStartLcn() - runs.get(i).getStartLcn());
                        break;
                    }
                }
            } else {
                ++i;
            }
        }

        firstDirty = Integer.MAX_VALUE;
        lastDirty = 0;
    }
}
