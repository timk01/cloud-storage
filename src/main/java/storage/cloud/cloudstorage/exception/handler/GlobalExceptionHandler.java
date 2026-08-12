package storage.cloud.cloudstorage.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import storage.cloud.cloudstorage.exception.*;
import storage.cloud.cloudstorage.response.ErrorResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<Class<? extends BaseAppException>, HttpStatus> KNOWN_EXCEPTIONS_STATUS_MAP
            = new HashMap<>();

    static {
        KNOWN_EXCEPTIONS_STATUS_MAP.put(InvalidLoginDataException.class, HttpStatus.UNAUTHORIZED);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(UnauthorizedActionException.class, HttpStatus.UNAUTHORIZED);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(UserNotAuthenticatedException.class, HttpStatus.UNAUTHORIZED);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(UserAlreadyExistsException.class, HttpStatus.CONFLICT);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(FolderAlreadyExistsException.class, HttpStatus.CONFLICT);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(FolderNotFoundException.class, HttpStatus.NOT_FOUND);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(ParentFolderHasNotFoundException.class, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BaseAppException.class)
    public ResponseEntity<ErrorResponse> handleKnownExceptions(BaseAppException exception) {
        HttpStatus status = KNOWN_EXCEPTIONS_STATUS_MAP.getOrDefault(
                exception.getClass(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage());

        return new ResponseEntity<>(
                errorResponse,
                status
        );
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return new ResponseEntity<>(
                new ErrorResponse("Validation failed"),
                status
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(
                new ErrorResponse("Unknown exception"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
