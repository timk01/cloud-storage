package storage.cloud.cloudstorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.exception.managed.UserNotAuthenticatedException;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.ErrorResponse;
import storage.cloud.cloudstorage.response.UserResponse;
import storage.cloud.cloudstorage.response.UsernameResponse;
import storage.cloud.cloudstorage.service.UserService;

@ApiResponse(
        responseCode = "500",
        description = "Unknown server error",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
        )
)
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    @Operation(
            tags = "Authentication",
            summary = "Sign up",
            description = "Registers a new user and creates a session"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsernameResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/auth/sign-up")
    public ResponseEntity<UsernameResponse> registerUser(
            @Valid @RequestBody UserRegisterRequest userRegisterDto,
            HttpSession session
    ) {
        UserResponse register = service.register(userRegisterDto);
        session.setAttribute("userId", register.id());
        session.setAttribute("username", register.username());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UsernameResponse(register.username()));
    }

    @Operation(
            tags = "Authentication",
            summary = "Sign out",
            description = "Invalidates current user session"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User signed out"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "User is not authorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/auth/sign-out")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true)
            @SessionAttribute(name = "userId", required = false) Long userId,
            HttpSession session
    ) {
        if (userId == null) {
            throw new UserNotAuthenticatedException(
                    "Cannot do logout since user is not authorized"
            );
        }

        session.invalidate();

        log.info(
                "User is logged out: userId={}",
                userId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @Operation(
            tags = "Authentication",
            summary = "Sign in",
            description = "Authenticates user and creates a session"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User authenticated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsernameResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/auth/sign-in")
    public ResponseEntity<UsernameResponse> login(
            @Valid @RequestBody UserLoginRequest userLoginDto,
            HttpSession session
    ) {
        UserResponse login = service.login(userLoginDto);
        session.setAttribute("userId", login.id());
        session.setAttribute("username", login.username());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new UsernameResponse(login.username()));
    }

    @Operation(
            tags = "Users",
            summary = "Current user",
            description = "Gets current authenticated user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Current user received",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsernameResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "User is not authorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/user/me")
    public ResponseEntity<UsernameResponse> getCurrentUser(
            @Parameter(hidden = true)
            @SessionAttribute(name = "userId", required = false) Long userId,
            @Parameter(hidden = true)
            @SessionAttribute(name = "username", required = false) String username
    ) {
        if (userId == null || username == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        log.debug(
                "Got current user: userId={}, username={}",
                userId,
                username
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new UsernameResponse(username));
    }
}