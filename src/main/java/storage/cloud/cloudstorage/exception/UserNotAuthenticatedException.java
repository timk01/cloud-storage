package storage.cloud.cloudstorage.exception;

public class UserNotAuthenticatedException extends BaseAppException {
    public UserNotAuthenticatedException(String message) {
        super(message);
    }

    public UserNotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
