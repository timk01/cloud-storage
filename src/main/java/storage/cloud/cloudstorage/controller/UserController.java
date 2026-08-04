package storage.cloud.cloudstorage.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.dto.UserLoginDto;
import storage.cloud.cloudstorage.dto.UserRegisterDto;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.response.UserResponse;
import storage.cloud.cloudstorage.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegisterDto userRegisterDto) {
        try {
            UserResponse register = service.register(userRegisterDto);
            return new ResponseEntity<>(register.userName(), HttpStatus.CREATED);
        } catch (RuntimeException rte) {
            return new ResponseEntity<>(rte.getMessage(), HttpStatus.BAD_REQUEST);
        }
        //подумать про ошибку ? (тут же не должно всее превращаться в 3 проект)
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginDto userLoginDto, HttpSession session) {
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
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to view this page."); //401! потом глобалЭксепшенХенлдр
        }

        return new ResponseEntity<>(userName, HttpStatus.OK);
    }
}
