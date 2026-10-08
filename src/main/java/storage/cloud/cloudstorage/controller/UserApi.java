package storage.cloud.cloudstorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.ErrorResponse;
import storage.cloud.cloudstorage.response.UsernameResponse;

@ApiResponse(
        responseCode = "500",
        description = "Unknown server error"
)
@RequestMapping("/api")
public interface UserApi {

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
                    description = "Validation error"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username already exists"
            )
    })
    @PostMapping("/auth/sign-up")
    ResponseEntity<UsernameResponse> registerUser(
            @Valid @RequestBody UserRegisterRequest userRegisterDto,
            HttpSession session
    );

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
                    description = "User is not authorized"
            )
    })
    @PostMapping("/auth/sign-out")
    ResponseEntity<Void> logout(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            HttpSession session
    );

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
                    description = "Validation error"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            )
    })
    @PostMapping("/auth/sign-in")
    ResponseEntity<UsernameResponse> login(
            @Valid @RequestBody UserLoginRequest userLoginDto,
            HttpSession session
    );

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
                    description = "User is not authorized"
            )
    })
    @GetMapping("/user/me")
    ResponseEntity<UsernameResponse> getCurrentUser(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @Parameter(hidden = true)
            @SessionAttribute(name = "username", required = false) String username
    );
}
