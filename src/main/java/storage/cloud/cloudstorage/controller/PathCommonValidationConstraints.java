package storage.cloud.cloudstorage.controller;

public final class PathCommonValidationConstraints {

    public static final String PATH_STRICT_VALIDATOR_REGEXP =
            "^$|^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";

    public static final String WRONG_PATH =
            "Wrong path is provided";

    public static final String PATH_COMMON_VALIDATOR_REGEXP = "^[a-zA-Zа-яА-ЯёЁ0-9_\\s./-]+$";
    public static final String INVALID_SYMBOLS_IN_PATH = "Invalid symbols in path are detected";

    public static final String PATH_POST_STRICT_VALIDATOR_REGEXP = "^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";
    private PathCommonValidationConstraints() {
    }
}
