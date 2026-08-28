package storage.cloud.cloudstorage.exception.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import storage.cloud.cloudstorage.exception.managed.*;
import storage.cloud.cloudstorage.response.ErrorResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<Class<? extends BaseAppException>, HttpStatus> KNOWN_EXCEPTIONS_STATUS_MAP
            = new HashMap<>();

    static {
        KNOWN_EXCEPTIONS_STATUS_MAP.put(InvalidFilesException.class, HttpStatus.BAD_REQUEST);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(InvalidFileNameException.class, HttpStatus.BAD_REQUEST);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(ResourceTypeMismatchException.class, HttpStatus.BAD_REQUEST);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(InvalidLoginDataException.class, HttpStatus.UNAUTHORIZED);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(UnauthorizedActionException.class, HttpStatus.UNAUTHORIZED);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(UserNotAuthenticatedException.class, HttpStatus.UNAUTHORIZED);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(ParentFolderHasNotFoundException.class, HttpStatus.NOT_FOUND);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(FolderNotFoundException.class, HttpStatus.NOT_FOUND);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(SourceResourceNotFoundException.class, HttpStatus.NOT_FOUND);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(UserAlreadyExistsException.class, HttpStatus.CONFLICT);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(FolderAlreadyExistsException.class, HttpStatus.CONFLICT);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(FileAlreadyExistsException.class, HttpStatus.CONFLICT);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(DestinationResourceAlreadyExistsException.class, HttpStatus.CONFLICT);

        KNOWN_EXCEPTIONS_STATUS_MAP.put(SourceAndDestinationAreEqualException.class, HttpStatus.CONFLICT);
        KNOWN_EXCEPTIONS_STATUS_MAP.put(ResourceMoveConflictException.class, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BaseAppException.class)
    public ResponseEntity<ErrorResponse> handleKnownExceptions(BaseAppException exception) {
        HttpStatus status = KNOWN_EXCEPTIONS_STATUS_MAP.getOrDefault(
                exception.getClass(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage());

        log.warn(
                "Handled application exception with status: {}, message: {}",
                status,
                exception.getMessage()
        );

        return new ResponseEntity<>(
                errorResponse,
                status
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception
    ) {
        HttpStatus status = HttpStatus.PAYLOAD_TOO_LARGE;

        log.warn(
                "Upload size exceeded with status: {}, message: {}",
                status,
                exception.getMessage()
        );

        return new ResponseEntity<>(
                new ErrorResponse("Maximum upload size exceeded"),
                status
        );
    }

    @ExceptionHandler(
            {
                    MethodArgumentNotValidException.class,
                    ConstraintViolationException.class,
                    HandlerMethodValidationException.class,
                    MissingServletRequestParameterException.class,
                    MissingServletRequestPartException.class
            }
    )
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            Exception exception
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        log.warn(
                "Invalid request with status: {}, message: {}",
                status,
                exception.getMessage()
        );

        return new ResponseEntity<>(
                new ErrorResponse("Validation failed"),
                status
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error(
                "Unknown exception happened during program work with status: {}; Exception stack:",
                status,
                exception
        );

        return new ResponseEntity<>(
                new ErrorResponse("Unknown exception"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
