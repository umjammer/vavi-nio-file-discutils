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

package DiscUtils.Vmdk;

import DiscUtils.Core.GenericDiskAdapterType;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDiskParameters;


/**
 * The parameters used to create a new VMDK file.
 */
public final class DiskParameters {
    /**
     * Initializes a new instance of the DiskParameters class with default
     * values.
     */
    public DiskParameters() {
    }

    /**
     * Initializes a new instance of the DiskParameters class with generic
     * parameters.
     *
     * @param genericParameters The generic parameters to copy.
     */
    public DiskParameters(VirtualDiskParameters genericParameters) {
        setCapacity(genericParameters.getCapacity());
        setGeometry(genericParameters.getGeometry());
        setBiosGeometry(genericParameters.getBiosGeometry());
        if (genericParameters.getExtendedParameters().containsKey(Disk.ExtendedParameterKeyCreateType)) {
            String stringCreateType = genericParameters.getExtendedParameters().get(Disk.ExtendedParameterKeyCreateType);
            setCreateType(DiskCreateType.valueOf(stringCreateType));
        } else {
            setCreateType(DiskCreateType.MonolithicSparse);
        }
        if (genericParameters.getAdapterType() == GenericDiskAdapterType.Ide) {
            setAdapterType(DiskAdapterType.Ide);
        } else {
            if (genericParameters.getExtendedParameters().containsKey(Disk.ExtendedParameterKeyAdapterType)) {
                String stringAdapterType = genericParameters.getExtendedParameters().get(Disk.ExtendedParameterKeyAdapterType);
                setAdapterType(DiskAdapterType.valueOf(stringAdapterType));
                // Don't refining sub-type of SCSI actually select IDE
                if (getAdapterType() == DiskAdapterType.Ide) {
                    setAdapterType(DiskAdapterType.LsiLogicScsi);
                }
            } else {
                setAdapterType(DiskAdapterType.LsiLogicScsi);
            }
        }
    }

    /**
     * Gets or sets the type of emulated disk adapter.
     */
    private DiskAdapterType _adapterType = DiskAdapterType.None;

    public DiskAdapterType getAdapterType() {
        return _adapterType;
    }

    public void setAdapterType(DiskAdapterType value) {
        _adapterType = value;
    }

    /**
     * Gets or sets the BIOS Geometry of the virtual disk.
     */
    private Geometry _biosGeometry;

    public Geometry getBiosGeometry() {
        return _biosGeometry;
    }

    public void setBiosGeometry(Geometry value) {
        _biosGeometry = value;
    }

    /**
     * Gets or sets the capacity of the virtual disk.
     */
    private long _capacity;

    public long getCapacity() {
        return _capacity;
    }

    public void setCapacity(long value) {
        _capacity = value;
    }

    /**
     * Gets or sets the type of VMDK file to create.
     */
    private DiskCreateType _createType = DiskCreateType.None;

    public DiskCreateType getCreateType() {
        return _createType;
    }

    public void setCreateType(DiskCreateType value) {
        _createType = value;
    }

    /**
     * Gets or sets the Physical Geometry of the virtual disk.
     */
    private Geometry _geometry;

    public Geometry getGeometry() {
        return _geometry;
    }

    public void setGeometry(Geometry value) {
        _geometry = value;
    }

}
