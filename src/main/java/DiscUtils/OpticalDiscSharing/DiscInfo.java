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

package DiscUtils.OpticalDiscSharing;

/**
 * Information about a shared Optical Disc.
 */
public final class DiscInfo {
    /**
     * Gets or sets the name of the disc (unique within an instance of
     * OpticalDiscService).
     */
    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    /**
     * Gets or sets the displayable volume label for the disc.
     */
    private String __VolumeLabel;

    public String getVolumeLabel() {
        return __VolumeLabel;
    }

    public void setVolumeLabel(String value) {
        __VolumeLabel = value;
    }

    /**
     * Gets or sets the volume type of the disc.
     */
    private String __VolumeType;

    public String getVolumeType() {
        return __VolumeType;
    }

    public void setVolumeType(String value) {
        __VolumeType = value;
    }

}
