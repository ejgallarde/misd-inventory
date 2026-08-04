package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import ph.gov.phlpost.inventory.misddashboard.model.Document;
import ph.gov.phlpost.inventory.misddashboard.repository.DocumentRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

        @Mock
        private DocumentStorageService storageService;

        @Mock
        private DocumentRepository documentRepository;

        @Mock
        private FleetVehicleRepository fleetVehicleRepository;

        @Mock
        private RealEstatePropertyRepository realEstatePropertyRepository;

        private DocumentService documentService;

        @BeforeEach
        void setUp() {
                documentService = new DocumentService(
                                storageService,
                                documentRepository,
                                fleetVehicleRepository,
                                realEstatePropertyRepository,
                                15,
                                3,
                                "pdf,jpg,jpeg,png,doc,docx,xls,xlsx",
                                List.of(
                                                "Official Receipt / Invoice",
                                                "Inspection Report",
                                                "Acceptance Report",
                                                "Serial Number Label",
                                                "Photographs",
                                                "Equipment Specification Sheet",
                                                "Repair or Service Report",
                                                "Service Report"),
                                List.of(
                                                "Delivery Receipt",
                                                "Original Receipt (OR)",
                                                "Certificate of Registration (CR)",
                                                "PMS Report",
                                                "Car Insurance Policy",
                                                "Stencil",
                                                "TPL",
                                                "Driver's License",
                                                "Warranty Certificate"),
                                List.of(
                                                "Title",
                                                "Tax Declaration",
                                                "Property Photo",
                                                "Deed of Sale",
                                                "Appendix 71"));
        }

        @Test
        void hasFilesHandlesNullAndEmptyInputs() {
                assertThat(documentService.hasFiles(null)).isFalse();
                assertThat(documentService.hasFiles(new MockMultipartFile[0])).isFalse();
                assertThat(documentService.hasFiles(new MockMultipartFile[] {
                                new MockMultipartFile("documentFiles", "", "application/octet-stream", new byte[0])
                })).isFalse();
        }

        @Test
        void uploadRejectsWhenFileCountExceedsConfiguredMaximum() {
                MockMultipartFile[] files = new MockMultipartFile[] {
                                file("a.pdf"), file("b.pdf"), file("c.pdf"), file("d.pdf")
                };

                assertThatThrownBy(() -> documentService.uploadAndSaveDocuments(
                                files,
                                "IT_EQUIPMENT",
                                "TAG-001",
                                new String[] { "Delivery Receipt", "Delivery Receipt", "Delivery Receipt",
                                                "Delivery Receipt" },
                                "tester"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("maximum of 3 files");
        }

        @Test
        void uploadRejectsInvalidExtension() {
                MockMultipartFile[] files = new MockMultipartFile[] {
                                new MockMultipartFile("documentFiles", "malware.exe", "application/octet-stream",
                                                "x".getBytes())
                };

                assertThatThrownBy(() -> documentService.uploadAndSaveDocuments(
                                files,
                                "IT_EQUIPMENT",
                                "TAG-001",
                                new String[] { "Delivery Receipt" },
                                "tester"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("not an allowed type");
        }

        @Test
        void uploadRejectsMismatchedCategoryCount() {
                MockMultipartFile[] files = new MockMultipartFile[] {
                                file("a.pdf"), file("b.pdf")
                };

                assertThatThrownBy(() -> documentService.uploadAndSaveDocuments(
                                files,
                                "IT_EQUIPMENT",
                                "TAG-001",
                                new String[] { "Delivery Receipt" },
                                "tester"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("must have a document category");
        }

        @Test
        void uploadRejectsDisallowedCategory() {
                MockMultipartFile[] files = new MockMultipartFile[] {
                                file("a.pdf")
                };

                assertThatThrownBy(() -> documentService.uploadAndSaveDocuments(
                                files,
                                "IT_EQUIPMENT",
                                "TAG-001",
                                new String[] { "Not Allowed" },
                                "tester"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("is not allowed");
        }

        @Test
        void uploadSavesDocumentRecordsForValidFiles() throws IOException {
                MockMultipartFile[] files = new MockMultipartFile[] {
                                file("a.pdf"), file("b.pdf")
                };

                when(storageService.uploadDocument(any(), eq("it-equipment"), eq("TAG-001")))
                                .thenReturn("it-equipment/TAG-001/first")
                                .thenReturn("it-equipment/TAG-001/second");

                documentService.uploadAndSaveDocuments(
                                files,
                                "IT_EQUIPMENT",
                                "TAG-001",
                                new String[] { "Delivery Receipt", "Inspection Report" },
                                "tester");

                ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
                verify(documentRepository, times(2)).save(docCaptor.capture());
                verify(storageService, times(2)).uploadDocument(any(), eq("it-equipment"), eq("TAG-001"));

                List<Document> savedDocs = docCaptor.getAllValues();
                assertThat(savedDocs).hasSize(2);
                assertThat(savedDocs.get(0).getDocumentCategory()).isEqualTo("Delivery Receipt");
                assertThat(savedDocs.get(1).getDocumentCategory()).isEqualTo("Inspection Report");
                assertThat(savedDocs.get(0).getUploadedBy()).isEqualTo("tester");
        }

        @Test
        void uploadRejectsUnsupportedReferenceType() {
                assertThatThrownBy(() -> documentService.uploadAndSaveDocument(
                                file("a.pdf"),
                                "UNKNOWN",
                                "REF-1",
                                "Delivery Receipt",
                                "tester"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Unsupported reference type");
        }

        private MockMultipartFile file(String filename) {
                return new MockMultipartFile(
                                "documentFiles",
                                filename,
                                "application/pdf",
                                "sample".getBytes());
        }
}
