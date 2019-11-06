
package DiscUtils.Core.Vfs;

import DiscUtils.Streams.SparseStream;


/**
 * Interface implemented by classes representing files, in file systems that
 * support multi-stream files.
 */
public interface IVfsFileWithStreams extends IVfsFile {
    /**
     * Creates a new stream.
     *
     * @param name The name of the stream.
     * @return An object representing the stream.
     */
    SparseStream createStream(String name);

    /**
     * Opens an existing stream.
     *
     * The implementation must not implicitly create the stream if it doesn't
     * already exist.
     *
     * @param name The name of the stream.
     * @return An object representing the stream.
     */
    SparseStream openExistingStream(String name);
}
