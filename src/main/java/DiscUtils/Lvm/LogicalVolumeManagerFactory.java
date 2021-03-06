//
// Copyright (c) 2008-2011, Kenneth Bell
// Copyright (c) 2016, Bianco Veigel
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

package DiscUtils.Lvm;

import java.util.List;
import java.util.Map;

import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.PhysicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Internal.LogicalVolumeFactory;
import DiscUtils.Core.Internal.LogicalVolumeFactoryAttribute;


@LogicalVolumeFactoryAttribute
public class LogicalVolumeManagerFactory extends LogicalVolumeFactory {
    public boolean handlesPhysicalVolume(PhysicalVolumeInfo volume) {
        return LogicalVolumeManager.handlesPhysicalVolume(volume);
    }

    public void mapDisks(List<VirtualDisk> disks, Map<String, LogicalVolumeInfo> result) {
        LogicalVolumeManager mgr = new LogicalVolumeManager(disks);
        for (LogicalVolumeInfo vol : mgr.getLogicalVolumes()) {
            result.put(vol.getIdentity(), vol);
//Debug.println("Ll: " + vol.getIdentity());
        }
    }
}
