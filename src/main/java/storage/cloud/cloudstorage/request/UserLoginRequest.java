package storage.cloud.cloudstorage.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
        @NotBlank(message = "Username should not be null and must contain at least one non-whitespace character")
        @Size(min = 5, max = 20, message = "User name must be between {min} and {max} characters")
        @Pattern(regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$", message = "Invalid username")
        String username,

        @NotBlank(message = "User password should not be null and must contain at least one non-whitespace character")
        @Size(min = 5, max = 20, message = "User password must be between {min} and {max} characters")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>\\[\\]\\\\/`~+=\\-_';]*$", message = "Invalid password")
        String password
) {
}