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

package discUtils.diagnostics;

/**
 * A record of an individual stream activity.
 */
public final class StreamTraceRecord {
    private int _id;

    private String _fileAction;

    private long _filePosition;

    private long _countArg;

    private long _result;

    private Exception _exThrown;

    private StackTraceElement[] _stack;

    public StreamTraceRecord(int id, String fileAction, long filePosition, StackTraceElement[] stack) {
        _id = id;
        _fileAction = fileAction;
        _filePosition = filePosition;
        _stack = stack;
    }

    /**
     * Unique identity for this record.
     */
    public int getId() {
        return _id;
    }

    /**
     * The type of action being performed.
     */
    public String getFileAction() {
        return _fileAction;
    }

    /**
     * The stream position when the action was performed.
     */
    public long getFilePosition() {
        return _filePosition;
    }

    /**
     * The count argument (if relevant) when the action was performed.
     */
    public long getCountArg() {
        return _countArg;
    }

    public void setCountArg(long value) {
        _countArg = value;
    }

    /**
     * The return value (if relevant) when the action was performed.
     */
    public long getResult() {
        return _result;
    }

    public void setResult(long value) {
        _result = value;
    }

    /**
     * The exception thrown during processing of this action.
     */
    public Exception getExceptionThrown() {
        return _exThrown;
    }

    public void setExceptionThrown(Exception value) {
        _exThrown = value;
    }

    /**
     * A full stack trace at the point the action was performed.
     */
    public StackTraceElement[] getStack() {
        return _stack;
    }

    /**
     * Gets a string representation of the common fields.
     *
     * @return
     */
    public String toString() {
        return String.format("%3d%1s:%5s  %10x  [count=%d, result=%d]",
                             _id,
                             _exThrown != null ? "E" : " ",
                             _fileAction,
                             _filePosition,
                             _countArg,
                             _result);
    }
}
