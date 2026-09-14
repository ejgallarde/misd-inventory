package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import software.amazon.awssdk.services.s3.S3Client;

class DocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private DocumentStorageService filesystemService() {
        @SuppressWarnings("unchecked")
        ObjectProvider<S3Client> s3ClientProvider = mock(ObjectProvider.class);
        return new DocumentStorageService(s3ClientProvider, "", "filesystem", tempDir.toString());
    }

    @Test
    void uploadingAnEmptyFileIsRejected() {
        DocumentStorageService service = filesystemService();
        MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.uploadDocument(empty, "VEHICLE", "17"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadThenReadThenDeleteRoundTripsOnTheFilesystem() throws IOException {
        DocumentStorageService service = filesystemService();
        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf",
                "content".getBytes());

        String objectKey = service.uploadDocument(file, "VEHICLE", "17");

        assertThat(objectKey).startsWith("VEHICLE/17/").endsWith("_receipt.pdf");
        try (InputStream in = service.readDocument(objectKey)) {
            assertThat(new String(in.readAllBytes())).isEqualTo("content");
        }

        service.deleteDocument(objectKey);
        assertThatThrownBy(() -> service.readDocument(objectKey)).isInstanceOf(IOException.class);
    }

    @Test
    void assetTypeAndEntityIdAreSanitizedAgainstPathTraversal() throws IOException {
        DocumentStorageService service = filesystemService();
        MockMultipartFile file = new MockMultipartFile("file", "leak.pdf", "application/pdf", "x".getBytes());

        String objectKey = service.uploadDocument(file, "../../etc", "../../../secrets");

        // sanitizePathSegment only strips path separators (and other unsafe
        // characters), not dots, so "../../etc" collapses to the single folder
        // name ".._.._etc" rather than the two directory-traversal segments
        // "..", "..", "etc" — it can no longer act as a path separator.
        assertThat(objectKey).doesNotContain("/../").doesNotStartWith("../");
        String[] segments = objectKey.split("/");
        assertThat(segments).hasSize(3);
        assertThat(segments[0]).isEqualTo(".._.._etc");
        assertThat(segments[1]).isEqualTo(".._.._.._secrets");
        // The written file must stay inside the configured storage root, directly
        // under one sanitized subfolder rather than escaping it.
        assertThat(Files.list(tempDir).count()).isEqualTo(1);
    }

    @Test
    void originalFilenameIsReducedToItsBaseNameEvenIfItCarriesAPath() throws IOException {
        DocumentStorageService service = filesystemService();
        MockMultipartFile file = new MockMultipartFile("file", "../../evil/../passwd", "application/pdf",
                "x".getBytes());

        String objectKey = service.uploadDocument(file, "VEHICLE", "17");

        assertThat(objectKey).doesNotContain("..");
        assertThat(objectKey).endsWith("_passwd");
    }

    @Test
    void minioModeWithoutAConfiguredClientFailsClearly() {
        @SuppressWarnings("unchecked")
        ObjectProvider<S3Client> s3ClientProvider = mock(ObjectProvider.class);
        when(s3ClientProvider.getIfAvailable()).thenReturn(null);
        DocumentStorageService service = new DocumentStorageService(s3ClientProvider, "misd-asset-registry", "minio",
                tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(file, "VEHICLE", "17"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3 client is not configured");
    }
}
