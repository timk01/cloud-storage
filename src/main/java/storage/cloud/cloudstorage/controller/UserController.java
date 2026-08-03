package storage.cloud.cloudstorage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import storage.cloud.cloudstorage.dto.UserRegisterDto;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegisterDto userRegisterDto) {
        try {
            User register = service.register(userRegisterDto);
            return new ResponseEntity<>(register.getUserName(), HttpStatus.CREATED);
        } catch (RuntimeException rte) {
            return new ResponseEntity<>(rte.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
