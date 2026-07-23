package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public DocumentStorageService(S3Client s3Client, @Value("${minio.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Uploads a file to a specific category directory in MinIO.
     * @param file The uploaded Multipart file from the controller
     * @param assetType e.g., "vehicles", "it-equipment", "properties"
     * @param entityId The unique identifier of the asset (e.g., PlateNumber or AssetTag)
     * @return The distinct storage path/key generated for the file
     */
    public String uploadDocument(MultipartFile file, String assetType, String entityId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        // Structure paths cleanly: assetType/entityId/UUID_filename.ext
        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + (originalFilename != null ? originalFilename.replaceAll("\\s+", "_") : "file");
        String objectKey = String.format("%s/%s/%s", assetType, entityId, uniqueFileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, 
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return objectKey; // Save this string value directly into your database reference table
    }
}