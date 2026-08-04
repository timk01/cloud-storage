package storage.cloud.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.dto.UserLoginDto;
import storage.cloud.cloudstorage.dto.UserRegisterDto;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.repository.UserRepository;
import storage.cloud.cloudstorage.response.UserResponse;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public UserResponse register(UserRegisterDto userRegisterDto) {
        if (repository.existsByUserName(userRegisterDto.userName())) {
            throw new RuntimeException("User name is already taken");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPass = encoder.encode(userRegisterDto.password());

        User user = repository.save(new User(userRegisterDto.userName(), encodedPass));
        return new UserResponse(user.getId(), user.getUserName()); //convert to back, since user -- entity
        //additionally, make encoder as field
    }

    //стринга в возвращаемом - временно, нужен по идее ДТО
    public UserResponse login(UserLoginDto userLoginDto) {
        User user = repository.findByUserName(userLoginDto.userName())
                .orElseThrow(() -> new RuntimeException("User is not found"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(userLoginDto.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentianls");
        }

        return new UserResponse(user.getId(), user.getUserName()); //convert to back, since user -- entity
    }
}
