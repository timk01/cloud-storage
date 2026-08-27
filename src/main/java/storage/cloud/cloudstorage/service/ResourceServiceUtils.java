package storage.cloud.cloudstorage.service;

import storage.cloud.cloudstorage.exception.managed.SourceResourceNotFoundException;

public final class ResourceServiceUtils {

    private ResourceServiceUtils() {
    }

    public static FolderPathParts getResult(String resourcePath, String fullMinioPath) {
        String trimmedFullPath = removeTrailingSlash(fullMinioPath);
        int lastFolderIndex = trimmedFullPath.lastIndexOf("/");
        String minioParentPath = trimmedFullPath.substring(0, lastFolderIndex + 1);

        String resourceParentPath = extractParentPathForFolder(resourcePath);
        String lastFolder = fullMinioPath.substring(lastFolderIndex + 1);
        String folderName = removeTrailingSlash(lastFolder);

        return new FolderPathParts(minioParentPath, resourceParentPath, folderName);
    }

    public record FolderPathParts(
            String minioParentPath,
            String resourceParentPath,
            String folderName
    ) {
    }

    public static String extractParentPathForFolder(String folderPath) {
        String trimmedOriginalPath = removeTrailingSlash(folderPath);
        return extractParentPathForFile(trimmedOriginalPath);
    }

    public static String extractParentPathForFile(String filePath) {
        int lastFolderIndex = filePath.lastIndexOf("/");
        return filePath.substring(0, lastFolderIndex + 1);
    }

    public static String removeTrailingSlash(String fullname) {
        return fullname.substring(0, fullname.length() - 1);
    }

    public static String extractName(String fullname) {
        return fullname.substring(fullname.lastIndexOf("/") + 1);
    }

    public static String buildPreparedPath(String preparedRoot, String path) {
        return preparedRoot + path;
    }

    public static String buildPreparedRoot(Long userId, String minioBucketName) {
        String[] splitBucket = minioBucketName.split("-");
        return splitBucket[0] + "-" + userId + "-" + splitBucket[1] + "/";
    }

    public static void validateResourceExists(boolean doesResourceExist, String pathTillResource) {
        if (!doesResourceExist) {
            throw new SourceResourceNotFoundException(
                    String.format(
                            "Resource is not found by path: %s ", pathTillResource
                    )
            );
        }
    }
}
