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

    private int id;

    private String fileAction;

    private long filePosition;

    private long countArg;

    private long result;

    private Exception exThrown;

    private StackTraceElement[] stack;

    public StreamTraceRecord(int id, String fileAction, long filePosition, StackTraceElement[] stack) {
        this.id = id;
        this.fileAction = fileAction;
        this.filePosition = filePosition;
        this.stack = stack;
    }

    /**
     * Unique identity for this record.
     */
    public int getId() {
        return id;
    }

    /**
     * The type of action being performed.
     */
    public String getFileAction() {
        return fileAction;
    }

    /**
     * The stream position when the action was performed.
     */
    public long getFilePosition() {
        return filePosition;
    }

    /**
     * The count argument (if relevant) when the action was performed.
     */
    public long getCountArg() {
        return countArg;
    }

    public void setCountArg(long value) {
        countArg = value;
    }

    /**
     * The return value (if relevant) when the action was performed.
     */
    public long getResult() {
        return result;
    }

    public void setResult(long value) {
        result = value;
    }

    /**
     * The exception thrown during processing of this action.
     */
    public Exception getExceptionThrown() {
        return exThrown;
    }

    public void setExceptionThrown(Exception value) {
        exThrown = value;
    }

    /**
     * A full stack trace at the point the action was performed.
     */
    public StackTraceElement[] getStack() {
        return stack;
    }

    /**
     * Gets a string representation of the common fields.
     */
    @Override
    public String toString() {
        return String.format("%3d%1s:%5s  %10x  [count=%d, result=%d]",
                id,
                exThrown != null ? "E" : " ",
                fileAction,
                filePosition,
                countArg,
                result);
    }
}
