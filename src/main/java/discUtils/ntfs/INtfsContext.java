
package discUtils.ntfs;

import dotnet4j.io.Stream;


public interface INtfsContext {

    AllocateFileFn getAllocateFile();

    AttributeDefinitions getAttributeDefinitions();

    BiosParameterBlock getBiosParameterBlock();

    ClusterBitmap getClusterBitmap();

    ForgetFileFn getForgetFile();

    GetDirectoryByIndexFn getGetDirectoryByIndex();

    GetDirectoryByRefFn getGetDirectoryByRef();

    GetFileByIndexFn getGetFileByIndex();

    GetFileByRefFn getGetFileByRef();

    MasterFileTable getMft();

    ObjectIds getObjectIds();

    NtfsOptions getOptions();

    Quotas getQuotas();

    Stream getRawStream();

    boolean getReadOnly();

    ReparsePoints getReparsePoints();

    SecurityDescriptors getSecurityDescriptors();

    UpperCase getUpperCase();
}
