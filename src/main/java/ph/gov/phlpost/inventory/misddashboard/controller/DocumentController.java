package ph.gov.phlpost.inventory.misddashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ph.gov.phlpost.inventory.misddashboard.model.AssetDocument;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetDocumentRepository;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentStorageService;

import java.io.IOException;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentStorageService storageService;
    private final AssetDocumentRepository documentRepository;

    public DocumentController(DocumentStorageService storageService, AssetDocumentRepository documentRepository) {
        this.storageService = storageService;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            @RequestParam("refType") String refType,
            @RequestParam("refId") String refId,
            @RequestParam("category") String category,
            RedirectAttributes redirectAttributes) {
        try {
            // 1. Upload to MinIO
            String objectKey = storageService.uploadDocument(file, refType.toLowerCase(), refId);

            // 2. Log metadata to Database
            AssetDocument doc = new AssetDocument();
            doc.setReferenceType(refType);
            doc.setReferenceId(refId);
            doc.setDocumentCategory(category);
            doc.setFileName(file.getOriginalFilename());
            doc.setMinioObjectKey(objectKey);
            doc.setContentType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setUploadedBy("SystemUser"); // Replace with SecurityContextHolder logic later

            documentRepository.save(doc);

            redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload failed: " + e.getMessage());
        }
        return "redirect:/dashboard"; // Redirect back to where the user was
    }
}