
package DiscUtils.PowerShell.Conpat;

import java.io.Serializable;


public class ErrorRecord implements Serializable {
    public ErrorRecord(ErrorRecord errorRecord, Exception replaceParentContainsErrorRecordException) {
    }

    public ErrorRecord(Exception exception, String errorId, ErrorCategory errorCategory, Object targetObject) {
    }

    protected ErrorRecord(SerializationInfo info, StreamingContext context) {
    }

    public ErrorCategoryInfo getCategoryInfo() {
        ;
    }

    public ErrorDetails getErrorDetails() {
        ;
    }

    public void setErrorDetails(ErrorDetails v) {
        ;
    }

    public Exception getException() {
    }

    public String getFullyQualifiedErrorId() {
        ;
    }

    public InvocationInfo getInvocationInfo() {
        ;
    }

    public ReadOnlyCollection<Integer> getPipelineIterationInfo() {
        ;
    }

    public String getScriptStackTrace() {
        ;
    }

    public Object getTargetObject() {
        ;
    }

    public abstract void GetObjectData(SerializationInfo info, StreamingContext context);
}
