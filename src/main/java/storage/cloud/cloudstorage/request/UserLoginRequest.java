package storage.cloud.cloudstorage.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
        @NotBlank(message = "Username should not be null and must contain at least one non-whitespace character")
        @Size(min = 3, max = 100, message = "User name must be between {min} and {max} characters")
        String username,

        @NotBlank(message = "User password should not be null and must contain at least one non-whitespace character")
        @Size(min = 3, max = 50, message = "User password must be between {min} and {max} characters")
        String password
) {
}