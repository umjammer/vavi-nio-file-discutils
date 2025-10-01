/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.chd;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import vavi.util.Debug;

import static java.lang.System.getLogger;


/**
 * ChdFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/09/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class ChdFileSystemRepository extends FileSystemRepositoryBase {

    private static final Logger logger = getLogger(ChdFileSystemRepository.class.getName());

    public ChdFileSystemRepository() {
        super("chd", new ChdFileSystemFactoryProvider());
    }

    /**
     * @param uri "chd://host/", sub url (after "vfs:") parts will be replaced by properties.
     *            if you don't use alias, the url must include username, password, host, port.
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(URI uri, Map<String, ?> env) throws IOException {
        String[] rawSchemeSpecificParts = uri.getRawSchemeSpecificPart().split("!");
        URI file = URI.create(rawSchemeSpecificParts[0]);
        if (!"file".equals(file.getScheme())) {
            // currently only support "file"
            throw new IllegalArgumentException(file.toString());
        }
        if (!file.getRawSchemeSpecificPart().startsWith("/")) {
            file = URI.create(file.getScheme() + ":" + System.getProperty("user.dir") + "/" + file.getRawSchemeSpecificPart());
        }

        Path path = Path.of(file);
Debug.print(path);
        UmdIsoReader manager = new UmdIsoReader(path.toString());

        ChdFileStore fileStore = new ChdFileStore(manager, factoryProvider.getAttributesFactory());
        return new ChdFileSystemDriver(fileStore, factoryProvider, manager, env);
    }

    /* ad-hoc hack for ignoring checking opacity */
    @Override
    protected void checkURI(@Nullable URI uri) {
        Objects.requireNonNull(uri);
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("uri is not absolute");
        }
        if (!getScheme().equals(uri.getScheme())) {
            throw new IllegalArgumentException("bad scheme");
        }
    }
}
