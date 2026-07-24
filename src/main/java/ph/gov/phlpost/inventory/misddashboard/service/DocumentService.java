package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import ph.gov.phlpost.inventory.misddashboard.model.Document;
import ph.gov.phlpost.inventory.misddashboard.repository.DocumentRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentStorageService storageService;
    private final DocumentRepository documentRepository;
    private final long maxFileSizeBytes;
    private final Set<String> allowedExtensions;

    public DocumentService(DocumentStorageService storageService, DocumentRepository documentRepository,
            @Value("${document.upload.max-size-mb:10}") long maxFileSizeMb,
            @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}") String allowedExtensionsConfig) {
        this.storageService = storageService;
        this.documentRepository = documentRepository;
        this.maxFileSizeBytes = Math.max(1L, maxFileSizeMb) * 1024L * 1024L;
        this.allowedExtensions = Arrays.stream(allowedExtensionsConfig.split(","))
                .map(String::trim)
                .filter(extension -> !extension.isBlank())
                .map(extension -> extension.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasFiles(MultipartFile[] files) {
        return files != null && Arrays.stream(files).anyMatch(file -> file != null && !file.isEmpty());
    }

    public void uploadAndSaveDocuments(MultipartFile[] files,
            String referenceType,
            String referenceId,
            String documentCategory,
            String uploadedBy) throws IOException {

        if (!hasFiles(files)) {
            return;
        }

        validateFiles(files);

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                uploadAndSaveDocument(file, referenceType, referenceId, documentCategory, uploadedBy);
            }
        }
    }

    public void uploadAndSaveDocument(MultipartFile file,
            String referenceType,
            String referenceId,
            String documentCategory,
            String uploadedBy) throws IOException {

        if (file == null || file.isEmpty()) {
            return;
        }

        validateFile(file);

        String normalizedReferenceType = normalizeReferenceType(referenceType);
        String normalizedReferenceId = referenceId == null ? "" : referenceId.trim();
        if (normalizedReferenceId.isEmpty()) {
            throw new IllegalArgumentException("Reference ID is required for document upload.");
        }

        String category = documentCategory == null || documentCategory.isBlank()
                ? "General"
                : documentCategory.trim();

        String uploadedByValue = uploadedBy == null || uploadedBy.isBlank()
                ? "SystemUser"
                : uploadedBy.trim();

        String objectKey = storageService.uploadDocument(file, toStorageFolder(normalizedReferenceType),
                normalizedReferenceId);

        Document document = new Document();
        document.setReferenceType(normalizedReferenceType);
        document.setReferenceId(normalizedReferenceId);
        document.setDocumentCategory(category);
        document.setFileName(resolveFilename(file));
        document.setMinioObjectKey(objectKey);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setUploadedBy(uploadedByValue);

        documentRepository.save(document);
    }

    private void validateFiles(MultipartFile[] files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                validateFile(file);
            }
        }
    }

    private void validateFile(MultipartFile file) {
        String filename = resolveFilename(file);
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "File '" + filename + "' exceeds the maximum allowed size of "
                            + formatMaxSizeMb() + " MB.");
        }

        String extension = extractExtension(filename);
        if (extension.isBlank() || !allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    "File '" + filename + "' is not an allowed type. Allowed types: "
                            + String.join(", ", allowedExtensions));
        }
    }

    private String resolveFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        return (originalFilename == null || originalFilename.isBlank()) ? "uploaded-file" : originalFilename;
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private String formatMaxSizeMb() {
        long maxSizeMb = Math.max(1L, maxFileSizeBytes / (1024L * 1024L));
        return String.valueOf(maxSizeMb);
    }

    private String normalizeReferenceType(String referenceType) {
        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("Reference type is required for document upload.");
        }
        return referenceType.trim().toUpperCase(Locale.ROOT);
    }

    private String toStorageFolder(String referenceType) {
        return switch (referenceType) {
            case "VEHICLE" -> "vehicles";
            case "PROPERTY" -> "properties";
            case "IT_EQUIPMENT" -> "it-equipment";
            default -> throw new IllegalArgumentException("Unsupported reference type: " + referenceType);
        };
    }
}