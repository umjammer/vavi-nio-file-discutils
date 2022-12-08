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

package discUtils.powerShell.virtualDiskProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskParameters;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import dotnet4j.io.FileAccess;
import dotnet4j.io.SeekOrigin;


public final class OnDemandVirtualDisk extends VirtualDisk {

    private DiscFileSystem fileSystem;

    private String path;

    private FileAccess access;

    public OnDemandVirtualDisk(String path, FileAccess access) {
        this.path = path;
        this.access = access;
    }

    public OnDemandVirtualDisk(DiscFileSystem fileSystem, String path, FileAccess access) {
        this.fileSystem = fileSystem;
        this.path = path;
        this.access = access;
    }

    public boolean getIsValid() {
        try {
            try (VirtualDisk disk = openDisk()) {
                return disk != null;
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override public Geometry getGeometry() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getGeometry();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override public VirtualDiskClass getDiskClass() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getDiskClass();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override public long getCapacity() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getCapacity();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override public VirtualDiskParameters getParameters() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getParameters();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override public SparseStream getContent() {
        return new StreamWrapper(fileSystem, path, access);
    }

    @Override public List<VirtualDiskLayer> getLayers() {
        throw new UnsupportedOperationException("Access to virtual disk layers is not implemented for on-demand disks");
    }

    @Override public VirtualDiskTypeInfo getDiskTypeInfo() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getDiskTypeInfo();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        VirtualDisk disk = openDisk();
        try {
            return disk.createDifferencingDisk(fileSystem, path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override public VirtualDisk createDifferencingDisk(String path) {
        VirtualDisk disk = openDisk();
        try {
            return disk.createDifferencingDisk(path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private VirtualDisk openDisk() {
        try {
            return VirtualDisk.openDisk(fileSystem, path, access);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private static class StreamWrapper extends SparseStream {

        private DiscFileSystem fileSystem;

        private String path;

        private FileAccess access;

        private long position;

        public StreamWrapper(DiscFileSystem fileSystem, String path, FileAccess access) {
            this.fileSystem = fileSystem;
            this.path = path;
            this.access = access;
        }

        @Override public List<StreamExtent> getExtents() {
            VirtualDisk disk = openDisk();
            try {
                return new ArrayList<>(disk.getContent().getExtents());
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        @Override public boolean canRead() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().canRead();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        @Override public boolean canSeek() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().canSeek();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        @Override public boolean canWrite() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().canWrite();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        @Override public void flush() {
        }

        @Override public long getLength() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().getLength();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        @Override public long position() {
            return position;
        }

        @Override public void position(long value) {
            position = value;
        }

        @Override public int read(byte[] buffer, int offset, int count) {
            VirtualDisk disk = openDisk();
            try {
                disk.getContent().position(position);
                return disk.getContent().read(buffer, offset, count);
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        @Override public long seek(long offset, SeekOrigin origin) {
            long effectiveOffset = offset;
            if (origin == SeekOrigin.Current) {
                effectiveOffset += position;
            } else if (origin == SeekOrigin.End) {
                effectiveOffset += getLength();
            }

            if (effectiveOffset < 0) {
                throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
            } else {
                position = effectiveOffset;
                return position;
            }
        }

        @Override public void setLength(long value) {
            VirtualDisk disk = openDisk();
            try {
                disk.getContent().setLength(value);
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        @Override public void write(byte[] buffer, int offset, int count) {
            VirtualDisk disk = openDisk();
            try {
                disk.getContent().position(position);
                disk.getContent().write(buffer, offset, count);
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        private VirtualDisk openDisk() {
            try {
                return VirtualDisk.openDisk(fileSystem, path, access);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
        }
    }
}
