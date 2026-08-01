package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import ph.gov.phlpost.inventory.misddashboard.model.Document;
import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import ph.gov.phlpost.inventory.misddashboard.repository.DocumentRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentStorageService storageService;
    private final DocumentRepository documentRepository;
    private final FleetVehicleRepository fleetVehicleRepository;
    private final RealEstatePropertyRepository realEstatePropertyRepository;
    private final long maxFileSizeBytes;
    private final int maxFileCount;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedCategories;

    public DocumentService(DocumentStorageService storageService, DocumentRepository documentRepository,
            FleetVehicleRepository fleetVehicleRepository,
            RealEstatePropertyRepository realEstatePropertyRepository,
            @Value("${document.upload.max-size-mb:15}") long maxFileSizeMb,
            @Value("${document.upload.max-files:25}") int maxFileCount,
            @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}") String allowedExtensionsConfig,
            @Value("#{'${document.upload.categories:Delivery Receipt,Official Receipt / Invoice,Warranty Certificate,Inspection Report,Acceptance Report,Appendix 71,Serial Number Label,Photographs,Equipment Specification Sheet,Repair or Service Report,OR/CR,Insurance Policy,Deed of Sale,Title,Tax Declaration,Property Photo,Service Report}'.split(',')}") List<String> allowedCategoriesConfig) {
        this.storageService = storageService;
        this.documentRepository = documentRepository;
        this.fleetVehicleRepository = fleetVehicleRepository;
        this.realEstatePropertyRepository = realEstatePropertyRepository;
        this.maxFileSizeBytes = Math.max(1L, maxFileSizeMb) * 1024L * 1024L;
        this.maxFileCount = Math.max(1, maxFileCount);
        this.allowedExtensions = Arrays.stream(allowedExtensionsConfig.split(","))
                .map(token -> token == null ? "" : token.trim())
                .filter(token -> !token.isBlank())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        this.allowedCategories = allowedCategoriesConfig.stream()
                .map(token -> token == null ? "" : token.trim())
                .filter(token -> !token.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasFiles(MultipartFile[] files) {
        return files != null && Arrays.stream(files).anyMatch(file -> file != null && !file.isEmpty());
    }

    public List<Document> findDocumentsByReference(String referenceType, String referenceId) {
        String normalizedReferenceType = normalizeReferenceType(referenceType);
        String normalizedReferenceId = referenceId == null ? "" : referenceId.trim();
        if (normalizedReferenceId.isEmpty()) {
            throw new IllegalArgumentException("Reference ID is required.");
        }

        return documentRepository.findByReferenceTypeAndReferenceId(normalizedReferenceType, normalizedReferenceId);
    }

    public Optional<Document> findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId);
    }

    public InputStream readDocumentContent(Document document) throws IOException {
        if (document == null || document.getMinioObjectKey() == null || document.getMinioObjectKey().isBlank()) {
            throw new IllegalArgumentException("Document storage key is missing.");
        }
        return storageService.readDocument(document.getMinioObjectKey());
    }

    public void deleteDocumentById(Integer documentId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));

        String objectKey = document.getMinioObjectKey();
        if (objectKey != null && !objectKey.isBlank()) {
            storageService.deleteDocument(objectKey);
        }

        documentRepository.delete(document);
    }

    public void uploadAndSaveDocuments(MultipartFile[] files,
            String referenceType,
            String referenceId,
            String documentCategory,
            String uploadedBy) throws IOException {

        if (!hasFiles(files)) {
            return;
        }

        String normalizedCategory = normalizeCategory(documentCategory);
        if (normalizedCategory == null) {
            throw new IllegalArgumentException("Document category is required for document upload.");
        }

        uploadAndSaveDocuments(files, referenceType, referenceId, repeatCategory(files, normalizedCategory),
                uploadedBy);
    }

    public void uploadAndSaveDocuments(MultipartFile[] files,
            String referenceType,
            String referenceId,
            String[] documentCategories,
            String uploadedBy) throws IOException {

        if (!hasFiles(files)) {
            return;
        }

        validateFiles(files);
        validateCategoryCount(files, documentCategories);

        for (int index = 0; index < files.length; index++) {
            MultipartFile file = files[index];
            if (file != null && !file.isEmpty()) {
                String category = documentCategories != null && index < documentCategories.length
                        ? documentCategories[index]
                        : null;
                uploadAndSaveDocument(file, referenceType, referenceId, category, uploadedBy);
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

        String category = normalizeCategory(documentCategory);
        if (category == null) {
            throw new IllegalArgumentException("Document category is required for document upload.");
        }

        String uploadedByValue = uploadedBy == null || uploadedBy.isBlank()
                ? "SystemUser"
                : uploadedBy.trim();

        String storageEntityId = resolveStorageEntityId(normalizedReferenceType, normalizedReferenceId);
        String objectKey = storageService.uploadDocument(file, toStorageFolder(normalizedReferenceType),
                storageEntityId);

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

    private void validateCategoryCount(MultipartFile[] files, String[] documentCategories) {
        int fileCount = (int) Arrays.stream(files).filter(file -> file != null && !file.isEmpty()).count();
        if (fileCount > maxFileCount) {
            throw new IllegalArgumentException(
                    "A maximum of " + maxFileCount + " files can be uploaded at once.");
        }

        int categoryCount = documentCategories == null ? 0
                : (int) Arrays.stream(documentCategories)
                        .filter(category -> category != null && !category.isBlank())
                        .count();

        if (fileCount != categoryCount) {
            throw new IllegalArgumentException(
                    "Each uploaded file must have a document category selected.");
        }
    }

    private String[] repeatCategory(MultipartFile[] files, String category) {
        if (!hasFiles(files)) {
            return new String[0];
        }

        int fileCount = (int) Arrays.stream(files)
                .filter(file -> file != null && !file.isEmpty())
                .count();

        String[] repeated = new String[fileCount];
        Arrays.fill(repeated, category);
        return repeated;
    }

    private String normalizeCategory(String documentCategory) {
        if (documentCategory == null || documentCategory.isBlank()) {
            return null;
        }

        String normalizedCategory = documentCategory.trim();
        if (!allowedCategories.contains(normalizedCategory)) {
            throw new IllegalArgumentException(
                    "Document category '" + normalizedCategory + "' is not allowed.");
        }
        return normalizedCategory;
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

    private String resolveStorageEntityId(String normalizedReferenceType, String normalizedReferenceId) {
        return switch (normalizedReferenceType) {
            case "VEHICLE" -> resolveVehicleStorageId(normalizedReferenceId);
            case "PROPERTY" -> resolvePropertyStorageId(normalizedReferenceId);
            default -> normalizedReferenceId;
        };
    }

    private String resolveVehicleStorageId(String normalizedReferenceId) {
        Integer vehicleId = parseInteger(normalizedReferenceId);
        if (vehicleId == null) {
            return normalizedReferenceId;
        }

        return fleetVehicleRepository.findById(vehicleId)
                .map(FleetVehicle::getPlateNumber)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .orElse(normalizedReferenceId);
    }

    private String resolvePropertyStorageId(String normalizedReferenceId) {
        Integer propertyId = parseInteger(normalizedReferenceId);
        if (propertyId == null) {
            return normalizedReferenceId;
        }

        return realEstatePropertyRepository.findById(propertyId)
                .map(this::resolvePropertyIdentifier)
                .orElse(normalizedReferenceId);
    }

    private String resolvePropertyIdentifier(RealEstateProperty property) {
        String titleNumber = property.getTitleNumber();
        if (titleNumber != null && !titleNumber.isBlank()) {
            return titleNumber.trim();
        }

        String taxDeclarationNumber = property.getTaxDeclarationNumber();
        if (taxDeclarationNumber != null && !taxDeclarationNumber.isBlank()) {
            return taxDeclarationNumber.trim();
        }

        Integer propertyId = property.getPropertyID();
        return propertyId == null ? "property" : "property-" + propertyId;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}