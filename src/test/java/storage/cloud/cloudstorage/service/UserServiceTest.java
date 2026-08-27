package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.exception.managed.InvalidLoginDataException;
import storage.cloud.cloudstorage.exception.managed.UserAlreadyExistsException;
import storage.cloud.cloudstorage.repository.UserRepository;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UserResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @Test
    public void registerIsSucceeded() {
        String username = "tim1";
        boolean nameIsFound = false;
        when(repository.existsByUsername(username)).thenReturn(nameIsFound);

        String passwordOriginal = "sadfasfkljkjl22##";
        String passwordHashed = "sadfasfkljkjl22##_mocked_hash";
        when(encoder.encode(passwordOriginal)).thenReturn(passwordHashed);

        User savedUser = new User(username, passwordHashed);
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        when(repository.save(any(User.class))).thenReturn(savedUser);

        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);
        UserResponse expected = new UserResponse(1L, username);
        UserResponse actual = service.register(dto);

        verify(repository, times(1)).existsByUsername(username);
        verify(encoder, times(1)).encode(passwordOriginal);
        verify(repository, times(1)).save(userArgumentCaptor.capture());

        User captorValue = userArgumentCaptor.getValue();

        assertThat(captorValue).isNotNull();
        assertThat(captorValue.getUsername()).isEqualTo(username);
        assertThat(captorValue.getPassword()).isEqualTo(passwordHashed);

        assertThat(actual).isNotNull();
        assertThat(actual.username()).isEqualTo(expected.username());
        assertThat(actual.id()).isEqualTo(expected.id());
    }

    @Test
    public void loginIsSucceeded() {
        String username = "tim1";
        String passwordHashed = "sadfasfkljkjl22##_mocked_hash";
        User user = new User(username, passwordHashed);
        ReflectionTestUtils.setField(user, "id", 1L);
        Optional<User> savedUser = Optional.of(user);
        when(repository.findByUsername(username)).thenReturn(savedUser);

        String passwordOriginal = "sadfasfkljkjl22##";
        when(encoder.matches(passwordOriginal, passwordHashed)).thenReturn(true);

        UserLoginRequest dto = new UserLoginRequest(username, passwordOriginal);
        UserResponse expected = new UserResponse(1L, username);
        UserResponse actual = service.login(dto);

        verify(repository, times(1)).findByUsername(username);
        verify(encoder, times(1)).matches(passwordOriginal, passwordHashed);
        assertThat(actual).isNotNull();
        assertThat(actual.username()).isEqualTo(expected.username());
        assertThat(actual.id()).isEqualTo(expected.id());
    }

    @Test
    public void registerIsFailedSinceUserExists() {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";

        boolean nameIsFound = true;
        when(repository.existsByUsername(username)).thenReturn(nameIsFound);

        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);

        assertThatThrownBy(() -> service.register(dto))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(repository, times(1)).existsByUsername(username);
        verify(encoder, never()).encode(passwordOriginal);
    }

    @Test
    public void loginIsFailedSinceUserIsNotFound() {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";
        String passwordHashed = "sadfasfkljkjl22##_mocked_hash";

        when(repository.findByUsername(username)).thenReturn(Optional.empty());

        UserLoginRequest dto = new UserLoginRequest(username, passwordOriginal);

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(InvalidLoginDataException.class);
        verify(repository, times(1)).findByUsername(username);
        verify(encoder, never()).matches(passwordOriginal, passwordHashed);
    }

    @Test
    public void loginIsFailedSincePasswordsAreDifferent() {
        String username = "tim1";
        String passwordHashed = "sadfasfkljkjl22##_mocked_hash";
        User user = new User(username, passwordHashed);
        ReflectionTestUtils.setField(user, "id", 1L);
        Optional<User> savedUser = Optional.of(user);
        when(repository.findByUsername(username)).thenReturn(savedUser);

        String passwordOriginal = "sadfasfkljkjl22##";
        when(encoder.matches(passwordOriginal, passwordHashed)).thenReturn(false);;

        UserLoginRequest dto = new UserLoginRequest(username, passwordOriginal);

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(InvalidLoginDataException.class);
        verify(repository, times(1)).findByUsername(username);
        verify(encoder, times(1)).matches(passwordOriginal, passwordHashed);
    }
}