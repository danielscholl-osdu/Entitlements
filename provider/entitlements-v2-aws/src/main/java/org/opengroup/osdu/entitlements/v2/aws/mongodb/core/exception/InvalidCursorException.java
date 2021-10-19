package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.exception;

public class InvalidCursorException extends RuntimeException {
    Exception innerException;

    public InvalidCursorException(String message)
    {
        super(message);
    }

    public InvalidCursorException(Exception innerException){
        super(innerException);
        this.innerException = innerException;
    }
}
