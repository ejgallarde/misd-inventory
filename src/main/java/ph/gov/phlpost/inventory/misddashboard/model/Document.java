package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "AssetDocument")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DocumentId")
    private Integer documentId;

    @Column(name = "ReferenceType", nullable = false)
    private String referenceType;

    @Column(name = "ReferenceId", nullable = false)
    private String referenceId;

    @Column(name = "DocumentCategory", nullable = false)
    private String documentCategory;

    @Column(name = "FileName", nullable = false)
    private String fileName;

    @Column(name = "MinioObjectKey", nullable = false)
    private String minioObjectKey;

    @Column(name = "ContentType")
    private String contentType;

    @Column(name = "FileSize")
    private Long fileSize;

    @Column(name = "UploadedBy")
    private String uploadedBy;

    @Column(name = "UploadDate", updatable = false)
    private LocalDateTime uploadDate;

    public Integer getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Integer documentId) {
        this.documentId = documentId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDocumentCategory() {
        return documentCategory;
    }

    public void setDocumentCategory(String documentCategory) {
        this.documentCategory = documentCategory;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMinioObjectKey() {
        return minioObjectKey;
    }

    public void setMinioObjectKey(String minioObjectKey) {
        this.minioObjectKey = minioObjectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDateTime.now();
    }
}