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
import storage.cloud.cloudstorage.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegisterRequst userRegisterDto) {
        UserResponse register = service.register(userRegisterDto);
        return new ResponseEntity<>(register.userName(), HttpStatus.CREATED);
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
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginRequest userLoginDto, HttpSession session) {
        UserResponse login = service.login(userLoginDto);
        session.setAttribute("userId", login.id());
        session.setAttribute("userName", login.userName());
        return new ResponseEntity<>(login.userName(), HttpStatus.OK);
    }


    @GetMapping("/user/me")
    public ResponseEntity<String> getCurrentUser(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @SessionAttribute(name = "userName", required = false) String userName
    ) {
        if (userId == null || userName == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        return new ResponseEntity<>(userName, HttpStatus.OK);
    }
}
