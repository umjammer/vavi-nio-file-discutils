/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.chd;

import java.io.IOException;
import java.nio.file.Path;
import java.util.StringJoiner;

import com.github.fge.filesystem.provider.FileSystemProviderBase;
import jpcsp.filesystems.umdiso.UmdIsoReader;

import static vavi.nio.file.chd.ChdFileSystemDriver.toPathString;


/**
 * ChdFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/09/30 umjammer initial version <br>
 */
public final class ChdFileSystemProvider extends FileSystemProviderBase {

    public static final String PARAM_ALIAS = "alias";

    public ChdFileSystemProvider() {
        super(new ChdFileSystemRepository());
    }

    /** */
    public static class Entity {
        final UmdIsoReader manager;
        boolean isDirectory;
        final Path path;
        final String name;

        public Entity(UmdIsoReader manager, Path path, String file) {
            this.manager = manager;
            this.path = path;
//logger.log(Level.TRACE, "path: " + path + ", file: " + file);
            int p = file.indexOf(";");
            if (p != -1) {
                this.name = file.substring(0, file.indexOf(";"));
            } else {
                this.name = file;
                try {
                    String pathString = path != null ? toPathString(path.resolve(file)) : file;
//logger.log(Level.TRACE, "TRY: " + pathString + ", " + path + ", " + file);
                    this.isDirectory = manager.isDirectory(pathString);
                } catch (IOException e) {
//logger.log(Level.ERROR, "ERROR: " + path + ", " + file + ", " + e);
                    this.isDirectory = false;
                }
            }
//logger.log(Level.TRACE, "NEW1: " + this);
        }

        public Entity(UmdIsoReader manager, Path path, String name, boolean isDirectory) {
            this.manager = manager;
            this.path = path;
            this.name = name;
            this.isDirectory = isDirectory;
//logger.log(Level.TRACE, "NEW2: " + this);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Entity.class.getSimpleName() + "[", "]")
                    .add("isDirectory=" + isDirectory)
                    .add("path=" + path)
                    .add("name='" + name + "'")
                    .toString();
        }
    }
}
