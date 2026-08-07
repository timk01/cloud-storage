package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.repository.UserRepository;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UserResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @Test
    public void registerIsSucceeded() {
        String username = "tim1";
        boolean nameIsFound = false;
        when(repository.existsByUsername(username)).thenReturn(nameIsFound);

        String passwordOriginal = "sadfasfkljkjl22##";
        String passwordHashed = "sadfasfkljkjl22##_mocked_hash";
        when(encoder.encode(passwordOriginal)).thenReturn(passwordHashed);

        User userEntity = new User(username, passwordHashed);
        User savedUser = new User(username, passwordHashed);
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        when(repository.save(any(User.class))).thenReturn(savedUser); //userEntity ? почему

        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);
        UserResponse expected = new UserResponse(1L, username);
        UserResponse actual = service.register(dto);

        verify(repository, times(1)).existsByUsername(username);
        verify(encoder, times(1)).encode(passwordOriginal);
        verify(repository, times(1)).save(any(User.class));

        assertThat(actual).isNotNull();
        assertThat(actual.username()).isEqualTo(expected.username());
        assertThat(actual.id()).isEqualTo(expected.id());
    }
}