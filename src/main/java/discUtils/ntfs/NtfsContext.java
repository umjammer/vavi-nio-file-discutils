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

package discUtils.ntfs;

import dotnet4j.io.Stream;


public final class NtfsContext implements INtfsContext {

    private Stream rawStream;

    @Override
    public Stream getRawStream() {
        return rawStream;
    }

    public void setRawStream(Stream value) {
        rawStream = value;
    }

    private AttributeDefinitions attributeDefinitions;

    @Override
    public AttributeDefinitions getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public void setAttributeDefinitions(AttributeDefinitions value) {
        attributeDefinitions = value;
    }

    private UpperCase upperCase;

    @Override
    public UpperCase getUpperCase() {
        return upperCase;
    }

    public void setUpperCase(UpperCase value) {
        upperCase = value;
    }

    private BiosParameterBlock biosParameterBlock;

    @Override
    public BiosParameterBlock getBiosParameterBlock() {
        return biosParameterBlock;
    }

    public void setBiosParameterBlock(BiosParameterBlock value) {
        biosParameterBlock = value;
    }

    private MasterFileTable mft;

    @Override
    public MasterFileTable getMft() {
        return mft;
    }

    public void setMft(MasterFileTable value) {
        mft = value;
    }

    private ClusterBitmap clusterBitmap;

    @Override
    public ClusterBitmap getClusterBitmap() {
        return clusterBitmap;
    }

    public void setClusterBitmap(ClusterBitmap value) {
        clusterBitmap = value;
    }

    private SecurityDescriptors securityDescriptors;

    @Override
    public SecurityDescriptors getSecurityDescriptors() {
        return securityDescriptors;
    }

    public void setSecurityDescriptors(SecurityDescriptors value) {
        securityDescriptors = value;
    }

    private ObjectIds objectIds;

    @Override
    public ObjectIds getObjectIds() {
        return objectIds;
    }

    public void setObjectIds(ObjectIds value) {
        objectIds = value;
    }

    private ReparsePoints reparsePoints;

    @Override
    public ReparsePoints getReparsePoints() {
        return reparsePoints;
    }

    public void setReparsePoints(ReparsePoints value) {
        reparsePoints = value;
    }

    private Quotas quotas;

    @Override
    public Quotas getQuotas() {
        return quotas;
    }

    public void setQuotas(Quotas value) {
        quotas = value;
    }

    private NtfsOptions options;

    @Override
    public NtfsOptions getOptions() {
        return options;
    }

    public void setOptions(NtfsOptions value) {
        options = value;
    }

    private GetFileByIndexFn getFileByIndex;

    @Override
    public GetFileByIndexFn getGetFileByIndex() {
        return getFileByIndex;
    }

    public void setGetFileByIndex(GetFileByIndexFn value) {
        getFileByIndex = value;
    }

    private GetFileByRefFn getFileByRef;

    @Override
    public GetFileByRefFn getGetFileByRef() {
        return getFileByRef;
    }

    public void setGetFileByRef(GetFileByRefFn value) {
        getFileByRef = value;
    }

    private GetDirectoryByIndexFn getDirectoryByIndex;

    @Override
    public GetDirectoryByIndexFn getGetDirectoryByIndex() {
        return getDirectoryByIndex;
    }

    public void setGetDirectoryByIndex(GetDirectoryByIndexFn value) {
        getDirectoryByIndex = value;
    }

    private GetDirectoryByRefFn getDirectoryByRef;

    @Override
    public GetDirectoryByRefFn getGetDirectoryByRef() {
        return getDirectoryByRef;
    }

    public void setGetDirectoryByRef(GetDirectoryByRefFn value) {
        getDirectoryByRef = value;
    }

    private AllocateFileFn allocateFile;

    @Override
    public AllocateFileFn getAllocateFile() {
        return allocateFile;
    }

    public void setAllocateFile(AllocateFileFn value) {
        allocateFile = value;
    }

    private ForgetFileFn forgetFile;

    @Override
    public ForgetFileFn getForgetFile() {
        return forgetFile;
    }

    public void setForgetFile(ForgetFileFn value) {
        forgetFile = value;
    }

    private boolean readOnly;

    @Override
    public boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean value) {
        readOnly = value;
    }
}
