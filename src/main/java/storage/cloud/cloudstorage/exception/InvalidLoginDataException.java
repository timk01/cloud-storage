package storage.cloud.cloudstorage.exception;

public class InvalidLoginDataException extends BaseAppException {
    public InvalidLoginDataException(String message) {
        super(message);
    }

    public InvalidLoginDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
