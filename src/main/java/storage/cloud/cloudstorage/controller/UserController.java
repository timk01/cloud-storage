package storage.cloud.cloudstorage.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UserResponse;
import storage.cloud.cloudstorage.response.UsernameResponse;
import storage.cloud.cloudstorage.service.UserService;

@Slf4j
@RequiredArgsConstructor
@RestController
public class UserController implements UserApi {

    private final UserService service;

    @Override
    public ResponseEntity<UsernameResponse> registerUser(
            UserRegisterRequest userRegisterDto,
            HttpSession session
    ) {
        UserResponse register = service.register(userRegisterDto);
        session.setAttribute("userId", register.id());
        session.setAttribute("username", register.username());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UsernameResponse(register.username()));
    }

    @Override
    public ResponseEntity<Void> logout(
            Long userId,
            HttpSession session
    ) {
        session.invalidate();

        log.info(
                "User is logged out: userId={}",
                userId
        );

        return ResponseEntity
                .noContent()
                .build();
    }


    @Override
    public ResponseEntity<UsernameResponse> login(
            UserLoginRequest userLoginDto,
            HttpSession session
    ) {
        UserResponse login = service.login(userLoginDto);
        session.setAttribute("userId", login.id());
        session.setAttribute("username", login.username());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new UsernameResponse(login.username()));
    }

    @Override
    public ResponseEntity<UsernameResponse> getCurrentUser(
            Long userId,
            String username
    ) {
        if (username == null) {
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