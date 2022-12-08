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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import discUtils.core.DiscFileSystem;
import discUtils.core.DiscFileSystemChecker;
import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public final class ValidatingFileSystemWrapperStream<Tfs extends DiscFileSystem & IDiagnosticTraceable, Tc extends DiscFileSystemChecker> extends SparseStream {

    private ValidatingFileSystem<Tfs, Tc> fileSystem;

    interface StreamOpenFn<TFileSystem> {
        SparseStream invoke(TFileSystem fs);
    }

    StreamOpenFn<Tfs> openFn;

    private long replayHandle;

    private static AtomicLong nextReplayHandle = new AtomicLong();

    private long shadowPosition;

    private boolean disposed;

    public ValidatingFileSystemWrapperStream(ValidatingFileSystem<Tfs, Tc> fileSystem,
            StreamOpenFn<Tfs> openFn) {
        this.fileSystem = fileSystem;
        this.openFn = openFn;
        replayHandle = nextReplayHandle.incrementAndGet();
    }

    @Override public void close() {
        if (!disposed && !fileSystem.getInLockdown()) {
            long pos = shadowPosition;
            fileSystem.performActivity((fs, context) -> {
                try {
                    getNativeStream(fs, context, pos).close();
                    disposed = true;
                    forgetNativeStream(context);
                    return 0;
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            });
        }

        // Don't call base.Dispose because it calls close
    }

    @Override public boolean canRead() {
        long pos = shadowPosition;
        return (Boolean) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).canRead());
    }

    @Override public boolean canSeek() {
        long pos = shadowPosition;
        return (Boolean) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).canSeek());
    }

    @Override public boolean canWrite() {
        long pos = shadowPosition;
        return (Boolean) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).canWrite());
    }

    @Override public void flush() {
        long pos = shadowPosition;
        fileSystem.performActivity((fs, context) -> {
            getNativeStream(fs, context, pos).flush();
            return 0;
        });
    }

    @Override public long getLength() {
        long pos = shadowPosition;
        return (Long) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).getLength());
    }

    @Override public long position() {
        long pos = shadowPosition;
        return (Long) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).position());
    }

    @Override public void position(long value) {
        long pos = shadowPosition;
        fileSystem.performActivity((fs, context) -> {
            getNativeStream(fs, context, pos).position(value);
            return 0;
        });
        shadowPosition = value;
    }

    @SuppressWarnings("unchecked")
    @Override public List<StreamExtent> getExtents() {
        long pos = shadowPosition;
        return (List) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).getExtents());
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        long pos = shadowPosition;
        // Avoid stomping on buffers we know nothing about by ditching the writes into gash buffer.
        byte[] tempBuffer = new byte[buffer.length];
        int numRead = (Integer) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).read(tempBuffer, offset, count));
        System.arraycopy(tempBuffer, 0, buffer, 0, numRead);
        shadowPosition += numRead;
        return numRead;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        long pos = shadowPosition;
        shadowPosition = (Long) fileSystem.performActivity((fs, context) -> getNativeStream(fs, context, pos).seek(offset, origin));
        return shadowPosition;
    }

    @Override public void setLength(long value) {
        long pos = shadowPosition;
        fileSystem.performActivity((fs, context) -> {
            getNativeStream(fs, context, pos).setLength(value);
            return 0;
        });
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        long pos = shadowPosition;
        // Take a copy of the buffer - otherwise who knows what we're messing with.
        byte[] tempBuffer = new byte[buffer.length];
        System.arraycopy(buffer, 0, tempBuffer, 0, buffer.length);
        fileSystem.performActivity((fs, context) -> {
            getNativeStream(fs, context, pos).write(tempBuffer, offset, count);
            return 0;
        });
        shadowPosition += count;
    }

    public void setNativeStream(Map<String, Object> context, Stream s) {
        String streamKey = "WrapStream#" + replayHandle + "_Stream";
        context.put(streamKey, s);
    }

    private SparseStream getNativeStream(Tfs fs, Map<String, Object> context, long shadowPosition) {
        String streamKey = "WrapStream#" + replayHandle + "_Stream";
//        Object streamObj = new Object();
        SparseStream s;
        if (context.containsKey(streamKey)) {
            s = (SparseStream) context.get(streamKey);
        } else {
            // The native stream isn't in the context.  This means we're replaying
            // but the stream open isn't part of the sequence being replayed.  We
            // do our best to re-create it...
            s = openFn.invoke(fs);
            context.put(streamKey,  s);
        }
        if (shadowPosition != s.position()) {
            s.position(shadowPosition);
        }

        return s;
    }

    private void forgetNativeStream(Map<String, Object> context) {
        String streamKey = "WrapStream#" + replayHandle + "_Stream";
        context.remove(streamKey);
    }
}
