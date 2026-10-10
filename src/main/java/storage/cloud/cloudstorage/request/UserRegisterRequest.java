package storage.cloud.cloudstorage.request;

public record UserRegisterRequest(
        @ValidUserName
        String username,

        @ValidPassword
        String password
) {
}