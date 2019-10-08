/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.dus;

import java.io.IOException;
import java.nio.file.FileStore;

import com.dropbox.core.v1.DbxAccountInfo.Quota;
import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.filestore.FileStoreBase;

import DiscUtils.Core.VirtualDisk;


/**
 * A simple DiskUtils {@link FileStore}
 *
 * <p>
 * This makes use of information available in {@link Quota}.
 * Information is computed in "real time".
 * </p>
 */
public final class DusFileStore extends FileStoreBase {

    private final VirtualDisk session;

    /**
     * Constructor
     *
     * @param session the (valid) OneDrive client to use
     */
    public DusFileStore(final VirtualDisk session, final FileAttributesFactory factory) {
        super("dus", factory, false);
        this.session = session;
    }

    /**
     * Returns the size, in bytes, of the file store.
     *
     * @return the size of the file store, in bytes
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getTotalSpace() {
        final Quota.Space quota = getQuota();
        return quota == null ? 0 : quota.available + quota.used;
    }

    /**
     * Returns the number of bytes available to this Java virtual machine on the
     * file store.
     * <p>
     * The returned number of available bytes is a hint, but not a
     * guarantee, that it is possible to use most or any of these bytes. The
     * number of usable bytes is most likely to be accurate immediately
     * after the space attributes are obtained. It is likely to be made
     * inaccurate
     * by any external I/O operations including those made on the system outside
     * of this Java virtual machine.
     *
     * @return the number of bytes available
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getUsableSpace() {
        final Quota.Space quota = getQuota();
        if (quota == null) {
            return 0;
        } else {
            return quota.available;
        }
    }

    /**
     * Returns the number of unallocated bytes in the file store.
     * <p>
     * The returned number of unallocated bytes is a hint, but not a
     * guarantee, that it is possible to use most or any of these bytes. The
     * number of unallocated bytes is most likely to be accurate immediately
     * after the space attributes are obtained. It is likely to be
     * made inaccurate by any external I/O operations including those made on
     * the system outside of this virtual machine.
     *
     * @return the number of unallocated bytes
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getUnallocatedSpace() {
        return 0;
    }

    /** */
    private Quota.Space cache; // TODO refresh

    /** */
    private Quota.Space getQuota() {
        if (cache != null) {
            return cache;
        } else {
            cache = session._getFeature(Quota.class).get();
            return cache;
        }
    }
}
