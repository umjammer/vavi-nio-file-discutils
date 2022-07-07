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
    private Stream _rawStream;

    public Stream getRawStream() {
        return _rawStream;
    }

    public void setRawStream(Stream value) {
        _rawStream = value;
    }

    private AttributeDefinitions _attributeDefinitions;

    public AttributeDefinitions getAttributeDefinitions() {
        return _attributeDefinitions;
    }

    public void setAttributeDefinitions(AttributeDefinitions value) {
        _attributeDefinitions = value;
    }

    private UpperCase _upperCase;

    public UpperCase getUpperCase() {
        return _upperCase;
    }

    public void setUpperCase(UpperCase value) {
        _upperCase = value;
    }

    private BiosParameterBlock _biosParameterBlock;

    public BiosParameterBlock getBiosParameterBlock() {
        return _biosParameterBlock;
    }

    public void setBiosParameterBlock(BiosParameterBlock value) {
        _biosParameterBlock = value;
    }

    private MasterFileTable _mft;

    public MasterFileTable getMft() {
        return _mft;
    }

    public void setMft(MasterFileTable value) {
        _mft = value;
    }

    private ClusterBitmap _clusterBitmap;

    public ClusterBitmap getClusterBitmap() {
        return _clusterBitmap;
    }

    public void setClusterBitmap(ClusterBitmap value) {
        _clusterBitmap = value;
    }

    private SecurityDescriptors _securityDescriptors;

    public SecurityDescriptors getSecurityDescriptors() {
        return _securityDescriptors;
    }

    public void setSecurityDescriptors(SecurityDescriptors value) {
        _securityDescriptors = value;
    }

    private ObjectIds _objectIds;

    public ObjectIds getObjectIds() {
        return _objectIds;
    }

    public void setObjectIds(ObjectIds value) {
        _objectIds = value;
    }

    private ReparsePoints _reparsePoints;

    public ReparsePoints getReparsePoints() {
        return _reparsePoints;
    }

    public void setReparsePoints(ReparsePoints value) {
        _reparsePoints = value;
    }

    private Quotas _quotas;

    public Quotas getQuotas() {
        return _quotas;
    }

    public void setQuotas(Quotas value) {
        _quotas = value;
    }

    private NtfsOptions _options;

    public NtfsOptions getOptions() {
        return _options;
    }

    public void setOptions(NtfsOptions value) {
        _options = value;
    }

    private GetFileByIndexFn _getFileByIndex;

    public GetFileByIndexFn getGetFileByIndex() {
        return _getFileByIndex;
    }

    public void setGetFileByIndex(GetFileByIndexFn value) {
        _getFileByIndex = value;
    }

    private GetFileByRefFn _getFileByRef;

    public GetFileByRefFn getGetFileByRef() {
        return _getFileByRef;
    }

    public void setGetFileByRef(GetFileByRefFn value) {
        _getFileByRef = value;
    }

    private GetDirectoryByIndexFn _getDirectoryByIndex;

    public GetDirectoryByIndexFn getGetDirectoryByIndex() {
        return _getDirectoryByIndex;
    }

    public void setGetDirectoryByIndex(GetDirectoryByIndexFn value) {
        _getDirectoryByIndex = value;
    }

    private GetDirectoryByRefFn _getDirectoryByRef;

    public GetDirectoryByRefFn getGetDirectoryByRef() {
        return _getDirectoryByRef;
    }

    public void setGetDirectoryByRef(GetDirectoryByRefFn value) {
        _getDirectoryByRef = value;
    }

    private AllocateFileFn _allocateFile;

    public AllocateFileFn getAllocateFile() {
        return _allocateFile;
    }

    public void setAllocateFile(AllocateFileFn value) {
        _allocateFile = value;
    }

    private ForgetFileFn _forgetFile;

    public ForgetFileFn getForgetFile() {
        return _forgetFile;
    }

    public void setForgetFile(ForgetFileFn value) {
        _forgetFile = value;
    }

    private boolean _readOnly;

    public boolean getReadOnly() {
        return _readOnly;
    }

    public void setReadOnly(boolean value) {
        _readOnly = value;
    }
}
