package ph.gov.phlpost.inventory.misddashboard.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ph.gov.phlpost.inventory.misddashboard.model.Document;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<?> listDocuments(@RequestParam("refType") String refType,
            @RequestParam("refId") String refId) {
        try {
            List<DocumentSummary> documents = documentService.findDocumentsByReference(refType, refId).stream()
                    .map(DocumentSummary::from)
                    .toList();
            return ResponseEntity.ok(documents);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addDocuments(@RequestParam("refType") String refType,
            @RequestParam("refId") String refId,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "documentCategories", required = false) String[] documentCategories,
            Authentication authentication) {
        try {
            String uploadedBy = authentication != null ? authentication.getName() : "SystemUser";
            documentService.uploadAndSaveDocuments(documentFiles, refType, refId, documentCategories, uploadedBy);
            return ResponseEntity.ok(Map.of("message", "Document(s) uploaded successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected upload failure: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteDocument(@PathVariable("id") Integer documentId) {
        try {
            documentService.deleteDocumentById(documentId);
            return ResponseEntity.ok(Map.of("message", "Document deleted."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Delete failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<InputStreamResource> viewDocument(@PathVariable("id") Integer documentId) {
        return streamDocument(documentId, false);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(@PathVariable("id") Integer documentId) {
        return streamDocument(documentId, true);
    }

    private ResponseEntity<InputStreamResource> streamDocument(Integer documentId, boolean asAttachment) {
        try {
            Document document = documentService.findDocumentById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found."));

            InputStream contentStream = documentService.readDocumentContent(document);
            InputStreamResource resource = new InputStreamResource(contentStream);

            String filename = (document.getFileName() == null || document.getFileName().isBlank())
                    ? "document"
                    : document.getFileName();
            String contentType = (document.getContentType() == null || document.getContentType().isBlank())
                    ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                    : document.getContentType();

            ContentDisposition disposition = asAttachment
                    ? ContentDisposition.attachment().filename(filename).build()
                    : ContentDisposition.inline().filename(filename).build();

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());

            if (document.getFileSize() != null && document.getFileSize() >= 0) {
                builder.contentLength(document.getFileSize());
            }

            return builder.body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private record DocumentSummary(Integer documentId, String documentCategory, String fileName,
            String contentType, Long fileSize, String uploadedBy, LocalDateTime uploadDate) {
        static DocumentSummary from(Document document) {
            return new DocumentSummary(
                    document.getDocumentId(),
                    document.getDocumentCategory(),
                    document.getFileName(),
                    document.getContentType(),
                    document.getFileSize(),
                    document.getUploadedBy(),
                    document.getUploadDate());
        }
    }
}