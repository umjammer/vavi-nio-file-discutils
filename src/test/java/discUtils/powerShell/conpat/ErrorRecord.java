
package discUtils.powerShell.conpat;

import java.io.Serializable;


public abstract class ErrorRecord implements Serializable {

    public ErrorRecord(ErrorRecord errorRecord, Exception replaceParentContainsErrorRecordException) {
    }

    public ErrorRecord(Exception exception, String errorId, ErrorCategory errorCategory, Object targetObject) {
    }

    protected ErrorRecord(SerializationInfo info, StreamingContext context) {
    }

    public abstract ErrorCategoryInfo getCategoryInfo();

    public abstract ErrorDetails getErrorDetails();

    public abstract void setErrorDetails(ErrorDetails v);

    public abstract Exception getException();

    public abstract String getFullyQualifiedErrorId();

    public abstract InvocationInfo getInvocationInfo();

    public abstract ReadOnlyCollection<Integer> getPipelineIterationInfo();

    public abstract String getScriptStackTrace();

    public abstract Object getTargetObject();

    public abstract void getObjectData(SerializationInfo info, StreamingContext context);
}
