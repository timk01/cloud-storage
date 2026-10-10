package storage.cloud.cloudstorage.request;

public record UserLoginRequest(
        @ValidUserName
        String username,

        @ValidPassword
        String password
) {
}