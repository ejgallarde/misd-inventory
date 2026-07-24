package ph.gov.phlpost.inventory.misddashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;

import java.io.IOException;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            @RequestParam("refType") String refType,
            @RequestParam("refId") String refId,
            @RequestParam("category") String category,
            RedirectAttributes redirectAttributes) {
        try {
            documentService.uploadAndSaveDocument(file, refType, refId, category, "SystemUser");

            redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }
}