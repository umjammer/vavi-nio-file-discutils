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

package discUtils.core.logicalDiskManager;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Map;

import discUtils.core.LogicalVolumeInfo;
import discUtils.core.PhysicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.internal.LogicalVolumeFactory;

import static java.lang.System.getLogger;


public class DynamicDiskManagerFactory implements LogicalVolumeFactory {

    private static final Logger logger = getLogger(DynamicDiskManagerFactory.class.getName());

    @Override
    public boolean handlesPhysicalVolume(PhysicalVolumeInfo volume) {
        return DynamicDiskManager.handlesPhysicalVolume(volume);
    }

    @Override
    public void mapDisks(List<VirtualDisk> disks, Map<String, LogicalVolumeInfo> result) {
        DynamicDiskManager mgr = new DynamicDiskManager();
        for (VirtualDisk disk : disks) {
            if (DynamicDiskManager.isDynamicDisk(disk)) {
                mgr.add(disk);
            }
        }
        for (LogicalVolumeInfo vol : mgr.getLogicalVolumes()) {
            result.put(vol.getIdentity(), vol);
logger.log(Level.DEBUG, "Ld: " + vol.getIdentity());
        }
    }
}
