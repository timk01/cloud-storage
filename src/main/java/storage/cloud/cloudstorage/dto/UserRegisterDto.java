package storage.cloud.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterDto(
        @NotBlank(message = "User name should not be null and must contain at least one non-whitespace character")
        @Size(min = 2, max = 100, message = "User name must be between {min} and {max} characters")
        String userName,

        @NotBlank(message = "User password should not be null and must contain at least one non-whitespace character")
        @Size(min = 2, max = 100, message = "User password must be between {min} and {max} characters")
        String password
) {
}