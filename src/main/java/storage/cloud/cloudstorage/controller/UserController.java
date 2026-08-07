package storage.cloud.cloudstorage.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.exception.UnauthorizedActionException;
import storage.cloud.cloudstorage.exception.UserNotAuthenticatedException;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequst;
import storage.cloud.cloudstorage.response.UserResponse;
import storage.cloud.cloudstorage.response.UsernameResponse;
import storage.cloud.cloudstorage.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<UsernameResponse> registerUser(@Valid @RequestBody UserRegisterRequst userRegisterDto,
            HttpSession session
    ) {
        UserResponse register = service.register(userRegisterDto);
        session.setAttribute("userId", register.id());
        session.setAttribute("username", register.username());
        return new ResponseEntity<>(
                new UsernameResponse(register.username()),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/auth/sign-out")
    public ResponseEntity<Void> logout(@SessionAttribute(name = "userId", required = false) Long userId,
                                       HttpSession session
    ) {
        if (userId == null) {
            throw new UserNotAuthenticatedException("Cannot do logout since user is not authorized");
        }

        session.invalidate();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<UsernameResponse> login(@Valid @RequestBody UserLoginRequest userLoginDto, HttpSession session) {
        UserResponse login = service.login(userLoginDto);
        session.setAttribute("userId", login.id());
        session.setAttribute("username", login.username());
        return new ResponseEntity<>(
                new UsernameResponse(login.username()),
                HttpStatus.OK
        );
    }


    @GetMapping("/user/me")
    public ResponseEntity<UsernameResponse> getCurrentUser(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @SessionAttribute(name = "username", required = false) String username
    ) {
        if (userId == null || username == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        return new ResponseEntity<>(new UsernameResponse(username), HttpStatus.OK);
    }
}
