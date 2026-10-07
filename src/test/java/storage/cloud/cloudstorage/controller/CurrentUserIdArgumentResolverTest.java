package storage.cloud.cloudstorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.NativeWebRequest;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserIdArgumentResolverTest {

    private final CurrentUserIdArgumentResolver resolver =
            new CurrentUserIdArgumentResolver();

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Test
    void resolvesUserIdFromSession() throws Exception {
        when(webRequest.getNativeRequest(HttpServletRequest.class))
                .thenReturn(request);

        when(request.getSession(false))
                .thenReturn(session);

        when(session.getAttribute("userId"))
                .thenReturn(1L);

        Object result = resolver.resolveArgument(
                null,
                null,
                webRequest,
                null
        );

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void throwsExceptionWhenSessionDoesNotExist() {
        when(webRequest.getNativeRequest(HttpServletRequest.class))
                .thenReturn(request);

        when(request.getSession(false))
                .thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(
                null,
                null,
                webRequest,
                null
        ))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void throwsExceptionWhenUserIdDoesNotExist() {
        when(webRequest.getNativeRequest(HttpServletRequest.class))
                .thenReturn(request);

        when(request.getSession(false))
                .thenReturn(session);

        when(session.getAttribute("userId"))
                .thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(
                null,
                null,
                webRequest,
                null
        ))
                .isInstanceOf(UnauthorizedActionException.class);
    }
}