package storage.cloud.cloudstorage.repository;

public record StorageItem(String resourcePath, boolean directory, long size) {
}
