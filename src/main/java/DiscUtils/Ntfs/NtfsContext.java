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

package DiscUtils.Ntfs;

import dotnet4j.io.Stream;


public final class NtfsContext implements INtfsContext {
    private Stream __RawStream;

    public Stream getRawStream() {
        return __RawStream;
    }

    public void setRawStream(Stream value) {
        __RawStream = value;
    }

    private AttributeDefinitions __AttributeDefinitions;

    public AttributeDefinitions getAttributeDefinitions() {
        return __AttributeDefinitions;
    }

    public void setAttributeDefinitions(AttributeDefinitions value) {
        __AttributeDefinitions = value;
    }

    private UpperCase __UpperCase;

    public UpperCase getUpperCase() {
        return __UpperCase;
    }

    public void setUpperCase(UpperCase value) {
        __UpperCase = value;
    }

    private BiosParameterBlock __BiosParameterBlock;

    public BiosParameterBlock getBiosParameterBlock() {
        return __BiosParameterBlock;
    }

    public void setBiosParameterBlock(BiosParameterBlock value) {
        __BiosParameterBlock = value;
    }

    private MasterFileTable __Mft;

    public MasterFileTable getMft() {
        return __Mft;
    }

    public void setMft(MasterFileTable value) {
        __Mft = value;
    }

    private ClusterBitmap __ClusterBitmap;

    public ClusterBitmap getClusterBitmap() {
        return __ClusterBitmap;
    }

    public void setClusterBitmap(ClusterBitmap value) {
        __ClusterBitmap = value;
    }

    private SecurityDescriptors __SecurityDescriptors;

    public SecurityDescriptors getSecurityDescriptors() {
        return __SecurityDescriptors;
    }

    public void setSecurityDescriptors(SecurityDescriptors value) {
        __SecurityDescriptors = value;
    }

    private ObjectIds __ObjectIds;

    public ObjectIds getObjectIds() {
        return __ObjectIds;
    }

    public void setObjectIds(ObjectIds value) {
        __ObjectIds = value;
    }

    private ReparsePoints __ReparsePoints;

    public ReparsePoints getReparsePoints() {
        return __ReparsePoints;
    }

    public void setReparsePoints(ReparsePoints value) {
        __ReparsePoints = value;
    }

    private Quotas __Quotas;

    public Quotas getQuotas() {
        return __Quotas;
    }

    public void setQuotas(Quotas value) {
        __Quotas = value;
    }

    private NtfsOptions __Options;

    public NtfsOptions getOptions() {
        return __Options;
    }

    public void setOptions(NtfsOptions value) {
        __Options = value;
    }

    private GetFileByIndexFn __GetFileByIndex;

    public GetFileByIndexFn getGetFileByIndex() {
        return __GetFileByIndex;
    }

    public void setGetFileByIndex(GetFileByIndexFn value) {
        __GetFileByIndex = value;
    }

    private GetFileByRefFn __GetFileByRef;

    public GetFileByRefFn getGetFileByRef() {
        return __GetFileByRef;
    }

    public void setGetFileByRef(GetFileByRefFn value) {
        __GetFileByRef = value;
    }

    private GetDirectoryByIndexFn __GetDirectoryByIndex;

    public GetDirectoryByIndexFn getGetDirectoryByIndex() {
        return __GetDirectoryByIndex;
    }

    public void setGetDirectoryByIndex(GetDirectoryByIndexFn value) {
        __GetDirectoryByIndex = value;
    }

    private GetDirectoryByRefFn __GetDirectoryByRef;

    public GetDirectoryByRefFn getGetDirectoryByRef() {
        return __GetDirectoryByRef;
    }

    public void setGetDirectoryByRef(GetDirectoryByRefFn value) {
        __GetDirectoryByRef = value;
    }

    private AllocateFileFn __AllocateFile;

    public AllocateFileFn getAllocateFile() {
        return __AllocateFile;
    }

    public void setAllocateFile(AllocateFileFn value) {
        __AllocateFile = value;
    }

    private ForgetFileFn __ForgetFile;

    public ForgetFileFn getForgetFile() {
        return __ForgetFile;
    }

    public void setForgetFile(ForgetFileFn value) {
        __ForgetFile = value;
    }

    private boolean __ReadOnly;

    public boolean getReadOnly() {
        return __ReadOnly;
    }

    public void setReadOnly(boolean value) {
        __ReadOnly = value;
    }
}
