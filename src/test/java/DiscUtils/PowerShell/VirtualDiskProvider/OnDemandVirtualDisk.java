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

package DiscUtils.PowerShell.VirtualDiskProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskClass;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskParameters;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


public final class OnDemandVirtualDisk extends VirtualDisk {
    private DiscFileSystem _fileSystem;

    private String _path;

    private FileAccess _access;

    public OnDemandVirtualDisk(String path, FileAccess access) {
        _path = path;
        _access = access;
    }

    public OnDemandVirtualDisk(DiscFileSystem fileSystem, String path, FileAccess access) {
        _fileSystem = fileSystem;
        _path = path;
        _access = access;
    }

    public boolean getIsValid() {
        try {
            try (VirtualDisk disk = openDisk()) {
                return disk != null;
            }
        } catch (IOException __dummyCatchVar0) {
            return false;
        }
    }

    public Geometry getGeometry() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getGeometry();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    public VirtualDiskClass getDiskClass() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getDiskClass();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    public long getCapacity() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getCapacity();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    public VirtualDiskParameters getParameters() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getParameters();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    public SparseStream getContent() {
        return new StreamWrapper(_fileSystem, _path, _access);
    }

    public List<VirtualDiskLayer> getLayers() {
        throw new UnsupportedOperationException("Access to virtual disk layers is not implemented for on-demand disks");
    }

    public VirtualDiskTypeInfo getDiskTypeInfo() {
        VirtualDisk disk = openDisk();
        try {
            return disk.getDiskTypeInfo();
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        VirtualDisk disk = openDisk();
        try {
            return disk.createDifferencingDisk(fileSystem, path);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    public VirtualDisk createDifferencingDisk(String path) {
        VirtualDisk disk = openDisk();
        try {
            return disk.createDifferencingDisk(path);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        } finally {
            if (disk != null)
                try {
                    disk.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    private VirtualDisk openDisk() {
        try {
            return VirtualDisk.openDisk(_fileSystem, _path, _access);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    private static class StreamWrapper extends SparseStream {
        private DiscFileSystem _fileSystem;

        private String _path;

        private FileAccess _access;

        private long _position;

        public StreamWrapper(DiscFileSystem fileSystem, String path, FileAccess access) {
            _fileSystem = fileSystem;
            _path = path;
            _access = access;
        }

        public List<StreamExtent> getExtents() {
            VirtualDisk disk = openDisk();
            try {
                return new ArrayList<>(disk.getContent().getExtents());
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        public boolean canRead() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().canRead();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        public boolean canSeek() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().canSeek();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        public boolean canWrite() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().canWrite();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        public void flush() {
        }

        public long getLength() {
            VirtualDisk disk = openDisk();
            try {
                return disk.getContent().getLength();
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        public long getPosition() {
            return _position;
        }

        public void setPosition(long value) {
            _position = value;
        }

        public int read(byte[] buffer, int offset, int count) {
            VirtualDisk disk = openDisk();
            try {
                disk.getContent().setPosition(_position);
                return disk.getContent().read(buffer, offset, count);
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        public long seek(long offset, SeekOrigin origin) {
            long effectiveOffset = offset;
            if (origin == SeekOrigin.Current) {
                effectiveOffset += _position;
            } else if (origin == SeekOrigin.End) {
                effectiveOffset += getLength();
            }

            if (effectiveOffset < 0) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
            } else {
                _position = effectiveOffset;
                return _position;
            }
        }

        public void setLength(long value) {
            VirtualDisk disk = openDisk();
            try {
                disk.getContent().setLength(value);
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        public void write(byte[] buffer, int offset, int count) {
            VirtualDisk disk = openDisk();
            try {
                disk.getContent().setPosition(_position);
                disk.getContent().write(buffer, offset, count);
            } finally {
                if (disk != null)
                    try {
                        disk.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }

        private VirtualDisk openDisk() {
            try {
                return VirtualDisk.openDisk(_fileSystem, _path, _access);
            } catch (IOException e) {
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
        }
    }
}
