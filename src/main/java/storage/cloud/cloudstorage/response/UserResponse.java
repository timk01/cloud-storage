package storage.cloud.cloudstorage.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public record UserResponse(
        Long id,
        String userName
) {
}