package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ph.gov.phlpost.inventory.misddashboard.model.Document;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

        @Mock
        private DocumentService documentService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(documentService)).build();
        }

        @Test
        void addDocumentsReturnsOkOnSuccess() throws Exception {
                doNothing().when(documentService).uploadAndSaveDocuments(
                                any(org.springframework.web.multipart.MultipartFile[].class),
                                eq("IT_EQUIPMENT"),
                                eq("TAG-1"),
                                any(String[].class),
                                eq("SystemUser"));

                MockMultipartFile file = new MockMultipartFile(
                                "documentFiles",
                                "receipt.pdf",
                                "application/pdf",
                                "sample".getBytes());

                mockMvc.perform(multipart("/documents/add")
                                .file(file)
                                .param("refType", "IT_EQUIPMENT")
                                .param("refId", "TAG-1")
                                .param("documentCategories", "Delivery Receipt"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("uploaded successfully")));
        }

        @Test
        void addDocumentsReturnsBadRequestForValidationErrors() throws Exception {
                doThrow(new IllegalArgumentException("bad input")).when(documentService)
                                .uploadAndSaveDocuments(
                                                any(org.springframework.web.multipart.MultipartFile[].class),
                                                any(String.class),
                                                any(String.class),
                                                any(String[].class),
                                                any(String.class));

                MockMultipartFile file = new MockMultipartFile(
                                "documentFiles",
                                "receipt.pdf",
                                "application/pdf",
                                "sample".getBytes());

                mockMvc.perform(multipart("/documents/add")
                                .file(file)
                                .param("refType", "IT_EQUIPMENT")
                                .param("refId", "TAG-1")
                                .param("documentCategories", "Delivery Receipt"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("bad input")));
        }

        @Test
        void addDocumentsReturnsServerErrorForIoExceptions() throws Exception {
                doThrow(new IOException("disk issue")).when(documentService)
                                .uploadAndSaveDocuments(
                                                any(org.springframework.web.multipart.MultipartFile[].class),
                                                any(String.class),
                                                any(String.class),
                                                any(String[].class),
                                                any(String.class));

                MockMultipartFile file = new MockMultipartFile(
                                "documentFiles",
                                "receipt.pdf",
                                "application/pdf",
                                "sample".getBytes());

                mockMvc.perform(multipart("/documents/add")
                                .file(file)
                                .param("refType", "IT_EQUIPMENT")
                                .param("refId", "TAG-1")
                                .param("documentCategories", "Delivery Receipt"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(containsString("Upload failed")));
        }

        @Test
        void listDocumentsReturnsBadRequestOnInvalidReference() throws Exception {
                when(documentService.findDocumentsByReference("IT_EQUIPMENT", ""))
                                .thenThrow(new IllegalArgumentException("Reference ID is required."));

                mockMvc.perform(get("/documents/list")
                                .param("refType", "IT_EQUIPMENT")
                                .param("refId", ""))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("Reference ID is required")));
        }

        @Test
        void listDocumentsReturnsOkAndPayload() throws Exception {
                Document doc = new Document();
                doc.setDocumentId(101);
                doc.setDocumentCategory("Delivery Receipt");
                doc.setFileName("receipt.pdf");
                doc.setContentType("application/pdf");
                doc.setFileSize(128L);
                doc.setUploadedBy("tester");

                when(documentService.findDocumentsByReference("IT_EQUIPMENT", "TAG-1")).thenReturn(List.of(doc));

                mockMvc.perform(get("/documents/list")
                                .param("refType", "IT_EQUIPMENT")
                                .param("refId", "TAG-1"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("receipt.pdf")));
        }

        @Test
        void deleteDocumentReturnsNotFoundWhenMissing() throws Exception {
                doThrow(new IllegalArgumentException("Document not found.")).when(documentService)
                                .deleteDocumentById(404);

                mockMvc.perform(delete("/documents/404"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string(containsString("Document not found")));
        }
}
