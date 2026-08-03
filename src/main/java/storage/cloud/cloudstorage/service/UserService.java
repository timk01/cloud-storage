package storage.cloud.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.dto.UserLoginDto;
import storage.cloud.cloudstorage.dto.UserRegisterDto;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public User register(UserRegisterDto userRegisterDto) {
        if (repository.existsByUserName(userRegisterDto.userName())) {
            throw new RuntimeException("User name is already taken");
        }

        //toDo something with pass here ? (hashing ?)

        User user = repository.save(new User(userRegisterDto.userName(), userRegisterDto.password()));
        return user;
    }

    public User login(UserLoginDto userLoginDto) {
        User user = repository.findByUserName(userLoginDto.userName())
                .orElseThrow(() -> new RuntimeException("User is not found"));

        //compare current Password with gotten

        //еесли ОК - создать сессию  редис ИЛИ ?

        return new User("1", "123");
    }
}
