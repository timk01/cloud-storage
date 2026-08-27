package storage.cloud.cloudstorage.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.exception.managed.UserNotAuthenticatedException;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UserResponse;
import storage.cloud.cloudstorage.response.UsernameResponse;
import storage.cloud.cloudstorage.service.UserService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<UsernameResponse> registerUser(@Valid @RequestBody UserRegisterRequest userRegisterDto,
                                                         HttpSession session
    ) {
        UserResponse register = service.register(userRegisterDto);
        session.setAttribute("userId", register.id());
        session.setAttribute("username", register.username());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UsernameResponse(register.username()));
    }

    @PostMapping("/auth/sign-out")
    public ResponseEntity<Void> logout(@SessionAttribute(name = "userId", required = false) Long userId,
                                       HttpSession session
    ) {
        if (userId == null) {
            throw new UserNotAuthenticatedException("Cannot do logout since user is not authorized");
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

    @PostMapping("/auth/sign-in")
    public ResponseEntity<UsernameResponse> login(@Valid @RequestBody UserLoginRequest userLoginDto, HttpSession session) {
        UserResponse login = service.login(userLoginDto);
        session.setAttribute("userId", login.id());
        session.setAttribute("username", login.username());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new UsernameResponse(login.username()));
    }

    @GetMapping("/user/me")
    public ResponseEntity<UsernameResponse> getCurrentUser(
            @SessionAttribute(name = "userId", required = false) Long userId,
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
