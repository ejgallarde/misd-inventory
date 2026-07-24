package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String storageMode;
    private final Path filesystemRootPath;

    public DocumentStorageService(ObjectProvider<S3Client> s3ClientProvider,
            @Value("${minio.bucket-name:}") String bucketName,
            @Value("${storage.mode:filesystem}") String storageMode,
            @Value("${storage.filesystem.root-path:D:/misd}") String filesystemRootPath) {
        this.s3Client = s3ClientProvider.getIfAvailable();
        this.bucketName = bucketName;
        this.storageMode = storageMode == null ? "filesystem" : storageMode.trim().toLowerCase(Locale.ROOT);
        this.filesystemRootPath = Paths.get(filesystemRootPath);
    }

    /**
     * Uploads a file to either local filesystem or MinIO, depending on
     * storage.mode.
     *
     * @param file      The uploaded Multipart file from the controller
     * @param assetType e.g., "vehicles", "it-equipment", "properties"
     * @param entityId  The unique identifier of the asset (e.g., PlateNumber or
     *                  AssetTag)
     * @return The generated storage key/path persisted in the database
     */
    public String uploadDocument(MultipartFile file, String assetType, String entityId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        String objectKey = buildObjectKey(assetType, entityId, file.getOriginalFilename());

        return switch (storageMode) {
            case "filesystem" -> uploadToFilesystem(file, objectKey);
            case "minio" -> uploadToMinio(file, objectKey);
            default -> throw new IllegalStateException("Unsupported storage mode: " + storageMode);
        };
    }

    public InputStream readDocument(String objectKey) throws IOException {
        return switch (storageMode) {
            case "filesystem" -> readFromFilesystem(objectKey);
            case "minio" -> readFromMinio(objectKey);
            default -> throw new IllegalStateException("Unsupported storage mode: " + storageMode);
        };
    }

    public void deleteDocument(String objectKey) throws IOException {
        switch (storageMode) {
            case "filesystem" -> deleteFromFilesystem(objectKey);
            case "minio" -> deleteFromMinio(objectKey);
            default -> throw new IllegalStateException("Unsupported storage mode: " + storageMode);
        }
    }

    private String uploadToFilesystem(MultipartFile file, String objectKey) throws IOException {
        Files.createDirectories(filesystemRootPath);

        Path targetFile = resolveFilesystemPath(objectKey);
        if (!targetFile.startsWith(filesystemRootPath.normalize())) {
            throw new IllegalArgumentException("Invalid file path generated for upload.");
        }

        Path targetFolder = targetFile.getParent();
        if (targetFolder != null) {
            Files.createDirectories(targetFolder);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return objectKey;
    }

    private InputStream readFromFilesystem(String objectKey) throws IOException {
        Path targetFile = resolveFilesystemPath(objectKey);
        if (!Files.exists(targetFile)) {
            throw new IOException("Document file not found in filesystem storage.");
        }
        return Files.newInputStream(targetFile);
    }

    private void deleteFromFilesystem(String objectKey) throws IOException {
        Path targetFile = resolveFilesystemPath(objectKey);
        Files.deleteIfExists(targetFile);
    }

    private String uploadToMinio(MultipartFile file, String objectKey) throws IOException {
        if (s3Client == null) {
            throw new IllegalStateException("MinIO storage mode is enabled, but S3 client is not configured.");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("MinIO storage mode is enabled, but bucket name is missing.");
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return objectKey;
    }

    private InputStream readFromMinio(String objectKey) {
        if (s3Client == null) {
            throw new IllegalStateException("MinIO storage mode is enabled, but S3 client is not configured.");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("MinIO storage mode is enabled, but bucket name is missing.");
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        return s3Client.getObject(request);
    }

    private void deleteFromMinio(String objectKey) {
        if (s3Client == null) {
            throw new IllegalStateException("MinIO storage mode is enabled, but S3 client is not configured.");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("MinIO storage mode is enabled, but bucket name is missing.");
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
    }

    private Path resolveFilesystemPath(String objectKey) {
        Path targetFile = filesystemRootPath.resolve(objectKey.replace('/', java.io.File.separatorChar)).normalize();
        if (!targetFile.startsWith(filesystemRootPath.normalize())) {
            throw new IllegalArgumentException("Invalid file path generated for storage operation.");
        }
        return targetFile;
    }

    private String buildObjectKey(String assetType, String entityId, String originalFilename) {
        String normalizedAssetType = sanitizePathSegment(assetType);
        String normalizedEntityId = sanitizePathSegment(entityId);

        String safeOriginalFilename = originalFilename == null ? "file"
                : Paths.get(originalFilename).getFileName().toString();
        String normalizedFilename = safeOriginalFilename.replaceAll("\\s+", "_");
        String uniqueFileName = UUID.randomUUID() + "_" + normalizedFilename;

        return normalizedAssetType + "/" + normalizedEntityId + "/" + uniqueFileName;
    }

    private String sanitizePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}