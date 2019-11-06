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

package DiscUtils.Ntfs;

import java.util.ArrayList;
import java.util.List;

import dotnet4j.io.IOException;


public class CookedDataRuns {
    private int _firstDirty = Integer.MAX_VALUE;

    private int _lastDirty;

    private final List<CookedDataRun> _runs;

    public CookedDataRuns() {
        _runs = new ArrayList<>();
    }

    public CookedDataRuns(List<DataRun> rawRuns, NonResidentAttributeRecord attributeExtent) {
        _runs = new ArrayList<>();
        append(rawRuns, attributeExtent);
    }

    public int getCount() {
        return _runs.size();
    }

    public CookedDataRun get___idx(int index) {
        return _runs.get(index);
    }

    public CookedDataRun getLast() {
        if (_runs.size() == 0) {
            return null;
        }

        return _runs.get(_runs.size() - 1);
    }

    public long getNextVirtualCluster() {
        if (_runs.size() == 0) {
            return 0;
        }

        int lastRun = _runs.size() - 1;
        return _runs.get(lastRun).getStartVcn() + _runs.get(lastRun).getLength();
    }

    public int findDataRun(long vcn, int startIdx) {
        int numRuns = _runs.size();
        if (numRuns > 0) {
            CookedDataRun run = _runs.get(numRuns - 1);
            if (vcn >= run.getStartVcn()) {
                if (run.getStartVcn() + run.getLength() > vcn) {
                    return numRuns - 1;
                }
                throw new IOException("Looking for VCN outside of data runs");
            }

            for (int i = startIdx; i < numRuns; ++i) {
                run = _runs.get(i);
                if (run.getStartVcn() + run.getLength() > vcn) {
                    return i;
                }
            }
        }

        throw new IOException("Looking for VCN outside of data runs");
    }

    public void append(DataRun rawRun, NonResidentAttributeRecord attributeExtent) {
        CookedDataRun last = getLast();
        _runs.add(new CookedDataRun(rawRun, getNextVirtualCluster(), last == null ? 0 : last.getStartLcn(), attributeExtent));
    }

    public void append(List<DataRun> rawRuns, NonResidentAttributeRecord attributeExtent) {
        long vcn = getNextVirtualCluster();
        long lcn = 0;
        for (DataRun run : rawRuns) {
            _runs.add(new CookedDataRun(run, vcn, lcn, attributeExtent));
            vcn += run.getRunLength();
            lcn += run.getRunOffset();
        }
    }

    public void makeSparse(int index) {
        if (index < _firstDirty) {
            _firstDirty = index;
        }

        if (index > _lastDirty) {
            _lastDirty = index;
        }

        long prevLcn = index == 0 ? 0 : _runs.get(index - 1).getStartLcn();
        CookedDataRun run = _runs.get(index);
        if (run.isSparse()) {
            throw new IllegalArgumentException("Run is already sparse");
        }

        _runs.set(index,
                  new CookedDataRun(new DataRun(0, run.getLength(), true),
                                    run.getStartVcn(),
                                    prevLcn,
                                    run.getAttributeExtent()));
        run.getAttributeExtent().replaceRun(run.getDataRun(), _runs.get(index).getDataRun());
        for (int i = index + 1; i < _runs.size(); ++i) {
            if (!_runs.get(i).isSparse()) {
                _runs.get(i).getDataRun().setRunOffset(_runs.get(i).getDataRun().getRunOffset() + run.getStartLcn() - prevLcn);
                break;
            }
        }
    }

    public void makeNonSparse(int index, List<DataRun> rawRuns) {
        if (index < _firstDirty) {
            _firstDirty = index;
        }

        if (index > _lastDirty) {
            _lastDirty = index;
        }

        long prevLcn = index == 0 ? 0 : _runs.get(index - 1).getStartLcn();
        CookedDataRun run = _runs.get(index);

        if (!run.isSparse()) {
            throw new IllegalArgumentException("Run is already non-sparse");
        }

        _runs.remove(index);
        int insertIdx = run.getAttributeExtent().removeRun(run.getDataRun());

        CookedDataRun lastNewRun = null;
        long lcn = prevLcn;
        long vcn = run.getStartVcn();
        for (DataRun rawRun : rawRuns) {
            CookedDataRun newRun = new CookedDataRun(rawRun, vcn, lcn, run.getAttributeExtent());

            _runs.add(index, newRun);
            run.getAttributeExtent().insertRun(insertIdx, rawRun);

            vcn += rawRun.getRunLength();
            lcn += rawRun.getRunOffset();

            lastNewRun = newRun;
            insertIdx++;

            index++;
        }

        for (int i = index; i < _runs.size(); ++i) {
            if (_runs.get(i).isSparse()) {
                _runs.get(i).setStartLcn(lastNewRun.getStartLcn());
            } else {
                _runs.get(i).getDataRun().setRunOffset(_runs.get(i).getStartLcn() - lastNewRun.getStartLcn());
                break;
            }
        }
    }

    public void splitRun(int runIdx, long vcn) {
        if (runIdx < _firstDirty) {
            _firstDirty = runIdx;
        }

        if (runIdx > _lastDirty) {
            _lastDirty = runIdx;
        }

        CookedDataRun run = _runs.get(runIdx);

        if (run.getStartVcn() >= vcn || run.getStartVcn() + run.getLength() <= vcn) {
            throw new IllegalArgumentException("Attempt to split run outside of it's range");
        }

        long distance = vcn - run.getStartVcn();
        long offset = run.isSparse() ? 0 : distance;
        CookedDataRun newRun = new CookedDataRun(new DataRun(offset, run.getLength() - distance, run.isSparse()),
                                                 vcn,
                                                 run.getStartLcn(),
                                                 run.getAttributeExtent());

        run.setLength(distance);

        _runs.add(runIdx + 1, newRun);
        run.getAttributeExtent().insertRun(run.getDataRun(), newRun.getDataRun());

        for (int i = runIdx + 2; i < _runs.size(); ++i) {
            if (_runs.get(i).isSparse()) {
                _runs.get(i).setStartLcn(_runs.get(i).getStartLcn() + offset);
            } else {
                _runs.get(i).getDataRun().setRunOffset(_runs.get(i).getDataRun().getRunOffset() - offset);
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
        while (index < _runs.size()) {
            _runs.get(index).getAttributeExtent().removeRun(_runs.get(index).getDataRun());
            _runs.remove(index);
        }
    }

    void collapseRuns() {
        int i = _firstDirty > 1 ? _firstDirty - 1 : 0;
        while (i < _runs.size() - 1 && i <= _lastDirty + 1) {
            if (_runs.get(i).isSparse() && _runs.get(i + 1).isSparse()) {
                _runs.get(i).setLength(_runs.get(i).getLength() + _runs.get(i + 1).getLength());
                _runs.get(i + 1).getAttributeExtent().removeRun(_runs.get(i + 1).getDataRun());
                _runs.remove(i + 1);
            } else if (!_runs.get(i).isSparse() && !_runs.get(i).isSparse() && // TODO bug report !_runs.get(i).isSparse() twice
                       _runs.get(i).getStartLcn() + _runs.get(i).getLength() == _runs.get(i + 1).getStartLcn()) {
                _runs.get(i).setLength(_runs.get(i).getLength() + _runs.get(i + 1).getLength());
                _runs.get(i + 1).getAttributeExtent().removeRun(_runs.get(i + 1).getDataRun());
                _runs.remove(i + 1);

                for (int j = i + 1; j < _runs.size(); ++j) {
                    if (_runs.get(j).isSparse()) {
                        _runs.get(j).setStartLcn(_runs.get(j).getStartLcn());
                    } else {
                        _runs.get(j).getDataRun().setRunOffset(_runs.get(j).getStartLcn() - _runs.get(i).getStartLcn());
                        break;
                    }
                }
            } else {
                ++i;
            }
        }

        _firstDirty = Integer.MAX_VALUE;
        _lastDirty = 0;
    }
}
