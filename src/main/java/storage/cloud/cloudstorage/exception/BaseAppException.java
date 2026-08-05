package storage.cloud.cloudstorage.exception;

public class BaseAppException extends RuntimeException {
    public BaseAppException(String message) {
        super(message);
    }

    public BaseAppException(String message, Throwable cause) {
        super(message, cause);
    }
}