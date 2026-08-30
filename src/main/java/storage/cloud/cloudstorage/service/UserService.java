package storage.cloud.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.exception.managed.InvalidLoginDataException;
import storage.cloud.cloudstorage.exception.managed.UserAlreadyExistsException;
import storage.cloud.cloudstorage.repository.UserRepository;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UserResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserResponse register(UserRegisterRequest userRegisterDto) {
        if (repository.existsByUsername(userRegisterDto.username())) {
            throw new UserAlreadyExistsException("User name is already taken");
        }

        String encodedPass = encoder.encode(userRegisterDto.password());
        User user = repository.save(new User(userRegisterDto.username(), encodedPass));

        log.info(
                "User is registered: userId={}, name={}",
                user.getId(),
                user.getUsername()
        );

        return new UserResponse(user.getId(), user.getUsername());
    }

    public UserResponse login(UserLoginRequest userLoginDto) {
        User user = repository.findByUsername(userLoginDto.username())
                .orElseThrow(() -> new InvalidLoginDataException("Invalid credentials"));

        if (!encoder.matches(userLoginDto.password(), user.getPassword())) {
            throw new InvalidLoginDataException("Invalid credentials");
        }

        log.info(
                "User is logged in: userId={}, name={}",
                user.getId(),
                user.getUsername()
        );

        return new UserResponse(user.getId(), user.getUsername());
    }
}
